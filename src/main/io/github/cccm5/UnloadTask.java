package io.github.cccm5;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;

import net.dandielo.citizens.traders_v3.traders.stock.Stock;
import net.dandielo.citizens.traders_v3.traders.stock.StockItem;
public class UnloadTask extends CargoTask
{
    public UnloadTask(Craft craft, Stock stock, StockItem item){
        super(craft,stock,item);
    }

    public void execute(){
        //************************
        //*     To Implement     *
        //************************
        //check if there's any chests with cargo, cancel if false - done
        //get the first chest with cargo - done
        //get the price of all the cargo
        //remove the items, pay the user while taking a tax
        Inventory inv = Utils.firstInventory(craft, item.getItem(), Material.CHEST, Material.TRAPPED_CHEST);
        if(inv == null){
            this.cancel();
            originalPilot.sendMessage(Main.SUCCES_TAG + "All cargo unloaded");
            return;
        }
        //FOR TESTING ONLY
        inv.clear();
    }
}
