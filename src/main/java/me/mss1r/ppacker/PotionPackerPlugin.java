package me.mss1r.ppacker;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.mss1r.ppacker.command.BrigadierRegistrar;
import me.mss1r.ppacker.listeners.PotionPackerListener;
import me.mss1r.ppacker.util.PotionPackerMessages;
import me.mss1r.ppacker.util.StackProfileResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class PotionPackerPlugin extends JavaPlugin {

    private int potionSize;
    private int splashSize;
    private int lingeringSize;

    private boolean profilesEnabled;
    private StackProfileResolver resolver;

    private PotionPackerMessages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();

        this.messages = new PotionPackerMessages(this);

        getServer().getPluginManager().registerEvents(new PotionPackerListener(this), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            new BrigadierRegistrar(this, messages).register(commands);
        });
    }

    public void reloadLocalConfig() {
        reloadConfig();

        this.profilesEnabled = getConfig().getBoolean("profiles.enabled", false);

        this.potionSize = clamp(getConfig().getInt("stack_sizes.potion", 16));
        this.splashSize = clamp(getConfig().getInt("stack_sizes.splash_potion", 16));
        this.lingeringSize = clamp(getConfig().getInt("stack_sizes.lingering_potion", 16));

        if (profilesEnabled) {
            this.resolver = new StackProfileResolver(getConfig());
            this.resolver.invalidateAll();
        } else {
            this.resolver = null;
        }
    }

    private int clamp(int v) {
        if (v < 1) return 1;
        return Math.min(v, 64);
    }

    public boolean profilesEnabled() {
        return profilesEnabled;
    }

    public void invalidateProfileCache(UUID uuid) {
        if (resolver != null) resolver.invalidate(uuid);
    }

    public void invalidateAllProfileCache() {
        if (resolver != null) resolver.invalidateAll();
    }

    public int desiredSize(Player player, Material m) {
        if (resolver != null) {
            return resolver.desired(m, resolver.sizesFor(player));
        }
        return switch (m) {
            case POTION -> potionSize;
            case SPLASH_POTION -> splashSize;
            case LINGERING_POTION -> lingeringSize;
            default -> m.getMaxStackSize();
        };
    }

    public int desiredSizeDefault(Material m) {
        if (resolver != null) {
            return resolver.desired(m, resolver.defaultSizes());
        }
        return switch (m) {
            case POTION -> potionSize;
            case SPLASH_POTION -> splashSize;
            case LINGERING_POTION -> lingeringSize;
            default -> m.getMaxStackSize();
        };
    }

    public PotionPackerMessages messages() { return messages; }
}
