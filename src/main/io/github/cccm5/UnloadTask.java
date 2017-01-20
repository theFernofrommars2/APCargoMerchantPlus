package io.github.cccm5;

import net.countercraft.movecraft.craft.Craft;
import net.dandielo.citizens.traders_v3.traders.stock.Stock;
import net.dandielo.citizens.traders_v3.traders.stock.StockItem;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.List;
public class UnloadTask extends CargoTask
{
    public UnloadTask(Craft craft, Stock stock, StockItem item){
        super(craft,stock,item);
    }

    public void execute(){
        List<Inventory> invs = Utils.getInventories(craft, item.getItem(), Material.CHEST, Material.TRAPPED_CHEST);
        
        Inventory inv = invs.get(0);
        int count = 0;
        for(int i = 0; i<inv.getSize();i++){
            if(inv.getItem(i) != null && inv.getItem(i).isSimilar(item.getItem())){
                count+=inv.getItem(i).getAmount();
                inv.setItem(i,null);
            }
        }
        originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "Unloaded " + count + " worth $" + String.format("%.2f", count*item.getPrice()) + " took a tax of " + String.format("%.2f",CargoMain.getUnloadTax()*count*item.getPrice()));
        CargoMain.getEconomy().depositPlayer(originalPilot,count*item.getPrice()*(1-CargoMain.getUnloadTax()));

        if(invs.size()<=1){
            this.cancel();
            CargoMain.getQue().remove(originalPilot);
            originalPilot.sendMessage(CargoMain.SUCCESS_TAG + "All cargo unloaded");
            return;
        }
        new ProcessingTask(originalPilot, item,invs.size()).runTaskTimer(CargoMain.getInstance(),0,20);
    }
}
