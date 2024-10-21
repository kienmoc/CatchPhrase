package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.swing.*;
import java.io.IOException;
import java.net.Socket;

public class RegisterController {
    @FXML
    private TextField usr, pwd, confirmed_pwd;
    @FXML
    private Button registerButton;
    @FXML
    private Hyperlink loginLink;
    UserData player;
    public void initialize(){}

    public void register() {
        String username = usr.getText();
        String password = pwd.getText();
        String confirmedPassword = confirmed_pwd.getText();
        if(username.isEmpty()){
            JOptionPane.showMessageDialog(null,"Name cannot be Empty..");
        } else if(username.equals("SERVER") || username.equals("Round")) {
            JOptionPane.showMessageDialog(null, "name \"" + username + "\" is not allowed, please change..");
            usr.clear();
        } else if(DBConnection.checkUserExists(username)){
            JOptionPane.showMessageDialog(null,"Username already exists !");
        } else if(password.isEmpty()){
            JOptionPane.showMessageDialog(null,"Password cannot be Empty.." );
        } else if(!password.equals(confirmedPassword)){
            JOptionPane.showMessageDialog(null,"Passwords do not match !");
        } else {
            if(DBConnection.registerUser(username, password)) {
                try {
                    double score = DBConnection.getScoreFromUser(username);
                    player = new UserData(username, password, score);
                    Socket server = new Socket("localhost", 6666);
                    player.setSocket(server);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,"No server running in the entered IP address\n"+
                            "  please recheck it and try again..","CONNECTION ERROR", JOptionPane.WARNING_MESSAGE);
                    System.out.println("Server not found..");
                    System.exit(0);
                }
            } else {
                JOptionPane.showMessageDialog(null,"Username already exists !");
            }
            try {
                FXMLLoader loader=new FXMLLoader(getClass().getResource("lobby.fxml"));
                Parent root=loader.load();
                LobbyController controller = loader.getController();
                controller.setUserData(player);
                Stage stage = (Stage) registerButton.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void showLoginForm() {
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root=loader.load();
            Stage stage = (Stage) loginLink.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}