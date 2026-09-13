package com.deisdev.alterant.item;

import com.deisdev.alterant.engine.PreservationService;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Server-confirmed, brief feedback. Weak player keys cannot retain disconnected worlds. */
public final class ToolFeedback {
    private static final Map<ServerPlayer, Integer> LAST_FAILURE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final Map<String, String> REASONS = Map.ofEntries(
            Map.entry("error.alterant.transfer_unsupported", "transfer_unsupported"),
            Map.entry("error.alterant.shape_unsupported", "unsupported"), Map.entry("error.alterant.shape_invalid", "unsupported"),
            Map.entry("error.alterant.shape_preview", "shape_preview"), Map.entry("error.alterant.shape_payment", "shape_payment"),
            Map.entry("error.alterant.shape_sample", "shape_sample"), Map.entry("error.alterant.shape_unchanged", "shape_same"),
            Map.entry("error.alterant.shape_obstructed", "shape_blocked"), Map.entry("error.alterant.shape_policy", "shape_policy"),
            Map.entry("error.alterant.preview_limit", "limit"), Map.entry("error.alterant.remove_first", "remove_first"),
            Map.entry("error.alterant.already_masked", "already_masked"), Map.entry("error.alterant.masked_target", "masked"), Map.entry("error.alterant.no_mask", "no_mask"),
            Map.entry("error.alterant.no_coating", "no_coating"), Map.entry("error.alterant.target_busy", "busy"), Map.entry("error.alterant.linked_busy", "busy"),
            Map.entry("error.alterant.transfer_busy", "busy"), Map.entry("error.alterant.target_unloaded", "range"),
            Map.entry("error.alterant.mask_unsupported", "unsupported"), Map.entry("error.alterant.player_access", "permission"),
            Map.entry("error.alterant.claim_denied", "permission"), Map.entry("error.alterant.charges", "charges"),
            Map.entry("error.alterant.coating_limit", "limit"), Map.entry("error.alterant.mask_limit", "limit"),
            Map.entry("error.alterant.unsafe_target", "unsupported"), Map.entry("error.alterant.chest_load", "linked_load"),
            Map.entry("error.alterant.linked_load", "linked_load"), Map.entry("error.alterant.linked_removal_load", "linked_load"),
            Map.entry("error.alterant.chest_incomplete", "linked_load"), Map.entry("error.alterant.tool_changed", "changed"),
            Map.entry("error.alterant.integration_changed", "changed"), Map.entry("error.alterant.target_changed", "changed"),
            Map.entry("error.alterant.remove_integrated_first", "remove_first"),
            Map.entry("error.alterant.solvent_charges", "solvent"), Map.entry("error.alterant.solvent_bottle", "solvent"),
            Map.entry("error.alterant.solvent_changed", "changed"), Map.entry("error.alterant.dispenser_changed", "changed"),
            Map.entry("error.alterant.growth_stage_unsupported", "unsupported"), Map.entry("error.alterant.growth_height_unsupported", "unsupported"),
            Map.entry("error.alterant.growth_stage_limit", "growth_limit"), Map.entry("error.alterant.growth_height_limit", "growth_limit"),
            Map.entry("error.alterant.growth_past_limit", "growth_past"), Map.entry("error.alterant.growth_root", "growth_root"),
            Map.entry("error.alterant.remove_tick_routes_first", "remove_first"),
            Map.entry("error.alterant.remove_formulation_first", "remove_first")
    );
    private ToolFeedback() {}
    public static void failure(ServerPlayer player, PreservationService.Result result) {
        int now = player.level().getServer().getTickCount(); var previous = LAST_FAILURE.get(player);
        if (previous != null && now >= previous && now - previous < 10) { return; }
        LAST_FAILURE.put(player, now);
        Component message = result.message();
        if (message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents retained
                && retained.getKey().equals("result.alterant.coating_retained") && retained.getArgs().length == 1 && retained.getArgs()[0] instanceof Component cause) { message = cause; }
        String reason = message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated ? translated.getKey() : "";
        player.sendOverlayMessage(Component.translatable("feedback.alterant." + REASONS.getOrDefault(reason, "unavailable")).withColor(0xE1BD84));
    }
    public static void scrape(ServerPlayer player, BlockPos pos, Direction face, ResidueFamily family) {
        var sound = switch (family) {
            case DRIED_COMPOUND -> SoundEvents.AXE_SCRAPE;
            case SEALANT_SCRAP -> SoundEvents.SLIME_BLOCK_HIT;
            case LATTICE_FRAGMENTS, CHRONAL_DROSS -> SoundEvents.AMETHYST_BLOCK_HIT;
        };
        player.level().playSound(null, pos, sound, SoundSource.BLOCKS, .35F, family == ResidueFamily.CHRONAL_DROSS ? .8F : 1.2F);
        particles(player, pos, face, family.item(), 4);
    }
    public static void mask(ServerPlayer player, BlockPos pos, Direction face) {
        player.level().playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, .3F, 1.1F);
        particles(player, pos, face, AlterantItems.MASKING_STRIPS.get(), 2);
    }
    public static void solvent(net.minecraft.server.level.ServerLevel level, BlockPos pos, Direction face) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .2F, 1.6F);
        var point = com.deisdev.alterant.engine.SurfaceTargets.hit(pos, face).getLocation();
        level.sendParticles(ParticleTypes.WAX_OFF, point.x + face.getStepX() * .08, point.y + face.getStepY() * .08,
                point.z + face.getStepZ() * .08, 4, .15, .15, .15, .01);
    }
    private static void particles(ServerPlayer player, BlockPos pos, Direction face, net.minecraft.world.item.Item item, int count) {
        var point = com.deisdev.alterant.engine.SurfaceTargets.hit(pos, face).getLocation();
        player.level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item), point.x + face.getStepX() * .08,
                point.y + face.getStepY() * .08, point.z + face.getStepZ() * .08, count, .08, .08, .08, .015);
    }
}
