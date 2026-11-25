package com.dificultad.client;

import com.dificultad.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Comando antes de cada cambio (timer = false) o comando con temporizador (timer = true). */
public class CommandScreen extends Base {
    private final boolean timer;

    public CommandScreen(Config cfg, Screen parent, boolean timer) {
        super(timer ? "Comando con temporizador" : "Comando antes de cada cambio", cfg, parent);
        this.timer = timer;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 150;
        int y = this.height / 2 - 40;
        if (!timer) {
            box(x, y, 300, cfg.cmdBefore, 300, s -> cfg.cmdBefore = s);
        } else {
            box(x, y, 300, cfg.timerCmd, 300, s -> cfg.timerCmd = s);
            box(x, y + 40, 80, String.valueOf(cfg.timerSeconds), 7, s -> cfg.timerSeconds = Math.max(1, whole(s, cfg.timerSeconds)));
            button(cfg.timerOn ? "Temporizador: activado" : "Temporizador: desactivado", x + 90, y + 40, 210, () -> {
                cfg.timerOn = !cfg.timerOn;
                this.rebuildWidgets();
            });
        }
        button("Guardar y volver", x + 80, y + 80, 140, this::onClose);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int x = this.width / 2 - 150;
        int y = this.height / 2 - 40;
        g.drawCenteredString(this.font, this.title, this.width / 2, y - 40, 0xFFFFFF);
        text(g, timer ? "Comando (sin /) que se ejecuta cada vez que acaba el temporizador" : "Comando (sin /) que se ejecuta antes de cada cambio", x, y - 12);
        if (timer) text(g, "Segundos", x, y + 28);
    }
}
