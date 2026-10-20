package ru.prokin.kitchen.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GravatarUtil {

    private static final String DEFAULT_GRAVATAR_BASE = "https://www.gravatar.com/avatar/";

    private GravatarUtil() {}

    /**
     * Generates a Gravatar URL for the given email with the specified pixel size.
     * Defaults to identicon fallback if user has no avatar registered on Gravatar.
     */
    public static String getGravatarUrl(String email, int size) {
        if (email == null) {
            email = "";
        }
        String clean = email.trim().toLowerCase();
        String hash = md5Hex(clean);
        return DEFAULT_GRAVATAR_BASE + hash + "?d=identicon&s=" + size;
    }

    public static String getGravatarUrl(String email) {
        return getGravatarUrl(email, 160);
    }

    private static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "00000000000000000000000000000000";
        }
    }
}
