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


public class LoginController {
    @FXML
    private TextField usr, pwd;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink registerLink;

    UserData player;

    public void initialize(){}

    public void login() throws IOException {
        String username = usr.getText();
        String password = pwd.getText();
        if(username.isEmpty()){
            JOptionPane.showMessageDialog(null,"Name cannot be Empty..");
        } else if(username.equals("SERVER") || username.equals("Round")){
            JOptionPane.showMessageDialog(null,"name \""+username+"\" is not allowed, please change..");
            usr.clear();
        } else {
            Socket server = new Socket("localhost", 6666);
            ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(server.getInputStream());

            String login = "Login:" + username + "/" + password;
            oos.writeObject(login);
            oos.flush();

            double score = ois.readDouble();
            System.out.println(score);

            oos.close();
            ois.close();
            server.close();

            if(score != -1.0) {
//                Socket ser = new Socket("192.168.0.177", 6666);
                Socket ser = new Socket("localhost", 6666);
                player = new UserData(username, password, score);
                player.setSocket(ser);
            } else {
                JOptionPane.showMessageDialog(null,"Username or password is incorrect !");
            }
//
//            if(DBConnection.authenticateUser(username, password)) {
//                try {
//                    double score = DBConnection.getScoreFromUser(username);
//                    player = new UserData(username, password, score);
////                    Socket server = new Socket("localhost", 6666);
//                    player.setSocket(server);
//                } catch (IOException e) {
//                    JOptionPane.showMessageDialog(null,"No server running in the entered IP address\n"+
//                            "  please recheck it and try again..","CONNECTION ERROR", JOptionPane.WARNING_MESSAGE);
//                    System.out.println("Server not found..");
//                    System.exit(0);
//                }
//
//            } else {
//                JOptionPane.showMessageDialog(null,"Username or password is incorrect !");
//            }
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
            } catch (IOException e) {e.printStackTrace();} catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
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
