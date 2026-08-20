package com.kira.kdownloader.settings;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SettingsCodec {
    private SettingsCodec() {}

    public static String encode(Map<String, String> values) {
        StringBuilder result = new StringBuilder("{\n");
        int index = 0;
        int safeSize = 0;
        for (String key : values.keySet()) if (!SettingsKeys.SENSITIVE_KEYS.contains(key)) safeSize++;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (SettingsKeys.SENSITIVE_KEYS.contains(entry.getKey())) continue;
            result.append("  ").append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
            if (++index < safeSize) result.append(',');
            result.append('\n');
        }
        return result.append('}').toString();
    }

    public static Map<String, String> decode(String json) {
        try {
            return new Parser(json).parseObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\"': result.append("\\\""); break;
                case '\\': result.append("\\\\"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\t': result.append("\\t"); break;
                default:
                    if (c < ' ') result.append(String.format("\\u%04x", (int) c));
                    else result.append(c);
            }
        }
        return result.append('\"').toString();
    }

    private static final class Parser {
        private final String source;
        private int index;

        Parser(String source) { this.source = source; }

        Map<String, String> parseObject() {
            skipWhitespace();
            if (peek() != '{') return null;
            index++;
            Map<String, String> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') { index++; return result; }
            while (true) {
                skipWhitespace();
                String key = parseString();
                if (key == null) return null;
                skipWhitespace();
                if (next() != ':') return null;
                skipWhitespace();
                String value = parseValue();
                if (value == null) return null;
                result.put(key, value);
                skipWhitespace();
                char separator = next();
                if (separator == '}') return result;
                if (separator != ',') return null;
            }
        }

        private String parseValue() {
            skipWhitespace();
            switch (peek()) {
                case '\"': return parseString();
                case 't': return literal("true", "true");
                case 'f': return literal("false", "false");
                case 'n': return literal("null", "");
                default: return parseNumber();
            }
        }

        private String literal(String token, String value) {
            if (index + token.length() > source.length() || !source.regionMatches(index, token, 0, token.length())) return null;
            index += token.length();
            return value;
        }

        private String parseNumber() {
            int start = index;
            while (index < source.length() && (Character.isDigit(source.charAt(index)) || "+-.eE".indexOf(source.charAt(index)) >= 0)) index++;
            return index == start ? null : source.substring(start, index);
        }

        private String parseString() {
            if (peek() != '\"') return null;
            index++;
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index++);
                if (c == '\"') return result.toString();
                if (c != '\\') { result.append(c); continue; }
                if (index >= source.length()) return null;
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '\"': result.append('\"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'u':
                        if (index + 4 > source.length()) return null;
                        result.append((char) Integer.parseInt(source.substring(index, index + 4), 16));
                        index += 4;
                        break;
                    default: result.append(escaped);
                }
            }
            return null;
        }

        private char peek() { return index < source.length() ? source.charAt(index) : '\u0000'; }
        private char next() { return index < source.length() ? source.charAt(index++) : '\u0000'; }
        private void skipWhitespace() { while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++; }
    }
}
