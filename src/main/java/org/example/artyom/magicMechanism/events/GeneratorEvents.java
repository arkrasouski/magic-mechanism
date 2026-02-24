package org.example.artyom.magicMechanism.events;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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
import org.example.artyom.magicMechanism.items.GeneratorItem;
import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.BlockUtil;
import org.example.artyom.magicMechanism.utils.LogUtil;
import org.example.artyom.magicMechanism.utils.ToolUtil;

public class GeneratorEvents extends BaseMechanismEvents {

 private GeneratorGuiManager guiManager;
 private GenInventory genInventory;
 private GeneratorManager generatorManager;
public GeneratorEvents(MagicMechanism plugin, GeneratorManager generatorManager, GeneratorGuiManager guiManager){//, GeneratorCellService genService, GenInventory genInventory) {
    super(plugin, generatorManager, MechanismType.GENERATOR);
    this.generatorManager = generatorManager;
    this.guiManager = guiManager;
    this.genInventory = new GenInventory();
}
    @EventHandler
    public void onGeneratorPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        // Проверяем, является ли предмет генератором
        if (!isGeneratorItem(item)) return;

        // Проверяем, можно ли ставить здесь
        if (!canPlaceGenerator(block, player)) {
            event.setCancelled(true);
            player.sendMessage("§cНельзя установить генератор здесь!");
            return;
        }

        // Создаем генератор через менеджер
        Generator generator = new Generator(block.getLocation(), player);
        generatorManager.addMechanismToIndex(generator);

        // Отправляем сообщение
        player.sendMessage("§a✓ Генератор успешно установлен!");

        // Визуальный эффект
        spawnPlaceEffect(block);
    }
    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private boolean isGeneratorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        // Проверка на наличие метки генератора
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "generator_item"), PersistentDataType.BOOLEAN);
    }

    private boolean canPlaceGenerator(Block block, Player player) {
        // Проверка на пустой блок
        if (block.getType() != Material.AIR && BlockUtil.isReplaceableBlock(block)) {
            return false;
        }

        // Проверка на наличие другого генератора
        if (generatorManager.isMechanism(block)) {
            return false;
        }

        // Проверка прав
        return player.hasPermission("generator.place");
    }

    private boolean canBreakGenerator(Generator generator, Player player) {
        // Владелец всегда может сломать
        if (generator.getOwner() != null && generator.getOwner().equals(player)) {
            return true;
        }

        // Админы могут ломать чужие
        return player.hasPermission("generator.break.others");
    }

    private boolean shouldDropGenerator() {
        return plugin.getConfig().getBoolean("generator.drop-on-break", true);
    }

    private ItemStack createGeneratorItem() {
        // Создание предмета генератора
        ItemStack item = new ItemStack(this.mechanismType.getMaterial());
        // ... настройка метаданных
        return item;
    }

    private void openGeneratorGUI(Player player, Generator generator) {
        // Открытие GUI
        player.sendMessage("§6Энергия: " + generator.getEnergyLevel() +
                "/" + generator.getCapacity());
        // Здесь открытие инвентаря
    }

    private void spawnPlaceEffect(Block block) {
        block.getWorld().playSound(block.getLocation(),
                org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
        block.getWorld().spawnParticle(org.bukkit.Particle.PORTAL,
                block.getLocation().add(0.5, 1, 0.5), 20, 0.3, 0.3, 0.3, 0.1);
    }

    private void spawnBreakEffect(Block block) {
        block.getWorld().playSound(block.getLocation(),
                org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.5f);
        block.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION,
                block.getLocation().add(0.5, 0.5, 0.5), 1);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (mechanismManager instanceof GeneratorManager generatorManager) {
            // Проверяем, является ли сломанный блок генератором
            if (generatorManager.getMechanism(block) != null) {
                // Удаляем генератор
                generatorManager.deleteMechanism(block.getLocation());

                ItemStack tool = player.getInventory().getItemInMainHand();
                if (!ToolUtil.canBreakWithTool(player, tool)) {
                    event.setCancelled(true);
                    player.sendMessage("§c" + mechanismType.getGuiTitle() + " можно сломать только киркой!");
                    return;
                }
                event.getPlayer().sendMessage("§cГенератор разрушен!");
                // Отменяем обычный дроп
                event.setDropItems(false);
                if (block.getState() instanceof Container cont) {
                    cont.getInventory().clear();
                    cont.update(true);
                }
                // Удаляем блок
                block.setType(Material.AIR);

                // Дропаем предмет генератора
                ItemStack generatorItem = new GeneratorItem(plugin).createItem(1);
                //ItemStack mechanismItem = this.mechanismItem.createItem(1); // но с данными блока!
                block.getWorld().dropItemNaturally(block.getLocation(), generatorItem);
            }
        }
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
            int frequency = generator.getFrequency();

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
            player.sendMessage(ChatColor.GRAY + "  Частота: " + frequency);
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

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        // Загружаем генераторы из чанка
        generatorManager.loadMechanismsFromChunk(chunk);

       //LogUtil.warn("Чанк загружен: " + chunk.getX() + ", " + chunk.getZ());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        // Сохраняем генераторы перед выгрузкой
        generatorManager.saveMechanismsFromChunk(chunk);

        // Очищаем кэш
        generatorManager.unloadChunkMechanisms(chunk);

       //LogUtil.warn("Чанк выгружен: " + chunk.getX() + ", " + chunk.getZ());
    }

}
