package com.dificultad.client;

import com.dificultad.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Zona que se cierra cada tanto tiempo y baja vida poco a poco. */
public class ZoneScreen extends Base {
    public ZoneScreen(Config cfg, Screen parent) {
        super("Zona que se cierra", cfg, parent);
    }

    @Override
    protected void init() {
        Config.Zone z = cfg.zone;
        int x = this.width / 2 - 150;
        int y = 40;
        box(x + 190, y, 110, String.valueOf(z.startRadius), 9, s -> z.startRadius = Math.max(1, num(s, z.startRadius)));
        box(x + 190, y + 24, 110, String.valueOf(z.minRadius), 9, s -> z.minRadius = Math.max(0, num(s, z.minRadius)));
        box(x + 190, y + 48, 110, String.valueOf(z.shrinkBlocks), 9, s -> z.shrinkBlocks = Math.max(0, num(s, z.shrinkBlocks)));
        box(x + 190, y + 72, 110, String.valueOf(z.shrinkSeconds), 7, s -> z.shrinkSeconds = Math.max(1, whole(s, z.shrinkSeconds)));
        box(x + 190, y + 96, 110, String.valueOf(z.damage), 6, s -> z.damage = Math.max(0, num(s, z.damage)));
        button(z.damageInside ? "Daño: dentro de la zona" : "Daño: fuera de la zona", x, y + 124, 300, () -> {
            z.damageInside = !z.damageInside;
            this.rebuildWidgets();
        });
        button("Activar zona aquí", x, y + 150, 146, () -> {
            save();
            action("ZONE_START");
            this.minecraft.setScreen(null);
        });
        button("Desactivar zona", x + 154, y + 150, 146, () -> {
            action("ZONE_STOP");
            this.minecraft.setScreen(null);
        });
        button("Guardar y volver", x + 80, y + 176, 140, this::onClose);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int x = this.width / 2 - 150;
        int y = 40;
        text(g, "Radio inicial (bloques)", x, y + 6);
        text(g, "Radio mínimo", x, y + 30);
        text(g, "Se cierra (bloques)", x, y + 54);
        text(g, "Cada (segundos)", x, y + 78);
        text(g, "Daño por segundo (vida)", x, y + 102);
        g.drawCenteredString(this.font, "Zona que se cierra", this.width / 2, 20, 0xFFFFFF);
    }
}
