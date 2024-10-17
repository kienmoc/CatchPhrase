package sample;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;

import static java.lang.Thread.*;

public class ImageviewController {
    @FXML
    private ImageView imView;
    @FXML
    private TextField message;
    @FXML
    private TextArea list;
    @FXML
    private Label displayTimer,playerDisplay,serverLabel;
    private UserData player;
    private ObjectOutputStream dOut;
    private ObjectInputStream dIn;
    boolean gameOver=false;

    public void onSave(){
        try{
            String path="/home/tarun/Desktop/paint1.png";
            Image snapshot=imView.snapshot(null,null);
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot,null),"png",new File(path));
        } catch (Exception e){ System.out.println("Failed to save image: "+e); }
    }

    public void onMessage(){
        message.setOnAction(e -> {
            String str=message.getText();
            str=str.trim();
            if(!str.isEmpty()) {
                try {
                    dOut.writeObject(str);
                    dOut.flush();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                System.out.println("Im done write object !!!");
                message.clear();
            }
        });
    }

    public void tellServer(String str){
        try { dOut.writeObject(str); dOut.flush(); }
        catch (IOException ioException) { ioException.printStackTrace(); }
    }

    public void onExit(){ System.exit(0); }

    public Thread setTimer(int duration){
        Thread t=new Thread(() -> {
            try {
                for (int dur=duration;dur>=1;dur--) {
                    int finalDur = dur;
                    Platform.runLater(() -> displayTimer.setText("Timer: "+finalDur));
                    sleep(1000);
                }
                Platform.runLater(() -> displayTimer.setText("Timer: "+0));
            } catch (InterruptedException e) { e.printStackTrace(); }
        });
        t.start();
        return t;
    }

    public Thread setWaitTimer(int duration){
        Thread t=new Thread(() -> {
            try {
                for (int dur=duration;dur>=1;dur--){
                    int finalDur = dur;
                    Platform.runLater(() -> displayTimer.setText("Next Round in: "+finalDur));
                    sleep(1000);
                }
                Platform.runLater(() -> displayTimer.setText("Next Round in: "+0));
            } catch (InterruptedException e) { e.printStackTrace(); }
        });
        t.start();
        return t;
    }

    public void setUserData(UserData u) throws IOException {
        player = u;
        dIn = player.ois;
        dOut = player.oos;
        playerDisplay.setText("PLAYER: " + player.getUsername());
        imageReceiver();
        allResponses();
    }

    public void imageReceiver() throws IOException {
        InputStream is = player.imageIn.getInputStream();
        new Thread(() -> {
            byte[] imSize, byteImage;
            int sizeInt;
            BufferedImage bIn;
            try {
                while (!gameOver) {
                    imSize = new byte[4];
                    is.read(imSize);
                    sizeInt = ByteBuffer.wrap(imSize).getInt();
                    byteImage = is.readNBytes(sizeInt);
                    bIn = ImageIO.read(new ByteArrayInputStream(byteImage));
                    Image im = SwingFXUtils.toFXImage(bIn, null);
                    Platform.runLater(() -> imView.setImage(im));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void allResponses(){
        Thread allres = new Thread(() -> {
            Thread timer=setTimer(0);
            try {
                while(true){
                    String res = (String) dIn.readObject();
                    if(res.startsWith("Round: ")){
                        timer.join();
                        timer = setTimer(30);
                        Platform.runLater(()->serverLabel.setText(res));
                    } else if(res.equals("ROUND OVER")) {
                        timer.join();
                        tellServer("IM_DONE_GUESSING");
                        String ans=(String) dIn.readObject();
                        Platform.runLater(()->{
                            serverLabel.setText(ans);
                            list.clear();
                        });
                        timer = setWaitTimer(5);
                    } else if(res.equals("GAME OVER")) {
                        timer.join();
                        String ans=(String) dIn.readObject();
                        Platform.runLater(()->serverLabel.setText(ans));
                        timer=setTimer(5);
                        timer.join();
                        Platform.runLater(() -> serverLabel.setText(res));
                        timer=setTimer(5);
                        timer.join();
                        player.server.close();
                        System.exit(0);

                    } else Platform.runLater(()->list.appendText(res+"\n"));
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) { e.printStackTrace(); }
        });
        allres.start();
    }
}
