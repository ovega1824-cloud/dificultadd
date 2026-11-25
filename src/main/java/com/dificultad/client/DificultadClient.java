package com.dificultad.client;

import com.dificultad.Config;
import com.dificultad.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class DificultadClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Net.OpenMenu.TYPE, (payload, ctx) ->
                Minecraft.getInstance().setScreen(new MainScreen(Config.fromJson(payload.json()))));
    }
}
