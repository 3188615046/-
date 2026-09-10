package com.ncvt.kebiao.viewmodel;

public abstract class ScrapeState {
    private ScrapeState() {}

    public static final class Loading extends ScrapeState {
        public static final Loading INSTANCE = new Loading();
        private Loading() {}
    }

    public static final class CaptchaRequired extends ScrapeState {
        public final byte[] captchaBytes;
        public CaptchaRequired(byte[] captchaBytes) { this.captchaBytes = captchaBytes.clone(); }
    }

    public static final class Success extends ScrapeState {
        public final String message;
        public Success(String message) { this.message = message; }
    }

    public static final class Error extends ScrapeState {
        public final String message;
        public Error(String message) { this.message = message; }
    }
}
