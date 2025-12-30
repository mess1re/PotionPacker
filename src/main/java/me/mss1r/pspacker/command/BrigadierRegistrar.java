package me.mss1r.pspacker.command;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mss1r.pspacker.PotionPackerPlugin;
import me.mss1r.pspacker.util.PotionPackerMessages;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class BrigadierRegistrar {

    private final PotionPackerPlugin plugin;
    private final PotionPackerMessages msg;

    public BrigadierRegistrar(PotionPackerPlugin plugin, PotionPackerMessages msg) {
        this.plugin = plugin;
        this.msg = msg;
    }

    public void register(Commands commands) {
        var root = Commands.literal("potionpacker")
                .executes(ctx -> {
                    sender(ctx).sendMessage(msg.c("messages.help_header"));
                    for (String line : plugin.getConfig().getStringList("messages.help")) {
                        sender(ctx).sendMessage(msg.cRaw(line));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .requires(src -> src.getSender().hasPermission("potionpacker.reload"))
                        .executes(ctx -> {
                            plugin.reloadLocalConfig();
                            sender(ctx).sendMessage(msg.c("messages.reloaded"));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();

        commands.register(root, "PotionPacker command", List.of("pp"));
    }

    private static CommandSender sender(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }
}
