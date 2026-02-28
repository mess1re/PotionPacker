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

    private final Set<UUID> scheduledFullNormalize = ConcurrentHashMap.newKeySet();
    private final Set<UUID> scheduledSlotNormalize = ConcurrentHashMap.newKeySet();
    private final Map<UUID, AtomicInteger> pendingRawSlot = new ConcurrentHashMap<>();

    public PotionPackerListener(PotionPackerPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isBrewing(Inventory inv) {
        return inv != null && inv.getType() == InventoryType.BREWING;
    }

    private int viewSlotLimit(InventoryView view) {
        if (view == null) return 0;
        return view.countSlots();
    }

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

        if (!isBrewing(live.getTopInventory())) {
            PotionStackUtil.normalizeInventoryComponentsOnly(plugin, p, live.getTopInventory());
        }
        PotionStackUtil.normalizeInventoryComponentsOnly(plugin, p, live.getBottomInventory());

        // cursor = carry
        ItemStack cursor = p.getItemOnCursor();
        if (PotionStackUtil.normalizeCarryForPlayer(plugin, p, cursor)) {
            p.setItemOnCursor(cursor);
        }

        normalizeCreativeCarry(p);
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

                int slot = pendingRawSlot.getOrDefault(id, new AtomicInteger(-1)).get();
                int limit = viewSlotLimit(live);
                if (slot < 0 || slot >= limit) return;

                int topSize = live.getTopInventory().getSize();

                if (isBrewing(live.getTopInventory()) && slot < topSize) {
                    ItemStack cursor = p.getItemOnCursor();
                    if (PotionStackUtil.normalizeCarryForPlayer(plugin, p, cursor)) {
                        p.setItemOnCursor(cursor);
                    }
                    return;
                }

                if (slot < topSize) {
                    ItemStack it = live.getTopInventory().getItem(slot);
                    if (PotionStackUtil.normalizeCarryForPlayer(plugin, p, it)) {
                        live.getTopInventory().setItem(slot, it);
                    }
                } else {
                    int bottomSlot = slot - topSize;
                    ItemStack it = live.getBottomInventory().getItem(bottomSlot);
                    if (PotionStackUtil.normalizeCarryForPlayer(plugin, p, it)) {
                        live.getBottomInventory().setItem(bottomSlot, it);
                    }
                }

                ItemStack cursor = p.getItemOnCursor();
                if (PotionStackUtil.normalizeCarryForPlayer(plugin, p, cursor)) {
                    p.setItemOnCursor(cursor);
                }

                normalizeCreativeCarry(p);
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
        plugin.invalidateProfileCache(id);
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        scheduledFullNormalize.remove(id);
        scheduledSlotNormalize.remove(id);
        pendingRawSlot.remove(id);
        plugin.invalidateProfileCache(id);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        plugin.invalidateProfileCache(p.getUniqueId());

        SchedulerUtil.runAtEntity(plugin, p,
                () -> PotionStackUtil.normalizeInventoryComponentsOnly(plugin, p, p.getInventory()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        scheduleFullNormalizeOnce(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (isMassAction(e.getAction(), e.getClick())) scheduleFullNormalizeOnce(p);
        else scheduleSlotNormalize(p, e.getRawSlot());

        preValidateDropIfOverstack(e, p);
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
        preValidateDropIfOverstack(e, p);
    }

    // drop -> entity
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        Item ent = e.getItemDrop();
        ItemStack st = ent.getItemStack();
        if (st == null || st.getType().isAir()) return;

        if (PotionStackUtil.normalizeEntityForPlayer(plugin, p, st)) {
            ent.setItemStack(st);
        }
    }

    // pickup -> entity
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Item ent = e.getItem();
        ItemStack st = ent.getItemStack();

        if (PotionStackUtil.normalizeEntityForPlayer(plugin, p, st)) {
            ent.setItemStack(st);
        }
    }

    // spawn -> entity default
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        Item ent = e.getEntity();
        ItemStack st = ent.getItemStack();
        if (st == null || st.getType().isAir()) return;

        if (PotionStackUtil.normalizeEntityDefault(plugin, st)) {
            ent.setItemStack(st);
        }
    }

    // entity drop -> entity default
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDrop(EntityDropItemEvent e) {
        Item ent = e.getItemDrop();
        ItemStack st = ent.getItemStack();
        if (st == null || st.getType().isAir()) return;

        if (PotionStackUtil.normalizeEntityDefault(plugin, st)) {
            ent.setItemStack(st);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent e) {
        ItemStack item = e.getItem();
        if (item == null || item.getType().isAir()) return;

        if (PotionStackUtil.normalizeEntityDefault(plugin, item)) {
            e.setItem(item);
        }
        if (!PotionStackUtil.isPotionLike(item.getType())) return;

        Inventory dst = e.getDestination();
        Location loc = (e.getSource() != null ? e.getSource().getLocation() : null);
        if (loc == null && dst != null) loc = dst.getLocation();

        Location finalLoc = loc;
        SchedulerUtil.runAtLocation(plugin, finalLoc,
                () -> PotionStackUtil.normalizeInventoryComponentsOnlyDefault(plugin, dst));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent e) {
        Item ent = e.getItem();
        ItemStack st = ent.getItemStack();
        if (PotionStackUtil.normalizeEntityDefault(plugin, st)) {
            ent.setItemStack(st);
        }

        if (!PotionStackUtil.isPotionLike(st.getType())) return;

        Inventory inv = e.getInventory();
        Location loc = inv != null ? inv.getLocation() : null;

        SchedulerUtil.runAtLocation(plugin, loc,
                () -> PotionStackUtil.normalizeInventoryComponentsOnlyDefault(plugin, inv));
    }

    private void preValidateDropIfOverstack(InventoryClickEvent e, Player p) {
        if (e.getRawSlot() == -999) {
            ItemStack cursor = e.getCursor();
            if (cursor == null || cursor.getType().isAir()) return;

            if (PotionStackUtil.normalizeEntityForPlayer(plugin, p, cursor)) {
                e.setCursor(cursor);
                SchedulerUtil.runAtEntity(plugin, p, p::updateInventory);
            }
            return;
        }

        boolean isDropClick =
                e.getClick() == ClickType.DROP ||
                        e.getClick() == ClickType.CONTROL_DROP ||
                        e.getAction() == InventoryAction.DROP_ALL_SLOT ||
                        e.getAction() == InventoryAction.DROP_ONE_SLOT ||
                        e.getAction() == InventoryAction.DROP_ALL_CURSOR ||
                        e.getAction() == InventoryAction.DROP_ONE_CURSOR;

        if (!isDropClick) return;

        ItemStack target = switch (e.getAction()) {
            case DROP_ALL_CURSOR, DROP_ONE_CURSOR -> e.getCursor();
            default -> e.getCurrentItem();
        };

        if (target == null || target.getType().isAir()) return;
        if (!PotionStackUtil.isPotionLike(target.getType())) return;

        boolean isCursorDrop =
                e.getAction() == InventoryAction.DROP_ALL_CURSOR
                        || e.getAction() == InventoryAction.DROP_ONE_CURSOR;

        if (PotionStackUtil.normalizeEntityForPlayer(plugin, p, target)) {
            if (isCursorDrop) e.setCursor(target);
            else e.setCurrentItem(target);

            SchedulerUtil.runAtEntity(plugin, p, () -> {
                p.updateInventory();
                scheduleFullNormalizeOnce(p);
            });
        }
    }

    private void normalizeCreativeCarry(Player p) {
        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) return;

        var inv = p.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (PotionStackUtil.normalizeCarryForPlayer(plugin, p, st)) {
                inv.setItem(i, st);
            }
        }

        ItemStack cursor = p.getItemOnCursor();
        if (PotionStackUtil.normalizeCarryForPlayer(plugin, p, cursor)) {
            p.setItemOnCursor(cursor);
        }
    }
}