package org.example.artyom.magicMechanism.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;

import java.util.ArrayList;
import java.util.List;

public class BaseMechanismItem {
    private final MagicMechanism plugin;
    private final MechanismType mechanismType;
    private NamespacedKey generatorKey;

    public BaseMechanismItem(MagicMechanism plugin, MechanismType mechanismType) {
        this.plugin = plugin;
        this.mechanismType = mechanismType;
        this.generatorKey = new NamespacedKey(plugin, mechanismType.name() + "_item");
    }

    public ItemStack createItem(int amount) {
        // Создаем базовый предмет (например, алмазный блок)
        ItemStack item = new ItemStack(mechanismType.getMaterial(), amount);

        // Получаем метаданные
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Устанавливаем название
        meta.setDisplayName("§6⚡ " + this.mechanismType.getGuiTitle() + " ⚡");

        // Устанавливаем описание
        List<String> lore = new ArrayList<>();
        lore.add("§7" +  this.mechanismType.getGuiLore());

        meta.setLore(lore);

        // Добавляем эффект свечения (чтобы предмет выглядел особенным)
        meta.addEnchant(Enchantment.FORTUNE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Сохраняем метку, что это предмет-генератор
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(generatorKey, PersistentDataType.BOOLEAN, true);

        // Также можно сохранить дополнительные параметры
//        pdc.set(new NamespacedKey(plugin, "generator_max_energy"),
//                PersistentDataType.INTEGER, 1000);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Проверяет, является ли предмет генератором
     */
    public boolean isMechanismItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(generatorKey, PersistentDataType.BOOLEAN);
    }

    /**
     * Получает максимальную энергию из предмета
     */
//    public int getMaxEnergyFromItem(ItemStack item) {
//        if (!isGeneratorItem(item)) return 1000; // значение по умолчанию
//
//        ItemMeta meta = item.getItemMeta();
//        PersistentDataContainer pdc = meta.getPersistentDataContainer();
//        NamespacedKey key = new NamespacedKey(plugin, "generator_max_energy");
//
//        return pdc.getOrDefault(key, PersistentDataType.INTEGER, 1000);
//    }

    public MechanismType getMechanismType() {return mechanismType;}
}
