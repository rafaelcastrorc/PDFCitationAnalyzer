package com.rc.PDFCitationAnalyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

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

	public static Logger getInstance() {
		if (instance == null) {
			instance = new Logger();
		}
		return instance;
	}

	public void writeToLogFile(String s) {
		try {
			logWriter.write(s);
			logWriter.flush();
		} catch (IOException e) {
			System.err.println("Cannot write to file");
			e.printStackTrace();
		}
	}

	void newLine() {
		try {
			logWriter.newLine();
			logWriter.flush();
		} catch (IOException e) {
			System.err.println("Cannot write to file");
			e.printStackTrace();
		}
	}

	public void closeLogger() {
		try {
			logWriter.flush();
			logWriter.close();
			if (outputWriter != null) {
				outputWriter.flush();
				outputWriter.close();
			}

		} catch (IOException e) {

		}
	}

}
