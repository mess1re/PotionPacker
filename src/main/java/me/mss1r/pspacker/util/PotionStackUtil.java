package me.mss1r.pspacker.util;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mss1r.pspacker.PotionPackerPlugin;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class PotionStackUtil {
    private PotionStackUtil() {}

    public static boolean isPotionLike(Material m) {
        return m == Material.POTION || m == Material.SPLASH_POTION || m == Material.LINGERING_POTION;
    }

    public static int desiredSize(PotionPackerPlugin plugin, Material m) {
        return switch (m) {
            case POTION -> plugin.getPotionSize();
            case SPLASH_POTION -> plugin.getSplashSize();
            case LINGERING_POTION -> plugin.getLingeringSize();
            default -> 1;
        };
    }

    public static boolean applyComponent(PotionPackerPlugin plugin, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;

        Material m = stack.getType();
        if (!isPotionLike(m)) return false;

        int size = desiredSize(plugin, m);

        // Restore vanilla behavior (potions normally stack to 1)
        if (size <= 1) {
            int cur = stack.getData(DataComponentTypes.MAX_STACK_SIZE);
            if (cur != 1) { // only touch if we actually changed it before
                stack.resetData(DataComponentTypes.MAX_STACK_SIZE);
                return true;
            }
            return false;
        }

        int cur = stack.getData(DataComponentTypes.MAX_STACK_SIZE);
        if (cur != size) {
            stack.setData(DataComponentTypes.MAX_STACK_SIZE, size);
            return true;
        }

        return false;
    }

    public static void normalizeInventoryComponentsOnly(PotionPackerPlugin plugin, Inventory inv) {
        if (inv == null) return;
        if (inv.getType() == InventoryType.BREWING) return;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            if (!isPotionLike(it.getType())) continue;

            if (applyComponent(plugin, it)) inv.setItem(i, it);
        }
    }
}
