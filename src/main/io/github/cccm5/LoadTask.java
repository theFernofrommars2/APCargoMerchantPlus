package io.github.cccm5;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;

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
        //check if there's any chests with space for the cargo, cancel if false - done
        //get the first chest with space - done
        //get the price to fill the chest
        //if greater than the players balance, fill until balance depleted
        //add the items to chest
        //charge user price of cargo plus tax
        Inventory inv = Utils.firstInventoryWithSpace(craft, item.getItem(), Material.CHEST,Material.TRAPPED_CHEST);
        if(inv == null){
            this.cancel();
            originalPilot.sendMessage(Main.SUCCES_TAG + "All cargo unloaded");
            return;
        }
        //FOR TESTING ONLY
        for(int i =0; i < inv.getSize() ; i++){
            inv.setItem(i,item.getItem().clone());
        }
    }
}
