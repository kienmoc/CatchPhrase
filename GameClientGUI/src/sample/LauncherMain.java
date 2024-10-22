package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class LauncherMain extends Application {
    Stage stage;
    @Override
    public void start(Stage primaryStage) throws Exception {
        stage=primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        stage.setTitle("Catch The Word");

        Image applicationIcon=new Image(getClass().getResourceAsStream("paint1.png"));
        stage.getIcons().add(applicationIcon);

        Scene scene=new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}