package com.dificultad.client;

import com.dificultad.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Menú principal del palo. */
public class MainScreen extends Base {
    public MainScreen(Config cfg) {
        super("Dificultad", cfg, null);
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 110;
        int y = this.height / 2 - 84;
        button(cfg.running ? "Detener cambios" : "Iniciar cambios", x, y, 220, () -> {
            save();
            action(cfg.running ? "STOP" : "START");
            this.minecraft.setScreen(null);
        });
        button("Editar cambios", x, y + 26, 220, () -> this.minecraft.setScreen(new ChangesScreen(cfg, this)));
        button("Comando antes de cada cambio", x, y + 52, 220, () -> this.minecraft.setScreen(new CommandScreen(cfg, this, false)));
        button("Comando con temporizador", x, y + 78, 220, () -> this.minecraft.setScreen(new CommandScreen(cfg, this, true)));
        button("Eventos aleatorios", x, y + 104, 220, () -> this.minecraft.setScreen(new EventsScreen(cfg, this)));
        button("Zona que se cierra", x, y + 130, 220, () -> this.minecraft.setScreen(new ZoneScreen(cfg, this)));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(this.font, "Dificultad", this.width / 2, this.height / 2 - 102, 0xFFFFFF);
    }

    /** El menú principal cierra de verdad (guardando). */
    @Override
    public void onClose() {
        save();
        this.minecraft.setScreen(null);
    }
}
