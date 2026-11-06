package org.awang05.kitbattlelite.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 职业与页面在内存里的唯一来源，负责 {@code kits.yml} 的读取与写回。
 *
 * <p>生命周期：组合根创建，onEnable 时 {@link #load()}；{@code /kbl save}、页面编辑器关闭、
 * onDisable 时 {@link #save()}。读写都在主线程。
 *
 * <p>页面只存 kit id：删除职业必须走 {@link #deleteKit(String)} 级联清理引用；
 * 未被引用的职业用 {@link #pruneKits()} 清理。加载时引用了不存在的 id 只告警，
 * 原样保留 id（便于管理员修配置），运行时由调用方按空处理。
 */
public final class KitRepository {
    public static final String FILE_NAME = "kits.yml";
    public static final int DATA_VERSION = 1;

    private final JavaPlugin plugin;
    private final Logger logger;
    private final KitCodec codec;
    private final File file;

    private final NavigableMap<String, Kit> kits = new TreeMap<>();
    private final NavigableMap<Integer, KitPage> pages = new TreeMap<>();
    private int revision;

    public KitRepository(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = this.plugin.getLogger();
        this.codec = new KitCodec(logger);
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
    }

    /** 重新读取 kits.yml；旧内容全部丢弃，revision 自增。 */
    public void load() {
        kits.clear();
        pages.clear();

        YamlConfiguration config = readFile();
        checkVersion(config);
        loadKits(config);
        loadPages(config);

        revision++;
        logger.info("已加载 " + kits.size() + " 个职业、" + pages.size() + " 个页面（revision " + revision + "）");
    }

    /** 写回 kits.yml；失败只记日志，不抛出。 */
    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("version", DATA_VERSION);
        writePages(config);
        writeKits(config);
        saveQuietly(config);
    }

    /** 每次 load 自增；菜单可用它判断自己是不是基于旧数据打开的。 */
    public int revision() {
        return revision;
    }

    public int kitCount() {
        return kits.size();
    }

    /** 全部职业（按 id 排序，只读视图；元素是模型本体，别直接改）。 */
    public Collection<Kit> kits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public Optional<Kit> findKit(String kitId) {
        if (kitId == null || kitId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(kits.get(kitId.trim()));
    }

    /** 新增或替换一个职业（模型实例由仓库持有）。 */
    public void putKit(Kit kit) {
        Objects.requireNonNull(kit, "kit");
        kits.put(kit.id(), kit);
    }

    /** 只删职业本身，不动页面引用；级联删除用 {@link #deleteKit(String)}。 */
    public boolean removeKit(String kitId) {
        if (kitId == null) {
            return false;
        }
        return kits.remove(kitId.trim()) != null;
    }

    /** 读取页面；不存在返回 null（不隐式创建）。 */
    public KitPage page(int index) {
        return pages.get(index);
    }

    /** 读取页面，不存在则创建并加入仓库（供编辑与翻页使用）。 */
    public KitPage ensurePage(int index) {
        return pages.computeIfAbsent(index, KitPage::new);
    }

    /** 已存在的页面下标（升序，只读）。 */
    public Set<Integer> pageIndexes() {
        return Collections.unmodifiableSet(pages.keySet());
    }

    /**
     * 上一个已存在的页面下标。
     *
     * @return 没有更小的页面时返回 null
     */
    public Integer previousPage(int index) {
        return pages.lowerKey(index);
    }

    /**
     * 下一个已存在的页面下标。
     *
     * @return 没有更大的页面时返回 null
     */
    public Integer nextPage(int index) {
        return pages.higherKey(index);
    }

    /** 最大页面下标；没有页面返回 0。 */
    public int maxPageIndex() {
        return pages.isEmpty() ? 0 : pages.lastKey();
    }

    /** 找出所有引用指定 kit id 的页面槽位。 */
    public List<SlotRef> referencingSlots(String kitId) {
        if (kitId == null || kitId.isBlank()) {
            return List.of();
        }
        String target = kitId.trim();
        List<SlotRef> refs = new ArrayList<>();
        for (KitPage page : pages.values()) {
            for (int position = 0; position < KitPage.SIZE; position++) {
                if (target.equals(page.kitId(position))) {
                    refs.add(new SlotRef(page.index(), position));
                }
            }
        }
        return refs;
    }

    /**
     * 删除职业，并清掉所有页面引用。
     *
     * @return 被清掉的页面槽位数量；职业不存在时返回 0
     */
    public int deleteKit(String kitId) {
        if (kitId == null) {
            return 0;
        }
        String target = kitId.trim();
        if (kits.remove(target) == null) {
            return 0;
        }
        List<SlotRef> refs = referencingSlots(target);
        for (SlotRef ref : refs) {
            KitPage page = pages.get(ref.page());
            if (page != null) {
                page.setKitId(ref.position(), null);
            }
        }
        save();
        return refs.size();
    }

    /** 未被任何页面引用的职业 id（升序）。 */
    public Set<String> unreferencedKitIds() {
        Set<String> referenced = new HashSet<>();
        for (KitPage page : pages.values()) {
            for (int position = 0; position < KitPage.SIZE; position++) {
                String kitId = page.kitId(position);
                if (kitId != null) {
                    referenced.add(kitId);
                }
            }
        }
        Set<String> orphans = new TreeSet<>();
        for (String kitId : kits.keySet()) {
            if (!referenced.contains(kitId)) {
                orphans.add(kitId);
            }
        }
        return orphans;
    }

    /**
     * 删除所有未被引用的职业。
     *
     * @return 被删除的 kit id（升序）
     */
    public List<String> pruneKits() {
        List<String> removed = new ArrayList<>(unreferencedKitIds());
        removed.forEach(kits::remove);
        save();
        return removed;
    }

    private YamlConfiguration readFile() {
        if (!file.exists()) {
            YamlConfiguration created = new YamlConfiguration();
            created.set("version", DATA_VERSION);
            saveQuietly(created);
            logger.info(FILE_NAME + " 不存在，已创建空文件");
            return created;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void checkVersion(YamlConfiguration config) {
        int version = config.getInt("version", -1);
        if (version < 0) {
            logger.warning(FILE_NAME + " 缺少 version，按当前格式读取；下次保存会补上");
            return;
        }
        if (version > DATA_VERSION) {
            logger.warning(FILE_NAME + " 的 version=" + version
                    + " 高于本插件支持的 " + DATA_VERSION + "，未知字段会被忽略");
        }
    }

    private void loadKits(YamlConfiguration config) {
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection == null) {
            // logger.info(FILE_NAME + " 未配置职业（kits 段缺失）");
            return;
        }
        for (String rawId : kitsSection.getKeys(false)) {
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(rawId);
            if (kitSection == null) {
                // logger.warning("跳过职业 " + rawId + "：不是配置段");
                continue;
            }
            Kit kit;
            try {
                kit = new Kit(rawId, codec.read(kitSection, "icon", "kits." + rawId + ".icon"), null);
            } catch (IllegalArgumentException e) {
                logger.warning("跳过职业 " + rawId + "：" + e.getMessage());
                continue;
            }
            loadContents(kit, kitSection);
            loadPotion(kit, kitSection);
            kits.put(kit.id(), kit);
        }
    }

    private void loadContents(Kit kit, ConfigurationSection kitSection) {
        ConfigurationSection contents = kitSection.getConfigurationSection("contents");
        if (contents == null) {
            return;
        }
        for (String rawSlot : contents.getKeys(false)) {
            int slot = parseIndex(rawSlot, "kits." + kit.id() + ".contents", 0, KitSlots.SIZE - 1);
            if (slot < 0) {
                continue;
            }
            ItemStack item = codec.read(contents, rawSlot, "kits." + kit.id() + ".contents." + rawSlot);
            if (item != null) {
                kit.setContent(slot, item);
            }
        }
    }

    /**
     * 读取药水效果：{@code potion} 键存成一个带 {@link PotionMeta} 的 POTION 物品。
     *
     * <p>缺字段静默视为无效果；物品损坏会由 {@link KitCodec#read} 记 WARNING 后按无效果处理；
     * 若存的不是药水（没有 {@link PotionMeta}）同样按无效果处理。
     */
    private void loadPotion(Kit kit, ConfigurationSection kitSection) {
        ItemStack item = codec.read(kitSection, "potion", "kits." + kit.id() + ".potion");
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof PotionMeta potion) {
            kit.setPotion(potion);
        }
    }

    private void loadPages(YamlConfiguration config) {
        ConfigurationSection pagesSection = config.getConfigurationSection("pages");
        if (pagesSection == null) {
            logger.info(FILE_NAME + " 未配置页面（pages 段缺失）");
            return;
        }
        for (String rawIndex : pagesSection.getKeys(false)) {
            int index = parseIndex(rawIndex, "pages", 1, Integer.MAX_VALUE);
            if (index < 0) {
                continue;
            }
            KitPage page = new KitPage(index);
            ConfigurationSection pageSection = pagesSection.getConfigurationSection(rawIndex);
            ConfigurationSection slots = pageSection == null ? null : pageSection.getConfigurationSection("slots");
            if (slots != null) {
                loadPageSlots(page, slots, rawIndex);
            }
            pages.put(index, page);
        }
    }

    private void loadPageSlots(KitPage page, ConfigurationSection slots, String rawIndex) {
        for (String rawSlot : slots.getKeys(false)) {
            int position = parseIndex(rawSlot, "pages." + rawIndex + ".slots", 0, KitPage.SIZE - 1);
            if (position < 0) {
                continue;
            }
            Object rawKitId = slots.get(rawSlot);
            if (!(rawKitId instanceof String kitId) || kitId.isBlank()) {
                logger.warning("跳过 pages." + rawIndex + ".slots." + rawSlot + "：值必须是职业 id 字符串");
                continue;
            }
            kitId = kitId.trim();
            page.setKitId(position, kitId);
            if (!kits.containsKey(kitId)) {
                logger.warning("页面 " + page.index() + " 的槽位 " + position
                        + " 引用了不存在的职业 " + kitId + "，运行时按空处理");
            }
        }
    }

    /**
     * 解析数字下标；失败时记日志并返回 -1。
     */
    private int parseIndex(String raw, String context, int min, int max) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                logger.warning("跳过 " + context + "." + raw + "：下标必须在 " + min + ".." + max);
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            logger.warning("跳过 " + context + "." + raw + "：不是数字下标");
            return -1;
        }
    }

    private void writePages(YamlConfiguration config) {
        ConfigurationSection pagesSection = config.createSection("pages");
        for (KitPage page : pages.values()) {
            ConfigurationSection pageSection = pagesSection.createSection(Integer.toString(page.index()));
            ConfigurationSection slots = pageSection.createSection("slots");
            for (int position = 0; position < KitPage.SIZE; position++) {
                String kitId = page.kitId(position);
                if (kitId != null) {
                    slots.set(Integer.toString(position), kitId);
                }
            }
        }
    }

    private void writeKits(YamlConfiguration config) {
        ConfigurationSection kitsSection = config.createSection("kits");
        for (Kit kit : kits.values()) {
            ConfigurationSection kitSection = kitsSection.createSection(kit.id());
            codec.write(kitSection, "icon", kit.icon());
            ConfigurationSection contents = kitSection.createSection("contents");
            boolean hasItem = false;
            for (int slot = 0; slot < KitSlots.SIZE; slot++) {
                ItemStack item = kit.content(slot);
                if (item == null) {
                    continue;
                }
                contents.set(Integer.toString(slot), item);
                hasItem = true;
            }
            if (!hasItem) {
                kitSection.set("contents", null);
            }
            codec.write(kitSection, "potion", potionItem(kit.potion()));
        }
    }

    /** 把药水效果包成一个 POTION 物品用于落盘；无效果返回 null（写盘时会移除该键）。 */
    private static ItemStack potionItem(PotionMeta potion) {
        if (potion == null) {
            return null;
        }
        ItemStack item = new ItemStack(Material.POTION);
        item.setItemMeta(potion);
        return item;
    }

    private void saveQuietly(YamlConfiguration config) {
        try {
            File folder = file.getParentFile();
            if (folder != null && !folder.isDirectory() && !folder.mkdirs()) {
                throw new IOException("无法创建目录 " + folder);
            }
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "保存 " + FILE_NAME + " 失败", e);
        }
    }

    /** 页面里的一个槽位引用。 */
    public record SlotRef(int page, int position) {
    }
}
