package org.awang05.kitbattlelite.player;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 玩家进图前的状态快照
 *
 * <p>进图时 {@link #capture(Player)}，离场时 {@link #restore(Player)}。
 */
public final class PlayerSnapshot {
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack offHand;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean flying;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int level;
    private final float exp;
    private final int totalExperience;
    private final List<PotionEffect> effects;

    private PlayerSnapshot(ItemStack[] storage, ItemStack[] armor, ItemStack offHand, GameMode gameMode,
                           boolean allowFlight, boolean flying, double health, int foodLevel, float saturation,
                           int level, float exp, int totalExperience, List<PotionEffect> effects) {
        this.storage = storage;
        this.armor = armor;
        this.offHand = offHand;
        this.gameMode = gameMode;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.level = level;
        this.exp = exp;
        this.totalExperience = totalExperience;
        this.effects = effects;
    }

    /** 记录玩家当前状态；所有物品都克隆，之后改玩家背包不会影响快照。 */
    public static PlayerSnapshot capture(Player player) {
        Objects.requireNonNull(player, "player");
        PlayerInventory inventory = player.getInventory();
        inventory.getItemInOffHand();
        return new PlayerSnapshot(
                cloneItems(inventory.getStorageContents()),
                cloneItems(inventory.getArmorContents()),
                inventory.getItemInOffHand().clone(),
                player.getGameMode(),
                player.getAllowFlight(),
                player.isFlying(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getLevel(),
                player.getExp(),
                player.getTotalExperience(),
                List.copyOf(player.getActivePotionEffects())
        );
    }

    /** 把快照写回玩家；背包、装备、模式、飞行、血量、饥饿、经验、药水效果全部恢复。 */
    public void restore(Player player) {
        Objects.requireNonNull(player, "player");
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setStorageContents(cloneItems(storage));
        inventory.setArmorContents(cloneItems(armor));
        inventory.setItemInOffHand(offHand == null ? null : offHand.clone());

        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);

        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);
        player.setTotalExperience(totalExperience);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.addPotionEffects(effects);

        double maxHealth = maxHealth(player);
        player.setHealth(Math.max(0.0, Math.min(health, maxHealth)));
    }

    private static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attribute == null ? 20.0 : attribute.getValue();
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
