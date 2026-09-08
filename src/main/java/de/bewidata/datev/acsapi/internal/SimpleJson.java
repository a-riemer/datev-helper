package de.bewidata.datev.acsapi.internal;

/**
 * Minimal JSON value extraction — avoids pulling in a JSON library.
 * Only handles flat JSON with string and integer fields.
 * Package-private — internal use only.
 */
public final class SimpleJson {

    private SimpleJson() {}

    /**
     * Returns the string value for {@code key}, or {@code null} if the key is absent.
     * Handles basic backslash escaping within the value.
     */
    public static String getString(String json, String key) {
        int ki = json.indexOf("\"" + key + "\"");
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + key.length() + 2);
        if (colon < 0) return null;
        int pos = colon + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        if (pos >= json.length() || json.charAt(pos) != '"') return null;
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos++);
            if (c == '\\' && pos < json.length()) {
                sb.append(json.charAt(pos++));
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the integer value for {@code key}.
     * Throws {@link IllegalArgumentException} if the key is absent or the value is not numeric.
     */
    public static int getInt(String json, String key) {
        int ki = json.indexOf("\"" + key + "\"");
        if (ki < 0) throw new IllegalArgumentException("Key '" + key + "' not found in: " + json);
        int colon = json.indexOf(':', ki + key.length() + 2);
        if (colon < 0) throw new IllegalArgumentException("Key '" + key + "' has no value in: " + json);
        int pos = colon + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        int end = pos;
        if (end < json.length() && json.charAt(end) == '-') end++;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == pos) throw new IllegalArgumentException("Key '" + key + "' is not an integer in: " + json);
        return Integer.parseInt(json.substring(pos, end));
    }
}
