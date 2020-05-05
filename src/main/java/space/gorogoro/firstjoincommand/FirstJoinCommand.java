package space.gorogoro.firstjoincommand;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/*
 * FirstJoinCommand
 * @license    LGPLv3
 * @copyright  Copyright gorogoro.space 2019
 * @author     kubotan
 * @see        <a href="https://blog.gorogoro.space">Kubotan's blog.</a>
 */
public class FirstJoinCommand extends JavaPlugin implements Listener{

  /**
   * JavaPlugin method onEnable.
   */
  @Override
  public void onEnable(){
    try{
      getLogger().info("The Plugin Has Been Enabled!");
      // If there is no setting file, it is created
      if(!getDataFolder().exists()){
        getDataFolder().mkdir();
      }
      File configFile = new File(getDataFolder() + "/config.yml");
      if(!configFile.exists()){
        saveDefaultConfig();
      }
      getServer().getPluginManager().registerEvents(this, this);
    } catch (Exception e){
      logStackTrace(e);
    }
  }

  /**
   * onPlayerJoin
   */
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    try{
      if ( !event.getPlayer().hasPlayedBefore() ) {
        // 初参加プレイヤー
        for(String cur : getConfig().getStringList("firstJoinCommand")) {
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format(cur, event.getPlayer().getName()));
        }
      }
    } catch (Exception e) {
      logStackTrace(e);
    }
  }

  /**
   * logStackTrace
   */
  private void logStackTrace(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    pw.flush();
    getLogger().warning(sw.toString());
  }

  /**
   * JavaPlugin method onDisable.
   */
  @Override
  public void onDisable(){
    try{
      getLogger().info("The Plugin Has Been Disabled!");
    } catch (Exception e){
      logStackTrace(e);
    }
  }
}
