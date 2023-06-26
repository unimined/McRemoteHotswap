package xyz.wagyourtail.mchotswap.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import xyz.wagyourtail.mchotswap.server.ChangeListener;

import java.io.IOException;

public class McHotswapFabric implements ModInitializer {

    private static ChangeListener listener;

    @Override
    public void onInitialize() {
        System.out.println("[Remote Hotswap] Bootstrapping mchotswap Fabric");
        try {
            listener = new ChangeListener();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
