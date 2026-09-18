package net.kpkh_buyer.economy;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kpkh_buyer.EconomyScreenHandler;
import net.kpkh_buyer.economy.net.EconomyNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

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
                            Component.literal("Баланс: " + EconomyManager.format(balance)), false);
                        return 1;
                    })
            );

            // /pay <ник> <сумма>
            dispatcher.register(
                Commands.literal("pay")
                    .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> {
                                ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                String targetName = StringArgumentType.getString(ctx, "target");
                                double amount = DoubleArgumentType.getDouble(ctx, "amount");

                                UUID targetUuid = EconomyManager.resolveByName(targetName);

                                if (sender.getUUID().equals(targetUuid)) {
                                    ctx.getSource().sendFailure(
                                        Component.literal("Нельзя переводить самому себе"));
                                    return 0;
                                }

                                if (!EconomyManager.transfer(sender.getUUID(), targetUuid, amount, "pay command")) {
                                    ctx.getSource().sendFailure(
                                        Component.literal("Недостаточно средств"));
                                    return 0;
                                }

                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Переведено " + EconomyManager.format(amount)
                                    + " игроку " + targetName), false);

                                ServerPlayer target = ctx.getSource().getServer()
                                    .getPlayerList().getPlayerByName(targetName);
                                if (target != null) {
                                    target.sendSystemMessage(Component.literal(
                                        "Получено " + EconomyManager.format(amount)
                                        + " от " + sender.getName().getString()));
                                }
                                return 1;
                            })))
            );

            // /eco — открывает GUI экономики (только отправляет пакет, клиент сам открывает экран)
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
}