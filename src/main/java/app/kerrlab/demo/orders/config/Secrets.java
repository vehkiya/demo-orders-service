package app.kerrlab.demo.orders.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Helpers for showing that a secret is present (and which one) without revealing it.
 */
public final class Secrets {

    private static final Pattern SENSITIVE = Pattern.compile("(?i).*(key|secret|password|token|credential).*");

    private Secrets() {
    }

    public static boolean isSensitive(String propertyName) {
        return SENSITIVE.matcher(propertyName).matches();
    }

    /** Returns {@code sha256:<first 8 hex chars>}, enough to tell two secrets apart. */
    public static String fingerprint(String value) {
        if (value == null || value.isBlank()) {
            return "<not set>";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest).substring(0, 8);
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
