package org.example.artyom.magicMechanism.commands;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.example.artyom.magicMechanism.utils.EnergyCellUtil;
import org.example.artyom.magicMechanism.utils.GeneratorUtil;
import org.example.artyom.magicMechanism.utils.ItemUtil;
import org.jetbrains.annotations.NotNull;

public class GeneratorCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(command.getName().equalsIgnoreCase("getgen")){
            if(commandSender instanceof Player p){
                p.sendMessage("Выдан генератор");
                GeneratorUtil generator = new GeneratorUtil();
                ItemStack item = generator.createMechanismItem();


                p.getInventory().setItemInMainHand(item);
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("givecell")) {

            if(commandSender instanceof Player p){
                EnergyCellUtil energyCell = new EnergyCellUtil();
                p.getInventory().setItemInMainHand(energyCell.makeEnergyItem(150));
                return true;
            }
        }

    return true;
}
}