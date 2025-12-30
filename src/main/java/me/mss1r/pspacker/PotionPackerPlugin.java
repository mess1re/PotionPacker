package me.mss1r.pspacker;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.mss1r.pspacker.command.BrigadierRegistrar;
import me.mss1r.pspacker.listeners.PotionPackerListener;
import me.mss1r.pspacker.util.PotionPackerMessages;
import org.bukkit.plugin.java.JavaPlugin;

public final class PotionPackerPlugin extends JavaPlugin {

    private int potionSize;
    private int splashSize;
    private int lingeringSize;

    private PotionPackerMessages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();

        this.messages = new PotionPackerMessages(this);

        getServer().getPluginManager().registerEvents(new PotionPackerListener(this), this);

        // Brigadier registration
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            new BrigadierRegistrar(this, messages).register(commands);
        });
    }

    public void reloadLocalConfig() {
        reloadConfig();
        this.potionSize = clamp(getConfig().getInt("stack_sizes.potion", 16));
        this.splashSize = clamp(getConfig().getInt("stack_sizes.splash_potion", 16));
        this.lingeringSize = clamp(getConfig().getInt("stack_sizes.lingering_potion", 16));
    }

    private int clamp(int v) {
        if (v < 1) return 1;
        if (v > 64) return 64;
        return v;
    }

    public int getPotionSize() { return potionSize; }
    public int getSplashSize() { return splashSize; }
    public int getLingeringSize() { return lingeringSize; }

    public PotionPackerMessages messages() { return messages; }
}
