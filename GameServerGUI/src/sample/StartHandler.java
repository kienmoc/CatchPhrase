package sample;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.beans.Statement;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class StartHandler extends Thread{
    private ObjectInputStream dIn;
    private ObjectOutputStream dOut;
    private ArrayList<StartHandler> clientsList;
    private int slno;
    private String username;
    private static Map<String, ObjectOutputStream> userOutputStreams = new ConcurrentHashMap<>();

    public StartHandler(ArrayList<StartHandler> sList,int num,
                        ObjectOutputStream oos,ObjectInputStream ois) {
        clientsList=sList;
        slno=num;
        dOut=oos;
        dIn=ois;
    }
    @Override
    public void run() {

        try {
            Socket s1=ServerMain.listener1.accept();
            ServerMain.socketList.add(s1);
            ServerMain.canvasOut.add(s1.getOutputStream());
            username= (String) dIn.readObject();
            System.out.println("StartHandler: " + username + " has joined");

            userOutputStreams.put(username, dOut);

            ServerMain.names.add(username);
            ServerMain.scoreList.add(0);
            sendFriendsList();

//            executor = Executors.newSingleThreadExecutor();
//            readerTask = executor.submit(() -> {
//            while (listening.get()) {
                System.out.println(username + "is listening !");
                Object request = null;
                try {
                    request = dIn.readObject();
                    if (request instanceof String message) {
                        if (message.startsWith("Invite:")) {
                            // Ví dụ: "Invite:usernameTarget"
                            String targetUsername = message.substring(7).trim();
                            sendInviteToTarget(username, targetUsername);
                            start();
                        } else if (message.startsWith("Accept:") || message.startsWith("Decline:")) {
                            // Ví dụ: "Accept:usernameTarget" hoặc "Decline:usernameTarget"
                            String[] parts = message.split(":");
                            if (parts.length == 2) {
                                String response = parts[0];
                                String targetUsername = parts[1].trim();
                                notifyInviteResponse(username, targetUsername, response);
                                sendEnterLobby(targetUsername, username);
                            }
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

//            }
//            });
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client " + username + " disconnected.");
        }
    }

    private void sendEnterLobby(String user1, String user2){
        ObjectOutputStream out1 = userOutputStreams.get(user1);
        ObjectOutputStream out2 = userOutputStreams.get(user2);

        if (out1 != null && out2 != null) {
            try {
                out1.writeObject("Enter Lobby");
                out1.flush();

                out2.writeObject("Enter Lobby");
                out2.flush();

                System.out.println("Both " + user1 + " and " + user2 + " are entering the lobby.");
//
                Platform.runLater(() -> {
                    loadCanvas();
                });
//

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("One or both users are not online. Cannot enter lobby.");
        }
    }

    private void loadCanvas() {

        try {
            sleep(1000);
            outToAll();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("canvas.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.setScene(scene);
//            stage.setTitle("Game");
//            stage.show();

//            CanvasController controller = loader.getController();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendFriendsList() throws IOException {
        for (StartHandler acl : clientsList) {
            Map<String, String> friendsMap = ServerMain.getFriendsMap(acl.username);
            acl.dOut.writeObject(friendsMap); // Gửi Map bạn bè
            dOut.flush(); // Đảm bảo dữ liệu được gửi
        }
    }

    private void sendInviteToTarget(String fromUsername, String targetUsername) {
        int targetIndex = ServerMain.names.indexOf(targetUsername);
        if (targetIndex != -1) {
            try {
                StartHandler targetHandler = ServerMain.clients.get(targetIndex);
                targetHandler.dOut.writeObject("Invite from " + fromUsername);
                targetHandler.dOut.flush();
                System.out.println("Invite from " + fromUsername + " sent to " + targetUsername);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                // Nếu không tìm thấy người dùng mục tiêu, gửi thông báo lỗi về client gửi yêu cầu
                dOut.writeObject("User " + targetUsername + " not found.");
                dOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Target user " + targetUsername + " not found.");
        }
    }

    private void notifyInviteResponse(String fromUsername, String targetUsername, String response) {
        int targetIndex = ServerMain.names.indexOf(fromUsername);
        if (targetIndex != -1) {
            try {
                StartHandler inviterHandler = ServerMain.clients.get(targetIndex);
                inviterHandler.dOut.writeObject(response + ":" + targetUsername);
                inviterHandler.dOut.flush();
                System.out.println("Notify " + fromUsername + " about response from " + targetUsername + ": " + response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void outToAll() throws IOException {
        for(StartHandler acl:clientsList){
            for(String name:ServerMain.names) {
                acl.dOut.writeObject(name);
                dOut.flush();
            }
        }
    }
}

// to check if connection is lost
//            while(true){
//                if(client.getInputStream().read()==-1){
//                    System.out.println(name+" has left");
//                    client.close();
//                    break;
//                }
//                sleep(1000);
//            }
