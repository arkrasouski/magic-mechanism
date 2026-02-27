package org.example.artyom.magicMechanism.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.managers.CableManager;
import org.example.artyom.magicMechanism.managers.EnergyManager;
import org.example.artyom.magicMechanism.mechanisms.Cable;

public class CableEvents implements Listener {

    private final CableManager cableManager;
    private final MagicMechanism plugin;
    public CableEvents (MagicMechanism plugin) {
        this.cableManager = new CableManager(plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Проверяем, является ли блок кабелем
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "cable_item"))) {

            cableManager.markAsCable(block, player);
            // Регистрируем новый кабель
            Location loc = block.getLocation();
            Cable cable = Cable.create(loc, player.getUniqueId(), cableManager);
            //EnergyManager.registerHandler(loc, cable);

            // Сканируем соединения
            cable.scanConnections(block);

            // Обновляем соединения соседних кабелей
            cableManager.updateNeighborCables(block, player);

            // Сообщение игроку (опционально)
            player.sendMessage("§a[Сеть] Кабель размещен! Найдено соединений: " +
                    cable.getAllConnections().size());
        }
    }

    // Событие разрушения блока
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Cable cable = cableManager.getCable(block);

        if (cable != null && cable.isCable(block)) {
           // Location loc = block.getLocation();
            cableManager.removeCable(block, player);
            // Удаляем кабель из менеджера
            //EnergyManager.removeHandler(loc);

            // Обновляем соединения соседних кабелей
            cableManager.updateNeighborCables(block, player);

            player.sendMessage("§c[Сеть] Кабель удален");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (player.getInventory().getItemInMainHand().getType() == Material.STICK) {

            // getCable() теперь сам загрузит кабель если нужно
            Cable cable = cableManager.getCable(block);

            if (cable != null) {
                cable.scanConnections(block);

                player.sendMessage("§6=== Информация о кабеле ===");
                player.sendMessage("§7Кабелей: §a" + cable.getConnectedCables().size());
                player.sendMessage("§7Механизмов: §b" + cable.getConnectedMachines().size());
                player.sendMessage("§7Генераторов: §e" + cable.getConnectedGenerators().size());

                // Визуализация
                visualizeConnections(player, cable);
            }
        }
    }



    private void visualizeConnections(Player player, Cable cable) {
        Location center = cable.getLocation().add(0.5, 0.5, 0.5);

        for (Location target : cable.getConnectedCables()) {
            Location targetLoc = target.clone().add(0.5, 0.5, 0.5);
            spawnParticleLine(player, center, targetLoc, Particle.END_ROD);
        }

        for (Location target : cable.getConnectedMachines()) {
            Location targetLoc = target.clone().add(0.5, 0.5, 0.5);
            spawnParticleLine(player, center, targetLoc, Particle.END_ROD);
        }

        for (Location target : cable.getConnectedGenerators()) {
            Location targetLoc = target.clone().add(0.5, 0.5, 0.5);
            spawnParticleLine(player, center, targetLoc, Particle.END_ROD);
        }
    }

    private void spawnParticleLine(Player player, Location from, Location to, Particle particle) {
        double distance = from.distance(to);
        int steps = (int) (distance * 4);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = from.getX() + (to.getX() - from.getX()) * t;
            double y = from.getY() + (to.getY() - from.getY()) * t;
            double z = from.getZ() + (to.getZ() - from.getZ()) * t;

            Location particleLoc = new Location(from.getWorld(), x, y, z);
            player.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }


}