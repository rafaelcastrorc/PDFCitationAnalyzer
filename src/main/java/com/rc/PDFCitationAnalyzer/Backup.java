package com.rc.PDFCitationAnalyzer;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Generates and retrieves a backup of a multiple analysis
 */
class Backup {


    /**
     * Stores the current MultipleFileSetup obj
     */
    static void storeBackup(MultipleFilesSetup currentSetup, String backupName, GUILabelManagement guiLabelManagement) {
        Executors.newSingleThreadExecutor().execute(() -> {
            File file = new File("./Backups");
            if (!file.exists()) {
                file.mkdir();
            }
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("" +
                        "./Backups/" + backupName + ".bin"));

                objectOutputStream.writeObject(currentSetup);
                guiLabelManagement.setStatus("Backup completed!");
            } catch (IOException e) {
                e.printStackTrace();
                guiLabelManagement.setAlertPopUp(e.getMessage());
            }
        });
    }

    /**
     * Recovers a previously stored backup
     *
     * @return MultipleFileSetup obj
     */
    static MultipleFilesSetup recoverBackup(File backupFile, GUILabelManagement guiLabelManagement) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(backupFile));
            guiLabelManagement.setStatus("Recovering data, please wait...");
            return (MultipleFilesSetup) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }
        return null;

    }
}
