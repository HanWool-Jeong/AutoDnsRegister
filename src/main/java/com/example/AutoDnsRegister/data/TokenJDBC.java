package com.example.AutoDnsRegister.data;

import org.springframework.stereotype.Repository;

import java.sql.*;

@Repository
public class TokenJDBC {

    private static final String databaseName = "AutoDns";
    private static final String tableName = "Token";
    private static final String url = "jdbc:mariadb://localhost:3306/" + databaseName;
    private static final String user = "hanwool";
    private static final String password = "2341";

    private Connection connection;
    private Statement statement;

    public TokenJDBC() throws Exception {
        connection = DriverManager.getConnection(url, user, password);
        statement = connection.createStatement();
    }

    public void close() throws Exception {
        statement.close();
        connection.close();
    }

    public String getCloudflareToken() throws Exception {
        String sql = String.format("SELECT value FROM %s WHERE comment=%s", tableName, "'Cloudflare api token'");
        ResultSet rs = statement.executeQuery(sql);
        rs.next();
        String result = rs.getString("value");
        rs.close();
        return result;
    }

    public String getGoogleClientId() throws Exception {
        String sql = String.format("SELECT value FROM %s WHERE comment=%s", tableName, "'Google app client id'");
        ResultSet rs = statement.executeQuery(sql);
        rs.next();
        String result = rs.getString("value");
        rs.close();
        return result;
    }

    public String getGoogleClientSecret() throws Exception {
        String sql = String.format("SELECT value FROM %s WHERE comment=%s", tableName, "'Google app client secret'");
        ResultSet rs = statement.executeQuery(sql);
        rs.next();
        String result = rs.getString("value");
        rs.close();
        return result;
    }

    public String getGoogleRefreshToken() throws Exception {
        String sql = String.format("SELECT value FROM %s WHERE comment=%s", tableName, "'Google api refresh token'");
        ResultSet rs = statement.executeQuery(sql);
        rs.next();
        String result = rs.getString("value");
        rs.close();
        return result;
    }

    public void updateGoogleAccessToken(String token) throws Exception {
        String sql = String.format("UPDATE %s SET value='%s' WHERE comment=%s",
                tableName, token, "'Google api access token'");
        statement.executeUpdate(sql);
    }

    public void updateGoogleRefreshToken(String token) throws Exception {
        String sql = String.format("UPDATE %s SET value='%s' WHERE comment=%s",
                tableName, token, "'Google api refresh token'");
        statement.executeUpdate(sql);
    }

}
