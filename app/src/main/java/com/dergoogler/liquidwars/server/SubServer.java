

package com.dergoogler.liquidwars.server;

import java.lang.Thread;
import java.lang.InterruptedException;
import java.net.Socket;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class SubServer extends Thread {
    private SubServerCallbacks subServerCallbacks;
    private Socket socket;
    public int id;
    private boolean sendLocked = true;

    public SubServer(SubServerCallbacks subServerCallbacks, Socket socket, int id) {
        this.subServerCallbacks = subServerCallbacks;
        this.socket = socket;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            socket.getOutputStream().write(id);
            sendLocked = false;
            byte[] buffer = new byte[512];
            int[] intBuffer = new int[128];
            IntBuffer inIB = ByteBuffer.wrap(buffer).asIntBuffer();
            InputStream is = socket.getInputStream();
            while(true) {
                int count = is.read();
                if(count == -1)
                    break;
                is.read(buffer, 0, count);
                inIB.get(intBuffer);
                inIB.rewind();
                subServerCallbacks.onSubServerMessageReceived(id, count, intBuffer);
            }
        } catch(IOException e) { }
        subServerCallbacks.onSubServerDisconnect(id);
    }

    public void destroy() {
        if(socket != null) {
            try {
                socket.close();
            } catch(IOException e) { }
            socket = null;
        }
    }

    public void send(int argc, int[] args) {
        while(sendLocked) {
            try { Thread.sleep(10); } catch (InterruptedException ie) { }
        }
        sendLocked = true;
        try {
            ByteBuffer bb = ByteBuffer.allocate(argc*4);
            IntBuffer ib = bb.asIntBuffer();
            ib.put(args, 0, argc);
            OutputStream os = socket.getOutputStream();
            os.write(argc*4);
            os.write(bb.array());
            os.flush();
        } catch(IOException e) { }
        sendLocked = false;
    }

    public interface SubServerCallbacks {
        public void onSubServerMessageReceived(int id, int argc, int[] args);
        public void onSubServerDisconnect(int id);
    }
}
