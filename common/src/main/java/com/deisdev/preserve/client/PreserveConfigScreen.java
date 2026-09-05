package com.deisdev.preserve.client;

import com.deisdev.preserve.client.ClientConfig.TooltipMode;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Referenced only by client screen factories after checking that YACL is installed. */
public final class PreserveConfigScreen {
    private PreserveConfigScreen() {}
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
                        .build())
                .category(ConfigCategory.createBuilder().name(text("gameplay"))
                        .option(dev.isxander.yacl3.api.LabelOption.create(text("gameplay.description")))
                        .option(dev.isxander.yacl3.api.ButtonOption.createBuilder().name(text("gameplay.open"))
                                .text(text("gameplay.edit")).action((screen, option) -> GameplayConfigScreen.open(screen)).build())
                        .build())
                .save(() -> {
                    if (!config.save(new ClientConfig.Settings(draft.tooltip, draft.outlines, draft.preview))) {
                        SystemToast.add(Minecraft.getInstance().gui.toastManager(), SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                                text("title"), text("save_failed"));
                    }
                }).build().generateScreen(parent);
    }
    private static Component text(String key) { return Component.translatable("config.deisdev." + key); }
    private static final class Draft {
        private TooltipMode tooltip;
        private boolean outlines, preview;
        private Draft(ClientConfig.Settings settings) {
            tooltip = settings.tooltip(); outlines = settings.coatingOutlines(); preview = settings.surfacePreview();
        }
    }
}
