package com.team1.apartment.apartmentmanagement;

import connection.Db_connection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Test database connection
 * @author acer
 */
public class ApartmentManagement {
    
    public static void main(String[] args) {
        System.out.println("Hello World!");
        
        // ✅ Test database connection with proper exception handling
        try (Connection conn = Db_connection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Database connection successful!");
            }
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed!");
            e.printStackTrace();
        }
    }
}