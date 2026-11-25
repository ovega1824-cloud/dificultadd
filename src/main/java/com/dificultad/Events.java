package com.dificultad;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Los 10 eventos aleatorios. No muestran nada: solo suben la dificultad. */
public final class Events {
    private static final Random RNG = new Random();

    private Events() {
    }

    public static void runRandom(MinecraftServer server) {
        List<EventType> pool = new ArrayList<>();
        for (Config.Entry e : Manager.cfg.events) {
            EventType t = EventType.byId(e.id);
            if (e.enabled && t != null) pool.add(t);
        }
        if (pool.isEmpty()) return;
        run(server, pool.get(RNG.nextInt(pool.size())));
    }

    public static void run(MinecraftServer server, EventType type) {
        List<ServerPlayer> all = Manager.players(server);
        if (all.isEmpty()) return;
        ServerPlayer victim = all.get(RNG.nextInt(all.size()));
        ServerLevel level = victim.serverLevel();

        switch (type) {
            case HORDE -> {
                for (int i = 0; i < 10; i++) {
                    Zombie z = Manager.spawnNear(level, EntityType.ZOMBIE, victim.position(), 8, 16, false);
                    if (z != null) z.setTarget(victim);
                }
            }
            case CREEPERS -> {
                for (int i = 0; i < 5; i++) {
                    Creeper c = Manager.spawnNear(level, EntityType.CREEPER, victim.position(), 8, 14, false);
                    if (c != null) c.setTarget(victim);
                }
            }
            case LIGHTNING_STORM -> {
                server.overworld().setWeatherParameters(0, 6000, true, true);
                for (int i = 0; i < 6; i++) Manager.strike(level, victim.position(), 20);
            }
            case PHANTOMS -> {
                for (int i = 0; i < 5; i++) {
                    Phantom ph = Manager.spawnNear(level, EntityType.PHANTOM, victim.position(), 6, 16, true);
                    if (ph != null) {
                        ph.setTarget(victim);
                        ph.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                                MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
                    }
                }
            }
            case HUNGER -> {
                for (ServerPlayer p : all) {
                    p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 1200, 2, false, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0, false, false, false));
                }
            }
            case VEXES -> {
                for (int i = 0; i < 6; i++) {
                    Vex v = Manager.spawnNear(level, EntityType.VEX, victim.position(), 4, 10, true);
                    if (v != null) {
                        v.setLimitedLife(1200);
                        v.setTarget(victim);
                    }
                }
            }
            case WITHERS -> {
                for (int i = 0; i < 3; i++) {
                    var w = Manager.spawnNear(level, EntityType.WITHER_SKELETON, victim.position(), 8, 14, false);
                    if (w != null) w.setTarget(victim);
                }
            }
            case EMPOWER -> {
                for (ServerPlayer p : all) {
                    for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64.0),
                            mob -> mob instanceof Enemy)) {
                        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 1, false, false, false));
                        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800, 1, false, false, false));
                    }
                }
            }
            case ARMOR_WEAR -> {
                for (ServerPlayer p : all) {
                    for (ItemStack s : p.getInventory().armor) {
                        if (!s.isEmpty() && s.isDamageableItem()) {
                            int max = s.getMaxDamage();
                            s.setDamageValue(Math.min(max - 1, s.getDamageValue() + max / 4));
                        }
                    }
                }
            }
            case NIGHT -> {
                ServerLevel ow = server.overworld();
                long t = ow.getDayTime();
                long next = t - (t % 24000L) + 18000L;
                if (next < t) next += 24000L;
                ow.setDayTime(next);
            }
        }
    }
}
