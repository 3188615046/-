package com.ncvt.kebiao.viewmodel;

public final class Event<T> {
    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    public T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }

    public T peekContent() {
        return content;
    }
}
