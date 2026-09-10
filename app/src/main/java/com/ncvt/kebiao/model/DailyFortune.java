package com.ncvt.kebiao.model;

public final class DailyFortune {
    public final String id;
    public final String level;
    public final String title;
    public final String poem;
    public final String summary;
    public final String goodFor;
    public final String luckyFocus;
    public final String reminder;

    public DailyFortune(String id, String level, String title, String poem, String summary,
                        String goodFor, String luckyFocus, String reminder) {
        this.id = id;
        this.level = level;
        this.title = title;
        this.poem = poem;
        this.summary = summary;
        this.goodFor = goodFor;
        this.luckyFocus = luckyFocus;
        this.reminder = reminder;
    }
}
