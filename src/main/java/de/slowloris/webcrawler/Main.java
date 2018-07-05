/*
 * Copyright (c) 2018 Slowloris.de
 *
 * Development: Weichtier
 *
 * You're allowed to edit the Project.
 * Its not allowed to reupload this Project!
 */

package de.slowloris.webcrawler;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Main {
    private static DB db;
    private static String logfile = "webcrawler.log";
    private static Thread thread;
    public static boolean log;
    public static void main(String[] args) throws Exception{
        File cfg = new File("config.properties");

        System.out.println("  /$$$$$$                                   /$$                    \n" +
                " /$$__  $$                                 | $$                    \n" +
                "| $$  \\__/  /$$$$$$  /$$$$$$  /$$  /$$  /$$| $$  /$$$$$$   /$$$$$$ \n" +
                "| $$       /$$__  $$|____  $$| $$ | $$ | $$| $$ /$$__  $$ /$$__  $$\n" +
                "| $$      | $$  \\__/ /$$$$$$$| $$ | $$ | $$| $$| $$$$$$$$| $$  \\__/\n" +
                "| $$    $$| $$      /$$__  $$| $$ | $$ | $$| $$| $$_____/| $$      \n" +
                "|  $$$$$$/| $$     |  $$$$$$$|  $$$$$/$$$$/| $$|  $$$$$$$| $$      \n" +
                " \\______/ |__/      \\_______/ \\_____/\\___/ |__/ \\_______/|__/      \n\n");
        if(!cfg.exists()){
            write("------------");
            write("Please Setup MySQL in config.properties");
            write("------------");
            Properties p = new Properties();
            OutputStream os = new FileOutputStream("config.properties");
            p.setProperty("LOG", "true");
            p.setProperty("MYSQL_USER", "root");
            p.setProperty("MYSQL_PASSWORD", "");
            p.setProperty("MYSQL_HOST", "localhost");
            p.setProperty("MYSQL_DATABASE", "crawler");
            p.setProperty("BLACKLIST", "ja-jp.facebook.com;enable-javascript.com");
            p.store(os, null);
            return;
        }
        db = new DB();
        write("Type \"help\" for help");
        db.runSql2("CREATE TABLE IF NOT EXISTS `resource` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `url` varchar(255) NOT NULL,\n" +
                "  `title` varchar(150) NOT NULL,\n" +
                "  `description` varchar(500) NOT NULL,\n" +
                "  `keywords` varchar(300) NOT NULL,\n" +
                "  `priority` int(11) NOT NULL DEFAULT '0',\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4");
        Scanner scanner = new Scanner(System.in);
        while (true){
            String input = scanner.nextLine();
            if(log)write(logfile,"[INPUT]" + input);
            if(input.startsWith("crawl")){
                final String url = input.replace("crawl ", "");

                String sql = "select * from resource where url = '" + url + "'";
                ResultSet rs = db.runSql(sql);
                if(rs.next()){
                    write("Site already Indexed!");
                }else {
                    write("Start crawling");
                    thread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                processPage(url);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                }
            }else if(input.equalsIgnoreCase("stop")){
                thread.stop();
                write("Crawling cancelled");
            }else if(input.equalsIgnoreCase("exit")){
                break;
            }else if(input.equalsIgnoreCase("help")){
                write("---------[Hilfe]---------");
                write("Commands:");
                write("crawl <url> : Starts the crawler with specified URL");
                write("stop : Stops the crawler");
                write("exit : Stops this Programm");
                write("---------[Hilfe]---------");
            }else {
                write("Command not Found");
            }
        }
    }
    public static void processPage(String URL) throws SQLException, IOException {
        //check if the given URL is already in database
        Document doc;
        try {
            doc = Jsoup.connect(URL).get();
        }
        catch (Exception e){
            String errormsg = "Error while indexing " + URL + ": " + e.getClass().getSimpleName();
            write(errormsg);
            if(log)write(errormsg);
            return;
        }

            String keywords = "";
            try {
                keywords = doc.select("meta[name=keywords]").first().attr("content");
            }catch (NullPointerException e){
            }
                String description;
                if(doc.body().toString().length() < 100){
                    description = doc.body().toString();
                }else {
                    description = doc.body().toString().substring(0, 100) + "...";
                }
            try {
                description = doc.select("meta[name=description]").first().attr("content");
            }catch (NullPointerException e){
            }
            if(description.length() > 300){
                    description = description.substring(0, 295) + "...";
            }
            if(log)write("Indexing " + URL);
            //store the URL to database to avoid parsing again
            String sql = "INSERT INTO  resource (url, title, description, keywords) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = db.conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, URL);
            stmt.setString(2, doc.title());
            stmt.setString(3, description);
            stmt.setString(4, keywords);
            stmt.execute();
            //get all links and recursively call the processPage method
            Elements questions = doc.select("a[href]");
                for(Element link: questions){
                    sql = "select * from resource where url = '" + link.attr("abs:href") + "'";
                    ResultSet rs = db.runSql(sql);
                    if(rs.next()){
                    }else {
                        processPage(link.attr("abs:href"));
                    }
                }
    }
    public static void write(String s) throws IOException {
        write(logfile, s);
        System.out.println(s);
    }

    public static void write(String f, String s) throws IOException {
        TimeZone tz = TimeZone.getTimeZone("EST"); // or PST, MID, etc ...
        Date now = new Date();
        DateFormat df = new SimpleDateFormat("yyyy.mm.dd hh:mm:ss ");
        df.setTimeZone(tz);
        String currentTime = df.format(now);

        FileWriter aWriter = new FileWriter(f, true);
        aWriter.write(currentTime + " " + s + "\n");
        aWriter.flush();
        aWriter.close();
    }
}