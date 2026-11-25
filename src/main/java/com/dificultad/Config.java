package com.dificultad;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/** Todo lo configurable (y el estado que se guarda). Se manda como JSON entre cliente y servidor. */
public class Config {
    private static final Gson GSON = new Gson();

    public static class Entry {
        public String id = "";
        public boolean enabled = true;
        public double value = 1;
    }

    public static class Zone {
        public double startRadius = 100;
        public double minRadius = 10;
        public double shrinkBlocks = 10;
        public int shrinkSeconds = 60;
        public double damage = 1.0;
        public boolean damageInside = true;
        // estado (lo maneja el servidor)
        public boolean active = false;
        public double cx = 0;
        public double cz = 0;
        public double radius = 100;
        public String dim = "minecraft:overworld";
        public int ticksLeft = 0;
    }

    public int changeSeconds = 300;
    public boolean randomOrder = true;
    public String cmdBefore = "";
    public String timerCmd = "";
    public int timerSeconds = 60;
    public boolean timerOn = false;
    public boolean eventsOn = false;
    public int eventSeconds = 120;
    public List<Entry> changes = new ArrayList<>();
    public List<Entry> events = new ArrayList<>();
    public Zone zone = new Zone();
    // estado
    public boolean running = false;
    public List<String> activeChanges = new ArrayList<>();

    /** Completa lo que falte, en el orden de siempre. */
    public Config fill() {
        if (zone == null) zone = new Zone();
        if (activeChanges == null) activeChanges = new ArrayList<>();
        if (cmdBefore == null) cmdBefore = "";
        if (timerCmd == null) timerCmd = "";
        List<Entry> c = new ArrayList<>();
        for (ChangeType t : ChangeType.values()) {
            Entry e = find(changes, t.name());
            if (e == null) {
                e = new Entry();
                e.id = t.name();
                e.value = t.defaultValue;
            }
            c.add(e);
        }
        changes = c;
        List<Entry> ev = new ArrayList<>();
        for (EventType t : EventType.values()) {
            Entry e = find(events, t.name());
            if (e == null) {
                e = new Entry();
                e.id = t.name();
            }
            ev.add(e);
        }
        events = ev;
        return this;
    }

    public static Entry find(List<Entry> list, String id) {
        if (list == null) return null;
        for (Entry e : list) if (e != null && id.equals(e.id)) return e;
        return null;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Config fromJson(String json) {
        try {
            Config c = GSON.fromJson(json, Config.class);
            if (c != null) return c.fill();
        } catch (Exception ignored) {
        }
        return new Config().fill();
    }
}
