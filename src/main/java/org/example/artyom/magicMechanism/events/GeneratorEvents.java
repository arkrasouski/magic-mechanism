package org.example.artyom.magicMechanism.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.inventories.GenInventory;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.inventories.MechanismStorage;
import org.example.artyom.magicMechanism.linkservice.GeneratorCellService;
import org.example.artyom.magicMechanism.energyitems.EnergyCell;
import org.example.artyom.magicMechanism.mechanisms.Generator;

public class GeneratorEvents extends BaseMechanismEvents {

 private GeneratorGuiManager guiManager;
 private GeneratorCellService genService;
 private GenInventory genInventory;

public GeneratorEvents(GeneratorGuiManager guiManager, GeneratorCellService genService, GenInventory genInventory) {
    super(new Generator());

    this.guiManager = guiManager;
    this.genService = genService;
    this.genInventory = genInventory;
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
        MechanismStorage.saveItems(tile, e.getView().getTopInventory());
    }
}
@EventHandler
public void onInteract(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    Block b = e.getClickedBlock();
    if (b == null) return;

    // генератор — это, например, DROPPER
    if (!Generator.isGenerator(b)) return;

    TileState tile = (TileState) b.getState();



    e.setCancelled(true); // чтобы не открылся ванильный дроппер



    int buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
    int freq = tile.getPersistentDataContainer().getOrDefault(Keys.FREQ, PersistentDataType.INTEGER, this.mechanism.getFrequency());
    MechanismHolder holder = new MechanismHolder(b.getLocation(), MechanismType.GENERATOR);

    Inventory gui = this.genInventory.openMenu(e.getPlayer(), holder, (double) (buf * 100) / mechanism.getCapacity());
    holder.setInventory(gui);

    // если тебе надо переносить реальные предметы из дроппера в GUI — решай сам:
    // либо gui = tile.getInventory(), либо loadItems(tile, gui) из PDC-хранилища.
    MechanismStorage.loadItems(tile, gui);

    if (e.getPlayer() instanceof Player p) {
        p.sendMessage(ChatColor.YELLOW + this.mechanism.getName() + ": энергия=" + buf + "/" + this.mechanism.getCapacity()
                + " частота=" + freq);
        p.openInventory(gui);
    }
}
    @EventHandler
    public void onClickEnergySlot(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;
        int slot = e.getSlot(); // индекс в верхнем инвентаре
        Location loc = h.getLocation();
        Block b = loc.getBlock();
        BlockState st = b.getState();
        if(!Generator.isGenerator(b)) return;
        if(e.getRawSlot() >= 27) return;

        if (genInventory.isBlocked(slot)) {
            e.setCancelled(true);
        }
        Bukkit.getScheduler().runTask(MagicMechanism.getInstance(), () -> {
            ItemStack cell = top.getItem(9);

            if (!(st instanceof TileState tile)) return;
            Player p = (Player) e.getWhoClicked();

            if (EnergyCell.isEnergyCell(cell)) {
                p.sendMessage("Аккумулятор вставлен в слот 1!");
                GeneratorCellService.onCellInserted(h.getLocation());
            } else {
                GeneratorCellService.onCellRemoved(h.getLocation());
            }
            // КЛЮЧЕВОЕ: синхронизируем PDC сразу чтобы фоновые тики увидели аккумулятор
            MechanismStorage.saveItems(tile, top);
        });
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShiftToGenerator(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;

        // Только shift-перенос
        if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        Location loc = h.getLocation();
        Block b = loc.getBlock();
        if(!Generator.isGenerator(b)) return;
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
            if (topNow.getHolder() != h) return; // игрок мог открыть другой GUI

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

                MechanismStorage.saveItems(tile, topNow);
            }
        });
    }


    @Override
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        super.onPlace(e);
        Block block = e.getBlockPlaced();
        if(Generator.isGenerator(block)) {
            this.genService.registerGenerator(block);
        }
    }

    @Override
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        super.onBreak(e);
        Block block = e.getBlock();
        this.genService.unregisterGenerator(block);
    }
}
