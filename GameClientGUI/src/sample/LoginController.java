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


public class LoginController {
    @FXML
    private TextField usr, pwd;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink registerLink;

    UserData player;

    public void initialize(){}

    public void login() {
        String username = usr.getText();
        String password = pwd.getText();
        if(username.isEmpty()){
            JOptionPane.showMessageDialog(null,"Name cannot be Empty..");
        } else if(username.equals("SERVER") || username.equals("Round")){
            JOptionPane.showMessageDialog(null,"name \""+username+"\" is not allowed, please change..");
            usr.clear();
        } else {
            if(DBConnection.authenticateUser(username, password)) {
                try {
                    double score = DBConnection.getScoreFromUser(username);
                    player = new UserData(username, password, score);
                    System.out.println("Hi");
                    Socket server = new Socket("26.30.187.160", 6666);
                    player.setSocket(server);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,"No server running in the entered IP address\n"+
                            "  please recheck it and try again..","CONNECTION ERROR", JOptionPane.WARNING_MESSAGE);
                    System.out.println("Server not found..");
                    System.exit(0);
                }

            } else {
                JOptionPane.showMessageDialog(null,"Username or password is incorrect !");
            }
            try {
                FXMLLoader loader=new FXMLLoader(getClass().getResource("home.fxml"));
                Parent root=loader.load();
//                LobbyController controller = loader.getController();
                HomeController controller = loader.getController();
                controller.setUserData(player);
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Đuổi hình bắt chữ");
                stage.show();
            } catch (IOException e) {e.printStackTrace();}
        }
    }

    public void showRegisterForm(){
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("register.fxml"));
            Parent root=loader.load();
            Stage stage=(Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đuổi hình bắt chữ");
            stage.show();
        } catch (IOException e) {e.printStackTrace();}
    }
    public void onExit(){
        Platform.exit();
    }
}
