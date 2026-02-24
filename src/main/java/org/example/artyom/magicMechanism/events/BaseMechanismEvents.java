package org.example.artyom.magicMechanism.events;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;

import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.items.BaseMechanismItem;
import org.example.artyom.magicMechanism.managers.BaseManager;

import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.example.artyom.magicMechanism.utils.BlockUtil;
import org.example.artyom.magicMechanism.utils.ToolUtil;

public abstract class BaseMechanismEvents<Mechanism extends BaseMechanism, Manager extends BaseManager<Mechanism>> implements Listener {
    protected MagicMechanism plugin;
    protected final Manager mechanismManager;
    protected final MechanismType mechanismType;

    public BaseMechanismEvents(MagicMechanism plugin, Manager mechanismManager, MechanismType mechanismType) {
        this.plugin = plugin;
        this.mechanismManager= mechanismManager;
        this.mechanismType = mechanismType;
    }

    @EventHandler
    public void onGeneratorPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (!isMechanismItem(item)) {return;}

        if (!canPlaceMechanism(block, player)) {
            event.setCancelled(true);
            player.sendMessage("§cНельзя установить генератор здесь!");
            return;
        }

        Mechanism mechanism = mechanismManager.createMechanism(block.getLocation(), player);

        if (mechanism == null) {
            event.setCancelled(true);
            player.sendMessage("§cОшибка при создании " + mechanismType.getGuiTitle());
            return;
        }
        // ШАГ 5: Сообщение игроку
        player.sendMessage("§a✓ " + mechanismType.getGuiTitle() + " успешно установлен!");

        // ШАГ 6: Визуальный эффект
        spawnPlaceEffect(block);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

            // Проверяем, является ли сломанный блок генератором
            if (mechanismManager.getMechanism(block) != null) {
                // Удаляем генератор
                mechanismManager.deleteMechanism(block.getLocation());

                ItemStack tool = player.getInventory().getItemInMainHand();
                if (!ToolUtil.canBreakWithTool(player, tool)) {
                    event.setCancelled(true);
                    player.sendMessage("§c" + mechanismType.getGuiTitle() + " можно сломать только киркой!");
                    return;
                }
                event.getPlayer().sendMessage("§c" + mechanismType.getGuiTitle() + " разрушен!");
                // Отменяем обычный дроп
                event.setDropItems(false);
                if (block.getState() instanceof Container cont) {
                    cont.getInventory().clear();
                    cont.update(true);
                }
                // Удаляем блок
                block.setType(Material.AIR);

                // Дропаем предмет генератора
                ItemStack mechanismItem = new BaseMechanismItem(plugin, mechanismType).createItem(1);
                //ItemStack mechanismItem = this.mechanismItem.createItem(1); // но с данными блока!
                block.getWorld().dropItemNaturally(block.getLocation(), mechanismItem);
            }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        // Загружаем генераторы из чанка
        mechanismManager.loadMechanismsFromChunk(chunk);

        //LogUtil.warn("Чанк загружен: " + chunk.getX() + ", " + chunk.getZ());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        // Сохраняем генераторы перед выгрузкой
        mechanismManager.saveMechanismsFromChunk(chunk);

        // Очищаем кэш
        mechanismManager.unloadChunkMechanisms(chunk);

        //LogUtil.warn("Чанк выгружен: " + chunk.getX() + ", " + chunk.getZ());
    }








    private boolean isMechanismItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        // Проверка на наличие метки генератора
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, mechanismType.name() + "_item"), PersistentDataType.BOOLEAN);
    }

    private boolean canPlaceMechanism(Block block, Player player) {
        // Проверка на пустой блок
        if (block.getType() != Material.AIR && BlockUtil.isReplaceableBlock(block)) {
            return false;
        }

        // Проверка на наличие другого генератора
        if (mechanismManager.isMechanism(block)) {
            return false;
        }

        // Проверка прав
        return true; //player.hasPermission("generator.place");
    }

    private boolean canBreakGenerator(Generator generator, Player player) {
        // Владелец всегда может сломать
        if (generator.getOwner() != null && generator.getOwner().equals(player)) {
            return true;
        }

        // Админы могут ломать чужие
        return true;// player.hasPermission("generator.break.others");
    }

    private boolean shouldDropGenerator() {
        return true;//plugin.getConfig().getBoolean("generator.drop-on-break", true);
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





}
