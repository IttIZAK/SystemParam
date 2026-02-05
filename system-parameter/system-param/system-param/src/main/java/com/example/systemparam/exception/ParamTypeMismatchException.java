package com.example.systemparam.exception;

public class ParamTypeMismatchException extends RuntimeException {

    private static final int DEFAULT_MAX_VALUE_LENGTH = 300;

    private final String key;
    private final String expected;
    private final String actualValue;
    private final int maxLength;

    public ParamTypeMismatchException(String key, String expected, String actualValue) {
        this(key, expected, actualValue, DEFAULT_MAX_VALUE_LENGTH);
    }

    public ParamTypeMismatchException(String key, String expected, String actualValue, int maxLength) {
        super(buildMessage(key, expected, actualValue, maxLength));
        this.key = key;
        this.expected = expected;
        this.actualValue = actualValue;
        this.maxLength = maxLength;
    }

    public String getKey() {
        return key;
    }

    public String getExpected() {
        return expected;
    }

    public String getActualValue() {
        return actualValue;
    }

    public int getMaxLength() {
        return maxLength;
    }

    private static String buildMessage(String key, String expected, String actualValue, int maxLength) {
        String safeActual = actualValue == null ? "null" : actualValue;
        if (maxLength > 0 && safeActual.length() > maxLength) {
            safeActual = safeActual.substring(0, maxLength) + "...";
        }
        return "Invalid value for '" + key + "': expected=" + expected + ", actual=" + safeActual;
    }
}
