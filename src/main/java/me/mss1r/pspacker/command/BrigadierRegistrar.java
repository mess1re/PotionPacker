package me.mss1r.pspacker.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mss1r.pspacker.PotionPackerPlugin;
import me.mss1r.pspacker.util.PotionPackerMessages;
import me.mss1r.pspacker.util.PotionStackUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class BrigadierRegistrar {

    private final PotionPackerPlugin plugin;
    private final PotionPackerMessages msg;

    public BrigadierRegistrar(PotionPackerPlugin plugin, PotionPackerMessages msg) {
        this.plugin = plugin;
        this.msg = msg;
    }

    public void register(Commands commands) {
        LiteralCommandNode<CommandSourceStack> root = buildRoot();

        List<String> aliases = loadAliases();
        commands.register(root, "PotionPacker command", aliases);
    }

    private List<String> loadAliases() {
        List<String> raw = plugin.getConfig().getStringList("command.aliases");
        if (raw == null) raw = List.of();

        var out = new LinkedHashSet<String>();
        for (String a : raw) {
            if (a == null) continue;
            String alias = a.trim();
            if (alias.isEmpty()) continue;

            if (alias.equalsIgnoreCase("potionpacker")) continue;

            if (alias.indexOf(' ') >= 0) continue;

            out.add(alias);
        }

        if (out.isEmpty()) out.add("pp");
        return new ArrayList<>(out);
    }

    private LiteralCommandNode<CommandSourceStack> buildRoot() {
        return Commands.literal("potionpacker")
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

                            for (var p : plugin.getServer().getOnlinePlayers()) {
                                plugin.invalidateProfileCache(p.getUniqueId());
                                PotionStackUtil.normalizeInventoryComponentsOnly(plugin, p, p.getInventory());
                            }

                            sender(ctx).sendMessage(msg.c("messages.reloaded"));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    private static CommandSender sender(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }
}
