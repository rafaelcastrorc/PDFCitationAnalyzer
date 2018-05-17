package com.rc.PDFCitationAnalyzer;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Handles the user's preferences.
 */
class UserPreferences {
    private static String excelConfiguration = "";
    private static String excelLocation = "";
    private static Preferences userPrefs;

    /**
     * Reads the stored preferences that the user has, if any.
     */
    static void readPreferences() {
        userPrefs = Preferences.userNodeForPackage(UserPreferences.class);
        try {
            String[] keys = userPrefs.keys();
            //Verify that the user has any preferences, if so, retrieve them
            if (keys != null && keys.length != 0) {
                //Retrieve user preferences
                String excelConfig = userPrefs.get("excelConfig", "");
                String excelLocation = userPrefs.get("excelLocation", "");
                System.out.println(excelConfig);
                System.out.println(excelLocation);
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stores the configuration that the excel file will have (where each relevant column is located)
     * @param config String with the configuration
     */
    static void storeExcelConfiguration(String config) {
        userPrefs.put("excelConfig", config);

    }

    /**
     * Stores the location of the excel file that contains the multiple pairs of twins
     * @param file Location of the file
     */
    static void storeExcelFile(File file) {
        userPrefs.put("excelLocation", file.getPath());

    }

    static String getExcelConfiguration() {
        return excelConfiguration;
    }

    /**
     * Retrieves the location of the excel file.
     * @return Location of the excel file, or null if it there is no such file or user has not stored any file
     */
    static String getExcelLocation() {
        if (excelConfiguration.isEmpty()) return null;
        //Verify that the file still exists (user could have deleted it)
        File excelFile = new File(excelConfiguration);
        if (!excelFile.exists() || !excelFile.canRead()) return  null;
        return excelLocation;
    }


}
