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
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class CanvasController {
    @FXML
    private Canvas canvas;
    @FXML
    private TextField message;

    @FXML
    private TextArea list;

    @FXML
    private Label displayTimer, wordLabel;
    private GraphicsContext g;
    //xxx
    @FXML
    private ImageView imageView;
    private final String imagesPath = "images";
    Thread timer;
    public static AtomicBoolean isOneCorrect = new AtomicBoolean(false);

    public File getRandomImageFile() {
        File folder = new File(imagesPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));

        if (files != null && files.length > 0) {
            Random rand = new Random();
            return files[rand.nextInt(files.length)];
        }
        return null;
    }

    public void loadImageFromServer(Image image) {
        Platform.runLater(() -> imageView.setImage(image));
    }
//xxx

    //    File newFile = new File("cursor.png");
//    private final Image penCursor = new Image(newFile.toURI().toString());
    private CheckStatus checker = new CheckStatus();

    @FXML
    public void initialize() {
        new Thread(this::gameHandler).start();
        list.setWrapText(true);
        // Tùy chỉnh khác liên quan đến ImageView nếu cần
    }

    public void ClearImageView() {
        Platform.runLater(() -> {
            // Đặt nội dung ImageView thành null để xóa ảnh hiện tại
            imageView.setImage(null);
        });
    }

    public void setWord(String word){
        wordLabel.setText("GUESS THIS: " + word);
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

    public void onExit() { System.exit(0); }

    public Thread setWaitTimer(int duration){
        Thread t=new Thread(() -> {
            try {
                for (int dur=duration;dur>=1;dur--) {
                    int finalDur = dur;
                    Platform.runLater(() -> displayTimer.setText("Next Round in: " + finalDur));
                    sleep(1000);
                }
                Platform.runLater(() -> displayTimer.setText(""));
            } catch (InterruptedException e) {
                System.out.println("Wait timer was interrupted.");
            }
        });
        t.start();
        return t;
    }

    public Thread setTimer(int duration){
        Thread t=new Thread(() -> {
            try {
                for (int dur=duration;dur>=1;dur--) {
                    int finalDur = dur;
                    Platform.runLater(() -> displayTimer.setText("Timer: " + finalDur));
                    Thread.sleep(1000);
                }
                Platform.runLater(() -> displayTimer.setText("Timer: " + 0));
            } catch (InterruptedException e) {
                System.out.println("Timer was interrupted.");
            }
        });
        t.start();
        return t;
    }

    public void gameHandler() {
        int pc=ServerMain.playerCount;
        Image[] images = new Image[3];
        String[] words = new String[3];

        for (int i = 0; i < 3; i++) {
            File imageFile = getRandomImageFile();
            images[i] = new Image(imageFile.toURI().toString());
            words[i] = imageFile.getName().replace(",", " ").replace(".png", "");
        }

        for(int round = 1; round <= ServerMain.rounds; round++) {

            Platform.runLater(this::ClearImageView);
            Platform.runLater(() -> {
                list.clear();
            });
            System.out.print("GameHandler round: " + round);
            int i = round - 1;
            String word = words[i];
            String wordLength = getWordLength(word);
            System.out.println(", Word chosen: " + word);
            try {
                sendResOut("Round: " + round + "   word: " + wordLength);
                sendImage(images[i]);
            } catch (IOException e) { e.printStackTrace(); }

            int finalI = i;
            Platform.runLater(() -> {
                setWord(words[finalI]);
                loadImageFromServer(images[finalI]);
            });
            Thread timer=setTimer(35),waitTimer;
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
                    waitTimer=setWaitTimer(5);
                    sendResOut("ROUND OVER");
                    for(int pnum=0;pnum<pc;pnum++) play[pnum].join();
                    sendResOut("Round: "+round+"   word: "+word);
                    waitTimer.join();
                }
                else {
                    String result = ServerMain.getWinners();
                    if(result.equals("draw")) {
                        sendResOut("Draw game \n Everyone get 0.5 point");
                        for(i=0;i < ServerMain.names.size();i++){
                            String username = ServerMain.names.get(i);
                            GameDAO.IncreasePoint(username, 0.5);
                        }
                    }

                    sendResOut("Winner:\n" + ServerMain.getWinners());
                    sendResOut("GAME OVER");
                    sendResOut("Round: " + round + "   word: " + word);
                    System.exit(0);
                }
            } catch (InterruptedException | IOException e) { e.printStackTrace(); }
        }
    }

    public void gamePlay(int pnum,String word,Thread timer){
        String pname=ServerMain.names.get(pnum);
        try {
            ObjectInputStream ois = ServerMain.oisList.get(pnum);
            while (timer.isAlive()){
                String guess = (String) ois.readObject();
                if(guess.equals("IM_DONE_GUESSING")) break;
                System.out.println(pname + "s message is: " + guess);

                Platform.runLater(()->list.appendText(pname+": "+guess+"\n"));
                if (word.equals(guess.toLowerCase()) && timer.isAlive()){
                    int score = ServerMain.scoreList.get(pnum);
                    ServerMain.scoreList.set(pnum, score+10);
                    sendResOut(pname + ": Got it Correct!");
//
                    if(ServerMain.scoreList.get(pnum) == 20) {
                        sendResOut("Winner:\n" + ServerMain.getWinners() + "\n" + ServerMain.getWinners() + " get 1 points");
                        GameDAO.IncreasePoint(pname, 1.0);
                        sendResOut("GAME OVER");
                    }
//
                } else sendResOut(pname + ": " + guess);
            }
            isOneCorrect.set(true);

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

