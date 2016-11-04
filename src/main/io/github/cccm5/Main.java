package io.github.cccm5;


import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;
public class Main extends JavaPlugin implements Listener {
    private CraftManager craftManager;
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        craftManager = CraftManager.getInstance();
    }

    public void onDisable() {

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { // Plugin
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("unload") && sender instanceof Player) {
            return true;
        }
        if (command.getName().equalsIgnoreCase("load") && sender instanceof Player) {
            return true;
        }
        return false;

    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getClickedBlock().getType() == Material.SIGN || e.getClickedBlock().getType() == Material.SIGN_POST) {
                Sign sign = (Sign) e.getClickedBlock().getState();
                Player p = e.getPlayer();
                if (sign.getLine(0).equals(ChatColor.GREEN + "[Load]")) {
                    Craft craft = craftManager.getCraftByPlayer(e.getPlayer());
                    ItemStack heldItem = p.getInventory().getItemInMainHand();
                    if(craft!=null && heldItem !=null){
                        for(Inventory inv : Utils.getInventorysOnCraft(craft,p.getInventory().getItemInMainHand())){
                            for(ItemStack playerStack : p.getInventory().getContents()){
                                if(playerStack.isSimilar(heldItem)){
                                    int limit = Utils.addLimit(inv,playerStack);
                                    if(limit>0){
                                        if(Utils.hasSpace(inv,playerStack)){
                                            inv.addItem(playerStack);
                                            p.getInventory().removeItem(playerStack);
                                        }else{
                                            ItemStack itemClone = playerStack.clone();
                                            itemClone.setAmount(limit);
                                            inv.addItem(itemClone);
                                            p.getInventory().removeItem(itemClone);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return;
                }

                if (sign.getLine(0).equals(ChatColor.GREEN + "[UnLoad]")) {

                    Craft craft = craftManager.getCraftByPlayer(e.getPlayer());
                    ItemStack heldItem = p.getInventory().getItemInMainHand();
                    Inventory playerInv = p.getInventory();
                    if(craft!=null && heldItem !=null){
                        for(Inventory inv : Utils.getInventorysOnCraft(craft,p.getInventory().getItemInMainHand())){
                            for(ItemStack cargoStack : inv){
                                if(cargoStack.isSimilar(heldItem)){
                                    int limit = Utils.addLimit(playerInv,cargoStack);

                                    if(limit>0){
                                        if(Utils.hasSpace(playerInv,cargoStack)){
                                            inv.addItem(cargoStack);
                                            p.getInventory().removeItem(cargoStack);
                                        }else{
                                            ItemStack itemClone = cargoStack.clone();
                                            itemClone.setAmount(limit);
                                            inv.addItem(itemClone);
                                            p.getInventory().removeItem(itemClone);
                                        }
                                    }}
                            }
                        }
                    }
                    return;

                }
            }
        }
    }

    @EventHandler
    public void onSignPlace(SignChangeEvent e){
        if(e.getBlock().getType().equals(Material.SIGN) || e.getBlock().getType().equals(Material.WALL_SIGN) || e.getBlock().getType().equals(Material.SIGN_POST)){
            
            Bukkit.broadcastMessage(e.getLine(0));
            if(ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[Load]") || ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[UnLoad]")){
                if((!e.getLine(2).trim().equals("") && Utils.isInventoryHolder(Material.getMaterial(e.getLine(1)))) && (!e.getLine(2).trim().equals("") && Utils.isInventoryHolder(Material.getMaterial(e.getLine(2))))){
                    e.setLine(0,ChatColor.GREEN + ChatColor.stripColor(e.getLine(0)));
                }else{
                    e.setLine(0,ChatColor.RED + ChatColor.stripColor(e.getLine(0)));
                }
            }
        }
    }
}
