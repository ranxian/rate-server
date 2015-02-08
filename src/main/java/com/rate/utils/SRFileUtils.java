package com.rate.utils;

import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

/**
 * Created by Ran Xian on 4/3/14.
 */
public class SRFileUtils {

    private static Logger logger = Logger.getLogger(SRFileUtils.class);

    public static Boolean sendFile(File file, Socket socket) throws IOException {
        logger.debug("begin send file");
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        FileInputStream fileInputStream = new FileInputStream(file);

        long totalLength = file.length();
        long haveRead = 0;

        int readSize = 1024 * 1024;
        byte[] buffer = new byte[readSize+10];


        while (haveRead < totalLength) {
            long to_read = Math.min(readSize, totalLength-haveRead);
            int real_read = fileInputStream.read(buffer, 0, (int)to_read);
            if (real_read < 0)
                throw new IOException("Should not be the eof");
            outputStream.write(buffer, 0, real_read);
            outputStream.flush();
            haveRead += real_read;
        }

        outputStream.flush();
        logger.debug("finish send file");
        fileInputStream.close();

        return true;
    }

    public static Boolean reiceiveFile(File dst, BufferedInputStream inputStream, int filesize) throws IOException {
        logger.debug("begin receive file of length: " + filesize);
        FileOutputStream outputStream = new FileOutputStream(dst);
        byte[] buffer = new byte[filesize];
        int received = 0;

        while (received != filesize) {
            int nread = inputStream.read(buffer, received, filesize-received);
            if (nread < 0) {
                throw new IOException("File not fully transformed");
            }
            received += nread;
            logger.debug(received + "bytes received");
        }

        outputStream.write(buffer, 0, filesize);

        outputStream.close();
        logger.debug("file received");
        return true;
    }

    public static Boolean downloadFiles(File dstDir, BufferedInputStream inputStream) throws IOException {
        String uuid = UUID.randomUUID().toString();
        File tempFile = new File(uuid + ".zip");

        int filesize = Integer.parseInt(StringUtils.readline(inputStream));

        reiceiveFile(tempFile, inputStream, filesize);

        File[] files = null;
        try {
            files = ZipUtils.unzip(tempFile, dstDir.getAbsolutePath(), null);
            FileUtils.forceDelete(tempFile);
        } catch (ZipException e) {
            System.out.println(e.getMessage());

            return false;
        }

        for (File file : files) {
            FileUtils.copyFileToDirectory(file, dstDir);
        }
        FileUtils.deleteDirectory(files[0].getParentFile());
        return true;
    }

    public static Boolean uploadFiles(File srcDir, Socket socket) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        // see if path is right
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            logger.fatal("uploadFiles: Wrong src directory path, " + srcDir.getAbsoluteFile());
            return false;
        }
        // compress it to a zip file
        logger.info("Compress to send");
        String zipFilePath = ZipUtils.zip(srcDir.getAbsolutePath(), RateConfig.getZipRootDir() + "/", null);
        if (zipFilePath == null) {
            logger.fatal("Can't create zip file");
            return false;
        } else {
            logger.debug("Zip created");
        }
        // send it over socket
        File zipFile = new File(zipFilePath);
        Boolean result = false;
        writer.println(zipFile.length());
        writer.flush();
        try {
            result = SRFileUtils.sendFile(zipFile, socket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtils.forceDelete(zipFile);
        }
        if (!result) {
            logger.debug("Can't send file");
        }
        return true;
    }
}
