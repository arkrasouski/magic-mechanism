package org.example.artyom.magicMechanism.linkservice;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.artyom.magicMechanism.data.Keys;
import org.example.artyom.magicMechanism.data.records.BlockPosKey;
import org.example.artyom.magicMechanism.mechanisms.Generator;

import java.util.Collection;
import java.util.Map;

public class GeneratorBarrierService {
    Collection<Map.Entry<BlockPosKey, Generator>> generators;
    public GeneratorBarrierService(Collection<Map.Entry<BlockPosKey, Generator>> generators) {
        this.generators = generators;
    }

    public void tickEnergybarrierGenerator(){
        for (Map.Entry<BlockPosKey, Generator> entry : generators) { // [web:72]
        BlockPosKey key = entry.getKey();            // [web:72]
        Generator gen = entry.getValue();        // [web:72]

        Block genBlock = BlockPosKey.blockFromKey(key); // или восстанови Block из key (world+x+y+z)
        BlockState bs = genBlock.getState();
        if(!(bs instanceof TileState tileGen)) return;
        int genEnergy = gen.getCurrentEnergy(tileGen);

        for (Block barrier : Generator.adjacentMechanisms(genBlock)) {

//
            if (genEnergy <= 0) break;

            BlockState barrierState = barrier.getState();
            if(!(barrierState instanceof TileState tile)) continue;

            PersistentDataContainer pdc =  tile.getPersistentDataContainer();
            int buf = pdc.getOrDefault(Keys.BUFFER, PersistentDataType.INTEGER, 0);

            int moved = Math.min(gen.getFrequency(), genEnergy);
            moved = Math.min(moved, Generator.capacity - buf);
            if (moved <= 0) continue;

            // Меняем ТОЛЬКО здесь
            genEnergy -= moved;
            gen.setCurrentEnergy(tileGen, genEnergy);
            //EnergyCell.setEnergy(cell, cellEnergy - moved); // внутри обновится lore

            pdc.set(Keys.BUFFER, PersistentDataType.INTEGER, buf + moved);

            tile.update();

            //GenStorage.saveItems(tile, inv);
        }
        tileGen.update();

    }
}
}
