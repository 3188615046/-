package com.ncvt.kebiao.data.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ScrapeResult {
    private ScrapeResult() {}

    public static final class LoginPageReady extends ScrapeResult {
        public final boolean requiresCaptcha;
        public LoginPageReady(boolean requiresCaptcha) {
            this.requiresCaptcha = requiresCaptcha;
        }
    }

    public static final class LoginSuccess extends ScrapeResult {
        public static final LoginSuccess INSTANCE = new LoginSuccess();
        private LoginSuccess() {}
    }

    public static final class LoginFailed extends ScrapeResult {
        public final String message;
        public LoginFailed(String message) { this.message = message; }
    }

    public static final class CoursesFetched extends ScrapeResult {
        public final List<CourseRaw> courses;
        public final ResolvedTimetableConfig resolvedConfig;

        public CoursesFetched(List<CourseRaw> courses) {
            this(courses, null);
        }

        public CoursesFetched(List<CourseRaw> courses, ResolvedTimetableConfig resolvedConfig) {
            this.courses = Collections.unmodifiableList(new ArrayList<>(courses));
            this.resolvedConfig = resolvedConfig;
        }
    }

    public static final class Error extends ScrapeResult {
        public final String message;
        public Error(String message) { this.message = message; }
    }
}
