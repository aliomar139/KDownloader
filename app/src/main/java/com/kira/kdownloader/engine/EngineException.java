package com.kira.kdownloader.engine;

public final class EngineException extends Exception {
    public EngineException(String message) {
        super(message);
    }

    public EngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
