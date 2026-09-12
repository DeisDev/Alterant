package com.deisdev.alterant.client;

import com.deisdev.alterant.client.ClientConfig.TooltipMode;
import com.deisdev.alterant.client.ClientConfig.HudAnchor;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Referenced only by client screen factories after checking that YACL is installed. */
public final class AlterantConfigScreen {
    private AlterantConfigScreen() {}
    public static Screen create(Screen parent) {
        var config = ClientConfig.get();
        var draft = new Draft(config.settings());
        return YetAnotherConfigLib.createBuilder()
                .title(text("title"))
                .category(ConfigCategory.createBuilder().name(text("display"))
                        .option(Option.<TooltipMode>createBuilder()
                                .name(text("tooltip")).description(OptionDescription.of(text("tooltip.description")))
                                .binding(ClientConfig.DEFAULTS.tooltip(), () -> draft.tooltip, value -> draft.tooltip = value)
                                .controller(option -> EnumControllerBuilder.create(option).enumClass(TooltipMode.class)
                                        .formatValue(value -> text("tooltip." + value.getSerializedName())))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(text("coating_outlines")).description(OptionDescription.of(text("coating_outlines.description")))
                                .binding(ClientConfig.DEFAULTS.coatingOutlines(), () -> draft.outlines, value -> draft.outlines = value)
                                .controller(TickBoxControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder()
                                .name(text("surface_preview")).description(OptionDescription.of(text("surface_preview.description")))
                                .binding(ClientConfig.DEFAULTS.surfacePreview(), () -> draft.preview, value -> draft.preview = value)
                                .controller(TickBoxControllerBuilder::create).build())
                        .group(draft.hudGroup())
                        .group(draft.serumGroup())
                        .build())
                .category(ConfigCategory.createBuilder().name(text("gameplay"))
                        .option(dev.isxander.yacl3.api.LabelOption.create(text("gameplay.description")))
                        .option(dev.isxander.yacl3.api.ButtonOption.createBuilder().name(text("gameplay.open"))
                                .text(text("gameplay.edit")).action((screen, option) -> GameplayConfigScreen.open(screen)).build())
                        .build())
                .save(() -> {
                    if (!config.save(draft.settings())) {
                        SystemToast.add(Minecraft.getInstance().gui.toastManager(), SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                                text("title"), text("save_failed"));
                    }
                }).build().generateScreen(parent);
    }
    private static Component text(String key) { return Component.translatable("config.alterant." + key); }
    private static final class Draft {
        private TooltipMode tooltip;
        private boolean outlines, preview;
        private HudAnchor anchor;
        private int hudX, hudY, serumX, serumY;
        private final StyleDraft hudStyle, serumStyle;
        private Draft(ClientConfig.Settings settings) {
            tooltip = settings.tooltip(); outlines = settings.coatingOutlines(); preview = settings.surfacePreview();
            anchor = settings.hud().anchor(); hudX = settings.hud().x(); hudY = settings.hud().y();
            serumX = settings.serum().x(); serumY = settings.serum().y();
            hudStyle = new StyleDraft(settings.hud().style()); serumStyle = new StyleDraft(settings.serum().style());
        }
        private ClientConfig.Settings settings() {
            return new ClientConfig.Settings(tooltip, outlines, preview,
                    new ClientConfig.Hud(anchor, hudX, hudY, hudStyle.style()),
                    new ClientConfig.SerumDisplay(serumX, serumY, serumStyle.style()));
        }
        private OptionGroup hudGroup() {
            var defaults = ClientConfig.Hud.DEFAULT;
            var group = OptionGroup.createBuilder().name(text("hud")).description(OptionDescription.of(text("hud.description")))
                    .option(Option.<HudAnchor>createBuilder().name(text("hud.anchor"))
                            .description(OptionDescription.of(text("hud.anchor.description")))
                            .binding(defaults.anchor(), () -> anchor, value -> anchor = value)
                            .controller(option -> EnumControllerBuilder.create(option).enumClass(HudAnchor.class)
                                    .formatValue(value -> text("hud.anchor." + value.getSerializedName()))).build())
                    .option(offset("hud.x", defaults.x(), () -> hudX, value -> hudX = value, -1000, 1000))
                    .option(offset("hud.y", defaults.y(), () -> hudY, value -> hudY = value, -1000, 1000));
            return hudStyle.addTo(group, DisplayStyle.HUD).build();
        }
        private OptionGroup serumGroup() {
            var defaults = ClientConfig.SerumDisplay.DEFAULT;
            var group = OptionGroup.createBuilder().name(text("serum")).description(OptionDescription.of(text("serum.description")))
                    .option(offset("serum.x", defaults.x(), () -> serumX, value -> serumX = value, -200, 200))
                    .option(offset("serum.y", defaults.y(), () -> serumY, value -> serumY = value, 0, 200));
            return serumStyle.addTo(group, DisplayStyle.SERUM).build();
        }
    }
    private static Option<Integer> offset(String key, int defaults, Supplier<Integer> get, Consumer<Integer> set, int min, int max) {
        return Option.<Integer>createBuilder().name(text(key)).description(OptionDescription.of(text(key + ".description")))
                .binding(defaults, get, set).controller(option -> IntegerFieldControllerBuilder.create(option).range(min, max)).build();
    }
    private static Option<Integer> percent(String key, int defaults, Supplier<Integer> get, Consumer<Integer> set, int min, int max, int step) {
        return Option.<Integer>createBuilder().name(text(key)).description(OptionDescription.of(text(key + ".description")))
                .binding(defaults, get, set).controller(option -> IntegerSliderControllerBuilder.create(option).range(min, max).step(step)
                        .formatValue(value -> Component.literal(value + "%"))).build();
    }
    private static final class StyleDraft {
        private int scale, opacity;
        private Color background;
        private boolean shadow;
        private StyleDraft(DisplayStyle style) {
            scale = style.scale(); opacity = style.opacity(); background = new Color(style.background()); shadow = style.shadow();
        }
        private DisplayStyle style() { return new DisplayStyle(scale, opacity, background.getRGB() & 0xFFFFFF, shadow); }
        private OptionGroup.Builder addTo(OptionGroup.Builder group, DisplayStyle defaults) {
            return group.option(percent("scale", defaults.scale(), () -> scale, value -> scale = value, 50, 200, 5))
                    .option(percent("opacity", defaults.opacity(), () -> opacity, value -> opacity = value, 0, 100, 1))
                    .option(Option.<Color>createBuilder().name(text("background")).description(OptionDescription.of(text("background.description")))
                            .binding(new Color(defaults.background()), () -> background, value -> background = value)
                            .controller(option -> ColorControllerBuilder.create(option).allowAlpha(false)).build())
                    .option(Option.<Boolean>createBuilder().name(text("shadow")).description(OptionDescription.of(text("shadow.description")))
                            .binding(defaults.shadow(), () -> shadow, value -> shadow = value)
                            .controller(TickBoxControllerBuilder::create).build());
        }
    }
}
