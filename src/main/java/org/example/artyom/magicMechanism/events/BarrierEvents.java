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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.enums.MenuAction;
import org.example.artyom.magicMechanism.data.enums.barrier.*;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.inventories.MechanismStorage;
import org.example.artyom.magicMechanism.inventories.barrier.AddPlayerInventory;
import org.example.artyom.magicMechanism.inventories.barrier.holders.AddPlayerHolder;
import org.example.artyom.magicMechanism.inventories.barrier.holders.BarrierHolder;
import org.example.artyom.magicMechanism.inventories.barrier.BarrierInventory;
import org.example.artyom.magicMechanism.inventories.barrier.holders.EditPlayerHolder;
import org.example.artyom.magicMechanism.inventories.barrier.EditPlayerInventory;
import org.example.artyom.magicMechanism.mechanisms.Barrier;
import org.example.artyom.magicMechanism.utils.ItemsUtil;
import org.example.artyom.magicMechanism.utils.LogUtil;

import java.util.List;

import static org.example.artyom.magicMechanism.data.enums.barrier.BarrierMenuActions.MAIN_MENU_EDIT_PLAYER;
import static org.example.artyom.magicMechanism.data.enums.barrier.BarrierPlayerListMenuActions.RETURN_BACK;
import static org.example.artyom.magicMechanism.data.enums.barrier.BarrierPlayerSettingsMenuActions.PLAYER_SETTINGS_ALLOW_CHEST;
import static org.example.artyom.magicMechanism.data.enums.barrier.BarrierPlayerSettingsMenuActions.PLAYER_SETTINGS_DENY_CHEST;


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
            // ...
            //  Inventory mainInventory = this.barrierInventory.openMenu(p, holder, energyPercent);

            // Загрузка + энергия
//            if(container.has(Keys.BARRIER_INV_MAIN)){
//                MechanismStorage.loadItems(tile, mainInventory, Keys.BARRIER_INV_MAIN);
//            }
            this.barrierInventory.updateEnergyBar(mainInventory, holder, energyPercent);

            //holder.setInventory(mainInventory);
            //e.getPlayer().openInventory(mainInventory);
            holder.setInventory(mainInventory);
            Player p = e.getPlayer();
                p.sendMessage(ChatColor.YELLOW + this.mechanism.getName() + ": энергия=" + this.mechanism.getCurrentEnergy(tile) + "/" + this.mechanism.getCapacity()
                        + " частота=" + this.mechanism.getFrequency());
                p.openInventory(mainInventory);

            }

    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        Object holder = e.getInventory().getHolder();
        LogUtil.info("Открытие: " + holder + " | " + e.getView().getTitle());
    }


    @EventHandler
    public void onClose(InventoryCloseEvent e) {

        Object holderObj = e.getView().getTopInventory().getHolder();
        System.out.println("Закрытие: " + (holderObj != null ? holderObj.getClass().getSimpleName() : "null"));

        BlockState st = null;
        NamespacedKey saveKey = null;

        // BarrierHolder (ТОЛЬКО главный класс)
        if (holderObj != null && holderObj.getClass() == BarrierHolder.class) {
            BarrierHolder h = (BarrierHolder) holderObj;
            if (h.getScreenCategory() == BarrierScreenCategory.MAIN_MENU) {
                st = h.getLocation().getBlock().getState();
                saveKey = Keys.BARRIER_INV_MAIN;
            }
        }
        // EditPlayerHolder (наследник)
        else if (holderObj instanceof EditPlayerHolder holder) {
            st = holder.getLocation().getBlock().getState();
            saveKey = Keys.BARRIER_INV_EDIT_PLAYER[holder.getSlot() - 1];
        }


        if (st instanceof TileState tile && saveKey != null) {
            System.out.println("Сохраняем под ключом: " + saveKey);
            MechanismStorage.saveItems(tile, e.getView().getTopInventory(), saveKey);
        } else {
            System.out.println("НЕ сохраняем: st=" + (st != null) + ", key=" + saveKey);
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

//    @EventHandler
//    public void onClickPlayerSettingsBtns(InventoryClickEvent e) {
//
//        Inventory top = e.getView().getTopInventory();
//        if (!(top.getHolder() instanceof MechanismHolder h)) return;
//        ItemStack stack = e.getCurrentItem();
//        if (stack == null) return;
//        MenuAction action = MenuAction.fromPDC(stack, Keys.INVENTORY_ITEM);
//        HumanEntity  human = e.getWhoClicked();
//
//        if(human instanceof Player player) {
//            if(action instanceof BarrierMenuActionSlot a) {
//
//                if(a.action == MAIN_MENU_EDIT_PLAYER) {
//
//
//                        // 1. СОЗДАЕМ НОВЫЙ инвентарь
//                        EditPlayerHolder newHolder = new EditPlayerHolder(
//                                h.getLocation(), MechanismType.BARRIER, h.getEnergyPercent(),
//                                BarrierScreenCategory.PLAYER_SETTINGS, a.getSlotPlayer()
//                        );
//
//                        Inventory newInv = new EditPlayerInventory().openMenu(player, newHolder, h.getEnergyPercent());
//
//                        // 2. Загружаем ТОЛЬКО в НОВЫЙ инвентарь!
//                        Location loc = h.getLocation();
//                        Block b = loc.getBlock();
//                        BlockState bs = b.getState();
//                        if(bs instanceof TileState tile) {
//                            NamespacedKey editKey = Keys.BARRIER_INV_EDIT_PLAYER[a.getSlotPlayer() - 1];
//                            if(tile.getPersistentDataContainer().has(editKey)){
//                                MechanismStorage.loadItems(tile, newInv, editKey); // ← newInv!!!
//
//                                this.barrierInventory.updateEnergyBar(newInv, newHolder , h.getEnergyPercent());
//                            }
//                        }
//                        if(e.getCurrentItem().getItemMeta().getDisplayName().startsWith("Игрок №")){
//                            return;
//                        }
//                        player.openInventory(newInv);
//                        return;
//
//                    }
//                }
//            else if (action instanceof BarrierMenuActions a) {
//                if(a == BarrierMenuActions.MAIN_MENU_ADD_PLAYER) {
//                    LogUtil.warn("Открываю игроков");
//                    AddPlayerHolder newHolder = new AddPlayerHolder(
//                            h.getLocation(),
//                            MechanismType.BARRIER,
//                            h.getEnergyPercent(),
//                            BarrierScreenCategory.PLAYER_LIST
//                    );
//                    Inventory newInv = new AddPlayerInventory().openMenu(player, newHolder, h.getEnergyPercent());
//                    player.openInventory(newInv);
//                    return;
//                }
//            }
//
//
//            else if (action instanceof BarrierPlayerSettingsMenuActions a) {
//
//                switch (a) {
//                    case PLAYER_SETTINGS_ALLOW_CHEST -> {
//                        System.out.println("Разрешены сундуки");
//                        top.setItem(0, ItemsUtil.create(Material.PINK_WOOL, 1,
//                "Запрет открывать сундуки", PLAYER_SETTINGS_DENY_CHEST.getPdcKey(),
//                List.of("Нажмите, чтобы запретить", "использовать сундуки, печки и т.д.")));
//        }
//                    case PLAYER_SETTINGS_DENY_CHEST -> {
//                        System.out.println("Запрещены сундуки");
//                        top.setItem(0, ItemsUtil.create(Material.LIME_WOOL, 1,
//                                "Доступ к сундукам", PLAYER_SETTINGS_ALLOW_CHEST.getPdcKey(),
//                                List.of("Нажмите, чтобы разрешить", "использовать сундуки, печки и т.д.")));
//                    }
//                    case PLAYER_SETTINGS_ALLOW_DAMAGE -> {
//                        System.out.println("Разрешен урон");
//                    }
//                    case PLAYER_SETTINGS_DENY_DAMAGE -> {
//                        System.out.println("Запрещен урон");
//                    }
//                    case PLAYER_SETTINGS_REMOVE_PLAYER -> {
//                        System.out.println("Удален игрок");
//
//                    }
//                    case PLAYER_SETTINGS_RETURN -> {
//                        LogUtil.info("Возврат в главное меню из " + h.getClass().getSimpleName());
//
//                        // 1. Создаем BarrierHolder для ГЛАВНОГО меню
//                        Location loc = h.getLocation();
//                        Block b = loc.getBlock();
//                        TileState tile = (TileState) b.getState();
//                        int buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
//                        double energyPercent = (double) (buf * 100) / mechanism.getCapacity();
//
//                        BarrierHolder mainHolder = new BarrierHolder(
//                                loc, MechanismType.BARRIER, energyPercent, BarrierScreenCategory.MAIN_MENU
//                        );
//
//                        // 2. Используем this.barrierInventory!
//                        Inventory mainInventory = this.barrierInventory.openMenu(player, mainHolder, energyPercent);
//
//                        // 3. Загружаем данные главного меню
//                        if(tile.getPersistentDataContainer().has(Keys.BARRIER_INV_MAIN)) {
//                            MechanismStorage.loadItems(tile, mainInventory, Keys.BARRIER_INV_MAIN);
//                        }
//
//                        // 4. Обновляем энергию
//                        this.barrierInventory.updateEnergyBar(mainInventory, mainHolder, energyPercent);
//
//                        mainHolder.setInventory(mainInventory);
//                        player.openInventory(mainInventory);
//                        return;
//                    }
//                    default -> {
//                        // для новых enum без кода
//                        LogUtil.warn("Неизвестное действие");
//                    }
//
//                }
//
//            }
//            else if (action instanceof BarrierPlayerListMenuActionSlot a) {
//                if (a.action == BarrierPlayerListMenuActions.PLAYERLIST) {
//                        ItemStack clicked = e.getCurrentItem();
//                        String playerName = clicked.getItemMeta().getDisplayName();
//                        e.getInventory().setItem(a.getSlotPlayer(), ItemsUtil.create(Material.LIME_WOOL, 1, playerName,
//                                a.getPdcKey(), List.of("Игрок добавлен в приват!")));
//
//                        Block barrier = h.getLocation().getBlock();
//                        BlockState barrierState = barrier.getState();
//                        if(barrierState instanceof TileState tile) {
//                            PersistentDataContainer container = tile.getPersistentDataContainer();
//                            for(int i = 0; i < 12; i++) {
//                                if(!container.has(Keys.BARRIER_ADDED_PLAYER_NAMES[i])) {
//                                    LogUtil.warn("kek +" + Keys.BARRIER_ADDED_PLAYER_NAMES[i]);
//                                    container.set(Keys.BARRIER_ADDED_PLAYER_NAMES[i], PersistentDataType.STRING, playerName);
//                                    tile.update();
//                                    break;
//                                }
//                            }
//                        }
//
//
//                }
//            }
//            else if (action instanceof BarrierPlayerListMenuActions a) {
//                switch (a) {
//                    case RETURN_BACK -> {
//                        LogUtil.info("Возврат в главное меню из " + h.getClass().getSimpleName());
//
//                        // 1. Создаем BarrierHolder для ГЛАВНОГО меню
//                        Location loc = h.getLocation();
//                        Block b = loc.getBlock();
//                        TileState tile = (TileState) b.getState();
//                        int buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
//                        double energyPercent = (double) (buf * 100) / mechanism.getCapacity();
//
//                        BarrierHolder mainHolder = new BarrierHolder(
//                                loc, MechanismType.BARRIER, energyPercent, BarrierScreenCategory.MAIN_MENU
//                        );
//
//                        // 2. Используем this.barrierInventory!
//                        Inventory mainInventory = this.barrierInventory.openMenu(player, mainHolder, energyPercent);
//
//                        // 3. Загружаем данные главного меню
////                        if(tile.getPersistentDataContainer().has(Keys.BARRIER_INV_MAIN)) {
////                            MechanismStorage.loadItems(tile, mainInventory, Keys.BARRIER_INV_MAIN);
////                        }
//
//                        // 4. Обновляем энергию
//                        this.barrierInventory.updateEnergyBar(mainInventory, mainHolder, energyPercent);
//
//                        mainHolder.setInventory(mainInventory);
//                        player.openInventory(mainInventory);
//                        return;
//                    }
//                }
//            }
//
//            else {
//                LogUtil.warn("Неизвестное действие главного меню барьера");
//
//            }
//       }
//
//
//    }

    @EventHandler
    public void onClickPlayerSettingsBtns(InventoryClickEvent e) {
        e.setCancelled(true); // Всегда отменяем!

        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;

        ItemStack stack = e.getCurrentItem();
        if (stack == null) return;

        MenuAction action = MenuAction.fromPDC(stack, Keys.INVENTORY_ITEM);
        if (!(e.getWhoClicked() instanceof Player player)) return;

        handleBarrierMenuAction(e, player, h, action);
    }

    private void handleBarrierMenuAction(InventoryClickEvent e, Player player, MechanismHolder h, MenuAction action) {
        switch (action) {
            case BarrierMenuActionSlot slotAction when slotAction.action == MAIN_MENU_EDIT_PLAYER ->
                    openEditPlayerMenu(e, player, h, (BarrierMenuActionSlot) action);

            case BarrierMenuActions addPlayer when addPlayer == BarrierMenuActions.MAIN_MENU_ADD_PLAYER ->
                    openAddPlayerMenu(player, h);

            case BarrierPlayerSettingsMenuActions settings -> handlePlayerSettings(e, player, h, settings);

            case BarrierPlayerListMenuActionSlot listSlot when listSlot.action == BarrierPlayerListMenuActions.PLAYERLIST ->
                    addPlayerToBarrier(e, player, h, (BarrierPlayerListMenuActionSlot) action);

            case BarrierPlayerListMenuActions.RETURN_BACK -> openMainMenu(player, h);

            default -> LogUtil.warn("Неизвестное действие: " + action);
        }
    }

    private void openEditPlayerMenu(InventoryClickEvent e, Player player, MechanismHolder h, BarrierMenuActionSlot action) {
        int slotPlayer = action.getSlotPlayer();

        // Проверяем, не EditPlayer ли уже открыт
        if (e.getCurrentItem().getItemMeta().getDisplayName().startsWith("Игрок №")) {
            return;
        }

        EditPlayerHolder newHolder = new EditPlayerHolder(
                h.getLocation(), MechanismType.BARRIER, h.getEnergyPercent(),
                BarrierScreenCategory.PLAYER_SETTINGS, slotPlayer
        );

        Inventory newInv = new EditPlayerInventory().openMenu(player, newHolder, h.getEnergyPercent());

        // Загружаем данные
        if (loadEditPlayerData(newInv, h.getLocation(), slotPlayer)) {
            barrierInventory.updateEnergyBar(newInv, newHolder, h.getEnergyPercent());
        }

        player.openInventory(newInv);
    }

    private void openAddPlayerMenu(Player player, MechanismHolder h) {
        AddPlayerHolder newHolder = new AddPlayerHolder(
                h.getLocation(), MechanismType.BARRIER, h.getEnergyPercent(), BarrierScreenCategory.PLAYER_LIST
        );
        Inventory newInv = new AddPlayerInventory().openMenu(player, newHolder, h.getEnergyPercent());
        player.openInventory(newInv);
    }

    private void handlePlayerSettings(InventoryClickEvent e, Player player, MechanismHolder h, BarrierPlayerSettingsMenuActions action) {
        switch (action) {
            case PLAYER_SETTINGS_ALLOW_CHEST -> updateChestSetting(e.getInventory(), true);
            case PLAYER_SETTINGS_DENY_CHEST -> updateChestSetting(e.getInventory(), false);
            case PLAYER_SETTINGS_ALLOW_DAMAGE -> LogUtil.info("Разрешен урон");
            case PLAYER_SETTINGS_DENY_DAMAGE -> LogUtil.info("Запрещен урон");
            case PLAYER_SETTINGS_REMOVE_PLAYER -> LogUtil.info("Удален игрок");
            case PLAYER_SETTINGS_RETURN -> openMainMenu(player, h);
        }
    }

    private void updateChestSetting(Inventory inv, boolean deny) {
        ItemStack chestItem = deny ?
                ItemsUtil.create(Material.PINK_WOOL, 1, "Запрет открывать сундуки",
                        PLAYER_SETTINGS_DENY_CHEST.getPdcKey(),
                        List.of("Нажмите, чтобы запретить", "использовать сундуки, печки и т.д.")) :
                ItemsUtil.create(Material.LIME_WOOL, 1, "Доступ к сундукам",
                        PLAYER_SETTINGS_ALLOW_CHEST.getPdcKey(),
                        List.of("Нажмите, чтобы разрешить", "использовать сундуки, печки и т.д."));

        inv.setItem(0, chestItem);
        LogUtil.info("Разрешен урон");
    }

    private boolean loadEditPlayerData(Inventory inv, Location loc, int slotIndex) {
        BlockState bs = loc.getBlock().getState();
        if (!(bs instanceof TileState tile)) return false;

        NamespacedKey editKey = Keys.BARRIER_INV_EDIT_PLAYER[slotIndex - 1];
        if (!tile.getPersistentDataContainer().has(editKey)) return false;

        MechanismStorage.loadItems(tile, inv, editKey);
        LogUtil.info("Загружено EditPlayer из " + editKey);
        return true;
    }

    private void addPlayerToBarrier(InventoryClickEvent e, Player player, MechanismHolder h, BarrierPlayerListMenuActionSlot action) {
        ItemStack clicked = e.getCurrentItem();
        String playerName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        e.getInventory().setItem(action.getSlotPlayer(),
                ItemsUtil.create(Material.LIME_WOOL, 1, playerName,
                        action.getPdcKey(), List.of("Игрок добавлен в приват!")));

        savePlayerNameToBarrier(h.getLocation(), playerName);
    }

    private void savePlayerNameToBarrier(Location loc, String playerName) {
        BlockState barrierState = loc.getBlock().getState();
        if (!(barrierState instanceof TileState tile)) return;

        PersistentDataContainer container = tile.getPersistentDataContainer();
        for (int i = 0; i < 12; i++) {
            if (!container.has(Keys.BARRIER_ADDED_PLAYER_NAMES[i])) {
                container.set(Keys.BARRIER_ADDED_PLAYER_NAMES[i], PersistentDataType.STRING, playerName);
                tile.update();
                LogUtil.info("Игрок '" + playerName + "' сохранен в слот " + i);
                break;
            }
        }
    }

    private void openMainMenu(Player player, MechanismHolder h) {
        Location loc = h.getLocation();
        BlockState bs = loc.getBlock().getState();
        if (!(bs instanceof TileState tile)) return;

        int buf = tile.getPersistentDataContainer().getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);
        double energyPercent = (double) (buf * 100) / mechanism.getCapacity();

        BarrierHolder mainHolder = new BarrierHolder(loc, MechanismType.BARRIER, energyPercent, BarrierScreenCategory.MAIN_MENU);
        Inventory mainInventory = barrierInventory.openMenu(player, mainHolder, energyPercent);

//        if (tile.getPersistentDataContainer().has(Keys.BARRIER_INV_MAIN)) {
//            MechanismStorage.loadItems(tile, mainInventory, Keys.BARRIER_INV_MAIN);
//        }

        barrierInventory.updateEnergyBar(mainInventory, mainHolder, energyPercent);
        mainHolder.setInventory(mainInventory);
        player.openInventory(mainInventory);
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
