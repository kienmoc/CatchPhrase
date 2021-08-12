package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.swing.*;
import java.io.IOException;
import java.net.Socket;


public class IntroController {
    @FXML
    private TextField name,ip;
    @FXML
    private Button go;

    UserData player;

    public void initialize(){}

    public void nextScene() {
        String pname = name.getText();
        String addr=ip.getText();
        if(pname.isEmpty()){
            JOptionPane.showMessageDialog(null,"Name cannot be Empty..");
        } else if(pname.equals("SERVER")||pname.equals("Round")){
            JOptionPane.showMessageDialog(null,"name \""+pname+"\" is not allowed, please change..");
            name.clear();
        } else {
            try {
                player = new UserData(pname, addr);
                Socket server = new Socket(addr, 6666);
                player.setSocket(server);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,"No server running in the entered IP address\n"+
                        "  please recheck it and try again..","CONNECTION ERROR", JOptionPane.WARNING_MESSAGE);
                System.out.println("Server not found..");
                System.exit(0);
            }

            try {
                FXMLLoader loader=new FXMLLoader(getClass().getResource("lobby.fxml"));
                Parent root=loader.load();
                LobbyController controller = loader.getController();
                controller.setUserData(player);
                Stage stage = (Stage) go.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {e.printStackTrace();}
        }
    }

    public void onExit(){
        Platform.exit();
    }
}
