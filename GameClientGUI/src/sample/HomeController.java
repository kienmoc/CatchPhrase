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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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

    //    private volatile boolean listening = true;
    private AtomicBoolean listening = new AtomicBoolean(true);
    private ExecutorService executor;
    private Future<?> readerTask;

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
        executor = Executors.newSingleThreadExecutor();
        readerTask = executor.submit(() -> {
            try {
                while (listening.get()) {
                    Object data = dIn.readObject();
                    System.out.println("Im listening ...");
                    if (data instanceof Map) {
                        friendsMap = (Map<String, String>) data;
                        updateFriendList(friendsMap);
                    } else if (data instanceof String message) {
                        if (message.startsWith("Invite from")) {
                            Platform.runLater(() -> {
                                showInviteDialog(message);
                            });
                        } else if (message.startsWith("Enter Lobby")) {
                            listening.set(false);
                            Platform.runLater(this::enterLobby);

                            readerTask.cancel(true);
                            executor.shutdownNow();
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    private void showInviteDialog(String message) {
        System.out.println("Message is: " + message);
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

        String targetUser = message.split(" ")[message.split(" ").length - 1];


        if (result.isPresent() && result.get() == acceptButton) {
            // Người dùng nhấn Accept
            acceptInvite(targetUser);
        } else {
            // Người dùng nhấn Decline hoặc đóng hộp thoại
            declineInvite();
        }
    }

    private void acceptInvite(String targetUser) {
        try {
            dOut.writeObject("Accept:" + targetUser);
            dOut.flush();
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
        listening.set(false);
        if(!listening.get()) {
            System.out.println("Noooo, i can't listening right now");
        }
        readerTask.cancel(true);
        executor.shutdownNow();
        try {
            // Tải và chuyển đến scene lobby.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby.fxml"));
            Parent root = loader.load();

            LobbyController controller = loader.getController();
            controller.setUserData(this.player);

            BorderPane borderPane = (BorderPane) usernameLabel.getScene().getRoot(); // Lấy root là BorderPane
            Stage stage = (Stage) borderPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Unable to load lobby scene.");
            errorAlert.showAndWait();
        }
    }

    public void closeConnection() {
        try {
            if (dIn != null) {
                dIn.close();
            }
            if (dOut != null) {
                dOut.close();
            }
            if (player.getSocket() != null && !player.getSocket().isClosed()) {
                player.getSocket().close();
            }
        } catch (IOException e) {
            e.printStackTrace();
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