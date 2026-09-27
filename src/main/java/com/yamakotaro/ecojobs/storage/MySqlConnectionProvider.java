package com.yamakotaro.ecojobs.storage;

import com.yamakotaro.ecojobs.EcoJobsPlugin;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class MySqlConnectionProvider {
   private final EcoJobsPlugin plugin;
   private final String url;
   private final String username;
   private final String password;
   private Connection connection;

   public MySqlConnectionProvider(EcoJobsPlugin plugin) {
      this.plugin = plugin;
      String host = plugin.config().getString("storage.mysql.host", "localhost");
      int port = plugin.config().getInt("storage.mysql.port", 3306);
      String database = plugin.config().getString("storage.mysql.database", "ecojobs");
      this.username = plugin.config().getString("storage.mysql.username", "root");
      this.password = plugin.config().getString("storage.mysql.password", "");
      this.url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
      this.connect();
   }

   private void connect() {
      try {
         this.connection = DriverManager.getConnection(this.url, this.username, this.password);
      } catch (SQLException var2) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL. Check the storage.mysql settings in config.yml.", (Throwable)var2);
      }
   }

   public Connection get() {
      try {
         if (this.connection == null || this.connection.isClosed() || !this.connection.isValid(2)) {
            this.connect();
         }
      } catch (SQLException var2) {
         this.connect();
      }

      return this.connection;
   }

   public void close() {
      try {
         if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
         }
      } catch (SQLException var2) {
         this.plugin.getLogger().log(Level.WARNING, "Failed to close the MySQL connection", (Throwable)var2);
      }
   }

   static {
      try {
         Class.forName("com.mysql.cj.jdbc.Driver");
      } catch (ClassNotFoundException var1) {
      }
   }
}
