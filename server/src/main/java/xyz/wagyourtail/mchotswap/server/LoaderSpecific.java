package xyz.wagyourtail.mchotswap.server;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class LoaderSpecific {
    public static final LoaderSpecific INSTANCE;
    static {
        Iterator<LoaderSpecific> resolverIterator = ServiceLoader.load(LoaderSpecific.class).iterator();
        LoaderSpecific instance = null;
        while (resolverIterator.hasNext()) {
            instance = resolverIterator.next();
            if (instance.willWork()) {
                break;
            }
        }
        assert instance != null;
        if (instance.willWork()) {
            INSTANCE = instance;
        } else {
            INSTANCE = null;
        }
    }

    public abstract Path getMod(String modId);

    public abstract void restart();

    protected abstract boolean willWork();
}
