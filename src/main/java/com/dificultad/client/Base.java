package com.dificultad.client;

import com.dificultad.Config;
import com.dificultad.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Pantalla base: botones, cajas de texto, y guardar al cerrar. */
public abstract class Base extends Screen {
    protected final Config cfg;
    protected final Screen parent;

    protected Base(String title, Config cfg, Screen parent) {
        super(Component.literal(title));
        this.cfg = cfg;
        this.parent = parent;
    }

    protected Button button(String label, int x, int y, int w, Runnable action) {
        Button b = Button.builder(Component.literal(label), btn -> action.run()).bounds(x, y, w, 20).build();
        addRenderableWidget(b);
        return b;
    }

    protected EditBox box(int x, int y, int w, String value, int max, Consumer<String> onChange) {
        EditBox e = new EditBox(this.font, x, y, w, 20, Component.empty());
        e.setMaxLength(max);
        e.setValue(value == null ? "" : value);
        e.setResponder(onChange);
        addRenderableWidget(e);
        return e;
    }

    protected void save() {
        ClientPlayNetworking.send(new Net.Save(cfg.toJson()));
    }

    protected void action(String id) {
        ClientPlayNetworking.send(new Net.Action(id));
    }

    protected static double num(String s, double fallback) {
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception e) {
            return fallback;
        }
    }

    protected static int whole(String s, int fallback) {
        return (int) Math.round(num(s, fallback));
    }

    protected void text(net.minecraft.client.gui.GuiGraphics g, String t, int x, int y) {
        g.drawString(this.font, t, x, y, 0xFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Al cerrar (ESC) se guarda y se vuelve al menú anterior. */
    @Override
    public void onClose() {
        save();
        this.minecraft.setScreen(parent);
    }
}
