package io.github.cccm5;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;

public abstract class CargoTask extends BukkitRunnable 
{
    private Craft craft;
    private final MovecraftLocation[] originalLocations;
    private final Player originalPilot;
    protected CargoTask(Craft craft){
        if (craft == null) 
            throw new IllegalArgumentException("craft must not be null");
        else 
            this.craft = craft;
        this.originalLocations = craft.getBlockList();
        this.originalPilot = CraftManager.getInstance().getPlayerFromCraft(craft);
    }

    @Override
    public void run() {
        if (CraftManager.getInstance().getPlayerFromCraft(craft)==null){
            this.cancel();
            return;
        }

        if (CraftManager.getInstance().getPlayerFromCraft(craft)!=originalPilot ){
            originalPilot.sendMessage("Pilots chaned!");
            this.cancel();
            return;
        }

        if( craft.getBlockList() != originalLocations) { 
            originalPilot.sendMessage("Blocks moved/changed!");
            this.cancel();
            return;
        }
        
        execute();
    }

    protected abstract void execute();
}
