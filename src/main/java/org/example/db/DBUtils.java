package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBUtils {
    static String url = "jdbc:sqlite:sqlite/amorphous";

    public static void setUrl(String url) {
        DBUtils.url = url;
    }

    public static void executeMultipleUpdatesTransactionally(ArrayList<String> updates) {

        try (Connection connection = DriverManager.getConnection(url)) {

            connection.setAutoCommit(false);

            for (var u : updates) {
                Statement statement = connection.createStatement();
                statement.executeUpdate(u);
            }

            connection.commit();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static ArrayList<Object[]> execute(String query) {
        try (Connection connection = DriverManager.getConnection(url)) {

            var statement = connection.createStatement();

            var results = statement.executeQuery(query);

            var columnCount = results.getMetaData().getColumnCount();
            var rows = new ArrayList<Object[]>();

            while (results.next()) {
                var row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = results.getObject(i);
                }
                rows.add(row);
            }

            return rows;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }
}

