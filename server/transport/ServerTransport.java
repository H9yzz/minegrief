package com.chebuya.minegriefserver.transport;

import java.io.IOException;
import java.net.Socket;

public interface ServerTransport {
    String sendData(Socket clientSocket, String data) throws IOException;
    String receiveData(Socket clientSocket) throws IOException;
    String transformData(String jsonData) throws Exception;
    String untransformData(String encryptedData) throws Exception;
}
