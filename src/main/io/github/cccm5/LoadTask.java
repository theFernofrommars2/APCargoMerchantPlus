package io.github.cccm5;

import java.util.List;

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
        List<Inventory> invs = Utils.getInventoriesWithSpace(craft, item.getItem(), Material.CHEST, Material.TRAPPED_CHEST);
        if(invs.size() <=0 ){
            this.cancel();
            CargoMain.getQue().remove(originalPilot);
            originalPilot.sendMessage(CargoMain.ERROR_TAG + "You have no space for " + item.getName() + " on this craft!");
            return;
        }
        Inventory inv = invs.get(0);
        int loaded=0;
        for(int i =0; i < inv.getSize() ; i++)
            if(inv.getItem(i)==null || inv.getItem(i).getType()==Material.AIR || inv.getItem(i).isSimilar(item.getItem())){
                int maxCount = (inv.getItem(i)==null || inv.getItem(i).getType()==Material.AIR) ? item.getItem().getMaxStackSize() : inv.getItem(i).getMaxStackSize() - inv.getItem(i).getAmount();
                if(CargoMain.getEconomy().has(originalPilot,item.getPrice()*(maxCount))){
                    loaded+=maxCount;
                    ItemStack tempItem = item.getItem().clone();
                    tempItem.setAmount(tempItem.getMaxStackSize());
                    inv.setItem(i,tempItem);
                }else{
                    maxCount = (int)(CargoMain.getEconomy().getBalance(originalPilot)/item.getPrice());
                    ItemStack tempItem = item.getItem().clone();
                    if(inv.getItem(i)==null || inv.getItem(i).getType()==Material.AIR) 
                        tempItem.setAmount(maxCount);
                    else
                        tempItem.setAmount(inv.getItem(i).getAmount()+maxCount);
                    inv.setItem(i,tempItem);
                    loaded+=maxCount;
                    this.cancel();
                    CargoMain.getQue().remove(originalPilot);
                    originalPilot.sendMessage(CargoMain.SUCCES_TAG + "Loaded " + loaded + " items for $" + String.format("%.2f", loaded*item.getPrice() - CargoMain.getTax()*loaded*item.getPrice()) + " took a tax of " + String.format("%.2f",CargoMain.getTax()*loaded*item.getPrice()));
                    originalPilot.sendMessage(CargoMain.SUCCES_TAG + "You ran out of money!");
                    break;
                }
            }

        originalPilot.sendMessage(CargoMain.SUCCES_TAG + "Loaded " + loaded + " items for $" + String.format("%.2f", loaded*item.getPrice() - CargoMain.getTax()*loaded*item.getPrice()) + " took a tax of " + String.format("%.2f",CargoMain.getTax()*loaded*item.getPrice()));
        CargoMain.getEconomy().withdrawPlayer(originalPilot,loaded*item.getPrice());

        if(invs.size()<= 1){
            this.cancel();
            CargoMain.getQue().remove(originalPilot);
            originalPilot.sendMessage(CargoMain.SUCCES_TAG + "All cargo loaded");
            return;
        }
        new ProcessingTask(originalPilot, item,invs.size()-1).runTaskTimer(CargoMain.getInstance(),0,20);
    }
}
