package com.dificultad;

/** Los 10 eventos aleatorios que suben la dificultad. */
public enum EventType {
    HORDE("Horda de zombis"),
    CREEPERS("Emboscada de creepers"),
    LIGHTNING_STORM("Tormenta de rayos"),
    PHANTOMS("Ataque de phantoms"),
    HUNGER("Hambre y debilidad"),
    VEXES("Enjambre de vexes"),
    WITHERS("Esqueletos wither"),
    EMPOWER("Mobs fortalecidos"),
    ARMOR_WEAR("Desgaste de armadura"),
    NIGHT("Noche forzada");

    public final String label;

    EventType(String label) {
        this.label = label;
    }

    public static EventType byId(String id) {
        for (EventType t : values()) if (t.name().equals(id)) return t;
        return null;
    }
}
