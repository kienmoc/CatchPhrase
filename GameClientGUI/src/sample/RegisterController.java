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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    public void register() throws IOException {
        String username = usr.getText();
        String password = pwd.getText();
        String confirmedPassword = confirmed_pwd.getText();
        if(username.isEmpty()){
            JOptionPane.showMessageDialog(null,"Name cannot be Empty..");
        } else if(username.equals("SERVER") || username.equals("Round")) {
            JOptionPane.showMessageDialog(null, "name \"" + username + "\" is not allowed, please change..");
            usr.clear();
//        } else if(DBConnection.checkUserExists(username)){
//            JOptionPane.showMessageDialog(null,"Username already exists !");
        } else if(password.isEmpty()){
            JOptionPane.showMessageDialog(null,"Password cannot be Empty.." );
        } else if(!password.equals(confirmedPassword)){
            JOptionPane.showMessageDialog(null,"Passwords do not match !");
        } else {
            Socket server = new Socket("localhost", 6666);
            ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(server.getInputStream());

            String register = "Register:" + username + "/" + password;
            oos.writeObject(register);
            oos.flush();

            double score = ois.readDouble();
            System.out.println(score);

            oos.close();
            ois.close();
            server.close();

            if(score != -1.0) {
                Socket ser = new Socket("localhost", 6666);
                player = new UserData(username, password, score);
                player.setSocket(ser);
            } else {
                JOptionPane.showMessageDialog(null,"Username or password is incorrect !");
            }
            try {
                FXMLLoader loader=new FXMLLoader(getClass().getResource("home.fxml"));
                Parent root=loader.load();
//                LobbyController controller = loader.getController();
                HomeController controller = loader.getController();
                controller.setUserData(player);
                Stage stage = (Stage) registerButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Đuổi hình bắt chữ");
                stage.show();
            } catch (IOException e) {e.printStackTrace();} catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
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