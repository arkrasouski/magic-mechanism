package org.example.artyom.magicMechanism.events;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.data.enums.*;
import org.example.artyom.magicMechanism.inventories.*;
import org.example.artyom.magicMechanism.mechanisms.Barrier;

import java.sql.SQLOutput;

import static org.example.artyom.magicMechanism.data.enums.BarrierMenuActions.*;
import static org.example.artyom.magicMechanism.data.enums.BarrierPlayerSettingsMenuActions.*;


public class BarrierEvents extends BaseMechanismEvents {

    BarrierInventory barrierInventory;

    public BarrierEvents(BarrierInventory barrierInventory) {
        super(new Barrier());
        this.barrierInventory = barrierInventory;
    }
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock(); // может быть null [web:41]
        if (clicked == null) return;

        if (Barrier.isBarrier(clicked)) {
            e.setCancelled(true);
            TileState tile = (TileState) clicked.getState();
            PersistentDataContainer container = tile.getPersistentDataContainer();
            int buf = container.getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
            //int freq = tile.getPersistentDataContainer().getOrDefault(Keys.FREQ, PersistentDataType.INTEGER, this.mechanism.getFrequency());
            double energyPercent = (double) (buf * 100) / mechanism.getCapacity();
            MechanismHolder holder = new BarrierHolder(clicked.getLocation(), MechanismType.BARRIER, energyPercent, BarrierScreenCategory.MAIN_MENU);
            Inventory mainInventory = this.barrierInventory.openMenu(e.getPlayer(), holder, energyPercent);
            if(container.has(Keys.BARRIER_INV_MAIN)){
                MechanismStorage.loadItems(tile, mainInventory, Keys.BARRIER_INV_MAIN);
            }
            holder.setInventory(mainInventory);
            Player p = e.getPlayer();
                p.sendMessage(ChatColor.YELLOW + this.mechanism.getName() + ": энергия=" + this.mechanism.getCurrentEnergy(tile) + "/" + this.mechanism.getCapacity()
                        + " частота=" + this.mechanism.getFrequency());
                p.openInventory(mainInventory);
            }

    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getInventory().getHolder() instanceof BarrierHolder h)) return;
        Location loc = h.getLocation();
        Block b = loc.getBlock();
        if (!Barrier.isBarrier(b)) return;
        if (!(b.getState() instanceof TileState tile)) return;
        System.out.println("lol");
        if (h.getScreenCategory() == BarrierScreenCategory.MAIN_MENU) {
            System.out.println("in");
            MechanismStorage.loadItems(tile, e.getView().getTopInventory(), Keys.BARRIER_INV_MAIN);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof BarrierHolder h)) return;

        BlockState st = h.getLocation().getBlock().getState();
        if (st instanceof TileState tile) {
            if (h.getScreenCategory() == BarrierScreenCategory.MAIN_MENU) {
                MechanismStorage.saveItems(tile, e.getView().getTopInventory(), Keys.BARRIER_INV_MAIN
                );
            }
            }

    }

    @EventHandler
    public void onClickBarrierInventory(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;
        int slot = e.getSlot(); // индекс в верхнем инвентаре
        Location loc = h.getLocation();
        Block b = loc.getBlock();

        if(!Barrier.isBarrier(b)) return;
        if(e.getRawSlot() >= barrierInventory.getSize()) return;

        if (barrierInventory.isBlocked(slot)) {
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void onClickPlayerSettingsBtns(InventoryClickEvent e) {

        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;
        ItemStack stack = e.getCurrentItem();
        if (stack == null) return;
        MenuAction action = MenuAction.fromPDC(stack, Keys.INVENTORY_ITEM);
        HumanEntity  human = e.getWhoClicked();
        if(human instanceof Player player) {
            if(action instanceof BarrierMenuActions a) {
                switch (a) {
                    case MAIN_MENU_EDIT_PLAYER -> {

                        EditPlayerHolder newHolder = new EditPlayerHolder(
                                h.getLocation(),
                                MechanismType.BARRIER,
                                h.getEnergyPercent(),
                                BarrierScreenCategory.PLAYER_SETTINGS
                        );


                            Inventory newInv = new EditPlayerInventory().openMenu(player, newHolder, h.getEnergyPercent());


//                            System.out.println("=== НОВЫЙ ИНВЕНТАРЬ ===");
//                            System.out.println("Заголовок: " + newInv.getHolder().toString());
//                            System.out.println("Размер: " + newInv.getSize());
//                            System.out.println("Холдер: " + newInv.getHolder().getClass().getSimpleName());
//
//                            int nonEmptySlots = 0;
//                            for (int i = 0; i < newInv.getSize(); i++) {
//                                if (newInv.getItem(i) != null) {
//                                    System.out.println("Слот " + i + ": " + newInv.getItem(i).getType());
//                                    nonEmptySlots++;
//                                }
//                            }
//                            System.out.println("Непустых слотов: " + nonEmptySlots);

                            player.openInventory(newInv);
                        return;




                    }
                    default -> {
                        // для новых enum без кода
                        System.out.println("Неизвестное действие");
                    }
                }
            }
            else if (action instanceof BarrierPlayerSettingsMenuActions a) {
                switch (a) {
                    case PLAYER_SETTINGS_ALLOW_CHEST -> {
                        System.out.println("Разрешены сундуки");
                    }
                    case PLAYER_SETTINGS_DENY_CHEST -> {
                        System.out.println("Запрещены сундуки");
                    }
                    case PLAYER_SETTINGS_ALLOW_DAMAGE -> {
                        System.out.println("Разрешен урон");
                    }
                    case PLAYER_SETTINGS_DENY_DAMAGE -> {
                        System.out.println("Запрещен урон");
                    }
                    case PLAYER_SETTINGS_REMOVE_PLAYER -> {
                        System.out.println("Удален игрок");
                    }
                    case PLAYER_SETTINGS_RETURN -> {
                        System.out.println("Возврат в главное меню");
                        Inventory mainBarrierInventory = (new BarrierInventory()).openMenu(player, h, h.getEnergyPercent());
                        player.openInventory(mainBarrierInventory);
                    }
                    default -> {
                        // для новых enum без кода
                        System.out.println("Неизвестное действие");
                    }
                }
            }
       }


    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShiftToGenerator(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;

        // Только shift-перенос
        if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        Location loc = h.getLocation();
        Block b = loc.getBlock();
        if(!Barrier.isBarrier(b)) return;
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

            int target = barrierInventory.findTargetSlot(topNow);
            if (target == -1) {
                // нет разрешённых мест — предмет остаётся у игрока
                return;
            }


        });
    }


}
