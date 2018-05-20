package com.rc.PDFCitationAnalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Handles the user's preferences.
 */
class UserPreferences {
    private static String excelConfiguration = "";
    private static String excelLocation = "";
    private static Preferences userPrefs;
    private static GUILabelManagement guiLabelManagement;

    /**
     * Reads the stored preferences that the user has, if any.
     */
    static void readPreferences(GUILabelManagement guiLabelManagement) {
        UserPreferences.guiLabelManagement = guiLabelManagement;
        userPrefs = Preferences.userNodeForPackage(UserPreferences.class);
        try {
            String[] keys = userPrefs.keys();
            //Verify that the user has any preferences, if so, retrieve them
            if (keys != null && keys.length != 0) {
                //Retrieve user preferences
                excelConfiguration = userPrefs.get("excelConfig", "");
                excelLocation = userPrefs.get("excelLocation", "");
                System.out.println(excelConfiguration);
                System.out.println(excelLocation);
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stores the configuration that the excel file will have (where each relevant column is located)
     *
     * @param config  String with the configuration (each column separated by a comma)
     * @param storeIt True if the user wants to store this value
     */
    static void storeExcelConfiguration(String config, boolean storeIt) {
        excelConfiguration = config;
        if (storeIt) {
            userPrefs.put("excelConfig", config);
        }

    }

    /**
     * Stores the location of the excel file that contains the multiple pairs of twins
     *
     * @param file    Location of the file
     * @param storeIt True if the user wants to store this value
     */
    static void storeExcelFile(File file, boolean storeIt) {
        excelLocation = file.getPath();
        if (storeIt) {
            userPrefs.put("excelLocation", file.getPath());
        }

    }

    /**
     * Removes an stored excel file if there was an error reading the file
     */
    static void removeExcelFile() {
        excelLocation = "";
        userPrefs.remove("excelLocation");
    }

    /**
     * Returns the excel configuration as an array list
     * index 0 = pairIdColumn
     * index 1 = titleTwin1Column
     * index 2 = titleTwin2Column
     * index 3 = titleCitingColumn
     * index 4 = authorTwin1Column
     * index 5 = authorTwin2Column
     * index 6 =  yearTwin1Column
     * index 7 = yearTwin2Column
     *
     * @return ArrayList with the configuration
     */
    static ArrayList<Integer> getExcelConfiguration() {
        ArrayList<Integer> configuration = new ArrayList<>();
        if (!excelConfiguration.isEmpty()) {
            String[] config = excelConfiguration.split(",");
            //Convert each string to int
            for (String col : config) {
                configuration.add(Integer.parseInt(col));
            }
        }
        return configuration;
    }

    /**
     * Retrieves the location of the excel file.
     *
     * @return Location of the excel file, or null if it there is no such file or user has not stored any file
     */
    static String getExcelLocation() {
        if (excelLocation.isEmpty()) return "";
        //Verify that the file still exists (user could have deleted it)
        File excelFile = new File(excelLocation);
        if (!excelFile.exists() || !excelFile.canRead()) {
            guiLabelManagement.setAlertPopUp("Unable to read your stored Excel file, please upload it again.");
            return "";
        }
        return excelFile.getPath();
    }


}
