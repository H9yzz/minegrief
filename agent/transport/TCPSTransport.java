package com.chebuya.minegriefagent.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TCPSTransport implements ClientTransport {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final String KEY = "TO_LAZY_TO_WRITE_DIFFIEHELLMAN";

    public TCPSTransport() {
    }

    private byte[] transform(byte[] inputBytes) {
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[inputBytes.length];

        for (int i = 0; i < inputBytes.length; i++) {
            result[i] = (byte) (inputBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        return result;
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
        if (jsonData == null) {
            return null;
        }

        return Base64.getEncoder().encodeToString(transform(jsonData.getBytes(Charset.forName("UTF-8"))));
    }

    @Override
    public String untransformData(String encryptedData) throws Exception {
        if (encryptedData == null) {
            return null;
        }
        return new String (transform(Base64.getDecoder().decode(encryptedData)));
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
