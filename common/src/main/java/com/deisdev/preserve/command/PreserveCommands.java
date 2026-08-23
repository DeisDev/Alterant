package com.deisdev.preserve.command;

import com.deisdev.preserve.engine.PreservationService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;

/** Coordinate commands require loaded targets. Whole-world cleanup requires an explicit start after backup. */
public final class PreserveCommands {
    private PreserveCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("preserve")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("prepare-uninstall")
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.literal("Back up the world first. Then run /preserve prepare-uninstall start. Cleanup loads treated areas and their neighbors across every dimension; use status or cancel to manage it. /preserve thaw only removes a loaded target."), false);
                            return 1;
                        })
                        .then(Commands.literal("start").executes(context -> {
                            var source = context.getSource();
                            var job = com.deisdev.preserve.engine.CleanupJob.get(source.getServer());
                            var player = source.getPlayer();
                            boolean started = job.start(player == null ? null : player.getUUID());
                            source.sendSuccess(() -> Component.literal(job.status() + " Use /preserve prepare-uninstall status to check progress."), false);
                            return started ? 1 : 0;
                        }))
                        .then(Commands.literal("status").executes(context -> {
                            var job = com.deisdev.preserve.engine.CleanupJob.get(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.literal(job.status()), false);
                            return job.phase() == com.deisdev.preserve.engine.CleanupJob.Phase.COMPLETE ? 1 : 0;
                        }))
                        .then(Commands.literal("cancel").executes(context -> {
                            var job = com.deisdev.preserve.engine.CleanupJob.get(context.getSource().getServer());
                            job.cancel();
                            context.getSource().sendSuccess(() -> Component.literal(job.status()), false);
                            return 1;
                        })))
                .then(Commands.literal("inspect").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                    var source = context.getSource();
                    var pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                    var report = com.deisdev.preserve.api.PreserveApi.inspect(source.getLevel(), pos, com.deisdev.preserve.api.Formulation.TEMPORAL_STASIS);
                    var treatment = PreservationService.get(source.getLevel()).store().get(pos.asLong());
                    source.sendSuccess(() -> Component.literal(report.formulation().getSerializedName() + " at " + pos.toShortString()
                            + "; " + report.coverage() + (report.treated() ? "; coated" : "; untreated")
                            + "; positions: " + report.positions() + "; routes: " + report.actions() + "; profiles: " + report.profiles()
                            + "; adapters: " + report.adapters() + "; retained work: " + (treatment == null ? 0 : treatment.deferred().size())
                            + (report.reason().isEmpty() ? "" : "; " + report.reason()) + "; " + String.join("; ", report.limitations())), false);
                    return report.applicable() ? 1 : 0;
                })))
                .then(Commands.literal("freeze").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                    var source = context.getSource();
                    var result = PreservationService.get(source.getLevel()).applyTemporal(
                            BlockPosArgument.getLoadedBlockPos(context, "pos"), source.getTextName(), false);
                    return report(source, result);
                })))
                .then(Commands.literal("thaw").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                    var source = context.getSource();
                    return report(source, PreservationService.get(source.getLevel()).remove(BlockPosArgument.getLoadedBlockPos(context, "pos")));
                }))));
    }

    private static int report(CommandSourceStack source, PreservationService.Result result) {
        if (result.changed()) { source.sendSuccess(() -> Component.literal(result.message()), true); }
        else { source.sendFailure(Component.literal(result.message())); }
        return result.changed() ? 1 : 0;
    }
}
