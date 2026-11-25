package com.dificultad.client;

import com.dificultad.ChangeType;
import com.dificultad.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/** Editar los 20 cambios: activar/desactivar y cambiar su valor. */
public class ChangesScreen extends Base {
    private static final int PER_PAGE = 7;
    private int page = 0;

    public ChangesScreen(Config cfg, Screen parent) {
        super("Editar cambios", cfg, parent);
    }

    private int left() {
        return this.width / 2 - 165;
    }

    @Override
    protected void init() {
        int x = left();
        box(x + 190, 28, 60, String.valueOf(cfg.changeSeconds), 7, s -> cfg.changeSeconds = Math.max(1, whole(s, cfg.changeSeconds)));
        button(cfg.randomOrder ? "Orden: al azar" : "Orden: en fila", x + 256, 28, 74, () -> {
            cfg.randomOrder = !cfg.randomOrder;
            this.rebuildWidgets();
        });

        List<Config.Entry> list = cfg.changes;
        int pages = (list.size() + PER_PAGE - 1) / PER_PAGE;
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= list.size()) break;
            Config.Entry e = list.get(idx);
            int y = 62 + i * 24;
            button(e.enabled ? "Sí" : "No", x, y, 30, () -> {
                e.enabled = !e.enabled;
                this.rebuildWidgets();
            });
            box(x + 270, y, 60, trim(e.value), 8, s -> e.value = num(s, e.value));
        }
        int by = 62 + PER_PAGE * 24 + 4;
        button("<", x, by, 30, () -> {
            if (page > 0) {
                page--;
                this.rebuildWidgets();
            }
        });
        button(">", x + 34, by, 30, () -> {
            if (page < pages - 1) {
                page++;
                this.rebuildWidgets();
            }
        });
        button("Guardar y volver", x + 190, by, 140, this::onClose);
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int x = left();
        text(g, "Segundos entre cambios", x, 34);
        text(g, "Cambio (Sí = participa)", x + 36, 50);
        text(g, "Valor", x + 274, 50);
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= cfg.changes.size()) break;
            ChangeType t = ChangeType.byId(cfg.changes.get(idx).id);
            if (t != null) text(g, t.label, x + 36, 68 + i * 24);
        }
        text(g, (page + 1) + "/" + ((cfg.changes.size() + PER_PAGE - 1) / PER_PAGE), x + 70, 62 + PER_PAGE * 24 + 10);
    }
}
