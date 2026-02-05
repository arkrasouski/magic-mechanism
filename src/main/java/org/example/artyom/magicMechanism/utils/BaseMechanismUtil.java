package org.example.artyom.magicMechanism.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.Keys;

import java.util.Arrays;

public abstract class BaseMechanismUtil {

    private Material material;

    public String getName() {
        return name;
    }

    private String name;

    public String getKey_type() {
        return key_type;
    }

    // NamespacedKey key;
    private String key_type;
    private String lore;

    public int getCapacity() {
        return capacity;
    }

    public int getFrequency() {
        return frequency;
    }

    public String getLore() {
        return lore;
    }

    public Material getMaterial() {
        return material;
    }

    private int capacity;
    private int frequency;

    public BaseMechanismUtil(Material material, String name,
                             //NamespacedKey key,
                             String key_type, String lore,
                             int capacity, int frequency) {
        this.material = material;
        this.name = name;
        //this.key = key;
        this.key_type = key_type;
        this.lore = lore;
        this.capacity = capacity;
        this.frequency = frequency;
    }

    public ItemStack createMechanismItem() {
        ItemStack item = new ItemStack(this.material, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§b" + this.name);
        meta.getPersistentDataContainer().set(
                Keys.MACHINE_TYPE,
                PersistentDataType.STRING,
                this.key_type
        );
        //meta.setCustomModelData(1001);
        meta.setLore(Arrays.asList(this.lore));
        item.setItemMeta(meta);
        return item;
    }

    public boolean isMechanismItem(ItemStack item){
        if (item == null) return false;

        if(item.getType() != this.material) return false;
        // Проверяем базовый тип блока

        ItemMeta meta = item.getItemMeta();

        // Проверяем, что блок поддерживает PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Проверяем наш ключ
        String type = pdc.get(Keys.MACHINE_TYPE, PersistentDataType.STRING);

        return this.key_type.equals(type);
    }

    public void setMechanismBlock(Block block) {
        if (block == null) return;

        // Убеждаемся, что это нужный базовый блок
        if (block.getType() != this.material) return; {
            block.setType(this.material);
        }

        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return;
        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        // Помечаем блок как наш механизм
        pdc.set(
                Keys.MACHINE_TYPE,
                PersistentDataType.STRING,
                this.key_type
        );
        pdc.set(Keys.BUFFER,  PersistentDataType.INTEGER, 0);
        pdc.set(Keys.CAPACITY, PersistentDataType.INTEGER, this.capacity);
        pdc.set(Keys.FREQ, PersistentDataType.INTEGER, this.frequency);

        //Location loc = block.getLocation().add(0.5, 0, 0.5);
        //ItemDisplay display = (ItemDisplay) block.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
        //display.setItemStack(GeneratorUtil.createGenerator()); // с CustomModelData
        //display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        //display.setBrightness(new Display.Brightness(15, 15));
        tile.update();

    }

    public boolean isMechanismBlock(Block block){
        if (block == null) return false;

        // Проверяем базовый тип блока
        if (block.getType() != this.material) return false;

        BlockState state = block.getState();

        // Проверяем, что блок поддерживает PDC
        if (!(state instanceof TileState tile)) return false;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        // Проверяем наш ключ
        String type = pdc.get(Keys.MACHINE_TYPE , PersistentDataType.STRING);

        return this.key_type.equals(type);
    }

    public String getMechanismType(TileState tile) {
        return tile.getPersistentDataContainer().get(Keys.MACHINE_TYPE, PersistentDataType.STRING);
    }
}
