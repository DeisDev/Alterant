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
                    var treatment = PreservationService.get(source.getLevel()).store().get(pos.asLong());
                    if (treatment == null) {
                        source.sendSuccess(() -> Component.literal("No Preserve coating at " + pos.toShortString()), false);
                    } else {
                        source.sendSuccess(() -> Component.literal(treatment.formulation().getSerializedName() + " at " + pos.toShortString()
                                + "; routes: " + treatment.actions() + "; profiles: " + treatment.profiles()
                                + "; retained work: " + treatment.deferred().size()
                                + "; adapters: " + treatment.adapters().stream().map(adapter -> adapter.id().toString()).toList()
                                + (com.deisdev.preserve.integration.IntegrationRegistry.available(treatment.adapters()) ? "" : "; restore missing adapter before thawing")
                                + ". External controllers and absolute-time machines require integration."), false);
                    }
                    return treatment == null ? 0 : 1;
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
