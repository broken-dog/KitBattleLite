package org.awang05.kitbattlelite.item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.awang05.kitbattlelite.config.PluginSettings;
import org.awang05.kitbattlelite.config.SettingsService;

import java.util.Objects;

/**
 * 蘑菇煲回血道具：应用职业时防堆叠打标，右键食用时回血并把蘑菇煲换成碗。
 *
 * <p>只负责道具语义，不判断玩家是否在竞技场
 */
public final class MushroomStewService {
    private final ItemService items;
    private final SettingsService settings;

    public MushroomStewService(ItemService items, SettingsService settings) {
        this.items = Objects.requireNonNull(items, "items");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /**
     * 道具是否可用：开关打开且回血量为正。
     *
     * <p>{@code heal-amount} 配成 0 或负数时整套效果都关掉
     */
    public boolean enabled() {
        PluginSettings current = settings.current();
        return current.mushroomStewEnabled() && current.mushroomStewHealAmount() > 0;
    }

    /**
     * 给玩家库存里所有还没打标的蘑菇煲盖上唯一的防堆叠标记。
     *
     * <p>在职业内容落地到玩家身上之后调用；道具未启用时直接返回。
     */
    public void markApplied(Player player) {
        Objects.requireNonNull(player, "player");
        if (!enabled()) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != Material.MUSHROOM_STEW
                    || items.isMushroomStew(item)) {
                continue;
            }
            // getContents() 给的是可写镜像，先克隆再打标，写回只走 setItem
            inventory.setItem(i, items.markMushroomStew(item.clone()));
        }
    }

    /**
     * 用掉主手的蘑菇煲：未满血就按配置回血（不超过上限）并把蘑菇煲换成碗。
     *
     * <p>回血可被其他插件监听、修改和取消。被取消则不消耗蘑菇煲。
     *
     * @return true 表示这次右键已归本道具处理（调用方应取消原版行为）；
     *         false 表示主手不是本插件发过的蘑菇煲，调用方不用管
     */
    public boolean use(Player player) {
        Objects.requireNonNull(player, "player");
        if (!enabled()) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItemInMainHand();
        if (!items.isMushroomStew(item)) {
            return false;
        }
        double max = maxHealth(player);
        double health = player.getHealth();
        if (Math.round(health) >= max) {
            return true;
        }

        // invoke heal event
        final double healAmount = settings.current().mushroomStewHealAmount();
        EntityRegainHealthEvent event = new EntityRegainHealthEvent(
                player,
                healAmount,
                EntityRegainHealthEvent.RegainReason.CUSTOM
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }

        double finalAmount = event.getAmount();
        player.setHealth(Math.min(max, health + finalAmount));
        if (item.getAmount() > 1)
            item.setAmount(item.getAmount() - 1);
        else
            inventory.setItemInMainHand(new ItemStack(Material.BOWL));
        return true;
    }

    private static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attribute == null ? 20.0 : attribute.getValue();
    }
}
