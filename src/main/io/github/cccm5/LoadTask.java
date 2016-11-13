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
            originalPilot.sendMessage(Main.SUCCES_TAG + "All cargo loaded");
            return;
        }
        //FOR TESTING ONLY
        int loaded=0;
        for(int i =0; i < inv.getSize() ; i++)
            if(inv.getItem(i).isSimilar(item.getItem()))
                if(Main.getEconomy().has(originalPilot,item.getPrice()*(inv.getItem(i).getMaxStackSize() - inv.getItem(i).getAmount()))){
                    loaded+=inv.getItem(i).getMaxStackSize() - inv.getItem(i).getAmount();
                    ItemStack tempItem = item.getItem().clone();
                    tempItem.setAmount(tempItem.getMaxStackSize());
                    inv.setItem(i,tempItem);
                }else{
                    int maxCount = (int)(Main.getEconomy().getBalance(originalPilot)/item.getPrice());
                    ItemStack tempItem = item.getItem().clone();
                    tempItem.setAmount(inv.getItem(i).getAmount()+maxCount);
                    inv.setItem(i,tempItem);
                    loaded+=maxCount;
                    this.cancel();
                    originalPilot.sendMessage(Main.SUCCES_TAG + "Sold " + loaded + " items for $" + String.format("%.2f", loaded*item.getPrice()));
                    originalPilot.sendMessage(Main.SUCCES_TAG + "You ran out of money!");
                    break;
                }
                
        originalPilot.sendMessage(Main.SUCCES_TAG + "Sold " + loaded + " items for $" + String.format("%.2f", loaded*item.getPrice()));
        Main.getEconomy().withdrawPlayer(originalPilot,loaded*item.getPrice());
    }
}
