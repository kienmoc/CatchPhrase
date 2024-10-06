package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HomeController {

    @FXML
    private Label usernameLabel;

    @FXML
    private Label scoreLabel;

    @FXML
    private ListView<String> rankingListView;

    @FXML
    private ListView<String> friendsListView;

    private UserData player;

    // Phương thức khởi tạo tự động gọi sau khi FXML được tải
    public void initialize() {
        // Có thể khởi tạo các thành phần không phụ thuộc vào UserData tại đây
    }

    // Phương thức để nhận dữ liệu từ IntroController
    public void setUserData(UserData player) {
        this.player = player;
        // Cập nhật thông tin người dùng
        usernameLabel.setText(player.getUsername());
        scoreLabel.setText("Score: " + player.getScore());

        // Tải bảng xếp hạng từ cơ sở dữ liệu
        loadRankings();

        // Kết nối để nhận danh sách bạn bè online
        loadFriendsOnline();
    }

    // Phương thức tải bảng xếp hạng từ cơ sở dữ liệu
    private void loadRankings() {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT username, point FROM user ORDER BY point DESC LIMIT 10";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            // Xóa danh sách hiện tại trước khi thêm mới
            Platform.runLater(() -> rankingListView.getItems().clear());

            while (rs.next()) {
                String user = rs.getString("username");
                int score = rs.getInt("point");
                String entry = String.format("%s - %d point", user, score);
                Platform.runLater(() -> rankingListView.getItems().add(entry));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Lỗi khi tải bảng xếp hạng từ cơ sở dữ liệu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Phương thức kết nối socket để nhận danh sách bạn bè online
    private void loadFriendsOnline() {
        // Giả sử server gửi danh sách bạn bè online theo định dạng dòng văn bản, mỗi bạn bè trên một dòng
        Socket socket = player.getSocket();
        if (socket == null || socket.isClosed()) {
            JOptionPane.showMessageDialog(null, "Không thể kết nối tới server để lấy danh sách bạn bè online.", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Tạo một luồng mới để lắng nghe dữ liệu từ server
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message;
                while ((message = in.readLine()) != null) {
                    // Giả sử server gửi danh sách bạn bè online như một chuỗi JSON hoặc theo định dạng cụ thể
                    // Ở đây, chúng ta giả sử mỗi bạn bè online là một dòng văn bản
                    String friend = message.trim();
                    if (!friend.isEmpty()) {
                        Platform.runLater(() -> {
                            if (!friendsListView.getItems().contains(friend)) {
                                friendsListView.getItems().add(friend);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> JOptionPane.showMessageDialog(null, "Lost connection to server.", "Connection error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    // Phương thức để thoát ứng dụng
    @FXML
    public void onExit(){
        try {
            if (player.getSocket() != null && !player.getSocket().isClosed()) {
                player.getSocket().close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Platform.exit();
    }

    // Các phương thức khác để xử lý sự kiện từ giao diện có thể được thêm vào đây
}
