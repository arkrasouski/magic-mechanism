package org.example.artyom.magicMechanism.energyitems;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.Keys;

public class EnergyCell extends BaseEnergyItem {

//    public EnergyCell(Material material, String key_type, int capacity, int frequency, String name, String lore) {
//        super(material, key_type, capacity, frequency, name, lore);
//    }
    private static final String key_type = "energy_cell";
    private static final Material material = Material.NETHER_STAR;
    public EnergyCell() {
        super(material, key_type, 300, 2, "Энерго-блок", "");
    }

    public EnergyCell(ItemStack itemStack) {
        super(itemStack.getType(), key_type, 300, 2, itemStack.getItemMeta().getDisplayName(), itemStack.getItemMeta().getLore().get(0));
    }
    public static boolean isEnergyCell(ItemStack item){
        if (item == null) return false;

        if(item.getType() != material) return false;
        // Проверяем базовый тип блока

        ItemMeta meta = item.getItemMeta();

        // Проверяем, что блок поддерживает PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Проверяем наш ключ
        String type = pdc.get(Keys.CELL , PersistentDataType.STRING);

        return key_type.equals(type);
    }


}
