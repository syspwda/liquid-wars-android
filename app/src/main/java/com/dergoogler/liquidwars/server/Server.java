

package com.dergoogler.liquidwars.server;

import java.lang.Thread;
import java.lang.Runnable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.io.IOException;
import android.content.Context;

public class Server implements SubServer.SubServerCallbacks, Runnable {
    public ServerCallbacks serverCallbacks;
    private int port;
    private ServerSocket serverSocket;
    private ArrayList<SubServer> subServers;
    private Context context;
    private boolean accepting = false;
    private static int MAX_CLIENTS = 5;
    public static int SET_MAP_IMAGE_COMMAND = 0x56;
    public static int TEAM_LIST_COMMAND = 0x57;
    public static int REQUEST_CHANGE_TEAM_COMMAND = 0x58;
    public static int START_GAME_COMMAND = 0x59;

    public Server(Context context, int port) {
        this.context = context;
        this.serverCallbacks = (ServerCallbacks)context;
        this.port = port;
        subServers = new ArrayList<SubServer>();
    }

    public void startAccepting() {
        if(!accepting)
            new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            int id = 1;
            accepting = true;
            while(accepting) {
                Socket socket = serverSocket.accept();
                if(subServers.size() < MAX_CLIENTS) {
                    SubServer subServer = new SubServer(this, socket, id);
                    subServer.start();
                    subServers.add(subServer);
                    if(serverCallbacks != null)
                        serverCallbacks.onClientConnected(id);
                    id++;
                } else {
                    socket.close();
                }
            }
        } catch(UnknownHostException u) { } catch(IOException e) { }
        serverSocket = null;
        accepting = false;
    }

    @Override
    public void onSubServerMessageReceived(int id, int argc, int[] args) {
        if(serverCallbacks != null)
            serverCallbacks.onClientMessageReceived(id, argc, args);
    }

    @Override
    public void onSubServerDisconnect(int id) {
        synchronized(subServers) {
            for(SubServer subServer : subServers) {
                if(subServer.id == id) {
                    subServers.remove(subServer);
                    break;
                }
            }
        }
        if(serverCallbacks != null)
            serverCallbacks.onClientDisconnected(id);
    }

    public void stopAccepting() {
        accepting = false;
        if(serverSocket != null) {
            try {
                serverSocket.close();
            } catch(IOException e) { }
        }
    }

    public void sendToOne(int id, int argc, int[] args) {
        for(SubServer subServer : subServers) {
            if(subServer.id == id) {
                subServer.send(argc, args);
                break;
            }
        }
    }

    public void sendToOne(int id, int arg1, int arg2) {
        int[] args = {arg1, arg2};
        sendToOne(id, 2, args);
    }

    public void sendToAll(int argc, int[] args) {
        for(SubServer subServer : subServers)
            subServer.send(argc, args);
    }

    public void sendToAll(int arg1, int arg2) {
        int[] args = {arg1, arg2};
        sendToAll(2, args);
    }

    public void sendToAll(int arg1, int arg2, int arg3) {
        int[] args = {arg1, arg2, arg3};
        sendToAll(3, args);
    }

    public void destroy() {
        stopAccepting();
        synchronized(subServers) {
            for(SubServer s : subServers)
                s.destroy();
        }
    }

    public void setCallbacks(ServerCallbacks sc) {
        serverCallbacks = sc;
    }

    public interface ServerCallbacks {
        public void onClientMessageReceived(int id, int argc, int[] args);
        public void onClientConnected(int id);
        public void onClientDisconnected(int id);
    }
}
