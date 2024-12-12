package com.chebuya.minegriefagent.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPTransport implements ClientTransport {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public TCPTransport() {
    }

    @Override
    public void sendData(String encryptedData) throws IOException {
        out.println(encryptedData);
    }

    @Override
    public String receiveData() throws IOException {
        return in.readLine();
    }

    @Override
    public String transformData(String jsonData) throws Exception {
        return jsonData;
    }

    @Override
    public String untransformData(String encryptedData) throws Exception {
        return encryptedData;
    }

    @Override
    public void openConnection(String ip, int port) throws Exception {
        this.socket = new Socket(ip, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void closeConnection() throws Exception {
        socket.close();
    }

}
