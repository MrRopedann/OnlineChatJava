package ru.kirill.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;

public class TCPConnection {

    private final Socket socket;
    private final Thread rxTheread;
    private final TCPConectionListner eventListner;
    private final BufferedReader in;
    private final BufferedWriter out;

    public TCPConnection(TCPConectionListner eventListner, String ipAddr, int port) throws  IOException {
        this(eventListner, new Socket(ipAddr, port));
    }

    public  TCPConnection(TCPConectionListner eventListner, Socket socket) throws IOException {
        this.eventListner = eventListner;
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")));
        rxTheread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventListner.onConnectionReady(TCPConnection.this);
                    while ( !rxTheread.isInterrupted()) {
                        eventListner.onReceiveString(TCPConnection.this, in.readLine());
                    }
                } catch (IOException e) {
                    eventListner.onException(TCPConnection.this, e);
                } finally {
                    eventListner.onDisconnect(TCPConnection.this);
                }
            }
        });
        rxTheread.start();
    }

    public synchronized void sendString(String value) {
        try {
            out.write(value + "\r\n");
            out.flush();
        } catch (IOException e) {
            eventListner.onException(TCPConnection.this, e);
            disconected();
        }
    }

    public synchronized void disconected() {
        rxTheread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventListner.onException(TCPConnection.this, e);
        }
    }

    @Override
    public  String toString() {
        return  "TCPConnecton: " + socket.getInetAddress() + ": " + socket.getPort();
    }
}
