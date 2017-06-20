package com.rc.PDFCitationAnalyzer;

import java.util.concurrent.ThreadFactory;

/**
 * Created by rafaelcastro on 6/16/17.
 * Custom thread creation to be used by the different executor services.
 */
class MyThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    }
}