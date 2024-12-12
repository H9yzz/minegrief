package com.chebuya.minegriefagent.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Filesystem {
    public static boolean pathExistsInZip(String zipFilePath, String searchPath) {
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().startsWith(searchPath)) {
                    return true;
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static String getFileExtension(File filePath) {
        String fileName = filePath.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1 || dotIndex == 0) ? "" : fileName.substring(dotIndex + 1);
    }
}
