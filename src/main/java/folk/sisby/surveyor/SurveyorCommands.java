package folk.sisby.surveyor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import folk.sisby.surveyor.util.TextUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class SurveyorCommands {
    private static final Multimap<UUID, UUID> requests = HashMultimap.create();

    public static int info(ServerSummary serverSummary, ServerPlayerEntity player, SurveyorExploration exploration, String ignored, Consumer<Text> feedback) {
        Set<PlayerSummary> group = serverSummary.groupPlayers(player.getUuid(), player.getServer());
        feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("---Map Exploration Summary---").formatted(Formatting.GRAY)));
        feedback.accept(
            Text.literal("You've explored ").formatted(Formatting.AQUA)
                .append(Text.literal("%d".formatted(exploration.chunkCount())).formatted(Formatting.WHITE))
                .append(Text.literal(" total chunks! (").formatted(Formatting.AQUA))
                .append(Text.literal("%d".formatted(group.stream().mapToInt(p -> p.exploration().chunkCount()).sum() /* Wrong, obvs - merged explore impl class later. */)).formatted(Formatting.WHITE))
                .append(Text.literal(" with friends)").formatted(Formatting.AQUA))
        );
        feedback.accept(
            Text.literal("You've explored ").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal("%d".formatted(exploration.structureCount())).formatted(Formatting.WHITE))
                .append(Text.literal(" structures! (").formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal("%d".formatted(group.stream().mapToInt(p -> p.exploration().structureCount()).sum() /* Wrong, obvs - merged explore impl class later. */)).formatted(Formatting.WHITE))
                .append(Text.literal(" with friends)").formatted(Formatting.LIGHT_PURPLE))
        );
        feedback.accept(
            Text.literal("You're sharing your map with ").formatted(Formatting.GOLD)
                .append(Text.literal("%d".formatted(group.size() - 1)).formatted(Formatting.WHITE))
                .append(Text.literal(" other players:").formatted(Formatting.GOLD))
        );
        feedback.accept(
            TextUtil.highlightStrings(group.stream().map(PlayerSummary::username).toList(), s -> Formatting.WHITE).formatted(Formatting.DARK_PURPLE)
        );
        feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("-------End Summary-------").formatted(Formatting.GRAY)));
        return 1;
    }

    private static int share(ServerSummary serverSummary, ServerPlayerEntity player, SurveyorExploration exploration, String username, Consumer<Text> feedback) {
        ServerPlayerEntity sharePlayer = player.getServer().getPlayerManager().getPlayer(username);
        if (sharePlayer == null) {
            feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("Can't find an online player named ").formatted(Formatting.YELLOW)).append(Text.literal(username).formatted(Formatting.WHITE)).append(Text.literal(".").formatted(Formatting.YELLOW)));
            return 0;
        }
        if (sharePlayer == player) {
            feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("You can't share map exploration with yourself!").formatted(Formatting.YELLOW)));
            return 0;
        }
        if (requests.containsEntry(player.getUuid(), sharePlayer.getUuid())) { // Accept Request
            if (serverSummary.groupSize(player.getUuid()) > 1 /* and other player is also in a group */) {
                feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("You're in a group! leave your group first with:").formatted(Formatting.YELLOW)));
                feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("/surveyor unshare").formatted(Formatting.GOLD)));
                return 0;
            }
            requests.removeAll(player.getUuid()); // clear all other requests
            ServerSummary.of(player.getServer()).joinGroup(player.getUuid(), sharePlayer.getUuid());
            feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("You're now sharing map exploration with ").formatted(Formatting.GREEN)).append(Text.literal("%d".formatted(serverSummary.groupSize(player.getUuid()))).formatted(Formatting.WHITE)).append(Text.literal(" players:").formatted(Formatting.GREEN)));
            feedback.accept(TextUtil.highlightStrings(serverSummary.groupPlayers(player.getUuid(), player.getServer()).stream().map(PlayerSummary::username).toList(), s -> Formatting.WHITE).formatted(Formatting.GOLD));
            return 1;
        } else { // Make Request
            requests.put(sharePlayer.getUuid(), player.getUuid());
            feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("Share request sent to ").formatted(Formatting.GREEN)).append(sharePlayer.getDisplayName().copy().formatted(Formatting.WHITE)).append(Text.literal(".").formatted(Formatting.GREEN)));
            feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("If accepted, they'll share with your group of ").formatted(Formatting.GREEN)).append(Text.literal("%d".formatted(serverSummary.groupSize(player.getUuid()))).formatted(Formatting.WHITE)).append(Text.literal(".").formatted(Formatting.GREEN)));
            sharePlayer.sendMessage(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(player.getDisplayName().copy().formatted(Formatting.WHITE)).append(Text.literal(" wants to share map exploration!").formatted(Formatting.AQUA)));
            sharePlayer.sendMessage(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("To share with their group of ").append(Text.literal("%d".formatted(serverSummary.groupSize(player.getUuid()))).formatted(Formatting.WHITE)).formatted(Formatting.AQUA)).append(Text.literal(", enter:").formatted(Formatting.AQUA)));
            sharePlayer.sendMessage(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("/surveyor share %s".formatted(player.getGameProfile().getName())).formatted(Formatting.GOLD)));
            return 1;
        }
    }

    private static int unshare(ServerSummary serverSummary, ServerPlayerEntity player, SurveyorExploration exploration, String ignored, Consumer<Text> feedback) {
        int shareNumber = serverSummary.groupSize(player.getUuid());
        if (shareNumber < 1) {
            feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("You're not sharing map exploration with anyone!").formatted(Formatting.YELLOW)));
            return 0;
        } else {
            ServerSummary.of(player.getServer()).leaveGroup(player.getUuid());
            feedback.accept(Text.literal("[Surveyor] ").formatted(Formatting.DARK_RED).append(Text.literal("Stopped sharing map exploration with ").formatted(Formatting.GREEN)).append(Text.literal("%d".formatted(shareNumber)).formatted(Formatting.WHITE)).append(Text.literal(" players.").formatted(Formatting.GREEN)));
            return 1;
        }
    }

    public interface SurveyorCommandExecutor {
        int execute(ServerSummary serverSummary, ServerPlayerEntity player, SurveyorExploration exploration, String arg, Consumer<Text> feedback);
    }

    public static int execute(CommandContext<ServerCommandSource> context, String arg, SurveyorCommandExecutor executor) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            Surveyor.LOGGER.error("[Surveyor] Commands cannot be invoked by a non-player");
            return 0;
        }

        SurveyorExploration exploration = SurveyorExploration.of(player);
        try {
            return executor.execute(ServerSummary.of(player.getServer()), player, exploration, arg != null ? context.getArgument(arg, String.class) : null, t -> context.getSource().sendFeedback(() -> t, false));
        } catch (Exception e) {
            context.getSource().sendFeedback(() -> Text.literal("Command failed! Check log for details.").formatted(Formatting.RED), false);
            Surveyor.LOGGER.error("[Surveyor] Error while executing command: {}", context.getInput(), e);
            return 0;
        }
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
            CommandManager.literal("surveyor")
                .then(
                    CommandManager.literal("share").then(
                        CommandManager.argument("player", StringArgumentType.word()).suggests((c, b) -> {
                            c.getSource().getServer().getPlayerManager().getPlayerList().stream().filter(p -> c.getSource().getPlayer() != p).map(p -> p.getGameProfile().getName()).forEach(b::suggest);
                            return b.buildFuture();
                        }).executes(c -> execute(c, "player", SurveyorCommands::share))
                    )
                )
                .then(
                    CommandManager.literal("unshare")
                        .executes(c -> execute(c, null, SurveyorCommands::unshare))
                )
                .executes(c -> execute(c, null, SurveyorCommands::info))
        );
    }
}
