package me.mss1r.ppacker.util;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class InventorySyncUtil {
    private InventorySyncUtil() {}

    @SuppressWarnings("deprecation")
    public static void setEventCursor(InventoryClickEvent event, ItemStack stack) {
        // The cursor must be normalized before Bukkit applies the click/drop action.
        event.setCursor(stack);
    }

    public static void refresh(Player player) {
        player.updateInventory();
    }
}
