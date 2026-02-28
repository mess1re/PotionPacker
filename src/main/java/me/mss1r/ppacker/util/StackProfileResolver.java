package me.mss1r.ppacker.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StackProfileResolver {

    private static final String ROOT = "stack_profiles";
    private static final String DEFAULT = "default";
    private static final String PERM_PREFIX = "potionpacker.profile.";

    private final Map<String, Profile> profiles;
    private final List<Profile> ordered;
    private final Profile def;


    private final Map<UUID, StackSizes> cache = new ConcurrentHashMap<>();

    public StackProfileResolver(ConfigurationSection cfgRoot) {
        ConfigurationSection root = cfgRoot.getConfigurationSection(ROOT);

        Profile defaultProfile = new Profile(DEFAULT, 0, new StackSizes(16, 16, 16));
        Map<String, Profile> map = new HashMap<>();
        Map<Integer, List<String>> byPriority = new HashMap<>();

        if (root == null) {
            this.def = defaultProfile;
            this.profiles = Map.of(DEFAULT, defaultProfile);
            this.ordered = List.of();
            return;
        }

        ConfigurationSection defSec = root.getConfigurationSection(DEFAULT);
        if (defSec != null) {
            int prio = defSec.getInt("priority", 0);
            int potion = clamp(defSec.getInt("potion", 16));
            int splash = clamp(defSec.getInt("splash_potion", 16));
            int lingering = clamp(defSec.getInt("lingering_potion", 16));

            defaultProfile = new Profile(DEFAULT, prio, new StackSizes(potion, splash, lingering));
        }

        map.put(DEFAULT, defaultProfile);

        for (String key : root.getKeys(false)) {
            if (DEFAULT.equalsIgnoreCase(key)) continue;

            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;

            int prio = sec.getInt("priority", 0);
            StackSizes base = defaultProfile.sizes;

            int potion = clamp(sec.contains("potion") ? sec.getInt("potion") : base.potion());
            int splash = clamp(sec.contains("splash_potion") ? sec.getInt("splash_potion") : base.splash());
            int lingering = clamp(sec.contains("lingering_potion") ? sec.getInt("lingering_potion") : base.lingering());

            map.put(key, new Profile(key, prio, new StackSizes(potion, splash, lingering)));
        }

        this.def = defaultProfile;
        this.profiles = Map.copyOf(map);
        this.ordered = profiles.values().stream()
                .filter(p -> !DEFAULT.equalsIgnoreCase(p.name))
                .sorted(Comparator.comparingInt(Profile::priority).reversed()
                        .thenComparing(p -> p.name.toLowerCase(Locale.ROOT)))
                .toList();
        for (Profile p : ordered) {
            byPriority
                    .computeIfAbsent(p.priority(), __ -> new ArrayList<>())
                    .add(p.name());
        }

        for (var e : byPriority.entrySet()) {
            if (e.getValue().size() > 1) {
                Bukkit.getLogger().warning(
                        "[PotionPacker] Multiple stack profiles share priority "
                                + e.getKey() + ": "
                                + String.join(", ", e.getValue())
                                + ". Alphabetical order will be used."
                );
            }
        }
    }

    private static int clamp(int v) {
        if (v < 1) return 1;
        if (v > 64) return 64;
        return v;
    }

    public void invalidate(UUID uuid) {
        if (uuid != null) cache.remove(uuid);
    }

    public void invalidateAll() {
        cache.clear();
    }

    public StackSizes defaultSizes() {
        return def.sizes;
    }

    public StackSizes sizesFor(Player p) {
        if (p == null) return def.sizes;
        return cache.computeIfAbsent(p.getUniqueId(), __ -> compute(p));
    }

    private StackSizes compute(Player p) {
        for (Profile prof : ordered) {
            String node = PERM_PREFIX + prof.name;
            if (p.isPermissionSet(node) && p.hasPermission(node)) {
                return prof.sizes;
            }
        }
        return def.sizes;
    }

    public int desired(Material m, StackSizes s) {
        return switch (m) {
            case POTION -> s.potion();
            case SPLASH_POTION -> s.splash();
            case LINGERING_POTION -> s.lingering();
            default -> m.getMaxStackSize();
        };
    }

    private record Profile(String name, int priority, StackSizes sizes) {}
}
