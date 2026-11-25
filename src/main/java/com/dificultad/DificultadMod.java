package com.dificultad;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class DificultadMod implements ModInitializer {
    public static final String MODID = "dificultad";

    public static final Item STICK = Registry.register(BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MODID, "palo_dificultad"),
            new StickItem(new Item.Properties().stacksTo(1)));

    @Override
    public void onInitialize() {
        Net.register();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(STICK));

        ServerLifecycleEvents.SERVER_STARTED.register(Manager::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> Manager.save());
        ServerTickEvents.END_SERVER_TICK.register(Manager::tick);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> Manager.onLoad(entity, level));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> Manager.onDeath(entity));
    }
}
