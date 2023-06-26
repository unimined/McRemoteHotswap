package xyz.wagyourtail.devserver.applier;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.devserver.ChangeListener;
import xyz.wagyourtail.devserver.LoaderSpecific;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InstrumentationChangeProvider implements ClassFileTransformer {
    private final Instrumentation instrumentation;
    private final boolean canRetransform;
    private final OverrideLoader overrideLoader = new OverrideLoader();
    private final Map<String, Path> overrides = new HashMap<>();

    public InstrumentationChangeProvider() {
        Instrumentation instrumentation;
        boolean canRetransform;
        try {
            ByteBuddyAgent.install();
            instrumentation = ByteBuddyAgent.getInstrumentation();
            instrumentation.addTransformer(this, true);
            canRetransform = instrumentation.isRetransformClassesSupported();
        } catch (Throwable e) {
            canRetransform = false;
            instrumentation = null;
        }
        this.instrumentation = instrumentation;
        this.canRetransform = canRetransform;


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[DevServer] Attempting to apply changes for mods before shutdown");
            try {
                // compile mods list to replace
                Map<Path, Path> replace = new HashMap<>();
                for (Map.Entry<String, Path> entry : overrides.entrySet()) {
                    Path path = LoaderSpecific.INSTANCE.getMod(entry.getKey());
                    if (path != null) {
                        replace.put(path, entry.getValue());
                    } else {
                        System.err.println("[DevServer] Failed to find mod " + entry.getKey() + " to apply changes");
                    }
                }
                // TODO: do in new process to avoid file locks
                for (Map.Entry<Path, Path> entry : replace.entrySet()) {
                    System.out.println("[DevServer] writing to " + entry.getKey() + " at shutdown");
                    Files.delete(entry.getKey());
                    Files.move(entry.getValue(), entry.getKey(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }));
    }

    public Set<String> readZipContents(Path path) throws IOException {
        Set<String> contents = new HashSet<>();
        try (ZipInputStream stream = new ZipInputStream(Files.newInputStream(path))) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                contents.add(entry.getName());
            }
        }
        return contents;
    }


    public void applyChanges(String modid, Path newFile) throws MalformedURLException {
        overrides.put(modid, newFile);
        overrideLoader.addOverride(modid, newFile.toUri().toURL());
        System.out.println("[DevServer] Attempting to apply changes for " + modid + " at runtime");
        if (!applyChangesRuntime(newFile)) {
            System.out.println("[DevServer] Failed to apply changes for " + modid + " at runtime, restarting");
            try {
                restartToApply();
            } catch (Exception e) {
                System.err.println("[DevServer] Failed to restart server to apply changes");
                e.printStackTrace();
            }
        } else {
            System.out.println("[DevServer] Successfully applied changes for " + modid + " at runtime");
        }
    }

    public boolean applyChangesRuntime(Path newFile) {
        if (!canRetransform) return false;
        Set<Class<?>> retransform = new HashSet<>();
        // get all classes in jar
        Set<String> paths;
        try {
            paths = readZipContents(newFile);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        System.out.println("[DevServer] searching for classes to retransform");
        Class<?>[] all = instrumentation.getAllLoadedClasses();
        System.out.println("found " + all.length + " classes to search through");
        for (Class<?> clazz : all) {
            // check if in fs
            if (paths.contains(clazz.getName().replace('.', '/') + ".class")) {
                retransform.add(clazz);
            }
        }
        try {
            System.out.println("[DevServer] retransforming " + retransform.size() + " classes");
            instrumentation.retransformClasses(retransform.toArray(new Class[0]));
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public void restartToApply() {
        System.out.println("[DevServer] Restarting to apply changes");
        LoaderSpecific.INSTANCE.restart();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        URL url = overrideLoader.getResource(className.replace('.', '/') + ".class");
        if (url != null) {
            try {
                return ChangeListener.readAllBytes(url.openStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return classfileBuffer;
    }

    private static class OverrideLoader {
        private final Map<String, URLClassLoader> byModId = new HashMap<>();

        public void addOverride(String modid, URL uri) {
            byModId.put(modid, new URLClassLoader(new URL[]{uri}, null));
        }

        @Nullable
        public URL getResource(String name) {
            for (URLClassLoader loader : byModId.values()) {
                URL url = loader.getResource(name);
                if (url != null) return url;
            }
            return null;
        }
    }
}
