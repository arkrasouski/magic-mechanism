package org.example.artyom.magicMechanism.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.Keys;
import org.example.artyom.magicMechanism.inventories.GenHolder;
import org.example.artyom.magicMechanism.inventories.GenStorage;
import org.example.artyom.magicMechanism.utils.EnergyCellUtil;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GeneratorService implements Listener {
    private static final Map<String, Set<UUID>> viewers = new ConcurrentHashMap<>();

    public GeneratorService() {}

    private static final Set<Location> ACTIVE = ConcurrentHashMap.newKeySet();

    public static void onCellInserted(Location loc) {
        ACTIVE.add(loc);
    }

    public static void onCellRemoved(Location loc) {
        ACTIVE.remove(loc);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getInventory().getHolder() instanceof GenHolder h)) return;
        viewers.computeIfAbsent(key(h.getLocation()), k -> ConcurrentHashMap.newKeySet())
                .add(e.getPlayer().getUniqueId());

    }
    //    @EventHandler
//    public void onClose(InventoryCloseEvent e) {
//        InventoryHolder h = e.getInventory().getHolder();
//        if (!(h instanceof GenHolder gen)) return;
//
//        Block block = gen.getLocation().getBlock();
//
//        // 3) Взяли TileState (подходит только для блоков с block entity)
//        BlockState state = block.getState();
//        if (!(state instanceof TileState tile)) return;
//
//        // 4) Сохранили items
//        GenStorage.saveItems(tile, e.getInventory());
//

    private static String key(Location loc) {
        return loc.getWorld().getUID() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof GenHolder h)) return;

        String k = key(h.getLocation());
        Set<UUID> set = viewers.get(k);
        if (set != null) {
            set.remove(e.getPlayer().getUniqueId());
            if (set.isEmpty()) viewers.remove(k);
        }

        // на close — сохранить PDC сериализацией (это нормально)
        BlockState st = h.getLocation().getBlock().getState();
        if (st instanceof TileState tile) {
            Inventory top = e.getView().getTopInventory();
            ItemStack cell = top.getItem(0);
            Bukkit.getLogger().info("close cellEnergy=" + EnergyCellUtil.getEnergy(cell));
            GenStorage.saveItems(tile, top); // внутри tile.update() [web:65]
        }
    }

    // вызывать раз в тик/20 тиков
    public static void tickAll() {
        ACTIVE.removeIf(loc -> !tickOne(loc));
    }
    private static Location parseKeyBack(String key) {
        String[] p = key.split(":");
        UUID worldId = UUID.fromString(p[0]);
        int x = Integer.parseInt(p[1]);
        int y = Integer.parseInt(p[2]);
        int z = Integer.parseInt(p[3]);

        World w = Bukkit.getWorld(worldId); // getWorld(UUID) [web:245]
        if (w == null) return null;
        return new Location(w, x, y, z);
    }
    public static void tickOpenGuis() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Inventory top = p.getOpenInventory().getTopInventory(); // верхний инвентарь [web:239]
            if (!(top.getHolder() instanceof GenHolder h)) continue;

            Location loc = h.getLocation();
            BlockState st = loc.getBlock().getState();
            if (!(st instanceof TileState tile)) continue;
            if (!GeneratorUtil.isGenerator(loc.getBlock())) continue;

            // Подтягиваем "истину" из PDC и просто отображаем
            Inventory tmp = Bukkit.createInventory(null, top.getSize());
            GenStorage.loadItems(tile, tmp);

            top.setItem(0, tmp.getItem(0)); // только UI-обновление
        }
    }

    // true = оставить активным, false = убрать
    private static boolean tickOne(Location loc) {
        World w = loc.getWorld();
        if (w == null) return false;

        Block b = w.getBlockAt(loc);
        if (!GeneratorUtil.isGenerator(b)) return false;

        BlockState st = b.getState();
        if (!(st instanceof TileState tile)) return false;

        Inventory inv = Bukkit.createInventory(null, 27);
        GenStorage.loadItems(tile, inv);

        ItemStack cell = inv.getItem(0);
        if (!EnergyCellUtil.isEnergyCell(cell)) return false;

        int cellEnergy = EnergyCellUtil.getEnergy(cell);
        if (cellEnergy <= 0) return true;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        int buf = pdc.getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);

        int moved = Math.min(2, cellEnergy);
        moved = Math.min(moved, GeneratorUtil.capacity - buf);
        if (moved <= 0) return true;

        // Меняем ТОЛЬКО здесь
        EnergyCellUtil.setEnergy(cell, cellEnergy - moved); // внутри обновится lore
        inv.setItem(0, cell);

        pdc.set(Keys.BUFFER, PersistentDataType.INTEGER, buf + moved);

        GenStorage.saveItems(tile, inv); // внутри tile.update() обязательно [web:65]
        return true;
    }

    public static boolean hasActive() {
        return !ACTIVE.isEmpty();
    }
}