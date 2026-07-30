package me.mss1r.ppacker.util;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StackProfileResolverTest {

    @Test
    void usesBuiltInDefaultsWhenProfilesAreMissing() {
        StackProfileResolver resolver = new StackProfileResolver(new YamlConfiguration());

        assertEquals(new StackSizes(16, 16, 16), resolver.defaultSizes());
        assertEquals(new StackSizes(16, 16, 16), resolver.sizesFor(null));
    }

    @Test
    void clampsValuesAndInheritsMissingProfileSizes() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("stack_profiles.default.potion", 0);
        config.set("stack_profiles.default.splash_potion", 80);
        config.set("stack_profiles.default.lingering_potion", 8);
        config.set("stack_profiles.vip.priority", 10);
        config.set("stack_profiles.vip.potion", 32);

        StackProfileResolver resolver = new StackProfileResolver(config);
        Player player = player(Map.of("potionpacker.profile.vip", true));

        assertEquals(new StackSizes(1, 64, 8), resolver.defaultSizes());
        assertEquals(new StackSizes(32, 64, 8), resolver.sizesFor(player));
    }

    @Test
    void selectsTheHighestPriorityGrantedProfile() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("stack_profiles.default.potion", 8);
        config.set("stack_profiles.vip.priority", 10);
        config.set("stack_profiles.vip.potion", 24);
        config.set("stack_profiles.admin.priority", 100);
        config.set("stack_profiles.admin.potion", 64);

        StackProfileResolver resolver = new StackProfileResolver(config);
        Player player = player(Map.of(
                "potionpacker.profile.vip", true,
                "potionpacker.profile.admin", true
        ));

        assertEquals(64, resolver.sizesFor(player).potion());
    }

    @Test
    void ignoresPermissionsThatAreNotExplicitlySet() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("stack_profiles.default.potion", 8);
        config.set("stack_profiles.vip.priority", 10);
        config.set("stack_profiles.vip.potion", 32);

        StackProfileResolver resolver = new StackProfileResolver(config);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isPermissionSet("potionpacker.profile.vip")).thenReturn(false);
        when(player.hasPermission("potionpacker.profile.vip")).thenReturn(true);

        assertEquals(8, resolver.sizesFor(player).potion());
    }

    @Test
    void invalidationRecomputesCachedPermissions() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("stack_profiles.default.potion", 8);
        config.set("stack_profiles.vip.priority", 10);
        config.set("stack_profiles.vip.potion", 32);

        StackProfileResolver resolver = new StackProfileResolver(config);
        UUID playerId = UUID.randomUUID();
        AtomicBoolean granted = new AtomicBoolean(true);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isPermissionSet("potionpacker.profile.vip"))
                .thenAnswer(ignored -> granted.get());
        when(player.hasPermission("potionpacker.profile.vip"))
                .thenAnswer(ignored -> granted.get());

        assertEquals(32, resolver.sizesFor(player).potion());
        granted.set(false);
        assertEquals(32, resolver.sizesFor(player).potion());

        resolver.invalidate(playerId);

        assertEquals(8, resolver.sizesFor(player).potion());
    }

    @Test
    void mapsStackSizesToPotionMaterials() {
        StackProfileResolver resolver = new StackProfileResolver(new YamlConfiguration());
        StackSizes sizes = new StackSizes(12, 24, 36);

        assertEquals(12, resolver.desired(Material.POTION, sizes));
        assertEquals(24, resolver.desired(Material.SPLASH_POTION, sizes));
        assertEquals(36, resolver.desired(Material.LINGERING_POTION, sizes));
    }

    private Player player(Map<String, Boolean> permissions) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isPermissionSet(anyString()))
                .thenAnswer(invocation -> permissions.containsKey(invocation.getArgument(0)));
        when(player.hasPermission(anyString()))
                .thenAnswer(invocation -> permissions.getOrDefault(invocation.getArgument(0), false));
        return player;
    }
}
