package io.github.cccm5;

import org.bukkit.entity.Player;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;

import net.dandielo.citizens.traders_v3.traders.stock.Stock;
import net.dandielo.citizens.traders_v3.traders.stock.StockItem;
public class LoadTask extends CargoTask
{
    public LoadTask(Craft craft, Stock stock, StockItem item){
        super(craft,stock,item);
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
