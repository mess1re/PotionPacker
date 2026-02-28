package me.mss1r.pspacker.util;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mss1r.pspacker.PotionPackerPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class PotionStackUtil {
    private PotionStackUtil() {}

    public static boolean isPotionLike(Material m) {
        return m == Material.POTION
                || m == Material.SPLASH_POTION
                || m == Material.LINGERING_POTION;
    }

    private static boolean applyMaxStack(ItemStack stack, int desired, int vanilla) {
        if (desired <= 0 || desired == vanilla) {
            Integer cur = stack.getData(DataComponentTypes.MAX_STACK_SIZE);
            if (cur != null) {
                stack.resetData(DataComponentTypes.MAX_STACK_SIZE);
                return true;
            }
            return false;
        }

        Integer cur = stack.getData(DataComponentTypes.MAX_STACK_SIZE);
        if (cur == null || cur != desired) {
            stack.setData(DataComponentTypes.MAX_STACK_SIZE, desired);
            return true;
        }
        return false;
    }

    public static boolean forceMaxStackToAmount(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        int amount = stack.getAmount();
        if (amount <= 0) return false;

        Integer cur = stack.getData(DataComponentTypes.MAX_STACK_SIZE);
        if (cur == null || cur < amount) {
            stack.setData(DataComponentTypes.MAX_STACK_SIZE, amount);
            return true;
        }
        return false;
    }

    public static boolean applyComponent(PotionPackerPlugin plugin, Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        Material m = stack.getType();
        if (!isPotionLike(m)) return false;

        return applyMaxStack(stack, plugin.desiredSize(player, m), m.getMaxStackSize());
    }

    public static boolean applyComponentDefault(PotionPackerPlugin plugin, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        Material m = stack.getType();
        if (!isPotionLike(m)) return false;

        return applyMaxStack(stack, plugin.desiredSizeDefault(m), m.getMaxStackSize());
    }

    public static void normalizeInventoryComponentsOnly(PotionPackerPlugin plugin, Player player, Inventory inv) {
        if (inv == null) return;
        if (inv.getType() == InventoryType.BREWING) return;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            if (!isPotionLike(it.getType())) continue;

            if (applyComponent(plugin, player, it)) inv.setItem(i, it);
        }
    }

    public static void normalizeInventoryComponentsOnlyDefault(PotionPackerPlugin plugin, Inventory inv) {
        if (inv == null) return;
        if (inv.getType() == InventoryType.BREWING) return;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            if (!isPotionLike(it.getType())) continue;

            if (applyComponentDefault(plugin, it)) inv.setItem(i, it);
        }
    }

    public static boolean normalizeCarryForPlayer(PotionPackerPlugin plugin, Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (!isPotionLike(stack.getType())) return false;

        return applyComponent(plugin, player, stack);
    }

    public static boolean normalizeEntityForPlayer(PotionPackerPlugin plugin, Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (!isPotionLike(stack.getType())) return false;

        int desired = plugin.desiredSize(player, stack.getType());
        if (desired > 0 && stack.getAmount() > desired) {
            return forceMaxStackToAmount(stack);
        }
        return applyComponent(plugin, player, stack);
    }

    public static boolean normalizeEntityDefault(PotionPackerPlugin plugin, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (!isPotionLike(stack.getType())) return false;

        int desired = plugin.desiredSizeDefault(stack.getType());
        if (desired > 0 && stack.getAmount() > desired) {
            return forceMaxStackToAmount(stack);
        }
        return applyComponentDefault(plugin, stack);
    }
}
