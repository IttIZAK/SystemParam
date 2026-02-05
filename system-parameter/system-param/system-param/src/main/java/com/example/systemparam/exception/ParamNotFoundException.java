package com.example.systemparam.exception;

public class ParamNotFoundException extends RuntimeException {
    private final String key;

    public ParamNotFoundException(String key) {
        super("Parameter not found: " + key);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
