package io.github.cccm5;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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

import net.citizensnpcs.api.npc.NPC;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;

import net.dandielo.citizens.traders_v3.traders.stock.Stock;
import net.dandielo.citizens.traders_v3.traders.stock.StockItem;
import net.dandielo.citizens.traders_v3.traits.TraderTrait;

import net.milkbowl.vault.economy.Economy;
public class Main extends JavaPlugin implements Listener {
    public static final String ERROR_TAG = ChatColor.RED + "Error: " + ChatColor.DARK_RED;
    public static final String SUCCES_TAG = ChatColor.DARK_AQUA + "Cargo: " + ChatColor.WHITE;
    public static Logger logger;
    private static Economy economy;

    private CraftManager craftManager;
    private FileConfiguration config;
    private boolean cardinalDistance;
    private static boolean debug;
    private double scanRange;
    public void onEnable() {
        logger = this.getLogger();
        this.getServer().getPluginManager().registerEvents(this, this);
        //************************
        //*       Configs        *
        //************************
        config = getConfig();
        config.addDefault("Scan range",100.0);
        config.addDefault("Cardinal distance",true);
        config.addDefault("Debug mode",false);
        config.options().copyDefaults(true);
        this.saveConfig();
        scanRange = config.getDouble("Scan range");
        cardinalDistance = config.getBoolean("Cardinal distance");
        debug = config.getBoolean("Debug mode");
        //************************
        //*    Load Movecraft    *
        //************************
        if(getServer().getPluginManager().getPlugin("Movecraft") == null || getServer().getPluginManager().getPlugin("Movecraft").isEnabled() == false) {
            logger.log(Level.SEVERE, "Movecraft not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);	
            return;
        }	
        craftManager = CraftManager.getInstance();
        //************************
        //*    Load  Citizens    *
        //************************
        if(getServer().getPluginManager().getPlugin("Citizens") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
            logger.log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);	
            return;
        }
        net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(CargoTrait.class));
        //************************
        //*      Load Vault      *
        //************************
        if (getServer().getPluginManager().getPlugin("Vault") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
            logger.log(Level.SEVERE, "Vault not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);	
            return;
        } 
        org.bukkit.plugin.RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.info("[AP-Merchant] Could not find compatible Vault plugin. Disabling Vault integration.");			
            getServer().getPluginManager().disablePlugin(this);	
            return;
        }
        economy = rsp.getProvider();
    }

    public void onDisable() {
        net.citizensnpcs.api.CitizensAPI.getTraitFactory().deregisterTrait(net.citizensnpcs.api.trait.TraitInfo.create(CargoTrait.class));
        logger = null;
        economy = null;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { // Plugin
        if (command.getName().equalsIgnoreCase("unload")) {
            if(!(sender instanceof Player)){
                sender.sendMessage(ERROR_TAG + "You need to be a player to execute that command!");
                return true;
            }
            Player player = (Player) sender;
            Craft playerCraft = craftManager.getCraftByPlayer(player);
            if(playerCraft == null){
                sender.sendMessage(ERROR_TAG + "You need to be piloting a craft to do that!");
                return true;
            }
            NPC cargoMerchant=null;
            double distance, lastScan = scanRange;
            MovecraftLocation loc = playerCraft.getBlockList()[0];
            for(NPC npc :Utils.getNPCsWithTrait(CargoTrait.class)){
                if(!npc.isSpawned())
                    continue;
                distance = cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2));
                if( distance <= lastScan){
                    lastScan = distance;
                    cargoMerchant = npc;
                }
            }
            if(cargoMerchant == null){
                sender.sendMessage(ERROR_TAG + "You need to be within " +  scanRange + " blocks of a merchant to use that command!");
                return true;
            }

            if(player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR){
                sender.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
                return true;
            }
            Stock stock = cargoMerchant.getTrait(TraderTrait.class).getStock();
            ItemStack compareItem = player.getInventory().getItemInMainHand().clone();
            StockItem finalItem=null;
            for(StockItem tempItem : stock.getStock("sell"))
                if(tempItem.getItem().isSimilar(compareItem)){
                    finalItem = tempItem;
                    break;
                }
            if(finalItem == null || !finalItem.hasPrice()){
                sender.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
                return true;
            }

            sender.sendMessage(SUCCES_TAG + "Started unloading cargo");
            new UnloadTask(craftManager.getCraftByPlayer(player),stock,finalItem ).runTaskTimer(this,10,10);
            return true;
        }

        if (command.getName().equalsIgnoreCase("load")) {
            if(!(sender instanceof Player)){
                sender.sendMessage(ERROR_TAG + "You need to be a player to execute that command!");
                return true;
            }
            Player player = (Player) sender;
            Craft playerCraft = craftManager.getCraftByPlayer(player);
            if(playerCraft == null){
                sender.sendMessage(ERROR_TAG + "You need to be piloting a craft to do that!");
                return true;
            }
            NPC cargoMerchant=null;
            double distance, lastScan = scanRange;
            MovecraftLocation loc = playerCraft.getBlockList()[0];
            for(NPC npc :Utils.getNPCsWithTrait(CargoTrait.class)){
                if(!npc.isSpawned())
                    continue;
                distance = cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2));
                if( distance <= lastScan){
                    lastScan = distance;
                    cargoMerchant = npc;
                }
            }
            if(cargoMerchant == null){
                sender.sendMessage(ERROR_TAG + "You need to be within " +  scanRange + " blocks of a merchant to use that command!");
                return true;
            }

            if(player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR){
                sender.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
                return true;
            }
            Stock stock = cargoMerchant.getTrait(TraderTrait.class).getStock();
            ItemStack compareItem = player.getInventory().getItemInMainHand().clone();
            StockItem finalItem=null;
            for(StockItem tempItem : stock.getStock("buy"))
                if(tempItem.getItem().isSimilar(compareItem)){
                    finalItem = tempItem;
                    break;
                }
            if(finalItem == null  || !finalItem.hasPrice()){
                sender.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
                return true;
            }
            new LoadTask(craftManager.getCraftByPlayer(player),stock,finalItem ).runTaskTimer(this,10,10);
            sender.sendMessage(SUCCES_TAG + "Started loading cargo");
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
                    ArrayList<Material> lookup = new ArrayList<Material>(3);
                    if(Utils.isInventoryHolder(sign.getLine(1)))
                        lookup.add(Material.getMaterial(sign.getLine(1)));
                    if(Utils.isInventoryHolder(sign.getLine(2)))
                        lookup.add(Material.getMaterial(sign.getLine(2)));
                    if(Utils.isInventoryHolder(sign.getLine(3)))
                        lookup.add(Material.getMaterial(sign.getLine(3)));
                    if(craft!=null && heldItem !=null){
                        for(Inventory inv : Utils.getInventorysOnCraft(craft,p.getInventory().getItemInMainHand(),lookup)){
                            //for(ItemStack playerStack : p.getInventory().getContents()){
                            for(int i = 0; i<inv.getSize(); i++){
                                ItemStack playerStack = inv.getItem(i);
                                if(playerStack != null && playerStack.isSimilar(heldItem)){
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
                    p.updateInventory();
                    return;
                }

                if (sign.getLine(0).equals(ChatColor.GREEN + "[UnLoad]")) {

                    Craft craft = craftManager.getCraftByPlayer(e.getPlayer());
                    ItemStack heldItem = p.getInventory().getItemInMainHand();
                    Inventory playerInv = p.getInventory();
                    ArrayList<Material> lookup = new ArrayList<Material>(3);
                    if(Utils.isInventoryHolder(sign.getLine(1)))
                        lookup.add(Material.getMaterial(sign.getLine(1).toUpperCase().replaceAll(" ","_")));
                    if(Utils.isInventoryHolder(sign.getLine(2)))
                        lookup.add(Material.getMaterial(sign.getLine(2).toUpperCase().replaceAll(" ","_")));
                    if(Utils.isInventoryHolder(sign.getLine(3)))
                        lookup.add(Material.getMaterial(sign.getLine(3).toUpperCase().replaceAll(" ","_")));
                    if(craft!=null && heldItem !=null){
                        for(Inventory inv : Utils.getInventorysOnCraft(craft,p.getInventory().getItemInMainHand(),lookup)){
                            for(int i = 0; i<inv.getSize(); i++){
                                ItemStack cargoStack = inv.getItem(i);
                                if(cargoStack != null && cargoStack.isSimilar(heldItem)){
                                    Bukkit.broadcastMessage(cargoStack.getType().name());
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
                                    }
                                }
                            }
                        }
                    }
                    p.updateInventory();
                }
            }
        }
    }

    @EventHandler
    public void onSignPlace(SignChangeEvent e){
        if(e.getBlock().getType().equals(Material.SIGN) || e.getBlock().getType().equals(Material.WALL_SIGN) || e.getBlock().getType().equals(Material.SIGN_POST)){
            if(ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[Load]") || ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[UnLoad]")){
                if((!e.getLine(1).trim().equals("") && Utils.isInventoryHolder(e.getLine(1))) || (!e.getLine(2).trim().equals("") && Utils.isInventoryHolder(e.getLine(2)))){
                    e.setLine(0,ChatColor.GREEN + ChatColor.stripColor(e.getLine(0)));
                }else{
                    e.setLine(0,ChatColor.RED + ChatColor.stripColor(e.getLine(0)));
                }
            }
        }
    }

    public static boolean isDebug(){
        return debug;
    }

    public static Economy getEconomy(){
        return economy;
    }
}
