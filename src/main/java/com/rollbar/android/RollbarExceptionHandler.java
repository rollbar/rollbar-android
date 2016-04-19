package com.rollbar.android;

import java.lang.Thread.UncaughtExceptionHandler;

public class RollbarExceptionHandler implements UncaughtExceptionHandler {
    private final UncaughtExceptionHandler existingHandler;
    private final Notifier notifier;

    private RollbarExceptionHandler(UncaughtExceptionHandler existingHandler, Notifier notifier) {
        this.existingHandler = existingHandler;
        this.notifier = notifier;
    }

    public static void register(Notifier notifier) {
        UncaughtExceptionHandler existingHandler = Thread.getDefaultUncaughtExceptionHandler();

        if (!(existingHandler instanceof RollbarExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new RollbarExceptionHandler(existingHandler, notifier));
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        notifier.uncaughtException(e);
        if (existingHandler != null) {
            existingHandler.uncaughtException(t, e);
        }
    }

}
