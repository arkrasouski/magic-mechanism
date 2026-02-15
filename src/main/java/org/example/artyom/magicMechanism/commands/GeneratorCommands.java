package org.example.artyom.magicMechanism.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.example.artyom.magicMechanism.mechanisms.Barrier;
import org.example.artyom.magicMechanism.energyitems.EnergyCell;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.jetbrains.annotations.NotNull;

public class GeneratorCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(command.getName().equalsIgnoreCase("getgen")){
            if(commandSender instanceof Player p){
                p.sendMessage("Выдан генератор");
                Generator generator = new Generator();
                ItemStack item = generator.createMechanismItem();


                p.getInventory().setItemInMainHand(item);
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("givecell")) {

            if(commandSender instanceof Player p){
                EnergyCell energyCell = new EnergyCell();
                p.getInventory().setItemInMainHand(energyCell.makeEnergyItem(150));
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("getbarrier")) {
            System.out.println("kej");
            if(commandSender instanceof Player p){
                System.out.println("krk lol");
                Barrier barrier = new Barrier();
                System.out.println("lol");
                ItemStack item = barrier.createMechanismItem();
                p.getInventory().setItemInMainHand(item);
                return true;
            }
        }

    return true;
}
}