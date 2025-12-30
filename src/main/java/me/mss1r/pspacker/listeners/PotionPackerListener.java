package me.mss1r.pspacker.listeners;

import me.mss1r.pspacker.PotionPackerPlugin;
import me.mss1r.pspacker.util.PotionStackUtil;
import me.mss1r.pspacker.util.SchedulerUtil;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PotionPackerListener implements Listener {

    private final PotionPackerPlugin plugin;

    // Debounce: at most one full normalize per tick per player
    private final Set<UUID> scheduledFullNormalize = ConcurrentHashMap.newKeySet();

    // Debounce: at most one slot normalize per tick per player (last slot wins)
    private final Set<UUID> scheduledSlotNormalize = ConcurrentHashMap.newKeySet();
    private final Map<UUID, AtomicInteger> pendingRawSlot = new ConcurrentHashMap<>();

    public PotionPackerListener(PotionPackerPlugin plugin) {
        this.plugin = plugin;
    }

    // Brewing stand must not be modified
    private boolean isBrewing(Inventory inv) {
        return inv != null && inv.getType() == InventoryType.BREWING;
    }

    private int viewSlotLimit(InventoryView view) {
        if (view == null) return 0;
        int top = view.getTopInventory() != null ? view.getTopInventory().getSize() : 0;
        int bottom = view.getBottomInventory() != null ? view.getBottomInventory().getSize() : 0;
        return top + bottom;
    }

    // Stored as strings for cross-version safety (enum values differ between MC versions)
    private static final Set<String> MASS_ACTIONS = Set.of(
            "MOVE_TO_OTHER_INVENTORY",
            "COLLECT_TO_CURSOR",
            "HOTBAR_SWAP",
            "CLONE_STACK",

            "PICKUP_FROM_BUNDLE",
            "PICKUP_ALL_INTO_BUNDLE",
            "PICKUP_SOME_INTO_BUNDLE",
            "PLACE_FROM_BUNDLE",
            "PLACE_ALL_INTO_BUNDLE",
            "PLACE_SOME_INTO_BUNDLE"
    );

    private boolean isMassAction(InventoryAction a, ClickType click) {
        if (a == null) return false;
        if (click == ClickType.DOUBLE_CLICK) return true;
        return MASS_ACTIONS.contains(a.name());
    }

    private void normalizeViewComponentsOnly(Player p) {
        InventoryView live = p.getOpenInventory();
        if (live == null) return;

        if (!isBrewing(live.getTopInventory())) {
            PotionStackUtil.normalizeInventoryComponentsOnly(plugin, live.getTopInventory());
        }
        PotionStackUtil.normalizeInventoryComponentsOnly(plugin, live.getBottomInventory());

        ItemStack cursor = p.getItemOnCursor();
        if (PotionStackUtil.applyComponent(plugin, cursor)) {
            p.setItemOnCursor(cursor);
        }
    }

    private void scheduleFullNormalizeOnce(Player p) {
        UUID id = p.getUniqueId();
        if (!scheduledFullNormalize.add(id)) return;

        SchedulerUtil.runAtEntity(plugin, p, () -> {
            try {
                normalizeViewComponentsOnly(p);
            } finally {
                scheduledFullNormalize.remove(id);
            }
        });
    }

    private void scheduleSlotNormalize(Player p, int rawSlot) {
        UUID id = p.getUniqueId();
        if (scheduledFullNormalize.contains(id)) return;

        pendingRawSlot.computeIfAbsent(id, __ -> new AtomicInteger(rawSlot)).set(rawSlot);
        if (!scheduledSlotNormalize.add(id)) return;

        SchedulerUtil.runAtEntity(plugin, p, () -> {
            try {
                if (scheduledFullNormalize.contains(id)) return;

                InventoryView live = p.getOpenInventory();
                if (live == null) return;

                int slot = pendingRawSlot.getOrDefault(id, new AtomicInteger(-1)).get();
                int limit = viewSlotLimit(live);
                if (slot < 0 || slot >= limit) return;

                int topSize = live.getTopInventory().getSize();

                // Never touch brewing stand slots; only normalize cursor in this case
                if (isBrewing(live.getTopInventory()) && slot < topSize) {
                    ItemStack cursor = p.getItemOnCursor();
                    if (PotionStackUtil.applyComponent(plugin, cursor)) {
                        p.setItemOnCursor(cursor);
                    }
                    return;
                }

                if (slot < topSize) {
                    ItemStack it = live.getTopInventory().getItem(slot);
                    if (PotionStackUtil.applyComponent(plugin, it)) {
                        live.getTopInventory().setItem(slot, it);
                    }
                } else {
                    int bottomSlot = slot - topSize;
                    ItemStack it = live.getBottomInventory().getItem(bottomSlot);
                    if (PotionStackUtil.applyComponent(plugin, it)) {
                        live.getBottomInventory().setItem(bottomSlot, it);
                    }
                }

                ItemStack cursor = p.getItemOnCursor();
                if (PotionStackUtil.applyComponent(plugin, cursor)) {
                    p.setItemOnCursor(cursor);
                }
            } finally {
                scheduledSlotNormalize.remove(id);
                pendingRawSlot.remove(id);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        scheduledFullNormalize.remove(id);
        scheduledSlotNormalize.remove(id);
        pendingRawSlot.remove(id);
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        scheduledFullNormalize.remove(id);
        scheduledSlotNormalize.remove(id);
        pendingRawSlot.remove(id);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        SchedulerUtil.runAtEntity(plugin, p,
                () -> PotionStackUtil.normalizeInventoryComponentsOnly(plugin, p.getInventory()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        scheduleFullNormalizeOnce(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (isMassAction(e.getAction(), e.getClick())) {
            scheduleFullNormalizeOnce(p);
        } else {
            scheduleSlotNormalize(p, e.getRawSlot());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        scheduleFullNormalizeOnce(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreative(InventoryCreativeEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        scheduleFullNormalizeOnce(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Item ent = e.getItemDrop();
        ItemStack st = ent.getItemStack();
        if (PotionStackUtil.applyComponent(plugin, st)) ent.setItemStack(st);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Item ent = e.getItem();
        ItemStack st = ent.getItemStack();
        if (PotionStackUtil.applyComponent(plugin, st)) ent.setItemStack(st);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        Item ent = e.getEntity();
        ItemStack st = ent.getItemStack();
        if (PotionStackUtil.applyComponent(plugin, st)) ent.setItemStack(st);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDrop(EntityDropItemEvent e) {
        Item ent = e.getItemDrop();
        ItemStack st = ent.getItemStack();
        if (PotionStackUtil.applyComponent(plugin, st)) ent.setItemStack(st);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent e) {
        ItemStack item = e.getItem();
        if (item == null || item.getType().isAir()) return;

        if (PotionStackUtil.applyComponent(plugin, item)) e.setItem(item);
        if (!PotionStackUtil.isPotionLike(item.getType())) return;

        Inventory dst = e.getDestination();
        Location loc = (e.getSource() != null ? e.getSource().getLocation() : null);
        if (loc == null && dst != null) loc = dst.getLocation();

        Location finalLoc = loc;
        SchedulerUtil.runAtLocation(plugin, finalLoc,
                () -> PotionStackUtil.normalizeInventoryComponentsOnly(plugin, dst));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent e) {
        Item ent = e.getItem();
        ItemStack st = ent.getItemStack();
        if (PotionStackUtil.applyComponent(plugin, st)) ent.setItemStack(st);

        if (!PotionStackUtil.isPotionLike(st.getType())) return;

        Inventory inv = e.getInventory();
        Location loc = inv != null ? inv.getLocation() : null;

        SchedulerUtil.runAtLocation(plugin, loc,
                () -> PotionStackUtil.normalizeInventoryComponentsOnly(plugin, inv));
    }
}
