package io.github.cccm5;

import org.bukkit.entity.Player;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;

public class LoadTask extends CargoTask
{
    public LoadTask(Craft craft){
        super(craft);
    }

    protected void execute(){
        //************************
        //*     To Implement     *
        //************************
        //check if there's any chests with space for the cargo, cancel if false
        //get the first chest with space
        //get the price to fill the chest
        //if greater than the players balance, fill until balance depleted
        //add the items to chest
        //charge user price of cargo plus tax
    }
}
