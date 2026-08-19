package com.kira.kdownloader.settings;

public final class ProxyValidator {
    private ProxyValidator() {}

    public interface Result {
        Valid Valid = new Valid();

        final class Valid implements Result {
            private Valid() {}
        }

        final class Invalid implements Result {
            private final Field field;
            private final String reason;

            public Invalid(Field field, String reason) {
                this.field = field;
                this.reason = reason;
            }

            public Field getField() { return field; }
            public String getReason() { return reason; }
        }
    }

    private static final Result VALID = Result.Valid;

    public enum Field { HOST, PORT, USERNAME, PASSWORD }

    public static Result validate(ProxyType type, String host, int port, String username, String password) {
        if (type == ProxyType.DISABLED) return VALID;
        String trimmedHost = host.trim();
        if (trimmedHost.isEmpty()) return new Result.Invalid(Field.HOST, "Host is required");
        if (!isValidHost(trimmedHost)) return new Result.Invalid(Field.HOST, "Enter a valid host name or IP address");
        if (!isValidPort(port)) return new Result.Invalid(Field.PORT, "Port must be between 1 and 65535");
        if (!password.isEmpty() && username.trim().isEmpty()) {
            return new Result.Invalid(Field.USERNAME, "Username is required when a password is set");
        }
        return VALID;
    }

    public static boolean isValidHost(String host) {
        String value = host.trim();
        if (value.isEmpty() || value.length() > 253) return false;
        if (value.startsWith("[") && value.endsWith("]")) return isIpv6(value.substring(1, value.length() - 1));
        return isIpv4(value) || isHostname(value);
    }

    public static boolean isValidPort(int port) { return port >= 1 && port <= 65535; }

    private static boolean isIpv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) return false;
            for (int i = 0; i < part.length(); i++) if (!Character.isDigit(part.charAt(i))) return false;
            try {
                if (Integer.parseInt(part) > 255) return false;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIpv6(String host) {
        if (host.isEmpty()) return false;
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (!Character.isDigit(c) && (c < 'a' || c > 'f') && (c < 'A' || c > 'F') && c != ':') return false;
        }
        return true;
    }

    private static boolean isHostname(String host) {
        if (host.endsWith(".")) return false;
        String[] labels = host.split("\\.", -1);
        String last = labels[labels.length - 1];
        if (!last.isEmpty()) {
            boolean numeric = true;
            for (int i = 0; i < last.length(); i++) numeric &= Character.isDigit(last.charAt(i));
            if (numeric) return false;
        }
        for (String label : labels) if (!isLabel(label)) return false;
        return true;
    }

    private static boolean isLabel(String label) {
        if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-")) return false;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (!((Character.isLetterOrDigit(c) && c < 128) || c == '-')) return false;
        }
        return true;
    }
}
