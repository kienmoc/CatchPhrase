package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
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
    private static final ExecutorService pool=Executors.newCachedThreadPool();
    Stage stage;
    @Override
    public void start(Stage primaryStage) throws Exception{}

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        getStartHandler();
        System.out.println("GameHandler Started..");
    }

    private static void getStartHandler() throws InterruptedException, IOException, ClassNotFoundException {
        System.out.println("Player count set to: " + playerCount);
        listener=new ServerSocket(6666);
        listener1=new ServerSocket(6677);
        System.out.println("Waiting for clients...");
        Socket client;

        int idx = 1;
        while(idx <= playerCount) {
            client=listener.accept();
            ObjectOutputStream dOut=new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream dIn=new ObjectInputStream(client.getInputStream());

            String userAct = (String) dIn.readObject();
            String action = userAct.split(":")[0];
            String username = userAct.split(":")[1].split("/")[0];
            String password = userAct.split(":")[1].split("/")[1];

            if(action.equals("Register")) {
                if(DBConnection.registerUser(username, password)) {
                    double score = DBConnection.getScoreFromUser(username);
                    dOut.writeDouble(score);
                    dOut.flush();
                    System.out.println("Im writing");
                    System.out.println("MAIN: Connected: "+ idx);

                    client.close();
                    dIn.close();
                    dOut.close();

                    client = listener.accept();
                    dOut=new ObjectOutputStream(client.getOutputStream());
                    dIn=new ObjectInputStream(client.getInputStream());
                    StartHandler sThread=new StartHandler(clients, idx, dOut, dIn, username);
                    oosList.add(dOut);
                    oisList.add(dIn);
                    clients.add(sThread);
                    pool.execute(sThread);
                    idx++;
                } else {
                    dOut.writeDouble(-1.0);
                    dOut.close();
                    dIn.close();
                    client.close();
                }
            } else {
                if(DBConnection.authenticateUser(username, password)) {

                    double score = DBConnection.getScoreFromUser(username);
                    dOut.writeDouble(score);
                    dOut.flush();
                    System.out.println("Im writing");
                    System.out.println("MAIN: Connected: "+ idx);

                    client.close();
                    dIn.close();
                    dOut.close();

                    client = listener.accept();
                    dOut=new ObjectOutputStream(client.getOutputStream());
                    dIn=new ObjectInputStream(client.getInputStream());

                    StartHandler sThread=new StartHandler(clients, idx, dOut, dIn, username);
                    oosList.add(dOut);
                    oisList.add(dIn);
                    clients.add(sThread);
                    pool.execute(sThread);
                    idx++;
                } else {
                    dOut.writeDouble(-1.0);
                    dOut.close();
                    dIn.close();
                    client.close();
                }
            }

        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("StartHandler Closed..");
    }

    public static Map<String, String> getFriendsMap(String excludeUsername) {
        Map<String, String> friendsMap = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (!name.equals(excludeUsername)) {
                String ipAddress = socketList.get(i).getInetAddress().getHostAddress();
                friendsMap.put(name, ipAddress);
            }
        }
        return friendsMap;
    }

    static String getWinners() {
        int i=0,ind=0,max=0;
        for(int score:scoreList){
            if(score>max){ind=i; max=score;}
            i++;
        }

        if(max==0) return "No winners";
        String wins="", result = "";
        int cnt = 0;
        for(i=0;i<names.size();i++){
            if(scoreList.get(i)==max){
                wins = names.get(i);
                cnt++;
            }
        }
        if(cnt == 2) {
            return "draw";
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
        ServerMain.socketList.remove(ind);

        if(ServerMain.names.size()==0){
            System.out.println("All players have Left...\nClosing SERVER..");
            System.exit(0);
        }
    }
}