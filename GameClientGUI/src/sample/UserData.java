package sample;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class UserData {
    public String username,serverIP;
    public String user, password;
    public double score;
    public Socket server,imageIn,allResIn;
    ObjectOutputStream oos;
    ObjectInputStream ois;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public UserData(String username, String password, double score) throws IOException {
        this.username = username;
        this.password = password;
        this.score = score;
        imageIn=new Socket("26.30.187.160",6677);
    }

//    public UserData(String name,String ip) throws IOException {
//        this.username=name;
//        this.serverIP=ip;
//        imageIn=new Socket(ip,6677);
//    }

    public Socket getSocket() {
        return server;
    }

    public void setSocket(Socket s) throws IOException {
        this.server=s;
        ois=new ObjectInputStream(server.getInputStream());
        oos=new ObjectOutputStream(server.getOutputStream());
        oos.writeObject(username);
        oos.flush();
    }
}
