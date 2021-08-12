package sample;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class UserData {
    public String username,serverIP;
    public Socket server,imageIn,allResIn;
    ObjectOutputStream oos;
    ObjectInputStream ois;

    public UserData(String name,String ip) throws IOException {
        this.username=name;
        this.serverIP=ip;
        imageIn=new Socket(ip,6677);
    }

    public void setSocket(Socket s) throws IOException {
        this.server=s;
        ois=new ObjectInputStream(server.getInputStream());
        oos=new ObjectOutputStream(server.getOutputStream());
        oos.writeObject(username);
        oos.flush();
    }
}
