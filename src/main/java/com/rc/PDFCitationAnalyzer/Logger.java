package com.rc.PDFCitationAnalyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Logs all the information into log.txt
 */
class Logger {
    private static BufferedWriter logWriter;
    private static Logger instance;
    private static BufferedWriter outputWriter;

    private Logger() {
        try {
            logWriter = new BufferedWriter(new FileWriter("log.txt"));
        } catch (IOException e) {
            System.err.println("Unable to create log file");
            e.printStackTrace();
        }
    }

    static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    /**
     * Writes to the log file
     *
     * @param s string that will be written to the log file
     */
    void writeToLogFile(String s) {
        try {
            logWriter.write(s);
            logWriter.flush();
        } catch (IOException e) {
            System.err.println("Cannot write to file");
            e.printStackTrace();
        }
    }

    /**
     * Inserts a new line in the log file.
     */
    void newLine() {
        try {
            logWriter.newLine();
            logWriter.flush();
        } catch (IOException e) {
            System.err.println("Cannot write to file");
            e.printStackTrace();
        }
    }

    /**
     * Closes the logger to avoid leaks.
     */
    void closeLogger() {
        try {
            logWriter.flush();
            logWriter.close();
            if (outputWriter != null) {
                outputWriter.flush();
                outputWriter.close();
            }

        } catch (IOException ignored) {

        }
    }

}
