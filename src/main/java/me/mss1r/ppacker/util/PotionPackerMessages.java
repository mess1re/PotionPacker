package me.mss1r.ppacker.util;

import me.mss1r.ppacker.PotionPackerPlugin;
import net.kyori.adventure.text.Component;

public final class PotionPackerMessages {

    private final PotionPackerPlugin plugin;

    public PotionPackerMessages(PotionPackerPlugin plugin) {
        this.plugin = plugin;
    }

    public Component c(String path, String... kv) {
        return TextUtil.mm(s(path, kv));
    }

    public String s(String path, String... kv) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String v = plugin.getConfig().getString(path, path);
        if (v == null) v = "";

        v = v.replace("%prefix%", prefix);

        if (kv != null) {
            for (int i = 0; i + 1 < kv.length; i += 2) {
                String k = kv[i];
                String val = kv[i + 1];
                if (k != null) v = v.replace(k, val == null ? "" : val);
            }
        }
        return v;
    }

    public Component cRaw(String raw) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String v = (raw == null ? "" : raw).replace("%prefix%", prefix);
        return TextUtil.mm(v);
    }

}
