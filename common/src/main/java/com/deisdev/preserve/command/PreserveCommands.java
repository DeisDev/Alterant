package com.deisdev.preserve.command;

import com.deisdev.preserve.engine.PreservationService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;

/** Operator diagnostics use loaded coordinates; no command here generates a chunk. */
public final class PreserveCommands {
    private PreserveCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("preserve")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
