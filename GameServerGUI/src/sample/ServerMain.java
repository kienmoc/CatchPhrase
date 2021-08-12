package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ServerMain extends Application{
    public static ServerSocket listener,listener1;
    static ArrayList<StartHandler> clients=new ArrayList<>();
    static ArrayList<Socket> socketList=new ArrayList<>();
    public static ArrayList<ObjectOutputStream> oosList=new ArrayList<>();
    public static ArrayList<ObjectInputStream> oisList=new ArrayList<>();
    public static ArrayList<OutputStream> canvasOut=new ArrayList<>();

    public static int playerCount=2;
    public static int rounds=3;

    public static ArrayList<String> names=new ArrayList<>();
    public static ArrayList<Integer> scoreList=new ArrayList<>();
    private static final ExecutorService pool=Executors.newFixedThreadPool(playerCount);

    public static ArrayList<String> words=new ArrayList<>();

    Stage stage;
    @Override
    public void start(Stage primaryStage) throws Exception{
        stage=primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("canvas.fxml"));
        stage.setTitle("Pictionary SERVER");
        Scene scene=new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        getWords();
        getStartHandler();
        System.out.println("GameHandler Started..");
        launch(args);
    }

    private static void getWords() throws FileNotFoundException {
        File wordsFile=new File("words.txt");
        Scanner sc=new Scanner(wordsFile);
        sc.useDelimiter(", ");
        while(sc.hasNext()) words.add(sc.next().toLowerCase(Locale.ROOT));
//        Pattern pat = Pattern.compile("[a-zA-Z]+");
//        while(sc.hasNext() && pat.matcher(sc.next()).matches()) words.add(sc.next().toLowerCase(Locale.ROOT));
    }

    private static void getStartHandler() throws InterruptedException, IOException {
        System.out.println("Player count set to: "+playerCount);
        listener=new ServerSocket(6666);
        listener1=new ServerSocket(6677);
        System.out.println("Waiting for clients...");
        Socket client;
        for(int i=1;i<=playerCount;i++){
            client=listener.accept();
            ObjectOutputStream dOut=new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream dIn=new ObjectInputStream(client.getInputStream());
            System.out.println("MAIN: Connected: "+i);
            StartHandler sThread=new StartHandler(clients,i,dOut,dIn);
            oosList.add(dOut);
            oisList.add(dIn);
            clients.add(sThread);
            pool.execute(sThread);
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("StartHandler Closed..");
    }

    static String getWinners(){
        int i=0,ind=0,max=0;
        for(int score:scoreList){
            if(score>max){ind=i; max=score;}
            i++;
        }
        if(max==0) return "No winners";
        String wins="";
        for(i=0;i<names.size();i++){
            if(scoreList.get(i)==max){
                wins+=names.get(i)+"\n";
            }
        }
        return wins;
    }
}

class CheckStatus{
    public void checkPresence() throws IOException {
        int i=0;
        for(Socket s:ServerMain.socketList){
            if(s.getInputStream().read()==-1){
                playerExitHandler(i);
                break;
            }
            i++;
        }
    }

    public static void playerExitHandler(int ind){
        System.out.println(ServerMain.names.get(ind)+" has left..");
        ServerMain.names.remove(ind);
        ServerMain.scoreList.remove(ind);
        ServerMain.oosList.remove(ind);
        ServerMain.oisList.remove(ind);
        ServerMain.canvasOut.remove(ind);

        if(ServerMain.names.size()==0){
            System.out.println("All players have Left...\nClosing SERVER..");
            System.exit(0);
        }
    }
}
