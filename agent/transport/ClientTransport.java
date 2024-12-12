package com.chebuya.minegriefagent.transport;

import java.io.IOException;

public interface ClientTransport {
    public void sendData(String encryptedData) throws IOException;

    public String receiveData() throws IOException;

    public String transformData(String jsonData) throws Exception;

    public String untransformData(String encryptedData) throws Exception;

    public void openConnection(String ip, int port) throws Exception;

    public void closeConnection() throws Exception;
}
