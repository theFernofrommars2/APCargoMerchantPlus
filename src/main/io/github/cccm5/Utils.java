package io.github.cccm5;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.World;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.MovecraftLocation;

public class Utils
{
    private static final Material[] INVENTORY_MATERIALS = new Material[]{Material.CHEST,Material.TRAPPED_CHEST, Material.FURNACE, Material.HOPPER,Material.DROPPER,Material.DISPENSER, Material.BREWING_STAND};
    /**
     * Check if a material's block form can contain an inventory
     * 
     * @param material the material to check
     * @return true if the material can contain an inventory, otherwise false
     */
    public static boolean isInventoryHolder(Material material){
        for(Material mat : INVENTORY_MATERIALS){
            if(material.equals(mat))
                return true;
        }
        return false;
    }

    public static boolean isInventoryHolder(String type){
        if(Material.getMaterial(type)!=null)
            return isInventoryHolder(Material.getMaterial(type));
        return false;
    }

    /**
     * Check if a block can contain an inventory
     * 
     * @param block the block to check
     * @return true if the block can contain an inventory, otherwise false
     */
    public static boolean isInventoryHolder(Block block){
        return isInventoryHolder(block.getType());
    }

    public static ArrayList<Inventory> getInventorysOnCraft(Craft craft, ItemStack i){
        return getInventorysOnCraft(craft,i,null);
    }

    public static ArrayList<Inventory> getInventorysOnCraft(Craft craft, ItemStack i,List<Material> lookup){
        ArrayList<Inventory> inventories= new ArrayList<Inventory>();
        for(Location loc : movecraftLocationToBukkitLocation(craft.getBlockList(),craft.getW()))
            if(isInventoryHolder(loc.getBlock()))
                if(lookup==null || lookup.size()==0)
                    inventories.add( ((InventoryHolder)loc.getBlock().getState()).getInventory());
                else
                    for(Material mat : lookup)
                        if(loc.getBlock().getType()==mat)
                            inventories.add( ((InventoryHolder)loc.getBlock().getState()).getInventory());
        return inventories;
    }

    /**
     * Converts a movecraftLocation Object to a bukkit Location Object
     * 
     * @param movecraftLoc the movecraft location to be converted
     * @param world the world of the location
     * @return the converted location
     */
    public static Location movecraftLocationToBukkitLocation(MovecraftLocation movecraftLoc, World world){
        return new Location(world,movecraftLoc.getX(),movecraftLoc.getY(),movecraftLoc.getZ());
    }

    /**
     * Converts a list of movecraftLocation Object to a bukkit Location Object
     * 
     * @param movecraftLocations the movecraftLocations to be converted
     * @param world the world of the location
     * @return the converted location
     */
    public static ArrayList<Location> movecraftLocationToBukkitLocation(List<MovecraftLocation> movecraftLocations, World world){
        ArrayList<Location> locations = new ArrayList<Location>();
        for(MovecraftLocation movecraftLoc : movecraftLocations){
            locations.add(movecraftLocationToBukkitLocation(movecraftLoc,world));
        }
        return locations;
    }

    /**
     * Converts a list of movecraftLocation Object to a bukkit Location Object
     * 
     * @param movecraftLocations the movecraftLocations to be converted
     * @param world the world of the location
     * @return the converted location
     */
    public static ArrayList<Location> movecraftLocationToBukkitLocation(MovecraftLocation[] movecraftLocations, World world){
        ArrayList<Location> locations = new ArrayList<Location>();
        for(MovecraftLocation movecraftLoc : movecraftLocations){
            locations.add(movecraftLocationToBukkitLocation(movecraftLoc,world));
        }
        return locations;
    }

    public static int getItemCount(Inventory inv, ItemStack item){
        int count=0;
        for(ItemStack tempStack : inv.getContents())
            if(tempStack.isSimilar(item))
                count+=tempStack.getAmount();
        return count;   
    }

    public static boolean hasSpace(Inventory inv, ItemStack item){
        if(inv.firstEmpty()!=-1)
            return true;

        int occurance = 0;
        int count = 0;
        for(ItemStack tempStack : inv.getContents())
            if(tempStack.isSimilar(item)){
                occurance++;
                count+=tempStack.getAmount();
            }

        if(((double)(count+item.getAmount()))/occurance < item.getMaxStackSize())
            return true;
        return false;
    }

    public static int addLimit(Inventory inv, ItemStack item){      
        int count = 0;
        for(ItemStack tempStack : inv.getContents())
        {
            if(tempStack == null)
                count+=item.getMaxStackSize();
            if(tempStack.isSimilar(item)){
                count+=tempStack.getMaxStackSize()-tempStack.getAmount();
            }
        }
        return count;
    }
}
