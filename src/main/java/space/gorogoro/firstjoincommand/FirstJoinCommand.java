package space.gorogoro.firstjoincommand;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

/*
 * FirstJoinCommand
 * @license    LGPLv3
 * @copyright  Copyright gorogoro.space 2019
 * @author     kubotan
 * @see        <a href="https://blog.gorogoro.space">Kubotan's blog.</a>
 */
public class FirstJoinCommand extends JavaPlugin implements Listener{
  private Connection con;
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
      File configFile = new File(getDataFolder(), "config.yml");
      if(!configFile.exists()){
        saveDefaultConfig();
      }

      // JDBCドライバーの指定
      Class.forName("org.sqlite.JDBC");
      // データベースに接続する なければ作成される
      con = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + File.separator + "database.db");
      con.setAutoCommit(false);      // auto commit無効

      // Statementオブジェクト作成
      Statement stmt = con.createStatement();
      stmt.setQueryTimeout(10);    // タイムアウト設定

      // テーブル作成
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS location ("
        + " id INTEGER PRIMARY KEY AUTOINCREMENT"
        + ",uuid STRING NOT NULL"
        + ",playername STRING NOT NULL"
        + ",created_at DATETIME NOT NULL DEFAULT (datetime('now','localtime')) CHECK(created_at LIKE '____-__-__ __:__:__')"
        + ",updated_at DATETIME NOT NULL DEFAULT (datetime('now','localtime')) CHECK(updated_at LIKE '____-__-__ __:__:__')"
        + ");"
      );
      stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS uuid_uindex ON location (uuid);");
      stmt.executeUpdate("CREATE INDEX IF NOT EXISTS playername_index ON location (playername);");

      con.commit();
      stmt.close();

      getServer().getPluginManager().registerEvents(this, this);
    } catch (Exception e){
      logStackTrace(e);
      if(con != null) {
        try {
          con.rollback();
        } catch (SQLException e1) {
          logStackTrace(e1);
        }
      }
    }
  }

  /**
   * JavaPlugin method onCommand.
   *
   * @return boolean true:Success false:Display the usage dialog set in plugin.yml
   */
  public boolean onCommand( CommandSender sender, Command commandInfo, String label, String[] args) {
    try{
      if(!sender.isOp()) {
        return true;
      }

      if(!commandInfo.getName().equals("firstjoincommand")) {
        return true;
      }

      if(args.length <= 0) {
        return true;
      }

      if(args[0].equals("spawn")) {
        if(args.length != 3) {
          return false;
        }
        Player p = Bukkit.getPlayer(args[2]);
        if(p == null) {
          sendMsg(sender, "該当ユーザーが見つかりません。");
        }
        PreparedStatement prepStmt;
        ResultSet rs;
        switch(args[1]) {
          case "add":
            prepStmt = con.prepareStatement("SELECT id FROM location WHERE uuid=?");
            prepStmt.setString(1, p.getUniqueId().toString());
            rs = prepStmt.executeQuery();
            while (rs.next()) {
              sendMsg(sender, "該当プレイヤーの次回ログイン時スポーン地点は、既に登録済みです。");
              rs.close();
              prepStmt.close();
              return true;
            }
            rs.close();
            prepStmt.close();
            prepStmt = con.prepareStatement(
                "INSERT INTO location("
                + " uuid"
                + ",playername"
                + ") VALUES ("
                + " ?"
                + ",?"
                + ");"
              );
            prepStmt.setString(1, p.getUniqueId().toString());
            prepStmt.setString(2, p.getPlayer().getName());
            prepStmt.addBatch();
            prepStmt.executeBatch();
            con.commit();
            prepStmt.close();
            sendMsg(sender, "該当プレイヤーの次回ログイン時スポーン地点を登録しました。");
          break;

          case "check":
            prepStmt = con.prepareStatement("SELECT id FROM location WHERE uuid=?");
            prepStmt.setString(1, p.getUniqueId().toString());
            rs = prepStmt.executeQuery();
            if (rs.next()) {
              sendMsg(sender, "該当プレイヤーの次回ログイン時スポーン地点は、登録済みです。");
            } else {
              sendMsg(sender, "該当プレイヤーの次回ログイン時スポーン地点は、未登録です。");
            }
            rs.close();
            prepStmt.close();
            return true;

          case "delete":
            prepStmt = con.prepareStatement("SELECT id FROM location WHERE uuid=?");
            prepStmt.setString(1, p.getUniqueId().toString());
            rs = prepStmt.executeQuery();
            boolean isFound = false;
            if (rs.next()) {
              isFound = true;
            }
            if(isFound == false) {
              sendMsg(sender, "対象が見つかりません。");
              return true;
            }
            prepStmt = con.prepareStatement("DELETE FROM location WHERE uuid = ?;");
            prepStmt.setString(1, p.getUniqueId().toString());
            prepStmt.addBatch();
            prepStmt.executeBatch();
            con.commit();
            prepStmt.close();
            sendMsg(sender, "該当プレイヤーの次回ログイン時スポーン地点を削除しました。");
          break;

          default:
          return false;
        }
      }
    }catch(Exception e){
      logStackTrace(e);
      if(con != null) {
        try {
          con.rollback();
        } catch (SQLException e1) {
          logStackTrace(e1);
        }
      }
    }
    return true;
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

      PreparedStatement prepStmt;
      ResultSet rs;
      prepStmt = con.prepareStatement("SELECT id FROM location WHERE uuid=?");
      prepStmt.setString(1, event.getPlayer().getUniqueId().toString());
      rs = prepStmt.executeQuery();
      if (rs.next()) {
        Location l = new Location(
          getServer().getWorld(getConfig().getString("world")),
          getConfig().getDouble("x"),
          getConfig().getDouble("y"),
          getConfig().getDouble("z")
        );
        event.getPlayer().teleport(l);
        sendMsg(event.getPlayer(), "臨時メンテナンスがあった為、スポーン地点を調整しました。");

        prepStmt = con.prepareStatement("DELETE FROM location WHERE uuid = ?;");
        prepStmt.setString(1, event.getPlayer().getUniqueId().toString());
        prepStmt.addBatch();
        prepStmt.executeBatch();
        con.commit();
      }
      rs.close();
      prepStmt.close();
    } catch (Exception e) {
      logStackTrace(e);
      if(con != null) {
        try {
          con.rollback();
        } catch (SQLException e1) {
          logStackTrace(e1);
        }
      }
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
   * sendMsg
   */
  private void sendMsg(CommandSender sender, String message) {
    sender.sendMessage(ChatColor.RED + "[FirstJoinCommand] " + ChatColor.RESET + ChatColor.DARK_GRAY + message + ChatColor.RESET);
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
