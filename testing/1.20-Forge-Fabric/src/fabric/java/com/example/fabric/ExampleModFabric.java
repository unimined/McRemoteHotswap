package com.example.fabric;

import com.example.ExampleMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class ExampleModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ExampleMod.LOGGER.info("Hello from Fabric!");
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    public void onServerTick(MinecraftServer server) {
        ExampleMod.LOGGER.info(incompatible());
    }


    public static String incompatible() {
        return "Server tick4!";
    }
}
