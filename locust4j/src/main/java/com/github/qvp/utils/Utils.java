package com.github.qvp.utils;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author myzhan
 */
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static String md5(String... inputs) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            for (String input : inputs) {
                messageDigest.update(input.getBytes());
            }
            byte[] bytes = messageDigest.digest();
            StringBuilder sb = new StringBuilder(33);
            for (byte currentByte : bytes) {
                sb.append(Integer.toHexString((currentByte & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            logger.error("Cannot get the instance of the MD5 message digest", ex);
            return null;
        }
    }

    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    public static String getNodeID() {
        String hostname = getHostname();
        long timeInSecond = currentTimeInSeconds();
        int randomNumber = new Random().nextInt(1000);
        return String.format("%s_%s", hostname, md5(String.format("%d%d", timeInSecond, randomNumber)));
    }

    public static long round(long value, int places) {
        double round;
        double pow = Math.pow(10, places);
        double digit = pow * value;
        double div = digit % 1;
        if (div > 0.5f) {
            round = Math.ceil(digit);
        } else {
            round = Math.floor(digit);
        }
        double result = round / pow;
        return (long)result;
    }

    /**
     * Get the current timestamp in millis.
     *
     * @return current timestamp in millis
     */
    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Get the current timestamp in seconds.
     *
     * @return current timestamp in seconds
     */
    public static long currentTimeInSeconds() {
        return now() / 1000;
    }
}
