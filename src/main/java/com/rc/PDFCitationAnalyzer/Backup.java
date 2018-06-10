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
            guiLabelManagement.disableSaveProgressButton(true);
            File file = new File("./Backups");
            if (!file.exists()) {
                file.mkdir();
            }
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("" +
                        "./Backups/" + backupName + "_temp.bin"));

                objectOutputStream.writeObject(currentSetup);
                objectOutputStream.flush();
                objectOutputStream.reset();
                objectOutputStream.close();
                File newBackup = new File("./Backups/" + backupName + "_temp.bin");
                replaceOldBackup(backupName, newBackup);
                guiLabelManagement.setStatus("Backup completed!");
                guiLabelManagement.disableSaveProgressButton(false);
            } catch (IOException e) {
                e.printStackTrace();
                guiLabelManagement.setAlertPopUp(e.getMessage());
            }
        }


    /**
     * Replaces the old backup with the new pne
     */
    private static void replaceOldBackup(String backupName, File newBackup) {
        File oldBackup = new File("./Backups/" +backupName + ".bin");
        if (oldBackup.exists()) {
            //Delete the old one, if there is one
            oldBackup.delete();
        }
        //Rename the new one
        newBackup.renameTo(oldBackup);

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
            MultipleFilesSetup multipleFileSetup = (MultipleFilesSetup) objectInputStream.readObject();
            objectInputStream.close();
            return multipleFileSetup;
        } catch (IOException | ClassNotFoundException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }
        return null;

    }
}
