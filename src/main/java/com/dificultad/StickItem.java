package com.dificultad;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Palo de dificultad: clic derecho abre el menú. */
public class StickItem extends Item {
    public StickItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && Manager.allowed(sp)) {
            Net.openMenu(sp);
        }
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }
}
