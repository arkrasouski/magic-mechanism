package org.example.artyom.magicMechanism.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.data.enums.BarrierMenuActions;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.inventories.BarrierInventory;
import org.example.artyom.magicMechanism.inventories.EditPlayerInventory;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.mechanisms.Barrier;


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
            int buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
            //int freq = tile.getPersistentDataContainer().getOrDefault(Keys.FREQ, PersistentDataType.INTEGER, this.mechanism.getFrequency());
            double energyPercent = (double) (buf * 100) / mechanism.getCapacity();
            MechanismHolder holder = new MechanismHolder(clicked.getLocation(), MechanismType.BARRIER, energyPercent);
            Inventory gui = this.barrierInventory.openMenu(e.getPlayer(), holder, energyPercent);
            holder.setInventory(gui);

            Player p = e.getPlayer();
                p.sendMessage(ChatColor.YELLOW + this.mechanism.getName() + ": энергия=" + this.mechanism.getCurrentEnergy(tile) + "/" + this.mechanism.getCapacity()
                        + " частота=" + this.mechanism.getFrequency());
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

        if(!Barrier.isBarrier(b)) return;
        if(e.getRawSlot() >= barrierInventory.getSize()) return;

        if (barrierInventory.isBlocked(slot)) {
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void onClickEditPlayerBtn(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;
        ItemStack stack = e.getCurrentItem();
        if (stack == null) return;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        if(pdc.get(Keys.INVENTORY_ITEM, PersistentDataType.STRING)
                .equals(BarrierMenuActions.MAIN_MENU_EDIT_PLAYER.menuName)) {
            HumanEntity  human = e.getWhoClicked();
            if(human instanceof Player player) {
                Inventory editPlayerInventory = (new EditPlayerInventory()).openMenu(player, h, h.getEnergyPercent()); //Добавил перенос энергии между окнами

                player.openInventory(editPlayerInventory);
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
