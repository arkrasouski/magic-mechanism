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
import org.example.artyom.magicMechanism.Keys;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.GeneratorGuiManager;
import org.example.artyom.magicMechanism.inventories.FillGenInventory;
import org.example.artyom.magicMechanism.inventories.GenHolder;
import org.example.artyom.magicMechanism.inventories.GenStorage;
import org.example.artyom.magicMechanism.service.GeneratorService;
import org.example.artyom.magicMechanism.utils.EnergyCellUtil;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;

public class GeneratorEvents extends BaseMechanismEvents {

 private GeneratorGuiManager guiManager;
 private GeneratorService genService;

public GeneratorEvents(GeneratorGuiManager guiManager, GeneratorService genService) {
    super(new GeneratorUtil());

    this.guiManager = guiManager;
    this.genService = genService;
}

@EventHandler
public void onOpen(InventoryOpenEvent e) {
    if (!(e.getInventory().getHolder() instanceof GenHolder h)) return;
    guiManager.addViewer(h.getLocation(), e.getPlayer().getUniqueId());
}


@EventHandler
public void onClose(InventoryCloseEvent e) {
    if (!(e.getView().getTopInventory().getHolder() instanceof GenHolder h)) return;

    guiManager.removeViewer(h.getLocation(), e.getPlayer().getUniqueId());
    BlockState st = h.getLocation().getBlock().getState();
    if (st instanceof TileState tile) {
        GenStorage.saveItems(tile, e.getView().getTopInventory());
    }
}
@EventHandler
public void onInteract(PlayerInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    Block b = e.getClickedBlock();
    if (b == null) return;

    // генератор — это, например, DROPPER
    if (!GeneratorUtil.isGenerator(b)) return;

    TileState tile = (TileState) b.getState();



    e.setCancelled(true); // чтобы не открылся ванильный дроппер



    int buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
    int freq = tile.getPersistentDataContainer().getOrDefault(Keys.FREQ, PersistentDataType.INTEGER, this.mechanism.getFrequency());
    GenHolder holder = new GenHolder(b.getLocation());
    System.out.println((double) (buf * 100) / mechanism.getCapacity());
    Inventory gui = FillGenInventory.openMenu(e.getPlayer(), holder, (double) (buf * 100) / mechanism.getCapacity());
    holder.setInventory(gui);

    // если тебе надо переносить реальные предметы из дроппера в GUI — решай сам:
    // либо gui = tile.getInventory(), либо loadItems(tile, gui) из PDC-хранилища.
    GenStorage.loadItems(tile, gui);

    if (e.getPlayer() instanceof Player p) {
        p.sendMessage(ChatColor.YELLOW + this.mechanism.getName() + ": энергия=" + buf + "/" + this.mechanism.getCapacity()
                + " частота=" + freq);
        p.openInventory(gui);
    }
}
    @EventHandler
    public void onClickEnergySlot(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof GenHolder h)) return;
        int slot = e.getSlot(); // индекс в верхнем инвентаре
        if(e.getRawSlot() >= 27) return;
        if (isBlocked(slot)) {
            e.setCancelled(true);
        }
        Bukkit.getScheduler().runTask(MagicMechanism.getInstance(), () -> {
            ItemStack cell = top.getItem(9);
            Location loc = h.getLocation();

            BlockState st = loc.getBlock().getState();
            if (!(st instanceof TileState tile)) return;
            Player p = (Player) e.getWhoClicked();

            if (EnergyCellUtil.isEnergyCell(cell)) {
                p.sendMessage("Аккумулятор вставлен в слот 1!");
                GeneratorService.onCellInserted(h.getLocation());
            } else {
                GeneratorService.onCellRemoved(h.getLocation());
            }
            // КЛЮЧЕВОЕ: синхронизируем PDC сразу чтобы фоновые тики увидели аккумулятор
            GenStorage.saveItems(tile, top);
        });
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShiftToGenerator(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof GenHolder h)) return;

        // Только shift-перенос
        if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return;

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

            int target = findTargetSlot(topNow, movingNow);
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
            Location loc = h.getLocation();
            BlockState st = loc.getBlock().getState();
            if (st instanceof TileState tile) {
                ItemStack cell = topNow.getItem(9);

                if (EnergyCellUtil.isEnergyCell(cell)) {
                    p.sendMessage("Аккумулятор вставлен в слот 1!");
                    GeneratorService.onCellInserted(loc);
                } else {
                    GeneratorService.onCellRemoved(loc);
                }

                GenStorage.saveItems(tile, topNow);
            }
        });
    }
    private int findTargetSlot(Inventory top, ItemStack moving) {
        // Пример: разрешены только 19-27 и только если слот пустой
        for (int slot = 9; slot <= 10; slot++) {
            //if (isBlocked(slot, moving)) continue;
            ItemStack cur = top.getItem(slot);
            if (cur == null || cur.getType().isAir()) return slot;
        }
        return -1;
    }
    private boolean isBlocked(int slot) {
        // пример: заблокировать ВСЕ слоты верхнего инвентаря
        // return true;

        // или, например, только 10-11 и 19-27
        return slot != 10 && slot != 9;
    }

    @Override
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        super.onPlace(e);
        Block block = e.getBlockPlaced();
        if(GeneratorUtil.isGenerator(block)) {
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
