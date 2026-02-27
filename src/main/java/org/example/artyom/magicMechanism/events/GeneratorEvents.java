package org.example.artyom.magicMechanism.events;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.inventories.generator.GenInventory;
import org.example.artyom.magicMechanism.inventories.generator.GeneratorHolder;
import org.example.artyom.magicMechanism.inventories.MechanismStorage;

import org.example.artyom.magicMechanism.energyitems.EnergyCell;
import org.example.artyom.magicMechanism.linkservice.GeneratorBarrierService;
import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.LogUtil;

public class GeneratorEvents extends BaseMechanismEvents<Generator, GeneratorManager> {

 private GeneratorGuiManager guiManager;
 private GenInventory genInventory;
public GeneratorEvents(MagicMechanism plugin, GeneratorManager generatorManager, GeneratorGuiManager guiManager, GeneratorBarrierService service){//, GeneratorCellService genService, GenInventory genInventory) {
    super(plugin, generatorManager, MechanismType.GENERATOR, service);
    this.guiManager = guiManager;
    this.genInventory = new GenInventory();
}

@EventHandler
public void onOpen(InventoryOpenEvent e) {
    if (!(e.getInventory().getHolder() instanceof MechanismHolder h)) return;
    guiManager.addViewer(h.getLocation(), e.getPlayer().getUniqueId());
}


@EventHandler
public void onClose(InventoryCloseEvent e) {
    if (!(e.getView().getTopInventory().getHolder() instanceof MechanismHolder h)) return;

    guiManager.removeViewer(h.getLocation(), e.getPlayer().getUniqueId());
    BlockState st = h.getLocation().getBlock().getState();
    if (st instanceof TileState tile) {
        MechanismStorage.saveItems(tile, e.getView().getTopInventory(), Keys.KEY_ITEMS);
    }
}
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Проверяем, что это ПКМ по блоку
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();

        // Проверяем, является ли блок генератором через наш GeneratorManager
        if (mechanismManager instanceof GeneratorManager generatorManager) {
            Generator generator = generatorManager.getMechanism(block);
            if (generator == null) return;

            // Отменяем событие, чтобы не открывался ванильный интерфейс
            event.setCancelled(true);

            // Получаем TileState блока (для Dropper, Furnace и т.д.)
            if (!(block.getState() instanceof TileState)) {
                player.sendMessage(ChatColor.RED + "Ошибка: блок не является TileState!");
                return;
            }

            TileState tileState = (TileState) block.getState();

            // ===== РАБОТА С НАШИМИ ДАННЫМИ ИЗ PDC =====

            // 1. Получаем буфер (текущую энергию) из PDC блока
            // Используем генератор из нашего менеджера как основной источник
            int buffer = generator.getEnergyLevel();

            // Также можем получить из PDC, если нужно проверить синхронизацию
            int pdcBuffer = tileState.getPersistentDataContainer()
                    .getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);

            // Если данные расходятся, синхронизируем
            if (buffer != pdcBuffer) {
                plugin.getLogger().warning("Расхождение данных! Буфер: " + buffer + ", PDC: " + pdcBuffer);
                // Обновляем PDC из нашего генератора
                tileState.getPersistentDataContainer().set(Keys.BUFFER, PersistentDataType.INTEGER, buffer);
                tileState.update();
            }

            // 2. Получаем частоту из PDC
//            int frequency = generator.getFrequency();

            // 3. Получаем capacity из конфига механизма
            int capacity = generator.getCapacity();

            // 4. Рассчитываем процент энергии
            double energyPercent = (double) (buffer * 100) / capacity;

            // 5. Создаем Holder для GUI
            GeneratorHolder holder = new GeneratorHolder(
                    block.getLocation(),
                    MechanismType.GENERATOR,
                    energyPercent
            );

            // 6. Создаем или получаем GUI для генератора
            Inventory gui = genInventory.openMenu(player, holder, energyPercent);

            // 7. Устанавливаем holder'у инвентарь
            holder.setInventory(gui);

            // 8. Загружаем предметы из PDC в GUI (если есть)
            // Это нужно, если ваш генератор хранит предметы (как Dropper)
            if (block.getType() == Material.DROPPER || block.getType() == Material.HOPPER) {
                MechanismStorage.loadItems(tileState, gui, Keys.KEY_ITEMS);
            }

            // 9. Открываем GUI игроку
            player.openInventory(gui);

            // 10. Отправляем информационное сообщение
            player.sendMessage(ChatColor.YELLOW + "⚡ Генератор ⚡");
            player.sendMessage(ChatColor.GRAY + "  Энергия: " + formatEnergy(buffer, capacity));
           // player.sendMessage(ChatColor.GRAY + "  Частота: " + frequency);
            player.sendMessage(ChatColor.GRAY + "  Статус: " + (generator.isActive() ? "§aАктивен" : "§cНеактивен"));

            // 11. Логируем взаимодействие для отладки (можно убрать в продакшене)
            LogUtil.info("Игрок " + player.getName() + " открыл генератор на " +
                    block.getX() + ", " + block.getY() + ", " + block.getZ() +
                    " | Энергия: " + buffer + "/" + capacity);
        }
    }
    /**
     * Форматирует энергию для красивого отображения
     */
    private String formatEnergy(int current, int max) {
        double percent = (double) current / max * 100;
        String color;

        if (percent >= 75) color = "§a";
        else if (percent >= 50) color = "§e";
        else if (percent >= 25) color = "§6";
        else color = "§c";

        return color + current + "§7/§f" + max + " §7(" + String.format("%.1f", percent) + "%)";
    }

    @EventHandler
    public void onClickEnergySlot(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof GeneratorHolder holder)) return;
        int slot = e.getSlot(); // индекс в верхнем инвентаре
        Location loc = holder.getLocation();
        Block block = loc.getBlock();
        BlockState state = block.getState();
        if(mechanismManager instanceof GeneratorManager generatorManager) {
        Generator generator = generatorManager.getMechanism(block);
        if (generator == null) return;
        int topSize = top.getSize();
        if(e.getRawSlot() >= topSize) return;

        if (genInventory.isBlocked(slot)) {
            e.setCancelled(true);
        }
        Bukkit.getScheduler().runTask(MagicMechanism.getInstance(), () -> {
            ItemStack cell = top.getItem(9);

            if (!(state instanceof TileState tile)) return;
            Player p = (Player) e.getWhoClicked();

            if (EnergyCell.isEnergyCell(cell)) {
                p.sendMessage("Аккумулятор вставлен в слот 1!");
                GeneratorCellService.onCellInserted(loc);
            } else {
                GeneratorCellService.onCellRemoved(loc);
            }
            // КЛЮЧЕВОЕ: синхронизируем PDC сразу чтобы фоновые тики увидели аккумулятор
            MechanismStorage.saveItems(tile, top, Keys.KEY_ITEMS);
        });
    }
    }
//
//
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShiftToGenerator(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof GeneratorHolder holder)) return;

        // Только shift-перенос
        if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        Location loc = holder.getLocation();
        Block block = loc.getBlock();
        if(mechanismManager instanceof GeneratorManager generatorManager) {
        Generator generator = generatorManager.getMechanism(block);
        if (generator == null) return;
        int topSize = top.getSize();

        // Только shift-клик из НИЖНЕГО инвентаря (инвентарь игрока) -> вверх
        if (e.getRawSlot() < topSize) return;

        ItemStack moving = e.getCurrentItem();
        if (moving == null || moving.getType().isAir()) return;

        // Полностью отключаем ванильный перенос, дальше всё делаем вручную
        e.setCancelled(true);

        // Важно: работать с инвентарями лучше на следующем тике, чтобы ваниль/другие плагины не перетёрли изменения
        Bukkit.getScheduler().runTask(MagicMechanism.getInstance(), () -> {
            // Если игрок уже закрыл — выходим
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (p.getOpenInventory() == null) return;

            Inventory topNow = p.getOpenInventory().getTopInventory();
            if (topNow.getHolder() != holder) return; // игрок мог открыть другой GUI

            // Берём актуальный предмет из того же слота НИЖНЕГО инвентаря (куда кликнули)
            // rawSlot указывает на view; чтобы взять предмет "снизу", используем getClickedInventory в момент клика нельзя.
            // Поэтому проще: берём из bottom по e.getSlot() НЕЛЬЗЯ; используем вычисление через view.
            // Надёжный вариант: просто повторно читаем current item из bottom через rawSlot:
            ItemStack movingNow = p.getOpenInventory().getItem(e.getRawSlot());
            // В некоторых реализациях getItem(rawSlot) может вернуть null, тогда используем старое значение как fallback
            if (movingNow == null || movingNow.getType().isAir()) movingNow = moving.clone();

            int target = genInventory.findTargetSlot(topNow);
            if (target == -1) {
                // нет разрешённых мест — предмет остаётся у игрока
                return;
            }

            // Кладём 1:1 (как у тебя было). Если нужно стакание/частичный перенос — допишем отдельно.
            topNow.setItem(target, movingNow.clone());

            // Удаляем из инвентаря игрока то, что перенесли
            // Удаляем именно в rawSlot view
            p.getOpenInventory().setItem(e.getRawSlot(), null);

            // Дальше твоя доменная логика
            BlockState st = loc.getBlock().getState();
            if (st instanceof TileState tile) {
                ItemStack cell = topNow.getItem(9);

                if (EnergyCell.isEnergyCell(cell)) {
                    p.sendMessage("Аккумулятор вставлен в слот 1!");
                    GeneratorCellService.onCellInserted(loc);
                } else {
                    GeneratorCellService.onCellRemoved(loc);
                }

                MechanismStorage.saveItems(tile, topNow, Keys.KEY_ITEMS);
            }
        });
    }
    }


}
