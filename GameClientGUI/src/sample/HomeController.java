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
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
    private List<String> rankingList;

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

                // Ensure full width and text fill
                content.setMaxWidth(Double.MAX_VALUE);
                usernameLabel.setStyle("-fx-text-fill: white;"); // Set text color to white

            }

            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");  // Set empty cell background to transparent
                } else {
                    usernameLabel.setText(username);
                    usernameLabel.setAlignment(Pos.CENTER_LEFT);
                    inviteButton.setAlignment(Pos.CENTER_RIGHT);
                    content.setStyle("-fx-background-color: #1e1e1e;");
                    setGraphic(content);
                    setStyle("-fx-padding: 0;");
                }
            }
        });

        rankingListView.setCellFactory(param -> new ListCell<String>() {
            private HBox content;
            private Label rankLabel;

            {
                rankLabel = new Label();
                content = new HBox(rankLabel);
                content.setAlignment(Pos.CENTER_LEFT);
                content.setSpacing(10);

                // Ensure full width and text fill
                content.setMaxWidth(Double.MAX_VALUE);
                rankLabel.setStyle("-fx-text-fill: white;"); // Set text color to white
            }

            @Override
            protected void updateItem(String entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");  // Set empty cell background to transparent
                } else {
                    rankLabel.setText(entry);

                    // Example of changing background color for even and odd items
                    content.setStyle("-fx-background-color: #1e1e1e;");

                    // Ensure the background and text style are applied
                    setGraphic(content);
                    setStyle("-fx-padding: 0;");
                }
            }
        });
    }

    // Phương thức để nhận dữ liệu từ LoginController
    public void setUserData(UserData player) throws IOException, ClassNotFoundException {
        this.player = player;
        dIn = player.ois;
        dOut = player.oos;

        // Cập nhật thông tin người dùng
        usernameLabel.setText("Player's name: " + player.getUsername());
        scoreLabel.setText("Score: " + player.getScore());

        // Lắng nghe dữ liệu từ máy chủ
        listenForData();
    }

    // Phương thức cập nhật danh sách bạn bè
    private void updateFriendList(Map<String, String> friendsMap) {
        Platform.runLater(() -> {
            friendsListView.getItems().clear();

            for (String username : friendsMap.keySet()) {
                friendsListView.getItems().add(username); // Chỉ hiển thị username
            }
        });
    }

    // Phương thức để xử lý lời mời
    private void handleInvite(String username) {
        try {
            dOut.writeObject("Invite:" + username);
            dOut.flush();
            System.out.println("Invite request sent to " + username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Phương thức để lắng nghe dữ liệu từ máy chủ
    private void listenForData() {

        executor = Executors.newSingleThreadExecutor();
        readerTask = executor.submit(() -> {
            try {
                while (listening.get()) {
                    Object data = dIn.readObject();
                    System.out.println("Data is: " + data);
                    if (data instanceof Map) {
                        friendsMap = (Map<String, String>) data;
                        updateFriendList(friendsMap);
                    } else if(data instanceof List<?>) {
                        rankingList = (List<String>) data;
                        // Refresh the list view
                        Platform.runLater(() -> rankingListView.getItems().clear());
                        for(String entry: rankingList) {
                            Platform.runLater(() -> rankingListView.getItems().add(entry));
                        }
                    } else if (data instanceof String message) {
                        System.out.println(message);
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

    // Phương thức hiển thị hộp thoại mời
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

    // Phương thức để chấp nhận lời mời
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

    // Phương thức để từ chối lời mời
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

    // Phương thức để chuyển đến scene lobby.fxml
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

    // Phương thức để đóng kết nối
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