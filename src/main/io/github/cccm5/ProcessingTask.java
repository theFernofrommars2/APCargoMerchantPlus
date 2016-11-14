package io.github.cccm5;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import net.dandielo.citizens.traders_v3.traders.stock.StockItem;
/**
 * Write a description of class ProcessingTask here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class ProcessingTask extends BukkitRunnable implements Listener
{
    private int remainingTime,delay;
    private Player player;
    private StockItem item;
    private Scoreboard board;
    private Objective objective;
    private ProcessingTask(Player player, StockItem item, int totalTime, int delay){
        this.player = player;
        this.item = item;
        this.remainingTime = totalTime;
        this.delay = delay;
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = board.registerNewObjective(ChatColor.DARK_AQUA + "Cargo", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    @Override
    public void run() {
        if(remainingTime > 0)
            remainingTime-=delay;
        else{
            this.cancel();
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }
        Score score = objective.getScore(ChatColor.GREEN + "Time:"); //Get a fake offline player
        score.setScore(remainingTime);
        player.setScoreboard(board);
    }

    public static void generateTask(Player player, StockItem item, int totalTime, int delay){
        new ProcessingTask(player, item, totalTime,delay).runTaskTimer(CargoMain.getInstance(),delay,delay);
    }
}
