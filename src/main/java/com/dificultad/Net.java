package com.dificultad;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Paquetes: servidor abre el menú; cliente guarda ajustes y manda acciones. */
public class Net {

    public record OpenMenu(String json) implements CustomPacketPayload {
        public static final Type<OpenMenu> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(DificultadMod.MODID, "open_menu"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenu> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(262144), OpenMenu::json, OpenMenu::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Save(String json) implements CustomPacketPayload {
        public static final Type<Save> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(DificultadMod.MODID, "save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Save> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(30000), Save::json, Save::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Action(String id) implements CustomPacketPayload {
        public static final Type<Action> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(DificultadMod.MODID, "action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(32), Action::id, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenMenu.TYPE, OpenMenu.CODEC);
        PayloadTypeRegistry.playC2S().register(Save.TYPE, Save.CODEC);
        PayloadTypeRegistry.playC2S().register(Action.TYPE, Action.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Save.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            if (!Manager.allowed(p)) return;
            Manager.onSave(ctx.server(), Config.fromJson(payload.json()));
        });
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            if (!Manager.allowed(p)) return;
            Manager.onAction(ctx.server(), p, payload.id());
        });
    }

    public static void openMenu(ServerPlayer p) {
        ServerPlayNetworking.send(p, new OpenMenu(Manager.cfg.toJson()));
    }
}
