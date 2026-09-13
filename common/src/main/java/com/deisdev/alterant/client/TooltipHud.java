package com.deisdev.alterant.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Fits wrapped text in the viewport before applying a screen anchor and offsets. */
public final class TooltipHud {
    public record Panel(int x, int y, int width, int height, float scale, List<FormattedCharSequence> lines) {
        public Panel { lines = List.copyOf(lines); }
    }
    private TooltipHud() {}

    public static Panel layout(Font font, List<Component> lines, int screenWidth, int screenHeight, ClientConfig.Settings settings) {
        if (lines.isEmpty() || screenWidth < 48 || screenHeight < 48) { return null; }
        var hud = settings.hud();
        float scale = hud.style().factor();
        int availableWidth = (int) ((screenWidth - 16) / scale);
        int availableHeight = (int) ((screenHeight - 16) / scale);
        if (availableWidth < 24 || availableHeight < 19) { return null; }
        boolean basic = settings.tooltip() == ClientConfig.TooltipMode.BASIC;
        int width = Math.min(availableWidth, Math.min(basic ? 200 : 260,
                lines.stream().mapToInt(font::width).max().orElse(140) + 12));
        // Basic always stays within two rows, including long translations and renamed items.
        var wrapped = basic ? lines.stream().limit(2).map(line -> compact(font, line, width - 12)).toList()
                : lines.stream().flatMap(line -> font.split(line, width - 12).stream()).toList();
        int count = Math.min(wrapped.size(), (availableHeight - 8) / 11);
        if (count == 0) { return null; }
        var visible = new java.util.ArrayList<>(wrapped.subList(0, count));
        if (count < wrapped.size()) {
            // Even the overflow notice must fit when the window is narrow or the display is enlarged.
            visible.set(count - 1, net.minecraft.locale.Language.getInstance().getVisualOrder(
                    font.substrByWidth(Component.translatable("overlay.alterant.more"), width - 12)));
        }
        int height = 8 + count * 11;
        int remainingX = screenWidth - (int) Math.ceil(width * scale);
        int remainingY = screenHeight - (int) Math.ceil(height * scale);
        int x = Math.clamp(hud.anchor().x(remainingX) + hud.x(), 8, remainingX - 8);
        int y = Math.clamp(hud.anchor().y(remainingY) + hud.y(), 8, remainingY - 8);
        return new Panel(x, y, width, height, scale, visible);
    }

    private static FormattedCharSequence compact(Font font, Component line, int width) {
        if (font.width(line) <= width) { return line.getVisualOrderText(); }
        var ellipsis = Component.translatable("text.alterant.ellipsis").withStyle(line.getStyle());
        return net.minecraft.locale.Language.getInstance().getVisualOrder(net.minecraft.network.chat.FormattedText.composite(
                font.substrByWidth(line, width - font.width(ellipsis)), ellipsis));
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, List<Component> lines, ClientConfig.Settings settings) {
        var panel = layout(font, lines, graphics.guiWidth(), graphics.guiHeight(), settings);
        if (panel == null) { return; }
        var style = settings.hud().style();
        var pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(panel.x(), panel.y());
            pose.scale(panel.scale(), panel.scale());
            if (style.opacity() > 0) {
                int color = style.backgroundArgb();
                graphics.fill(2, 0, panel.width() - 2, panel.height(), color);
                graphics.fill(0, 2, 2, panel.height() - 2, color);
                graphics.fill(panel.width() - 2, 2, panel.width(), panel.height() - 2, color);
            }
            for (int line = 0; line < panel.lines().size(); line++) {
                graphics.text(font, panel.lines().get(line), 6, 5 + line * 11, 0xFFE4E6EA, style.shadow());
            }
        } finally { pose.popMatrix(); }
    }
}
