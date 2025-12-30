package me.mss1r.pspacker.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class SchedulerUtil {
    private SchedulerUtil() {}

    private static final boolean FOLIA = hasClass("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");

    private static boolean hasClass(String name) {
        try { Class.forName(name); return true; } catch (Throwable t) { return false; }
    }

    public static void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        if (entity == null) return;

        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, task);
            return;
        }
        entity.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    public static void runAtLocation(Plugin plugin, Location loc, Runnable task) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, task);
            return;
        }

        if (loc == null || loc.getWorld() == null) {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduled -> task.run());
            return;
        }

        Bukkit.getRegionScheduler().run(plugin, loc, scheduled -> task.run());
    }
}
