package org.example.artyom.magicMechanism.events;
import org.bukkit.Material;
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
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.data.enums.MechanismType;
import org.example.artyom.magicMechanism.items.BaseMechanismItem;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
import org.example.artyom.magicMechanism.mechanisms.BaseMechanism;
import org.example.artyom.magicMechanism.utils.LogUtil;
import org.example.artyom.magicMechanism.utils.ToolUtil;

public class BaseMechanismEvents implements Listener {
    protected MagicMechanism plugin;
    protected final GeneratorManager generatorManager;
    //private final BaseMechanism mechanism;
    protected final MechanismType mechanismType;

    public BaseMechanismEvents(MagicMechanism plugin, GeneratorManager generatorManager, MechanismType mechanismType) {
        this.plugin = plugin;
        this.generatorManager = generatorManager;
        this.mechanismType = mechanismType;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Проверяем, является ли поставленный блок генератором
        if (block.getType() == mechanismType.getMaterial()) {
            // Создаем новый генератор
            generatorManager.createGenerator(block, player);
            player.sendMessage("§aГенератор установлен!");
        }
    }



    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // При загрузке чанка мы можем предзагрузить все генераторы
        // Но так как мы используем ленивую загрузку (по запросу),
        // то ничего делать не обязательно
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // При выгрузке чанка все генераторы в нем уже сохранены
        // (мы сохраняем их каждый тик), но можно очистить кэш
        // для этого чанка, если хотите экономить память
    }
}
