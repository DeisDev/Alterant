package com.deisdev.alterant.command;

import com.deisdev.alterant.engine.PreservationService;
import com.deisdev.alterant.text.AlterantText;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;

/** Coordinate commands require loaded targets. Whole-world cleanup requires an explicit start after backup. */
public final class AlterantCommands {
    private AlterantCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("alterant")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("prepare-uninstall")
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.translatable("commands.alterant.cleanup.help", "/alterant prepare-uninstall start", "/alterant prepare-uninstall status", "/alterant prepare-uninstall cancel", "/alterant thaw"), false);
                            return 1;
                        })
                        .then(Commands.literal("start").executes(context -> {
                            var source = context.getSource();
                            var job = com.deisdev.alterant.engine.CleanupJob.get(source.getServer());
                            var player = source.getPlayer();
                            boolean started = job.start(player == null ? null : player.getUUID());
                            source.sendSuccess(() -> Component.translatable("commands.alterant.cleanup.started", job.status(), "/alterant prepare-uninstall status"), false);
                            return started ? 1 : 0;
                        }))
                        .then(Commands.literal("status").executes(context -> {
                            var job = com.deisdev.alterant.engine.CleanupJob.get(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> job.status(), false);
                            return job.phase() == com.deisdev.alterant.engine.CleanupJob.Phase.COMPLETE ? 1 : 0;
                        }))
                        .then(Commands.literal("cancel").executes(context -> {
                            var job = com.deisdev.alterant.engine.CleanupJob.get(context.getSource().getServer());
                            job.cancel();
                            context.getSource().sendSuccess(() -> job.status(), false);
                            return 1;
                        })))
                .then(Commands.literal("inspect").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                    var source = context.getSource();
                    var pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                    var report = com.deisdev.alterant.api.AlterantApi.inspect(source.getLevel(), pos, com.deisdev.alterant.api.Formulation.TEMPORAL_STASIS);
                    var treatment = PreservationService.get(source.getLevel()).store().get(pos.asLong());
                    source.sendSuccess(() -> net.minecraft.network.chat.CommonComponents.joinLines(
                            Component.translatable("commands.alterant.inspect.target", Component.translatable("item.alterant." + report.formulation().getSerializedName()), pos.toShortString(),
                                    Component.translatable("overlay.alterant.coverage." + report.coverage().name().toLowerCase(java.util.Locale.ROOT)),
                                    Component.translatable(report.treated() ? "commands.alterant.inspect.coated" : "commands.alterant.inspect.untreated")),
                            Component.translatable("commands.alterant.inspect.summary", report.positions(), treatment == null ? 0 : treatment.deferred().size()),
                            Component.translatable("overlay.alterant.protections", AlterantText.list(report.actions().stream().sorted().map(action -> Component.translatable("overlay.alterant.action." + action.getSerializedName())).toList())),
                            Component.translatable("commands.alterant.inspect.profiles", AlterantText.list(report.profiles().stream().map(Component::literal).toList())),
                            Component.translatable("commands.alterant.inspect.adapters", AlterantText.list(report.adapters())),
                            report.reason(), net.minecraft.network.chat.CommonComponents.joinLines(report.limitations())), false);
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
        if (result.changed()) { source.sendSuccess(() -> result.message(), true); }
        else { source.sendFailure(result.message()); }
        return result.changed() ? 1 : 0;
    }
}
