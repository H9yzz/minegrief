package com.chebuya.minegriefserver.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPTransport implements ServerTransport {

    public TCPTransport() {
    }

    @Override
    public String sendData(Socket clientSocket, String data) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println(data);
        return data;
    }

    @Override
    public String receiveData(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
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
}
