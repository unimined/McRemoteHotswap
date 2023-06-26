package xyz.wagyourtail.mchotswap.forge;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.fml.ModList;
import xyz.wagyourtail.mchotswap.server.LoaderSpecific;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public class ForgeModProvider extends LoaderSpecific {
    private static Executor server;

    public static void setServer(Executor server) {
        ForgeModProvider.server = server;
    }

    @Override
    public Path getMod(String modId) {
        return ModList.get().getModFileById(modId).getFile().getFilePath();
    }

    public static <T extends Throwable> void sneakyThrows(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public void restart() {
        server.execute(() -> {
            try {
                ((AutoCloseable)server).close();
            } catch (Throwable e) {
                sneakyThrows(e);
            }
        });
    }

    @Override
    protected boolean willWork() {
        try {
            Class.forName("net.minecraftforge.fml.ModList");
            Class.forName("net.minecraft.server.dedicated.DedicatedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
