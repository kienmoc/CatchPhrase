package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.net.InetAddress;
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
        getStartHandler();
        System.out.println("GameHandler Started..");
        launch(args);
    }

    private static void getStartHandler() throws InterruptedException, IOException {
        System.out.println("Player count set to: " + playerCount);
        listener=new ServerSocket(6666);
        listener1=new ServerSocket(6677);
        System.out.println("Waiting for clients...");
        Socket client;
//        int connectedPlayers = 0;
//        while (connectedPlayers < playerCount) {
//            client = listener.accept();
//            ObjectOutputStream dOut = new ObjectOutputStream(client.getOutputStream());
//            ObjectInputStream dIn = new ObjectInputStream(client.getInputStream());
//            connectedPlayers++;
//            System.out.println("MAIN: Connected: " + connectedPlayers);
//            StartHandler sThread = new StartHandler(clients, connectedPlayers, dOut, dIn);
//            oosList.add(dOut);
//            oisList.add(dIn);
//            socketList.add(client);
//            clients.add(sThread);
//
//            pool.execute(sThread);
//        }
        for(int i=1;i<=playerCount;i++){
            client=listener.accept();
            ObjectOutputStream dOut=new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream dIn=new ObjectInputStream(client.getInputStream());
            System.out.println("MAIN: Connected: "+i);
            StartHandler sThread=new StartHandler(clients, i, dOut, dIn);
            oosList.add(dOut);
            oisList.add(dIn);
            clients.add(sThread);
            socketList.add(client);

            String username = null; // Đọc tên người dùng từ client
            try {
                username = (String) dIn.readObject();
                names.add(username);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            pool.execute(sThread);
        }

        sendFriendsList();

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("StartHandler Closed..");
//        System.out.println("StartHandler Ready to accept more clients..");
    }

    public static void sendFriendsList() throws IOException {
        Map<String, String> friendsMap = new HashMap<>();
        StringBuilder friendsList = new StringBuilder();

        for (int i = 0; i < names.size(); i++) {
//            Socket socket = socketList.get(i);
//            InetAddress address = socket.getInetAddress(); // Lấy địa chỉ IP
//            String friendInfo = names.get(i) + " (" + address.getHostAddress() + ")"; // Kết hợp tên và IP
//            friendsList.append(friendInfo).append("\n");
//            friendsMap.put(names.get(i), friendInfo);
            Socket socket = socketList.get(i);
            InetAddress address = socket.getInetAddress(); // Lấy địa chỉ IP
            String friendInfo = address.getHostAddress(); // Lấy địa chỉ IP
            friendsMap.put(names.get(i), friendInfo);

        }
        for (ObjectOutputStream oos : oosList) {
            oos.writeObject(friendsMap); // Gửi danh sách bạn bè
            oos.flush(); // Đảm bảo dữ liệu được gửi
        }
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
        ServerMain.socketList.remove(ind);

        if(ServerMain.names.size()==0){
            System.out.println("All players have Left...\nClosing SERVER..");
            System.exit(0);
        }
    }
}
