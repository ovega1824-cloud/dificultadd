package com.dificultad;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.RangedAttackMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.WitherBoss;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Lógica del servidor: cambios progresivos, comandos con temporizador, eventos y zona. Todo invisible. */
public final class Manager {
    public static Config cfg = new Config().fill();

    private static final Random RNG = new Random();
    private static final String TAG_DUP = "dm_dup";
    private static final String TAG_SLIME = "dm_slime";
    private static final String TAG_ARROW = "dm_arrow";
    private static final String TAG_FIRE = "dm_fire";
    private static final String TAG_CREEPER = "dm_creeper";
    private static final String TAG_ARMOR = "dm_armor";

    private static int tick = 0;
    private static int changeCountdown = 0;
    private static int timerCountdown = 0;
    private static int eventCountdown = 0;
    private static final List<Runnable> queue = new ArrayList<>();
    private static final Map<Integer, Integer> lastShot = new HashMap<>();
    private static final Map<Integer, Integer> lastHit = new HashMap<>();
    private static final List<double[]> deadSlimes = new ArrayList<>(); // x, y, z, tick

    private Manager() {
    }

    // ------------------------------------------------------------------ permisos

    public static boolean allowed(ServerPlayer p) {
        return p.hasPermissions(2) || p.server.isSingleplayer();
    }

    // ------------------------------------------------------------------ guardar / cargar

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("dificultad.json");
    }

    public static void onServerStarted(MinecraftServer server) {
        try {
            Path f = file();
            if (Files.exists(f)) cfg = Config.fromJson(Files.readString(f, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
        if (cfg.running) changeCountdown = cfg.changeSeconds * 20;
        if (cfg.timerOn) timerCountdown = Math.max(1, cfg.timerSeconds) * 20;
        if (cfg.eventsOn) eventCountdown = Math.max(1, cfg.eventSeconds) * 20;
        applyAll(server);
    }

    public static void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, cfg.toJson(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ menú -> servidor

    public static void onSave(MinecraftServer server, Config in) {
        boolean timerWas = cfg.timerOn;
        boolean eventsWas = cfg.eventsOn;
        cfg.changeSeconds = Math.max(1, Math.min(in.changeSeconds, 86400 * 7));
        cfg.randomOrder = in.randomOrder;
        cfg.cmdBefore = limit(in.cmdBefore);
        cfg.timerCmd = limit(in.timerCmd);
        cfg.timerSeconds = Math.max(1, Math.min(in.timerSeconds, 86400 * 7));
        cfg.timerOn = in.timerOn;
        cfg.eventsOn = in.eventsOn;
        cfg.eventSeconds = Math.max(1, Math.min(in.eventSeconds, 86400 * 7));
        for (Config.Entry e : in.changes) {
            Config.Entry mine = Config.find(cfg.changes, e.id);
            if (mine != null) {
                mine.enabled = e.enabled;
                mine.value = Math.max(0, Math.min(e.value, 100000));
            }
        }
        for (Config.Entry e : in.events) {
            Config.Entry mine = Config.find(cfg.events, e.id);
            if (mine != null) mine.enabled = e.enabled;
        }
        Config.Zone z = cfg.zone, iz = in.zone;
        z.startRadius = Math.max(1, Math.min(iz.startRadius, 30000000));
        z.minRadius = Math.max(0, Math.min(iz.minRadius, z.startRadius));
        z.shrinkBlocks = Math.max(0, Math.min(iz.shrinkBlocks, 30000000));
        z.shrinkSeconds = Math.max(1, Math.min(iz.shrinkSeconds, 86400 * 7));
        z.damage = Math.max(0, Math.min(iz.damage, 1000));
        z.damageInside = iz.damageInside;
        if (cfg.timerOn && !timerWas) timerCountdown = cfg.timerSeconds * 20;
        if (cfg.eventsOn && !eventsWas) eventCountdown = cfg.eventSeconds * 20;
        applyAll(server);
        save();
    }

    private static String limit(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    public static void onAction(MinecraftServer server, ServerPlayer p, String id) {
        switch (id) {
            case "START" -> start(server);
            case "STOP" -> stop(server);
            case "ZONE_START" -> {
                Config.Zone z = cfg.zone;
                z.active = true;
                z.cx = p.getX();
                z.cz = p.getZ();
                z.radius = z.startRadius;
                z.dim = p.serverLevel().dimension().location().toString();
                z.ticksLeft = z.shrinkSeconds * 20;
                save();
            }
            case "ZONE_STOP" -> {
                cfg.zone.active = false;
                save();
            }
            default -> {
            }
        }
    }

    public static void start(MinecraftServer server) {
        cfg.activeChanges.clear();
        applyAll(server);
        cfg.running = true;
        changeCountdown = 0; // el primer cambio entra enseguida
        save();
    }

    public static void stop(MinecraftServer server) {
        cfg.running = false;
        cfg.activeChanges.clear();
        applyAll(server);
        save();
    }

    // ------------------------------------------------------------------ utilidades

    public static boolean active(ChangeType t) {
        return cfg.activeChanges.contains(t.name());
    }

    public static double value(ChangeType t) {
        Config.Entry e = Config.find(cfg.changes, t.name());
        return e == null ? t.defaultValue : e.value;
    }

    static void runCmd(MinecraftServer server, String cmd) {
        if (cmd == null) return;
        String c = cmd.trim();
        if (c.startsWith("/")) c = c.substring(1);
        if (c.isEmpty()) return;
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
        } catch (Exception ignored) {
        }
    }

    static List<ServerPlayer> players(MinecraftServer server) {
        List<ServerPlayer> list = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isSpectator() && !p.isCreative()) list.add(p);
        }
        return list;
    }

    /** Crea una entidad cerca de un punto (en el suelo, o en el aire). */
    static <T extends Entity> T spawnNear(ServerLevel level, EntityType<T> type, Vec3 c,
                                           double min, double max, boolean air) {
        double ang = RNG.nextDouble() * Math.PI * 2.0;
        double d = min + RNG.nextDouble() * (max - min);
        double x = c.x + Math.cos(ang) * d;
        double z = c.z + Math.sin(ang) * d;
        double y;
        if (air) y = c.y + 12 + RNG.nextInt(8);
        else if (level.dimensionType().hasCeiling()) y = c.y;
        else y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
        T e = type.create(level);
        if (e == null) return null;
        e.moveTo(x, y, z, RNG.nextFloat() * 360.0F, 0.0F);
        e.addTag(TAG_DUP);
        if (e instanceof Mob m) {
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.EVENT, null);
        }
        level.addFreshEntity(e);
        return e;
    }

    // ------------------------------------------------------------------ atributos y carga de entidades

    private static void mod(LivingEntity le, Holder<Attribute> attr, String key, boolean applies, boolean on,
                            AttributeModifier.Operation op, double amount) {
        AttributeInstance inst = le.getAttribute(attr);
        if (inst == null) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(DificultadMod.MODID, key);
        boolean had = inst.hasModifier(id);
        if (had) inst.removeModifier(id);
        if (applies && on) {
            inst.addPermanentModifier(new AttributeModifier(id, amount, op));
            if (!had && attr == Attributes.MAX_HEALTH) le.setHealth(le.getMaxHealth());
        }
    }

    private static boolean boss(Entity e) {
        return e instanceof WitherBoss || e instanceof EnderDragon;
    }

    /** Pone o quita los modificadores de esta entidad según los cambios activos. */
    static void applyTo(Entity e) {
        if (!(e instanceof LivingEntity le) || e instanceof net.minecraft.world.entity.player.Player) return;
        boolean enemy = e instanceof Enemy && !boss(e);
        AttributeModifier.Operation add = AttributeModifier.Operation.ADD_VALUE;
        AttributeModifier.Operation mul = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;

        mod(le, Attributes.ATTACK_DAMAGE, "zombie_damage", e instanceof Zombie,
                active(ChangeType.ZOMBIE_DAMAGE), add, value(ChangeType.ZOMBIE_DAMAGE) * 2.0);
        mod(le, Attributes.MOVEMENT_SPEED, "spider_speed", e instanceof Spider,
                active(ChangeType.SPIDER_SPEED), mul, value(ChangeType.SPIDER_SPEED) - 1.0);
        mod(le, Attributes.MAX_HEALTH, "hostile_health", enemy,
                active(ChangeType.HOSTILE_HEALTH), mul, value(ChangeType.HOSTILE_HEALTH) - 1.0);
        mod(le, Attributes.ATTACK_DAMAGE, "enderman_damage", e instanceof EnderMan,
                active(ChangeType.ENDERMAN_DAMAGE), mul, value(ChangeType.ENDERMAN_DAMAGE) - 1.0);
        mod(le, Attributes.ATTACK_KNOCKBACK, "knockback", enemy,
                active(ChangeType.KNOCKBACK), add, value(ChangeType.KNOCKBACK) * 0.4);
        mod(le, Attributes.FOLLOW_RANGE, "detection", enemy,
                active(ChangeType.DETECTION), mul, value(ChangeType.DETECTION) - 1.0);
        mod(le, Attributes.MAX_HEALTH, "ghast_health", e instanceof Ghast,
                active(ChangeType.GHAST_HEALTH), mul, value(ChangeType.GHAST_HEALTH) - 1.0);
        mod(le, Attributes.ATTACK_DAMAGE, "warden_damage", e instanceof Warden,
                active(ChangeType.WARDEN_DAMAGE), mul, value(ChangeType.WARDEN_DAMAGE) - 1.0);

        if (active(ChangeType.ZOMBIE_ARMOR) && e instanceof Zombie z && !z.getTags().contains(TAG_ARMOR)) {
            z.addTag(TAG_ARMOR);
            if (z.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            if (z.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) z.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            if (z.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) z.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            if (z.getItemBySlot(EquipmentSlot.FEET).isEmpty()) z.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }
    }

    static void applyAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) applyTo(e);
        }
    }

    public static void onDeath(LivingEntity e) {
        if (e instanceof Slime) {
            deadSlimes.add(new double[]{e.getX(), e.getY(), e.getZ(), tick});
        }
    }

    private static boolean slimeChild(Slime s) {
        deadSlimes.removeIf(d -> tick - d[3] > 100);
        for (double[] d : deadSlimes) {
            double dx = s.getX() - d[0], dy = s.getY() - d[1], dz = s.getZ() - d[2];
            if (dx * dx + dy * dy + dz * dz < 36.0) return true;
        }
        return false;
    }

    public static void onLoad(Entity entity, ServerLevel level) {
        // flechas de mobs
        if (entity instanceof AbstractArrow arrow) {
            if (active(ChangeType.ARROW_DAMAGE) && arrow.getOwner() instanceof Mob && !arrow.getTags().contains(TAG_ARROW)) {
                arrow.addTag(TAG_ARROW);
                arrow.setBaseDamage(arrow.getBaseDamage() * value(ChangeType.ARROW_DAMAGE));
            }
            return;
        }
        // bolas de fuego de blazes
        if (entity instanceof SmallFireball fb) {
            if (active(ChangeType.BLAZE_FIRE) && fb.getOwner() instanceof Blaze blaze && !fb.getTags().contains(TAG_FIRE)) {
                fb.addTag(TAG_FIRE);
                int extra = (int) Math.round(value(ChangeType.BLAZE_FIRE)) - 1;
                if (extra > 0) {
                    queue.add(() -> {
                        Vec3 dir = fb.getDeltaMovement();
                        for (int i = 0; i < extra; i++) {
                            Vec3 d = dir.add(RNG.nextGaussian() * 0.04, RNG.nextGaussian() * 0.04, RNG.nextGaussian() * 0.04);
                            SmallFireball nf = new SmallFireball(level, blaze, d);
                            nf.setPos(fb.getX(), fb.getY(), fb.getZ());
                            nf.addTag(TAG_FIRE);
                            level.addFreshEntity(nf);
                        }
                    });
                }
            }
            return;
        }
        if (!(entity instanceof LivingEntity)) return;

        applyTo(entity);

        // creepers: explosión más grande (una sola vez)
        if (entity instanceof Creeper c && active(ChangeType.CREEPER_BLAST) && !c.getTags().contains(TAG_CREEPER)) {
            c.addTag(TAG_CREEPER);
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            c.saveWithoutId(t);
            int base = t.contains("ExplosionRadius") ? t.getByte("ExplosionRadius") : 3;
            t.putByte("ExplosionRadius", (byte) Math.max(1, Math.min(127, Math.round(base * value(ChangeType.CREEPER_BLAST)))));
            c.load(t);
        }

        // slimes más grandes (los hijos de un slime que muere no cuentan)
        if (entity instanceof Slime s && active(ChangeType.SLIME_SIZE) && !s.getTags().contains(TAG_SLIME)) {
            s.addTag(TAG_SLIME);
            if (!slimeChild(s)) {
                s.setSize((int) Math.max(1, Math.min(48, Math.round(s.getSize() * value(ChangeType.SLIME_SIZE)))), true);
            }
        }

        // más hostiles: se duplican los que aparecen
        if (active(ChangeType.HOSTILE_SPAWN) && entity instanceof Mob m && entity instanceof Enemy
                && !boss(entity) && !(entity instanceof Warden) && !m.getTags().contains(TAG_DUP)) {
            m.addTag(TAG_DUP);
            int extra = (int) Math.round(value(ChangeType.HOSTILE_SPAWN)) - 1;
            for (int i = 0; i < Math.min(extra, 8); i++) {
                queue.add(() -> {
                    Entity clone = m.getType().create(level);
                    if (clone == null) return;
                    clone.moveTo(m.getX() + RNG.nextDouble() * 2 - 1, m.getY(), m.getZ() + RNG.nextDouble() * 2 - 1,
                            RNG.nextFloat() * 360.0F, 0.0F);
                    clone.addTag(TAG_DUP);
                    if (clone instanceof Mob cm) {
                        cm.finalizeSpawn(level, level.getCurrentDifficultyAt(clone.blockPosition()), MobSpawnType.NATURAL, null);
                    }
                    level.addFreshEntity(clone);
                });
            }
        }
    }

    // ------------------------------------------------------------------ tick

    public static void tick(MinecraftServer server) {
        tick++;
        if (!queue.isEmpty()) {
            List<Runnable> q = new ArrayList<>(queue);
            queue.clear();
            for (Runnable r : q) {
                try {
                    r.run();
                } catch (Exception ignored) {
                }
            }
        }

        // cambios progresivos
        if (cfg.running && --changeCountdown <= 0) {
            changeCountdown = cfg.changeSeconds * 20;
            activateNext(server);
        }
        // comando con temporizador
        if (cfg.timerOn && !cfg.timerCmd.isBlank() && --timerCountdown <= 0) {
            timerCountdown = Math.max(1, cfg.timerSeconds) * 20;
            runCmd(server, cfg.timerCmd);
        }
        // eventos aleatorios
        if (cfg.eventsOn && --eventCountdown <= 0) {
            eventCountdown = Math.max(1, cfg.eventSeconds) * 20;
            Events.runRandom(server);
        }

        if (tick % 2 == 0) tickMobs(server);
        if (tick % 20 == 0) {
            tickSecond(server);
            tickZone(server);
        }
    }

    private static void activateNext(MinecraftServer server) {
        List<Config.Entry> pool = new ArrayList<>();
        for (Config.Entry e : cfg.changes) {
            if (e.enabled && !cfg.activeChanges.contains(e.id)) pool.add(e);
        }
        if (pool.isEmpty()) return;
        Config.Entry pick = cfg.randomOrder ? pool.get(RNG.nextInt(pool.size())) : pool.get(0);
        runCmd(server, cfg.cmdBefore); // el comando va ANTES de cada cambio
        cfg.activeChanges.add(pick.id);
        applyAll(server);
        save();
    }

    private static void tickMobs(MinecraftServer server) {
        boolean skel = active(ChangeType.SKELETON_RATE);
        boolean atk = active(ChangeType.ATTACK_SPEED);
        boolean sun = active(ChangeType.SUN_RESIST);
        if (!(skel || atk || sun)) return;

        Set<Mob> mobs = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            mobs.addAll(p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(40.0)));
        }
        for (Mob m : mobs) {
            LivingEntity target = m.getTarget();
            if (skel && m instanceof AbstractSkeleton sk && target != null && sk.getMainHandItem().is(Items.BOW)) {
                int interval = (int) Math.max(6, 40.0 / Math.max(0.1, value(ChangeType.SKELETON_RATE)));
                int last = lastShot.getOrDefault(m.getId(), 0);
                if (tick - last >= interval && sk.hasLineOfSight(target) && sk.distanceToSqr(target) < 225.0) {
                    lastShot.put(m.getId(), tick);
                    sk.performRangedAttack(target, 1.0F);
                }
            }
            if (atk && target != null && m instanceof Enemy && !(m instanceof RangedAttackMob)
                    && !(m instanceof Creeper) && !boss(m)) {
                int interval = (int) Math.max(2, 20.0 / Math.max(0.1, value(ChangeType.ATTACK_SPEED)));
                int last = lastHit.getOrDefault(m.getId(), 0);
                if (tick - last >= interval && m.distanceToSqr(target) < 6.25) {
                    lastHit.put(m.getId(), tick);
                    target.invulnerableTime = 0;
                    m.doHurtTarget(target);
                }
            }
            if (sun && (m instanceof Zombie || m instanceof AbstractSkeleton) && m.level().isDay()) {
                int cyc = (int) Math.max(2, value(ChangeType.SUN_RESIST) * 10);
                if (m.tickCount % cyc >= 10) {
                    m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 8, 0, false, false, false));
                }
            }
        }
        if (lastShot.size() > 2000) lastShot.clear();
        if (lastHit.size() > 2000) lastHit.clear();
    }

    private static void tickSecond(MinecraftServer server) {
        int sec = tick / 20;
        for (ServerPlayer p : players(server)) {
            ServerLevel level = p.serverLevel();
            // phantoms desde el primer día
            if (active(ChangeType.PHANTOMS) && sec % 20 == 0 && level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                var box = p.getBoundingBox().inflate(64.0);
                if (level.getEntitiesOfClass(Phantom.class, box).isEmpty()) {
                    int n = (int) Math.max(1, Math.round(value(ChangeType.PHANTOMS)));
                    for (int i = 0; i < Math.min(n, 10); i++) {
                        Phantom ph = spawnNear(level, EntityType.PHANTOM, p.position(), 8, 20, true);
                        if (ph != null) {
                            ph.setTarget(p);
                            ph.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                                    MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
                        }
                    }
                }
            }
            // piglins hostiles en cualquier dimensión
            if (active(ChangeType.PIGLIN_BRUTES) && sec % 30 == 0) {
                var box = p.getBoundingBox().inflate(48.0);
                int max = (int) Math.max(1, Math.round(value(ChangeType.PIGLIN_BRUTES)));
                if (level.getEntitiesOfClass(PiglinBrute.class, box).size() < max) {
                    PiglinBrute b = spawnNear(level, EntityType.PIGLIN_BRUTE, p.position(), 14, 28, false);
                    if (b != null) {
                        b.setImmuneToZombification(true);
                        b.setTarget(p);
                    }
                }
            }
            // más rayos en tormenta
            if (active(ChangeType.LIGHTNING) && level.isThundering()) {
                double chance = Math.min(1.0, value(ChangeType.LIGHTNING) / 600.0);
                if (RNG.nextDouble() < chance) strike(level, p.position(), 40);
            }
        }
    }

    static void strike(ServerLevel level, Vec3 c, double radius) {
        net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        double ang = RNG.nextDouble() * Math.PI * 2.0;
        double d = 4 + RNG.nextDouble() * radius;
        double x = c.x + Math.cos(ang) * d, z = c.z + Math.sin(ang) * d;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        bolt.setPos(x, y, z);
        level.addFreshEntity(bolt);
    }

    // ------------------------------------------------------------------ zona

    private static void tickZone(MinecraftServer server) {
        Config.Zone z = cfg.zone;
        if (!z.active) return;
        z.ticksLeft -= 20;
        if (z.ticksLeft <= 0) {
            z.radius = Math.max(z.minRadius, z.radius - z.shrinkBlocks);
            z.ticksLeft = z.shrinkSeconds * 20;
            save();
        }
        for (ServerPlayer p : players(server)) {
            if (!p.serverLevel().dimension().location().toString().equals(z.dim)) continue;
            double dx = p.getX() - z.cx, dz = p.getZ() - z.cz;
            boolean inside = dx * dx + dz * dz <= z.radius * z.radius;
            if (inside == z.damageInside && z.damage > 0) {
                p.hurt(p.serverLevel().damageSources().generic(), (float) z.damage);
            }
        }
    }
}
