package io.github.cccm5;

import net.citizensnpcs.api.npc.NPC;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.dandielo.citizens.traders_v3.traders.stock.Stock;
import net.dandielo.citizens.traders_v3.traders.stock.StockItem;
import net.dandielo.citizens.traders_v3.traits.TraderTrait;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
public class CargoMain extends JavaPlugin implements Listener {
    public static final String ERROR_TAG = ChatColor.RED + "Error: " + ChatColor.DARK_RED;
    public static final String SUCCESS_TAG = ChatColor.DARK_AQUA + "Cargo: " + ChatColor.WHITE;
    public static Logger logger;
    private static Economy economy;
    private static ArrayList<Player> playersInQue;
    private static double unloadTax,loadTax;
    private static CargoMain instance;
    private static int delay;//ticks

    private CraftManager craftManager;
    private FileConfiguration config;
    private boolean cardinalDistance;
    private static boolean debug;
    private double scanRange;

    public void onEnable() {
        logger = this.getLogger();
        this.getServer().getPluginManager().registerEvents(this, this);
        playersInQue = new ArrayList<Player>();
        instance = this;
        //************************
        //*       Configs        *
        //************************
        config = getConfig();
        config.addDefault("Scan range",100.0);
        config.addDefault("Transfer delay ticks",300);
        config.addDefault("Load tax percent", 0.01D);
        config.addDefault("Unload tax percent", 0.01D);
        config.addDefault("Cardinal distance",true);
        config.addDefault("Debug mode",false);
        config.options().copyDefaults(true);
        this.saveConfig();
        scanRange = config.getDouble("Scan range") >= 1.0 ? config.getDouble("Scan range") : 100.0;
        delay = config.getInt("Transfer delay ticks");
        loadTax = config.getDouble("Load tax percent")<=1.0 && config.getDouble("Load tax percent")>=0.0 ? config.getDouble("Load tax percent") : 0.01;
        unloadTax = config.getDouble("Unload tax percent")<=1.0 && config.getDouble("Unload tax percent")>=0.0 ? config.getDouble("Unload tax percent") : 0.01;
        cardinalDistance = config.getBoolean("Cardinal distance");
        debug = config.getBoolean("Debug mode");
        //************************
        //*    Load Movecraft    *
        //************************
        if(getServer().getPluginManager().getPlugin("Movecraft") == null || !getServer().getPluginManager().getPlugin("Movecraft").isEnabled()) {
            logger.log(Level.SEVERE, "Movecraft not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        craftManager = CraftManager.getInstance();
        //************************
        //*    Load  Citizens    *
        //************************
        if(getServer().getPluginManager().getPlugin("Citizens") == null || !getServer().getPluginManager().getPlugin("Citizens").isEnabled()) {
            logger.log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);	
            return;
        }
        if(net.citizensnpcs.api.CitizensAPI.getTraitFactory().getTrait(CargoTrait.class)==null)
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(CargoTrait.class));
        //************************
        //*      Load Vault      *
        //************************
        if (getServer().getPluginManager().getPlugin("Vault") == null || !getServer().getPluginManager().getPlugin("Vault").isEnabled()) {
            logger.log(Level.SEVERE, "Vault not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);	
            return;
        } 
        economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
    }

    public void onDisable() {
        logger = null;
        economy = null;
        instance = null;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { // Plugin
        if (command.getName().equalsIgnoreCase("unload")) {
            if(!(sender instanceof Player)){
                sender.sendMessage(ERROR_TAG + "You need to be a player to execute that command!");
                return true;
            }

            if(!sender.hasPermission("Cargo.unload")){
                sender.sendMessage(ERROR_TAG + "You don't have permission to do that!");
                return true;
            }
            Player player = (Player) sender;
            Craft playerCraft = craftManager.getCraftByPlayer(player);
            if(playersInQue.contains(player)){
                sender.sendMessage(ERROR_TAG + "You're already moving cargo!");
                return true;
            }

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
                distance = cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getZ()-npc.getEntity().getLocation().getZ()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getZ()-npc.getEntity().getLocation().getZ(),2));
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

            int size = Utils.getInventories(playerCraft, finalItem.getItem(), Material.CHEST, Material.TRAPPED_CHEST).size();
            if(size <=0 ){
                player.sendMessage(CargoMain.ERROR_TAG + "You have no " + finalItem.getName() + " on this craft!");
                return true;
            }
            sender.sendMessage(SUCCESS_TAG + "Started unloading cargo");
            playersInQue.add(player);
            new UnloadTask(craftManager.getCraftByPlayer(player),stock,finalItem ).runTaskTimer(this,delay,delay);
            new ProcessingTask(player, finalItem,size).runTaskTimer(this,0,20);
            return true;
        }

        if (command.getName().equalsIgnoreCase("load")) {
            if(!(sender instanceof Player)){
                sender.sendMessage(ERROR_TAG + "You need to be a player to execute that command!");
                return true;
            }

            if(!sender.hasPermission("Cargo.load")){
                sender.sendMessage(ERROR_TAG + "You don't have permission to do that!");
                return true;
            }
            Player player = (Player) sender;
            Craft playerCraft = craftManager.getCraftByPlayer(player);
            if(playersInQue.contains(player)){
                sender.sendMessage(ERROR_TAG + "You're already moving cargo!");
                return true;
            }

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
                distance = cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getZ()-npc.getEntity().getLocation().getZ()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getZ()-npc.getEntity().getLocation().getZ(),2));
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

            if(!economy.has(player,finalItem.getPrice()*(1+loadTax))){
                sender.sendMessage(ERROR_TAG + "You don't have enough money to buy any " + finalItem.getName() + "!");
                return true;
            }

            int size = Utils.getInventoriesWithSpace(playerCraft, finalItem.getItem(), Material.CHEST, Material.TRAPPED_CHEST).size();
            if(size <=0 ){
                player.sendMessage(CargoMain.ERROR_TAG + "You don't have any space for " + finalItem.getName() + " on this craft!");
                return true;
            }
            playersInQue.add(player);
            new LoadTask(craftManager.getCraftByPlayer(player),stock,finalItem ).runTaskTimer(this,delay,delay);
            new ProcessingTask(player, finalItem,size).runTaskTimer(this,0,20);
            sender.sendMessage(SUCCESS_TAG + "Started loading cargo");
            return true;
        }

        if (command.getName().equalsIgnoreCase("cargo")) {
            if(!sender.hasPermission("Cargo.cargo")){
                sender.sendMessage(ERROR_TAG + "You don't have permission to do that!");
                return true;
            }
            sender.sendMessage(ChatColor.WHITE + "--[ " + ChatColor.DARK_AQUA + "  Movecraft Cargo " + ChatColor.WHITE + " ]--");
            sender.sendMessage(ChatColor.DARK_AQUA + "Scan Range: " + ChatColor.WHITE + scanRange + " Blocks");
            sender.sendMessage(ChatColor.DARK_AQUA + "Transfer Delay: " + ChatColor.WHITE + delay + " ticks");
            sender.sendMessage(ChatColor.DARK_AQUA + "Unload Tax: " + ChatColor.WHITE + String.format("%.2f",100*unloadTax) + "%");
            sender.sendMessage(ChatColor.DARK_AQUA + "Load Tax: " + ChatColor.WHITE + String.format("%.2f",100*loadTax) + "%");
            if(cardinalDistance)
                sender.sendMessage(ChatColor.DARK_AQUA + "Distance Type: " + ChatColor.WHITE + "Cardinal");
            else
                sender.sendMessage(ChatColor.DARK_AQUA + "Distance Type: " + ChatColor.WHITE + "Direct");
            return true;
        }
        return false;

    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getClickedBlock().getType() == Material.SIGN || e.getClickedBlock().getType() == Material.SIGN_POST) {
                Sign sign = (Sign) e.getClickedBlock().getState();
                if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[UnLoad]")) {
                    Player player = e.getPlayer();
                    if(!player.hasPermission("Cargo.unload")){
                        player.sendMessage(ERROR_TAG + "You don't have permission to do that!");
                        return;
                    }

                    Craft playerCraft = craftManager.getCraftByPlayer(e.getPlayer());
                    if(playersInQue.contains(player)){
                        player.sendMessage(ERROR_TAG + "You're already moving cargo!");
                        return;
                    }

                    if(playerCraft == null){
                        player.sendMessage(ERROR_TAG + "You need to be piloting a craft to do that!");
                        return;
                    }
                    NPC cargoMerchant=null;
                    double distance, lastScan = scanRange;
                    MovecraftLocation loc = playerCraft.getBlockList()[0];
                    for(NPC npc :Utils.getNPCsWithTrait(CargoTrait.class)){
                        if(!npc.isSpawned())
                            continue;
                        distance = cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getZ()-npc.getEntity().getLocation().getZ()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getZ()-npc.getEntity().getLocation().getZ(),2));
                        if( distance <= lastScan){
                            lastScan = distance;
                            cargoMerchant = npc;
                        }
                    }
                    if(cargoMerchant == null){
                        player.sendMessage(ERROR_TAG + "You need to be within " +  scanRange + " blocks of a merchant to use that command!");
                        return;
                    }

                    if(player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR){
                        player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
                        return;
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
                        player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
                        return;
                    }

                    int size = Utils.getInventories(playerCraft, finalItem.getItem(), Material.CHEST, Material.TRAPPED_CHEST).size();
                    if(size <=0 ){
                        player.sendMessage(CargoMain.ERROR_TAG + "You have no " + finalItem.getName() + " on this craft!");
                        return;
                    }
                    player.sendMessage(SUCCESS_TAG + "Started unloading cargo");
                    playersInQue.add(player);
                    new UnloadTask(craftManager.getCraftByPlayer(player),stock,finalItem ).runTaskTimer(this,delay,delay);
                    new ProcessingTask(player, finalItem,size).runTaskTimer(this,0,20);
                    return;
                }

                if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[Load]")) {
                    Player player = e.getPlayer();
                    if(!player.hasPermission("Cargo.load")){
                        player.sendMessage(ERROR_TAG + "You don't have permission to do that!");
                        return;
                    }

                    Craft playerCraft = craftManager.getCraftByPlayer(e.getPlayer());
                    if(playersInQue.contains(player)){
                        player.sendMessage(ERROR_TAG + "You're already moving cargo!");
                        return;
                    }

                    if(playerCraft == null){
                        player.sendMessage(ERROR_TAG + "You need to be piloting a craft to do that!");
                        return;
                    }
                    NPC cargoMerchant=null;
                    double distance, lastScan = scanRange;
                    MovecraftLocation loc = playerCraft.getBlockList()[0];
                    for(NPC npc :Utils.getNPCsWithTrait(CargoTrait.class)){
                        if(!npc.isSpawned())
                            continue;
                        distance = cardinalDistance ? Math.abs(loc.getX()-npc.getEntity().getLocation().getX()) + Math.abs(loc.getZ()-npc.getEntity().getLocation().getZ()) : Math.sqrt(Math.pow(loc.getX()-npc.getEntity().getLocation().getX(),2) + Math.pow(loc.getZ()-npc.getEntity().getLocation().getZ(),2));
                        if( distance <= lastScan){
                            lastScan = distance;
                            cargoMerchant = npc;
                        }
                    }
                    if(cargoMerchant == null){
                        player.sendMessage(ERROR_TAG + "You need to be within " +  scanRange + " blocks of a merchant to use that command!");
                        return;
                    }

                    if(player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() == Material.AIR){
                        player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
                        return;
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
                        player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
                        return;
                    }

                    if(!economy.has(player,finalItem.getPrice()*(1+loadTax))){
                        player.sendMessage(ERROR_TAG + "You don't have enough money to buy any " + finalItem.getName() + "!");
                        return;
                    }

                    int size = Utils.getInventoriesWithSpace(playerCraft, finalItem.getItem(), Material.CHEST, Material.TRAPPED_CHEST).size();
                    if(size <=0 ){
                        player.sendMessage(CargoMain.ERROR_TAG + "You have no space for " + finalItem.getName() + " on this craft!");
                        return;
                    }
                    playersInQue.add(player);
                    new LoadTask(craftManager.getCraftByPlayer(player),stock,finalItem ).runTaskTimer(this,delay,delay);
                    player.sendMessage(SUCCESS_TAG + "Started loading cargo");
                    new ProcessingTask(player, finalItem,size).runTaskTimer(this,0,20);
                }
            }
        }
    }

    @EventHandler
    public void onSignPlace(SignChangeEvent e){
        if(e.getBlock().getType().equals(Material.SIGN) || e.getBlock().getType().equals(Material.WALL_SIGN) || e.getBlock().getType().equals(Material.SIGN_POST)){
            if(ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[Load]") || ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[UnLoad]")){
                e.setLine(0,ChatColor.DARK_AQUA + (ChatColor.stripColor(e.getLine(0))).replaceAll("u","U").replaceAll("l","L"));
            }
        }
    }

    public static boolean isDebug(){
        return debug;
    }

    public static Economy getEconomy(){
        return economy;
    }

    public static List<Player> getQue(){
        return playersInQue;
    }

    public static double getLoadTax(){
        return loadTax;
    }

    public static double getUnloadTax(){
        return unloadTax;
    }

    public static int getDelay(){
        return delay;
    }

    public static CargoMain getInstance(){
        return instance;
    }

}
