package xyz.wagyourtail.mchotswap.forge;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import xyz.wagyourtail.mchotswap.server.ChangeListener;

import java.io.IOException;

@Mod("remote-hotswap")
public class McHotswapForge {

    private static ChangeListener listener;

    public McHotswapForge() {
    }

    @SubscribeEvent
    public void onClientInit(FMLClientSetupEvent event) {
        System.out.println("[Remote Hotswap] Bootstrapping mchotswap Forge");
        try {
            listener = new ChangeListener();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ForgeModProvider.setServer(Minecraft.getInstance());
    }

    @SubscribeEvent
    public void onInitServer(FMLDedicatedServerSetupEvent event) {
        System.out.println("[Remote Hotswap] Bootstrapping mchotswap Forge");
        try {
            listener = new ChangeListener();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
    }

    public void onServerStart(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server instanceof DedicatedServer)
            xyz.wagyourtail.mchotswap.forge.ForgeModProvider.setServer(server);
    }

}
