/*
 * Copyright (c) 2018 Slowloris.de
 *
 * Development: Weichtier
 *
 * You're allowed to edit the Project.
 * Its not allowed to reupload this Project!
 */

package de.slowloris.webcrawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DB {
    public Connection conn = null;
    public DB() throws IOException {
        Properties p = new Properties();
        InputStream is = new FileInputStream("config.properties");
        p.load(is);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + p.getProperty("MYSQL_HOST") + "/" + p.getProperty("MYSQL_DATABASE") + "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Europe/Berlin";
            conn = DriverManager.getConnection(url, p.getProperty("MYSQL_USER"), p.getProperty("MYSQL_PASSWORD"));
            if(p.getProperty("LOG").equalsIgnoreCase("true")){
                Main.log = true;
            }
            System.out.println("Successfully connected to the database");
        } catch (SQLException e) {
            Main.write("Database Error!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public ResultSet runSql(String sql) throws SQLException {
        Statement sta = conn.createStatement();
        return sta.executeQuery(sql);
    }
    public boolean runSql2(String sql) throws SQLException {
        Statement sta = conn.createStatement();
        return sta.execute(sql);
    }
    @Override
    protected void finalize() throws Throwable {
        if (conn != null || !conn.isClosed()) {
            conn.close();
        }
    }
}
