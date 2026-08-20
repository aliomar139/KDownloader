package com.kira.kdownloader.ui;

import com.kira.kdownloader.engine.MediaInfo;

public abstract class HomeUiState {
    private HomeUiState() {}

    public static final class Idle extends HomeUiState {
        public static final Idle INSTANCE = new Idle();
        private Idle() {}
    }

    public static final class Loading extends HomeUiState {
        public static final Loading INSTANCE = new Loading();
        private Loading() {}
    }

    public static final class Loaded extends HomeUiState {
        private final String sourceUrl;
        private final MediaInfo info;

        public Loaded(String sourceUrl, MediaInfo info) {
            this.sourceUrl = sourceUrl;
            this.info = info;
        }

        public String getSourceUrl() { return sourceUrl; }
        public MediaInfo getInfo() { return info; }
    }

    public static final class Error extends HomeUiState {
        private final String message;

        public Error(String message) { this.message = message; }

        public String getMessage() { return message; }
    }
}
