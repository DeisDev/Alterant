package com.deisdev.preserve.client;

import com.deisdev.preserve.engine.PreservationLevel;
import com.deisdev.preserve.integration.jade.JadeSerumData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Isolated optional client provider; detailed world status is opt-in with Advanced inspection. */
public final class JadeTreatmentProvider implements IBlockComponentProvider {
    public static final JadeTreatmentProvider INSTANCE = new JadeTreatmentProvider();
    private JadeTreatmentProvider() {}
    @Override public Identifier getUid() { return JadeSerumData.ID; }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (ClientConfig.get().settings().tooltip() != ClientConfig.TooltipMode.ADVANCED || !accessor.getLevel().hasChunkAt(accessor.getPosition())) { return; }
        var state = (PreservationLevel) accessor.getLevel();
        var treatment = state.preserve$treatments().get(accessor.getPosition().asLong());
        if (treatment == null) { return; }
        var formula = treatment.formulation();
        tooltip.add(Component.translatable("jade.deisdev.formula", Component.translatable("item.deisdev." + formula.getSerializedName()))
                .withColor(ToolOverlay.color(formula) & 0xFFFFFF));
        if (!formula.accelerates()) {
            tooltip.add(Component.translatable("formulation.deisdev." + formula.getSerializedName()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("jade.deisdev.permanent").withStyle(ChatFormatting.GRAY));
            return;
        }
        var sample = JadeSerumData.INSTANCE.decodeFromData(accessor).filter(value -> value.position() == accessor.getPosition().asLong()
                && value.dimension().equals(accessor.getLevel().dimension().identifier()) && value.formulation() == formula.ordinal()
                && value.block().equals(BuiltInRegistries.BLOCK.getKey(accessor.getBlock())));
        if (sample.isEmpty()) {
            var synced = state.preserve$clientTreatments().serum(accessor.getPosition().asLong());
            if (synced != null && accessor.getLevel().getGameTime() - synced.receivedAt() <= 30
                    && synced.payload().formulation() == formula.ordinal()
                    && synced.payload().block().equals(BuiltInRegistries.BLOCK.getKey(accessor.getBlock()))) { sample = java.util.Optional.of(synced.payload()); }
        }
        tooltip.add(sample.map(SerumFeedback::detail).orElseGet(() -> Component.translatable("overlay.deisdev.serum_updating")));
        tooltip.add(Component.translatable("jade.deisdev.loaded_time").withStyle(ChatFormatting.GRAY));
    }
}
