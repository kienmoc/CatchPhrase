package sample;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GameDAO {
    public static Connection con;
    private static final String URL = "jdbc:mysql://localhost:3306/users";
    private static final String USER = "root";
    private static final String PASSWORD = "152456";

    public GameDAO () throws SQLException, ClassNotFoundException {

        String dbClass = "com.mysql.cj.jdbc.Driver";
        Class.forName(dbClass);
        con = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static boolean IncreasePoint(String username, double point) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            String sql = "UPDATE user SET point = point + ? WHERE username = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "" + point);
            pstmt.setString(2, username);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
