package com.deisdev.preserve.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

/** Immutable, camera-facing world card. Uses vanilla depth-tested text and background pipelines. */
public final class SerumCard {
    public record Line(FormattedCharSequence text, int width, int color) {}
    public record Card(Vec3 anchor, List<Line> lines, int width, int height) {
        public Card { lines = List.copyOf(lines); }
    }
    private SerumCard() {}

    public static Card layout(Font font, Vec3 anchor, Component title, Component detail, int accent) {
        var lines = new ArrayList<Line>();
        for (var text : font.split(title, 200)) { lines.add(new Line(text, font.width(text), 0xFFF0F2F5)); }
        for (var text : font.split(detail, 200)) { lines.add(new Line(text, font.width(text), accent)); }
        return new Card(anchor, lines, lines.stream().mapToInt(Line::width).max().orElse(0) + 16, lines.size() * 11 + 10);
    }

    public static void submit(LevelRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        var card = ((OverlayRenderState) state).preserve$serumCard();
        if (card == null) { return; }
        var camera = state.cameraRenderState;
        pose.pushPose();
        pose.translate(card.anchor.x() - camera.pos.x(), card.anchor.y() - camera.pos.y(), card.anchor.z() - camera.pos.z());
        pose.mulPose(camera.orientation);
        pose.scale(0.01F, -0.01F, 0.01F);
        // The bottom is anchored above the block; extra translated lines grow upward.
        pose.translate(-card.width / 2.0F, -card.height, 0);
        collector.submitCustomGeometry(pose, RenderTypes.textBackground(), (matrix, buffer) -> {
            // Small clipped corners soften the silhouette without a border or accent strip.
            quad(matrix, buffer, 2, 0, card.width - 2, card.height);
            quad(matrix, buffer, 0, 2, 2, card.height - 2);
            quad(matrix, buffer, card.width - 2, 2, card.width, card.height - 2);
        });
        var text = collector.order(1);
        for (int i = 0; i < card.lines.size(); i++) {
            var line = card.lines.get(i);
            text.submitText(pose, (card.width - line.width) / 2.0F, 6 + i * 11, line.text, false,
                    Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, line.color, 0, 0);
        }
        pose.popPose();
    }

    private static void quad(PoseStack.Pose matrix, com.mojang.blaze3d.vertex.VertexConsumer buffer, float left, float top, float right, float bottom) {
        int color = 0xD9181C22;
        buffer.addVertex(matrix, left, top, -0.01F).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        buffer.addVertex(matrix, left, bottom, -0.01F).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        buffer.addVertex(matrix, right, bottom, -0.01F).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        buffer.addVertex(matrix, right, top, -0.01F).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
    }
}
