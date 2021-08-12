package sample;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.ImageCursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.*;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.ByteBuffer;

import static java.lang.Thread.sleep;

public class CanvasController {
    @FXML
    private Canvas canvas;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private TextField message;
    @FXML
    private CheckBox eraser;
    @FXML
    private TextArea list;
    @FXML
    private Slider brushSize;
    @FXML
    private Button clear;
    @FXML
    private Label displayTimer, wordLabel;
    private GraphicsContext g;

    File newFile = new File("cursor.png");
    private final Image penCursor = new Image(newFile.toURI().toString());
    private CheckStatus checker = new CheckStatus();

    @FXML
    public void initialize() {
        new Thread(this::gameHandler).start();

        brushSize.setMax(100);
        brushSize.setValue(8);
        g = canvas.getGraphicsContext2D();
        canvas.setCursor(new ImageCursor(penCursor, 0, penCursor.getHeight()));
        list.setWrapText(true);

        canvas.setOnMouseDragged(e -> {
            double size = Double.parseDouble(String.valueOf(brushSize.getValue()));
            double x = e.getX() - size/2;
            double y = e.getY() - size/2;
            if (eraser.isSelected()) {
                g.clearRect(x, y, size, size);
            } else {
                g.setFill(colorPicker.getValue());
                g.fillOval(x,y,size,size);
            }
        });

        canvas.setOnMouseClicked(e->{
            Image snapshot = canvas.snapshot(null,null);
            sendImage(snapshot);
        });

        clear.setOnAction(e -> ClearCanvas());
    }

    public void ClearCanvas(){
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        Image snapshot = canvas.snapshot(null, null);
        sendImage(snapshot);
    }

    public void setWord(String word){
        wordLabel.setText("DRAW THIS: "+word);
    }

    public void onSave() {
        try {
            String path = "/home/tarun/Desktop/paint2.png";
            Image snapshot = canvas.snapshot(null, null);
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", new File(path));
        } catch (Exception e) {
            System.out.println("Failed to save image: " + e);
        }
    }

    public void onMessage() {
        message.setOnAction(e -> {
            String str = message.getText();
            if (!str.isEmpty()){
                list.appendText("SERVER: " + str + "\n");
                try {
                    sendResOut("SERVER: "+str);
                } catch (IOException ioe) { ioe.printStackTrace(); }
                message.clear();
            }
        });
    }

    public void onExit() { System.exit(0); }

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
                for (int dur=duration;dur>=1;dur--) {
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

    public void sendImage(Image im){
        new Thread(()->{
            try {
                byte[] imSize;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(SwingFXUtils.fromFXImage(im, null), "png", byteArrayOutputStream);
                imSize = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array();

                for(OutputStream os:ServerMain.canvasOut) {
                    os.write(imSize);
                    os.write(byteArrayOutputStream.toByteArray());
                    os.flush();
                }
                byteArrayOutputStream.close();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    public void gameHandler() {
        int pc=ServerMain.playerCount;

        for(int round=1 ; round<=ServerMain.rounds ; round++) {

            Platform.runLater(this::ClearCanvas);
            System.out.print("GameHandler round: " + round);
            String word = ServerMain.words.get((int) (Math.random() * ServerMain.words.size()));
            System.out.println(", Word chosen: " + word);
            String wordLength=getWordLength(word);
            try {
                sendResOut("Round: "+round+"   word: "+wordLength);
            } catch (IOException e) { e.printStackTrace(); }

            Platform.runLater(() -> setWord(word));
            Thread timer=setTimer(90),waitTimer;
            Thread[] play=new Thread[pc];
            for(int pnum=0;pnum<pc;pnum++) {
                int finalPnum = pnum;
                play[pnum]=new Thread(()->gamePlay(finalPnum,word,timer));
                play[pnum].start();
            }
            try {
                timer.join();
                sendScores(round);
                if(round<ServerMain.rounds) {
                    waitTimer=setWaitTimer(15);
                    sendResOut("ROUND OVER");
                    for(int pnum=0;pnum<pc;pnum++) play[pnum].join();
                    sendResOut("Round: "+round+"   word: "+word);
                    waitTimer.join();
                }
                else {
                    sendResOut("Winner:\n"+ServerMain.getWinners());
                    sendResOut("GAME OVER");
                    sendResOut("Round: "+round+"   word: "+word);
                    System.exit(0);
                }
            } catch (InterruptedException | IOException e) { e.printStackTrace(); }

        }
    }

    public void gamePlay(int pnum,String word,Thread timer){
        String pname=ServerMain.names.get(pnum);
        try {
            ObjectInputStream ois = ServerMain.oisList.get(pnum);
            boolean answered=false;
            while (timer.isAlive()){
                String guess = (String) ois.readObject();
                if(guess.equals("IM_DONE_GUESSING")) break;

                Platform.runLater(()->list.appendText(pname+": "+guess+"\n"));
                if (word.equals(guess.toLowerCase())){
                    int score = ServerMain.scoreList.get(pnum);
                    if(!answered && timer.isAlive()) {
                        ServerMain.scoreList.set(pnum, score+10);
                        sendResOut(pname+": Got it Correct!");
                        answered=true;
                    }
                    else sendResOut(pname+": Already Answered!");
                }
                else sendResOut(pname+": "+guess);
            }
        } catch (IOException | ClassNotFoundException e) {
            try {
                checker.checkPresence();
            } catch (IOException ioException) { ioException.printStackTrace(); }
        }
    }

    public synchronized void sendResOut(String res) throws IOException {
        for(ObjectOutputStream oos:ServerMain.oosList){
            oos.writeObject(res);
            oos.flush();
        }
    }

    public void sendScores(int round) throws IOException {
        if(round==ServerMain.rounds) sendResOut("\nSERVER:\nFINAL SCORES");
        else sendResOut("\nSERVER:\nScores after round:"+round);

        Platform.runLater(()->list.appendText("Scores:\n"));
        int l=ServerMain.names.size();
        for(int i=0;i<l;i++){
            String res=ServerMain.names.get(i)+" - "+ServerMain.scoreList.get(i);
            Platform.runLater(()->list.appendText(res+"\n"));
            sendResOut(res);
        }
    }

    public String getWordLength(String word){
        StringBuilder wl= new StringBuilder();
        for(char c:word.toCharArray()){
            if(c!=' ') wl.append("_ ");
            else wl.append("+ ");
        }
        return wl.toString();
    }

}

