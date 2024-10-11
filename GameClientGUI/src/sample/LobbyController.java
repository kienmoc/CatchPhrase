package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.Thread.sleep;

public class LobbyController {
    @FXML
    private TextArea playerList;
    @FXML
    private Label displayTimer;

    private ObjectInputStream dIn;
    private UserData player;

    private ExecutorService executor;
    private Future<?> readerTask;

    public void initialize() {

    }

    public void setUserData(UserData u) throws IOException {
        player=u;
        dIn=player.ois;
        getPlayers();
    }

    public void getPlayers(){
        new Thread(() -> {
            try {
                for(int i=1;i<=2;i++){
                    int finalI = i;
                    String friend = (String) dIn.readObject();
                    if(!friend.trim().startsWith("Word:")) {
                        System.out.println("Friend: " + friend);
                        Platform.runLater(() -> playerList.appendText(finalI + ". " + friend + "\n"));
                        System.out.println(i + ": " + friend);
                    }
                }
                setTimer(5);
            } catch (IOException | ClassNotFoundException e) {e.printStackTrace();}
        }).start();
    }

    public void setTimer(int duration){
        executor = Executors.newSingleThreadExecutor();
        readerTask = executor.submit(() -> {
            try {
                for (int dur = duration; dur >= 1; dur--) {
                    int finalDur = dur;
                    Platform.runLater(() -> displayTimer.setText("Game Starts in: " + finalDur));
                    sleep(1000);
                }
                Platform.runLater(() -> nextScene());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void nextScene(){
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("imageview.fxml"));
            Parent root=loader.load();

            ImageviewController controller = loader.getController();
            controller.setUserData(player);

            Stage stage = (Stage) displayTimer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {e.printStackTrace();}
    }
}
