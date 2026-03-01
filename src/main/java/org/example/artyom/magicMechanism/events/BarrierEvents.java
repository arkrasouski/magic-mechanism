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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.data.enums.MenuAction;
import org.example.artyom.magicMechanism.data.enums.barrier.*;
import org.example.artyom.magicMechanism.database.DatabaseManager;
import org.example.artyom.magicMechanism.inventories.MechanismHolder;
import org.example.artyom.magicMechanism.inventories.MechanismStorage;
import org.example.artyom.magicMechanism.inventories.barrier.AddPlayerInventory;
import org.example.artyom.magicMechanism.inventories.barrier.holders.AddPlayerHolder;
import org.example.artyom.magicMechanism.inventories.barrier.holders.BarrierHolder;
import org.example.artyom.magicMechanism.inventories.barrier.BarrierInventory;
import org.example.artyom.magicMechanism.inventories.barrier.holders.EditPlayerHolder;
import org.example.artyom.magicMechanism.inventories.barrier.EditPlayerInventory;
import org.example.artyom.magicMechanism.items.GeneratorItem;
import org.example.artyom.magicMechanism.linkservice.GeneratorBarrierService;
import org.example.artyom.magicMechanism.managers.BarrierManager;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.managers.NetworkManager;
import org.example.artyom.magicMechanism.mechanisms.Barrier;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.BlockUtil;
import org.example.artyom.magicMechanism.utils.ItemsUtil;
import org.example.artyom.magicMechanism.utils.LogUtil;
import org.example.artyom.magicMechanism.utils.ToolUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.example.artyom.magicMechanism.data.enums.barrier.BarrierMenuActions.MAIN_MENU_EDIT_PLAYER;
import static org.example.artyom.magicMechanism.data.enums.barrier.BarrierPlayerSettingsMenuActions.PLAYER_SETTINGS_ALLOW_CHEST;
import static org.example.artyom.magicMechanism.data.enums.barrier.BarrierPlayerSettingsMenuActions.PLAYER_SETTINGS_DENY_CHEST;


public class BarrierEvents extends BaseMechanismEvents<Barrier, BarrierManager> {
    BarrierInventory barrierInventory;
    DatabaseManager databaseManager;

    public BarrierEvents(MagicMechanism plugin, BarrierManager barrierManager, DatabaseManager db, GeneratorBarrierService service, NetworkManager networkManager) {
        super(plugin, barrierManager, MechanismType.BARRIER, service, networkManager);
        this.databaseManager = db;
        this.barrierInventory = new BarrierInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock(); // может быть null [web:41]
        if (clicked == null) return;

        Barrier barrier = mechanismManager.getMechanism(clicked);

        if (barrier != null) {

            e.setCancelled(true);
            if(!(clicked.getState() instanceof TileState tile)) {return;}
            PersistentDataContainer container = tile.getPersistentDataContainer();
            int buf = barrier.getEnergyLevel();
            double energyPercent = (double) (buf * 100) / barrier.getCapacity();
            MechanismHolder holder = new BarrierHolder(clicked.getLocation(), MechanismType.BARRIER, energyPercent, BarrierScreenCategory.MAIN_MENU);
            Inventory mainInventory = this.barrierInventory.openMenu(e.getPlayer(), holder, energyPercent);
            this.barrierInventory.updateEnergyBar(mainInventory, holder, energyPercent);
            holder.setInventory(mainInventory);
            Player p = e.getPlayer();

                p.sendMessage(ChatColor.YELLOW + ": энергия=" + barrier.getEnergyLevel() + "/" + barrier.getCapacity()
                        + " частота=");
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


        if (st instanceof TileState tile && saveKey != null) {

            MechanismStorage.saveItems(tile, e.getView().getTopInventory(), saveKey);
        }
    }

    @EventHandler
    public void onClickBarrierInventory(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;
        int slot = e.getSlot(); // индекс в верхнем инвентаре
        Location loc = h.getLocation();
        Block b = loc.getBlock();

        Barrier barrier = mechanismManager.getMechanism(b);
        if (barrier == null) return;
        if (e.getRawSlot() >= barrierInventory.getSize()) return;

        if (barrierInventory.isBlocked(slot)) {
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void onClickPlayerSettingsBtns(InventoryClickEvent e) {


        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;
        Location loc = h.getLocation();
        Block b = loc.getBlock();

            Barrier barrier = mechanismManager.getMechanism(b);
            if (barrier == null) return;
            ItemStack stack = e.getCurrentItem();
            if (stack == null || !stack.hasItemMeta()) return;

            MenuAction action = MenuAction.fromPDC(stack, Keys.INVENTORY_ITEM);
            if (!(e.getWhoClicked() instanceof Player player)) return;

            handleBarrierMenuAction(e, player, h, action);
    }

    private void handleBarrierMenuAction(InventoryClickEvent e, Player player, MechanismHolder h, MenuAction action) {
        LogUtil.warn(action.toString());
        switch (action) {
            case BarrierMenuActionSlot slotAction when slotAction.action == MAIN_MENU_EDIT_PLAYER ->
                    openEditPlayerMenu(e, player, h, (BarrierMenuActionSlot) action);

            case BarrierMenuActions addPlayer when addPlayer == BarrierMenuActions.MAIN_MENU_ADD_PLAYER ->
                    openAddPlayerMenu(player, h, 1);

            case BarrierPlayerSettingsMenuActions settings -> handlePlayerSettings(e, player, h, settings);

            case BarrierPlayerListMenuActionSlot listSlot when listSlot.action == BarrierPlayerListMenuActions.PLAYERLIST ->
                    addPlayerToBarrier(e, player, h, (BarrierPlayerListMenuActionSlot) action);

            case BarrierPlayerListMenuActions.PREVIOUS_PAGE -> openAddPlayerMenu(player, h, Integer.parseInt(e.getCurrentItem().getItemMeta().getDisplayName().split(" ")[1]));
            case BarrierPlayerListMenuActions.NEXT_PAGE -> openAddPlayerMenu(player, h, Integer.parseInt(e.getCurrentItem().getItemMeta().getDisplayName().split(" ")[1]));
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

    private void openAddPlayerMenu(Player player, MechanismHolder h, int page) {
        LogUtil.warn("_" + page);
        AddPlayerHolder newHolder = new AddPlayerHolder(
                h.getLocation(), MechanismType.BARRIER, h.getEnergyPercent(), BarrierScreenCategory.PLAYER_LIST
        );
        ArrayList<String> playerNames = new ArrayList<>();
        String sql = String.format("""
            SELECT name FROM Players
            ORDER BY name ASC
            OFFSET %d ROWS FETCH FIRST 16 ROWS ONLY;
        """, 16 * (page - 1));
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    playerNames.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Ошибка получения игроков", e);

        }
        Inventory newInv = new AddPlayerInventory(page, playerNames).openMenu(player, newHolder, h.getEnergyPercent());
        player.openInventory(newInv);
    }

    private void handlePlayerSettings(InventoryClickEvent e, Player player, MechanismHolder h, BarrierPlayerSettingsMenuActions action) {
        switch (action) {
            case PLAYER_SETTINGS_ALLOW_CHEST -> updateChestSetting(e.getInventory(), false);
            case PLAYER_SETTINGS_DENY_CHEST -> updateChestSetting(e.getInventory(), true);
            case PLAYER_SETTINGS_ALLOW_DAMAGE -> LogUtil.info("Разрешен урон");
            case PLAYER_SETTINGS_DENY_DAMAGE -> LogUtil.info("Запрещен урон");
            case PLAYER_SETTINGS_REMOVE_PLAYER -> removePlayer(player, h);
            case PLAYER_SETTINGS_RETURN -> openMainMenu(player, h);
        }
    }

    private void updateChestSetting(Inventory inv, boolean deny) {
        ItemStack chestItem = deny ?
                ItemsUtil.create(Material.LIME_WOOL, 1, "Доступ к сундукам",
                        PLAYER_SETTINGS_ALLOW_CHEST.getPdcKey(),
                        List.of("Нажмите, чтобы разрешить", "использовать сундуки, печки и т.д."))
                :
                ItemsUtil.create(Material.PINK_WOOL, 1, "Запрет открывать сундуки",
                PLAYER_SETTINGS_DENY_CHEST.getPdcKey(),
                List.of("Нажмите, чтобы запретить", "использовать сундуки, печки и т.д."))
                ;

        inv.setItem(0, chestItem);

        if(inv.getHolder() instanceof EditPlayerHolder eph) {
            int index = eph.getSlot() - 1;
            if(eph.getLocation().getBlock().getState() instanceof TileState tile) {
                PersistentDataContainer pdc = tile.getPersistentDataContainer();
                // Сохраняем разрешение, а не запрет
                LogUtil.warn("!deny " + !deny);
                pdc.set(Keys.BARRIER_ALLOW_CHEST[index], PersistentDataType.BOOLEAN, !deny);
                tile.update();
                // Для отладки
                LogUtil.info("Слот " + index + ": " + (deny ? "ЗАПРЕЩЕН" : "РАЗРЕШЕН") + " доступ к сундукам");
            }
        }

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
        if(clicked.getType() == Material.LIME_WOOL && clicked.hasItemMeta()) {
            return;
        }

        e.getInventory().setItem(e.getSlot(),
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
        Block block = loc.getBlock();
        BlockState bs = block.getState();
        if (!(bs instanceof TileState)) return;

            Barrier barrier = mechanismManager.getMechanism(block);
            if (barrier != null) {
                int buf = barrier.getEnergyLevel();
                double energyPercent = (double) (buf * 100) / barrier.getCapacity();

                BarrierHolder mainHolder = new BarrierHolder(loc, MechanismType.BARRIER, energyPercent, BarrierScreenCategory.MAIN_MENU);
                Inventory mainInventory = barrierInventory.openMenu(player, mainHolder, energyPercent);

//        if (tile.getPersistentDataContainer().has(Keys.BARRIER_INV_MAIN)) {
//            MechanismStorage.loadItems(tile, mainInventory, Keys.BARRIER_INV_MAIN);
//        }

                barrierInventory.updateEnergyBar(mainInventory, mainHolder, energyPercent);
                mainHolder.setInventory(mainInventory);
                player.openInventory(mainInventory);
            }

    }
    private void removePlayer( Player player, MechanismHolder h){
            if(h instanceof EditPlayerHolder eph){
                if(h.getLocation().getBlock().getState() instanceof TileState tile){
                    int index = eph.getSlot() - 1;

                    PersistentDataContainer pdc = tile.getPersistentDataContainer();

                    // Удаляем
                    pdc.remove(Keys.BARRIER_ADDED_PLAYER_NAMES[index]);
                    pdc.remove(Keys.BARRIER_ALLOW_CHEST[index]);


                    // Сохраняем изменения
                    tile.update();
                }

            }
            openMainMenu(player, h);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShiftToGenerator(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MechanismHolder h)) return;

        // Только shift-перенос
        if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        Location loc = h.getLocation();
        Block b = loc.getBlock();

            Barrier barrier = mechanismManager.getMechanism(b);
            if (barrier != null) return;
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
    // Временно добавьте команду для проверки
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (player.getInventory().getItemInMainHand().getType() == Material.STICK) {

            player.sendMessage("§6=== ДИАГНОСТИКА БАРЬЕРОВ ===");

            // Все барьеры в менеджере
            Collection<Barrier> barriers = mechanismManager.getAllMechanisms();
            player.sendMessage("§7Барьеров в менеджере: §f" + barriers.size());

            for (Barrier barrier : barriers) {
                player.sendMessage("§7- " + barrier.getLocation() +
                        " : " + barrier.getEnergyLevel() + "/" + barrier.getCapacity());
            }

            // Проверка текущего блока
            boolean isBarrier = mechanismManager.hasMechanism(block.getLocation());
            player.sendMessage("§7Этот блок барьер? §" + (isBarrier ? "aДа" : "cНет"));

            if (isBarrier) {
                Barrier barrier = mechanismManager.getMechanism(block.getLocation());
                player.sendMessage("§7Энергия: §e" + barrier.getEnergyLevel() + "/" + barrier.getCapacity());
            }
        }
    }
}


