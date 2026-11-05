
package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final String DB_URL =
        "jdbc:mysql://mysql:3306/userdb";

    private static final String DB_USER =
        System.getenv("MYSQL_USER");

    private static final String DB_PASSWORD =
        System.getenv("MYSQL_PASSWORD");

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        // Get form parameters
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Validate input
        if (username == null || password == null ||
            !username.matches("[A-Za-z0-9_.-]{3,100}") ||
            password.length() < 12 ||
            password.length() > 128) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            out.print(
                "{\"success\":false,"
                + "\"message\":\"Invalid input\"}"
            );
            return;
        }

        // Hash password using BCrypt
        String hashedPassword =
            BCrypt.hashpw(password, BCrypt.gensalt(12));

        // SQL query
        String sql =
            "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try {

            // Explicitly load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connect to MySQL and insert user
            try (
                Connection connection =
                    DriverManager.getConnection(
                        DB_URL, DB_USER, DB_PASSWORD
                    );

                PreparedStatement statement =
                    connection.prepareStatement(sql)
            ) {

                statement.setString(1, username);
                statement.setString(2, hashedPassword);

                statement.executeUpdate();

                response.setStatus(HttpServletResponse.SC_OK);

                out.print(
                    "{\"success\":true,"
                    + "\"message\":\"User registered successfully!\"}"
                );
            }

        } catch (ClassNotFoundException e) {

            e.printStackTrace();

            getServletContext().log(
                "MySQL JDBC driver not found", e
            );

            response.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            out.print(
                "{\"success\":false,"
                + "\"message\":\"MySQL JDBC driver not found\"}"
            );

        } catch (SQLIntegrityConstraintViolationException e) {

            response.setStatus(
                HttpServletResponse.SC_CONFLICT
            );

            out.print(
                "{\"success\":false,"
                + "\"message\":\"Username already exists\"}"
            );

        } catch (SQLException e) {

            e.printStackTrace();

            getServletContext().log(
                "Database operation failed", e
            );

            response.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            out.print(
                "{\"success\":false,"
                + "\"message\":\"Database operation failed\"}"
            );
        }
    }
}
