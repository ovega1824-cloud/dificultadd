package com.dificultad.client;

import com.dificultad.Config;
import com.dificultad.EventType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Los 10 eventos aleatorios. */
public class EventsScreen extends Base {
    public EventsScreen(Config cfg, Screen parent) {
        super("Eventos aleatorios", cfg, parent);
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 170;
        button(cfg.eventsOn ? "Eventos: activados" : "Eventos: desactivados", x, 34, 170, () -> {
            cfg.eventsOn = !cfg.eventsOn;
            this.rebuildWidgets();
        });
        box(x + 270, 34, 70, String.valueOf(cfg.eventSeconds), 7, s -> cfg.eventSeconds = Math.max(1, whole(s, cfg.eventSeconds)));
        for (int i = 0; i < cfg.events.size(); i++) {
            Config.Entry e = cfg.events.get(i);
            EventType t = EventType.byId(e.id);
            if (t == null) continue;
            int col = i % 2, row = i / 2;
            button((e.enabled ? "[Sí] " : "[No] ") + t.label, x + col * 172, 66 + row * 24, 168, () -> {
                e.enabled = !e.enabled;
                this.rebuildWidgets();
            });
        }
        button("Guardar y volver", x + 100, 66 + 5 * 24 + 10, 140, this::onClose);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int x = this.width / 2 - 170;
        text(g, "Segundos entre eventos", x + 172, 22);
        text(g, "Cada tanto tiempo se elige uno de los activos al azar.", x, 54);
    }
}
