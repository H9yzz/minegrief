package com.chebuya.minegriefagent.util;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import static com.chebuya.minegriefagent.util.Logging.LOGGER;

public class Network {

    private static final int TIMEOUT = 5000;

    public static ArrayList<String> expandCIDR(String cidr) throws Exception {
        String[] parts = cidr.split("/");
        String ip = parts[0];
        int prefix;
        prefix = Integer.parseInt(parts[1]);

        String[] octets = ip.split("\\.");
        int ipInt = 0;
        for (String octet : octets) {
            ipInt = ipInt << 8 | Integer.parseInt(octet);
        }

        int mask = 0xffffffff << (32 - prefix);
        int network = ipInt & mask;
        int broadcast = network | (~mask);

        ArrayList<String> targets = new ArrayList<>();
        for (int i = network; i <= broadcast; i++) {
            byte[] bytes = new byte[4];
            bytes[0] = (byte) ((i >> 24) & 0xFF);
            bytes[1] = (byte) ((i >> 16) & 0xFF);
            bytes[2] = (byte) ((i >> 8) & 0xFF);
            bytes[3] = (byte) (i & 0xFF);

            InetAddress address = InetAddress.getByAddress(bytes);
            targets.add(address.getHostAddress());
        }

        return targets;
    }


    public static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean detectService(String urlString, String searchString) {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOGGER.warning("HTTP request failed with response code: " + responseCode);
                return false;
            }

            reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8")
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
                if (response.toString().contains(searchString)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            LOGGER.warning("error during service detection: " + e.getMessage());
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    LOGGER.warning("unable to close file: " + e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
