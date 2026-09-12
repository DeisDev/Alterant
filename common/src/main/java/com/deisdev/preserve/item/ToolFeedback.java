package com.deisdev.preserve.item;

import com.deisdev.preserve.engine.PreservationService;
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
            Map.entry("This target has no supported item or fluid transfer route", "transfer_unsupported"),
            Map.entry("This block does not support shaping", "unsupported"), Map.entry("Unsupported decorative shape", "unsupported"),
            Map.entry("Preview this shape before committing", "shape_preview"), Map.entry("Hold Structural Stasis offhand to shape this block", "shape_payment"),
            Map.entry("Sample a matching block shape first", "shape_sample"), Map.entry("This shape is unchanged", "shape_same"),
            Map.entry("An entity is in the selected shape", "shape_blocked"), Map.entry("This coating does not preserve the selected shape", "shape_policy"),
            Map.entry("This chunk has reached its preview limit", "limit"), Map.entry("Remove the existing coating first", "remove_first"),
            Map.entry("Already masked", "already_masked"), Map.entry("Masked target", "masked"), Map.entry("No mask to peel", "no_mask"),
            Map.entry("No coating here", "no_coating"), Map.entry("Target is busy", "busy"), Map.entry("Linked target is busy", "busy"),
            Map.entry("Wait for the current transfer to finish", "busy"), Map.entry("Target is not loaded", "range"),
            Map.entry("Target is not loaded or cannot be masked", "unsupported"), Map.entry("You cannot modify this target from here", "permission"),
            Map.entry("This claim does not allow changing preservation", "permission"), Map.entry("Not enough charges for the entire linked target", "charges"),
            Map.entry("This chunk has reached its coating limit", "limit"), Map.entry("This chunk has reached its mask limit", "limit"),
            Map.entry("This target cannot be preserved safely", "unsupported"), Map.entry("Load both halves of the chest first", "linked_load"),
            Map.entry("Load every member of the linked target first", "linked_load"), Map.entry("Load every member of the linked target before removal", "linked_load"),
            Map.entry("Chest connection is incomplete", "linked_load"), Map.entry("The tool or available compound changed", "changed"),
            Map.entry("Target changed during integration validation", "changed"), Map.entry("Target changed during validation", "changed"),
            Map.entry("Remove the integrated or linked coating before switching formulations", "remove_first"),
            Map.entry("Not enough solvent for the entire linked target", "solvent"), Map.entry("Hold a usable solvent bottle", "solvent"),
            Map.entry("The solvent bottle changed", "changed"), Map.entry("The dispenser or solvent changed", "changed"),
            Map.entry("This plant does not support stage regulation", "unsupported"), Map.entry("This plant does not support height regulation", "unsupported"),
            Map.entry("Choose a stage supported by this plant", "growth_limit"), Map.entry("Choose a height supported by this plant", "growth_limit"),
            Map.entry("This plant is already beyond the selected limit", "growth_past"), Map.entry("Apply height regulation at the root", "growth_root"),
            Map.entry("Remove the existing coating before switching its suspended tick routes", "remove_first"),
            Map.entry("Remove the existing coating before switching formulations", "remove_first")
    );
    private ToolFeedback() {}
    public static void failure(ServerPlayer player, PreservationService.Result result) {
        int now = player.level().getServer().getTickCount(); var previous = LAST_FAILURE.get(player);
        if (previous != null && now >= previous && now - previous < 10) { return; }
        LAST_FAILURE.put(player, now);
        String reason = result.message().startsWith("Coating retained: ") ? result.message().substring("Coating retained: ".length()) : result.message();
        player.sendOverlayMessage(Component.translatable("feedback.deisdev." + REASONS.getOrDefault(reason, "unavailable")).withColor(0xE1BD84));
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
        particles(player, pos, face, PreserveItems.MASKING_STRIPS.get(), 2);
    }
    public static void solvent(net.minecraft.server.level.ServerLevel level, BlockPos pos, Direction face) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .2F, 1.6F);
        var point = com.deisdev.preserve.engine.SurfaceTargets.hit(pos, face).getLocation();
        level.sendParticles(ParticleTypes.WAX_OFF, point.x + face.getStepX() * .08, point.y + face.getStepY() * .08,
                point.z + face.getStepZ() * .08, 4, .15, .15, .15, .01);
    }
    private static void particles(ServerPlayer player, BlockPos pos, Direction face, net.minecraft.world.item.Item item, int count) {
        var point = com.deisdev.preserve.engine.SurfaceTargets.hit(pos, face).getLocation();
        player.level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item), point.x + face.getStepX() * .08,
                point.y + face.getStepY() * .08, point.z + face.getStepZ() * .08, count, .08, .08, .08, .015);
    }
}
