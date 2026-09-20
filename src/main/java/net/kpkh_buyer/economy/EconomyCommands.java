package net.kpkh_buyer.economy;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kpkh_buyer.economy.net.EconomyNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class EconomyCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // /bal — показывает баланс
            dispatcher.register(
                Commands.literal("bal")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        double balance = EconomyManager.getBalance(player.getUUID());
                        ctx.getSource().sendSuccess(() ->
                                Component.literal("Баланс: ")
                                        .append(EconomyManager.formatComponent(balance)), false);
                        return 1;
                    })
            );

            // /pay <ник> <сумма> [комментарий]
            dispatcher.register(
                Commands.literal("pay")
                    .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> doPay(ctx, null))
                            .then(Commands.argument("comment", StringArgumentType.greedyString())
                                .executes(ctx -> doPay(ctx,
                                        StringArgumentType.getString(ctx, "comment")))
                            )
                        )
                    )
            );

            // /eco — открывает GUI экономики
            dispatcher.register(
                Commands.literal("eco")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        EconomyNetworking.sendToPlayer(player);
                        return 1;
                    })
            );
        });
    }

    private static int doPay(CommandContext<CommandSourceStack> ctx, String comment)
            throws CommandSyntaxException {
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(ctx, "target");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");

        UUID targetUuid = EconomyManager.resolveByName(targetName);

        if (sender.getUUID().equals(targetUuid)) {
            ctx.getSource().sendFailure(Component.literal("Нельзя переводить самому себе"));
            return 0;
        }

        if (!EconomyManager.transfer(sender.getUUID(), targetUuid, amount, "pay command", comment)) {
            ctx.getSource().sendFailure(Component.literal("Недостаточно средств"));
            return 0;
        }

        // Сообщение отправителю
        ctx.getSource().sendSuccess(() ->
                Component.literal("Переведено ")
                        .append(EconomyManager.formatComponent(amount))
                        .append(Component.literal(" игроку " + targetName)), false);

        // Сообщение получателю (если онлайн)
        ServerPlayer target = ctx.getSource().getServer()
                .getPlayerList().getPlayerByName(targetName);
        if (target != null) {
            Component msg = Component.literal("Получено ")
                    .append(EconomyManager.formatComponent(amount))
                    .append(Component.literal(" от " + sender.getName().getString()));
            if (comment != null && !comment.isEmpty()) {
                msg = msg.copy().append(Component.literal(" (" + comment + ")"));
            }
            target.sendSystemMessage(msg);
        }
        return 1;
    }
}