package xyz.wagyourtail.mchotswap.fabric;

import net.fabricmc.loader.api.FabricLoader;
import xyz.wagyourtail.mchotswap.server.LoaderSpecific;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public class FabricModProvider extends LoaderSpecific {
    @Override
    public Path getMod(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).orElseThrow(() -> new RuntimeException("Mod " + modId + " not found")).getOrigin().getPaths().get(0);
    }

    @Override
    protected boolean willWork() {
        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static <T extends Throwable> void sneakyThrows(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public void restart() {
        Object gameInstance = FabricLoader.getInstance().getGameInstance();
        if (gameInstance instanceof Executor executor) {
            executor.execute(() -> {
                try {
                    ((AutoCloseable) executor).close();
                } catch (Throwable e) {
                    sneakyThrows(e);
                }
            });
        } else {
            // 1.16- has a null dedicatedserver instance...
            // TODO: figure out how to restart the server gracefully
            System.exit(0);
        }
    }
}
