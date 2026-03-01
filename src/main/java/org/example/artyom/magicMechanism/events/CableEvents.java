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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.linkservice.GeneratorBarrierService;
import org.example.artyom.magicMechanism.managers.CableManager;
import org.example.artyom.magicMechanism.managers.NetworkManager;
import org.example.artyom.magicMechanism.mechanisms.Cable;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.Set;

public class CableEvents implements Listener {

    private final CableManager cableManager;
    private final MagicMechanism plugin;
    private final NamespacedKey cableItemKey;
    private final NetworkManager networkManager;

    public CableEvents(MagicMechanism plugin,
                       CableManager cableManager, NetworkManager networkManager

    ) {
        this.plugin = plugin;
        this.cableManager = cableManager;
        this.cableItemKey = new NamespacedKey(plugin, "cable_item");
        this.networkManager = networkManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Проверяем, является ли предмет кабелем
        if (!isCableItem(item)) {
            return;
        }

        LogUtil.warn("=== РАЗМЕЩЕНИЕ КАБЕЛЯ ===");

        try {
            // 1. Помечаем блок как кабель (сохраняем в PDC)
            cableManager.markAsCable(block, player);

            // 2. Получаем созданный кабель
            Cable cable = cableManager.getCable(block);

            if (cable != null) {
                // 3. Сканируем прямых соседей
                cable.scanDirectConnections(block);

                // 4. NetworkManager сам обработает размещение (объединение сетей и т.д.)
                // Это уже происходит внутри markAsCable -> addCable -> networkManager.onCablePlaced

                // 5. Визуальный эффект
                spawnPlaceEffect(block);

                // 6. Сообщение игроку
                int connections = cable.getDirectConnections().size();
                player.sendMessage("§a[Сеть] Кабель размещен! Найдено соседей: " + connections);

                LogUtil.warn("✓ Кабель успешно размещен с " + connections + " соседями");
            }

        } catch (Exception e) {
            LogUtil.warn("❌ Ошибка при размещении кабеля: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§c[Ошибка] Не удалось разместить кабель");
        }

        LogUtil.warn("=== КОНЕЦ РАЗМЕЩЕНИЯ ===");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Проверяем, является ли блок кабелем
        if (!cableManager.isCable(block)) {
            return;
        }

        LogUtil.warn("=== УДАЛЕНИЕ КАБЕЛЯ ===");

        try {
            // Сохраняем соседей для эффекта
            Set<Location> neighbors = new java.util.HashSet<>();
            Cable cable = cableManager.getCable(block);
            if (cable != null) {
                neighbors.addAll(cable.getDirectConnections());
            }

            // Удаляем кабель (менеджер сам уведомит NetworkManager)
            cableManager.removeCable(block, player);

            // Визуальный эффект разрушения
            spawnBreakEffect(block, neighbors);

            player.sendMessage("§c[Сеть] Кабель удален");

            LogUtil.warn("✓ Кабель успешно удален");

        } catch (Exception e) {
            LogUtil.warn("❌ Ошибка при удалении кабеля: " + e.getMessage());
            e.printStackTrace();
        }

        LogUtil.warn("=== КОНЕЦ УДАЛЕНИЯ ===");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.STICK) {
            Cable cable = cableManager.getCable(block);
            if (cable != null) {
                showCableInfo(player, cable);
                // Добавляем отладку сетей
                networkManager.debugNetworkState();
            }
        }
    }
    /**
     * Проверяет, является ли предмет кабелем
     */
    private boolean isCableItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        return pdc.has(cableItemKey, PersistentDataType.BOOLEAN);
    }

    /**
     * Показывает информацию о кабеле
     */
    private void showCableInfo(Player player, Cable cable) {
        player.sendMessage("§6=== Информация о кабеле ===");
        player.sendMessage("§7Локация: §f" + formatLocation(cable.getLocation()));

        int neighbors = cable.getDirectConnections().size();
        player.sendMessage("§7Прямых соседей: §e" + neighbors);

        // Информация о сети
        if (cable.getNetwork() != null) {
            player.sendMessage("§7Сеть: §a" + cable.getNetwork().getId().toString().substring(0, 8) + "...");
            player.sendMessage("§7Генераторов в сети: §c" + cable.getNetwork().getGenerators().size());
            player.sendMessage("§7Потребителей в сети: §b" + cable.getNetwork().getConsumers().size());
            player.sendMessage("§7Всего узлов: §d" + cable.getNetwork().getNodes().size());
        } else {
            player.sendMessage("§7Сеть: §cне подключен");
        }
    }

    /**
     * Показывает информацию о механизме (не кабеле)
     */
    private void showMechanismInfo(Player player, Block block) {
        // Здесь можно добавить информацию о генераторах/барьерах
        // Пока просто заглушка
    }

    /**
     * Визуализирует соединения кабеля
     */
    private void visualizeConnections(Player player, Cable cable) {
        Location center = cable.getLocation().add(0.5, 0.5, 0.5);

        for (Location target : cable.getDirectConnections()) {
            Location targetLoc = target.clone().add(0.5, 0.5, 0.5);

            // Выбираем цвет в зависимости от типа
            Particle particle = getParticleForTarget(target);
            spawnParticleLine(player, center, targetLoc, particle);
        }
    }

    /**
     * Определяет частицу для типа соединения
     */
    private Particle getParticleForTarget(Location target) {
        // Здесь можно определить по типу блока
        return Particle.END_ROD; // По умолчанию
    }

    /**
     * Спавнит линию частиц
     */
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

    /**
     * Эффект при размещении
     */
    private void spawnPlaceEffect(Block block) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);

        block.getWorld().spawnParticle(
                Particle.END_ROD,
                center,
                30,
                0.5, 0.5, 0.5,
                0.1
        );
    }

    /**
     * Эффект при разрушении
     */
    private void spawnBreakEffect(Block block, Set<Location> neighbors) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);

        // Взрыв частиц на месте разрушения
        block.getWorld().spawnParticle(
                Particle.CLOUD,
                center,
                20,
                0.3, 0.3, 0.3,
                0.05
        );

        // Искры к соседям
        for (Location neighbor : neighbors) {
            Location target = neighbor.clone().add(0.5, 0.5, 0.5);
            double dx = target.getX() - center.getX();
            double dy = target.getY() - center.getY();
            double dz = target.getZ() - center.getZ();

            block.getWorld().spawnParticle(
                    Particle.CRIT,
                    center,
                    0,
                    dx, dy, dz,
                    0.3
            );
        }
    }

    /**
     * Форматирует локацию для вывода
     */
    private String formatLocation(Location loc) {
        return String.format("%s %d %d %d",
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }
}