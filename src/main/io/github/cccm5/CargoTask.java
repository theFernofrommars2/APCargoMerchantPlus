package io.github.cccm5;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;

import net.dandielo.citizens.traders_v3.traders.stock.Stock;
import net.dandielo.citizens.traders_v3.traders.stock.StockItem;
public abstract class CargoTask extends BukkitRunnable 
{
    protected Craft craft;
    protected final MovecraftLocation[] originalLocations;
    protected final Player originalPilot;
    protected Stock stock;
    protected final StockItem item;
    protected CargoTask(Craft craft, Stock stock, StockItem item){
        if (craft == null) {
            if(Main.isDebug()){

            }
            throw new IllegalArgumentException("craft must not be null");
        }
        else
            this.craft = craft;
        if (stock == null) 
            throw new IllegalArgumentException("stock must not be null");
        else
            this.stock = stock;
        if (item == null) 
            throw new IllegalArgumentException("item must not be null");
        else
            this.item = item;
        this.originalLocations = craft.getBlockList();
        this.originalPilot = CraftManager.getInstance().getPlayerFromCraft(craft);
    }

    @Override
    public void run() {
        if (CraftManager.getInstance().getCraftByPlayer(originalPilot)==null){
            this.cancel();
            return;
        }

        if (CraftManager.getInstance().getPlayerFromCraft(craft)!=originalPilot ){
            originalPilot.sendMessage("Pilots changed!");
            this.cancel();
            return;
        }

        if(!Arrays.deepEquals(craft.getBlockList(), originalLocations)) { 
            originalPilot.sendMessage("Blocks moved/changed!");
            this.cancel();
            return;
        }

        execute();
    }

    protected abstract void execute();
}
