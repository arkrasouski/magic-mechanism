package org.example.artyom.magicMechanism.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.example.artyom.magicMechanism.MagicMechanism;
import org.example.artyom.magicMechanism.energyitems.EnergyCell;
import org.example.artyom.magicMechanism.items.BarrierItem;
import org.example.artyom.magicMechanism.items.CableItem;
import org.example.artyom.magicMechanism.items.GeneratorItem;
import org.example.artyom.magicMechanism.managers.GeneratorManager;
//import org.example.artyom.magicMechanism.mechanisms.Barrier;
//import org.example.artyom.magicMechanism.energyitems.EnergyCell;
import org.example.artyom.magicMechanism.mechanisms.Barrier;
import org.example.artyom.magicMechanism.mechanisms.Generator;
import org.jetbrains.annotations.NotNull;

public class GeneratorCommands implements CommandExecutor {

    private final MagicMechanism plugin;


    public GeneratorCommands(MagicMechanism plugin) {
        this.plugin = plugin;

    }
    //private final GeneratorManager generatorManager;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(command.getName().equalsIgnoreCase("getgen")){
            if(commandSender instanceof Player p){


                ItemStack item = new GeneratorItem(plugin).createItem(1);
                p.sendMessage("Выдан генератор");

                p.getInventory().addItem(item);
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
            if(commandSender instanceof Player p){
                ItemStack item = new BarrierItem(plugin).createItem(1);
                p.getInventory().addItem(item);
                return true;
            }
        }

        if(command.getName().equalsIgnoreCase("getcable")){
            if(commandSender instanceof Player p){
                ItemStack item = new CableItem(plugin).createItem(64);
                p.getInventory().addItem(item);
                return true;
            }
        }

    return true;
}
}