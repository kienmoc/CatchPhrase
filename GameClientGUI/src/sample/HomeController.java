package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;

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
    private ObjectOutputStream dOut;
    private ObjectInputStream dIn;

    private Map<String, String> friendsMap;

    // Phương thức khởi tạo tự động gọi sau khi FXML được tải
    public void initialize() {
        friendsListView.setCellFactory(param -> new ListCell<String>() {
            private HBox content;
            private Label usernameLabel;
            private Button inviteButton;

            {
                usernameLabel = new Label();
                inviteButton = new Button("Invite");
                inviteButton.setOnAction(event -> {
                    String username = getItem();
                    handleInvite(username);
                });
                content = new HBox(10, usernameLabel, inviteButton);
                content.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    usernameLabel.setText(username);
                    setGraphic(content);
                }
            }
        });
    }

    // Phương thức để nhận dữ liệu từ IntroController
    public void setUserData(UserData player) {
        this.player = player;
        dIn = player.ois;
        dOut = player.oos;
        // Cập nhật thông tin người dùng
        usernameLabel.setText(player.getUsername());
        scoreLabel.setText("Score: " + player.getScore());

        // Tải bảng xếp hạng từ cơ sở dữ liệu
        loadRankings();
        listenForData();
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

    private void updateFriendList(Map<String, String> friendsMap) {
        Platform.runLater(() -> {
            friendsListView.getItems().clear();

            for (String username : friendsMap.keySet()) {
                friendsListView.getItems().add(username); // Chỉ hiển thị username
            }
        });
    }

    private void handleInvite(String username) {
        try {
            dOut.writeObject("Invite:" + username);
            dOut.flush();
            System.out.println("Invite request sent to " + username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForData() {
        new Thread(() -> {
            try {
                while (true) {
                    Object data = dIn.readObject();
                    if (data instanceof Map) {
                        friendsMap = (Map<String, String>) data;
                        updateFriendList(friendsMap);
                    } else if (data instanceof String) {
                        String message = (String) data;
                        if (message.startsWith("Invite from")) {
//                            Platform.runLater(() -> {
//                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
//                                alert.setTitle("Invite Received");
//                                alert.setHeaderText(null);
//                                alert.setContentText(message);
//                                alert.showAndWait();
//                            });
                            Platform.runLater(() -> {
                                showInviteDialog(message);
                            });
                        } else if (message.startsWith("Enter Lobby")) {
                            Platform.runLater(() -> {
                                System.out.println("Im entering lobby hihi");
                                enterLobby();
                            });
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showInviteDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Invite Received");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Tạo các nút Accept và Decline
        ButtonType acceptButton = new ButtonType("Accept");
        ButtonType declineButton = new ButtonType("Decline", ButtonBar.ButtonData.CANCEL_CLOSE);

        // Thiết lập các nút cho hộp thoại
        alert.getButtonTypes().setAll(acceptButton, declineButton);

        // Hiển thị hộp thoại và đợi phản hồi từ người dùng
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == acceptButton) {
            // Người dùng nhấn Accept
            acceptInvite();
        } else {
            // Người dùng nhấn Decline hoặc đóng hộp thoại
            declineInvite();
        }
    }

    private void acceptInvite() {
        try {
            // Gửi phản hồi chấp nhận tới máy chủ nếu cần
            dOut.writeObject("Accept:" + player.getUsername());
            dOut.flush();

            // Tải và chuyển đến scene lobby.fxml
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby.fxml"));
//            Parent root=loader.load();
//            LobbyController controller = loader.getController();
//            controller.setUserData(player);
//            // Lấy Stage hiện tại từ một node đã có (ví dụ: usernameLabel)
//            Stage stage = (Stage) usernameLabel.getScene().getWindow();
//            stage.setScene(new Scene(root));
//            stage.setTitle("Catch The Word");

        } catch (IOException e) {
            e.printStackTrace();
            // Hiển thị thông báo lỗi nếu không thể chuyển scene
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Unable to load lobby scene.");
            errorAlert.showAndWait();
        }
    }

    private void declineInvite() {
        try {
            // Gửi phản hồi từ chối tới máy chủ nếu cần
            dOut.writeObject("Decline Invite");
            dOut.flush();
            System.out.println("Invite declined.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enterLobby() {
        try {
            // Tải và chuyển đến scene lobby.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby.fxml"));
            Parent root = loader.load();

            // Nếu bạn cần truyền dữ liệu đến LobbyController, làm như sau:
            LobbyController controller = loader.getController();
            if (controller != null) {
                controller.setUserData(this.player);
            }


            // Lấy Stage hiện tại từ một node đã có (ví dụ: usernameLabel)
            BorderPane borderPane = (BorderPane) usernameLabel.getScene().getRoot(); // Lấy root là BorderPane
            Stage stage = (Stage) borderPane.getScene().getWindow();
//            Stage stage = (Stage) usernameLabel.getScene().getWindow();
//            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Hiển thị thông báo lỗi nếu không thể chuyển scene
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Unable to load lobby scene.");
            errorAlert.showAndWait();
        }
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