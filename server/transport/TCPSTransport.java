package com.chebuya.minegriefserver.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TCPSTransport implements ServerTransport {
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
        return Base64.getEncoder().encodeToString(transform(jsonData.getBytes(Charset.forName("UTF-8"))));
    }

    @Override
    public String untransformData(String encryptedData) throws Exception {
        return new String (transform(Base64.getDecoder().decode(encryptedData)));
    }
}
