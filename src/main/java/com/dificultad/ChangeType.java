package com.dificultad;

/** Los 20 cambios de dificultad. El valor es editable desde el menú. */
public enum ChangeType {
    ZOMBIE_DAMAGE("Zombis: + corazones de daño", 2),
    SKELETON_RATE("Esqueletos: disparo x", 2),
    CREEPER_BLAST("Creepers: explosión x", 3),
    SPIDER_SPEED("Arañas: velocidad x", 2),
    HOSTILE_HEALTH("Hostiles: vida x", 2),
    HOSTILE_SPAWN("Hostiles: cantidad x", 2),
    ENDERMAN_DAMAGE("Endermans: daño x", 3),
    SUN_RESIST("No-muertos: sol x más lento", 5),
    ZOMBIE_ARMOR("Zombis con armadura de hierro", 1),
    ARROW_DAMAGE("Flechas de mobs: daño x", 3),
    KNOCKBACK("Hostiles: empuje x", 5),
    PHANTOMS("Phantoms desde el día 1 (cant.)", 2),
    DETECTION("Hostiles: detección x", 2),
    SLIME_SIZE("Slimes: tamaño x", 5),
    BLAZE_FIRE("Blazes: bolas de fuego x", 3),
    GHAST_HEALTH("Ghasts: vida x", 3),
    WARDEN_DAMAGE("Wardens: daño x", 2),
    PIGLIN_BRUTES("Piglins hostiles (cantidad)", 2),
    LIGHTNING("Rayos en tormenta x", 10),
    ATTACK_SPEED("Hostiles: ataque x", 3);

    public final String label;
    public final double defaultValue;

    ChangeType(String label, double defaultValue) {
        this.label = label;
        this.defaultValue = defaultValue;
    }

    public static ChangeType byId(String id) {
        for (ChangeType t : values()) if (t.name().equals(id)) return t;
        return null;
    }
}
