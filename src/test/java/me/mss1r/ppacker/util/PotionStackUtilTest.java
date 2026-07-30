package me.mss1r.ppacker.util;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PotionStackUtilTest {

    @Test
    void recognizesOnlySupportedPotionMaterials() {
        assertTrue(PotionStackUtil.isPotionLike(Material.POTION));
        assertTrue(PotionStackUtil.isPotionLike(Material.SPLASH_POTION));
        assertTrue(PotionStackUtil.isPotionLike(Material.LINGERING_POTION));
        assertFalse(PotionStackUtil.isPotionLike(Material.GLASS_BOTTLE));
        assertFalse(PotionStackUtil.isPotionLike(Material.STONE));
    }
}
