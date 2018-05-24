package com.rc.PDFCitationAnalyzer;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by rafaelcastro on 7/26/17.
 * Organizes the downloaded files based on the twins that each paper cites
 */
public class TwinOrganizer extends Task {

    private GUILabelManagement guiLabelManagement;
    //Represents all the folders inside of Downloaded PDFs
    private File[] files;
    //Maps the title of the paper to the different pairIDs that it belongs to
    private HashMap<Object, ArrayList<Object>> paperTitleToCitedTwin;
    //Maps the title of the paper to the name of the folder where its located (inside of DownloadedPDFs)
    private HashMap<String, String> mapPaperTitleToFolder;
    private HashMap<String, String> mapFolderToPaperTitle;
    private HashMap<File, File> downloadedPDFstoReport;

    private boolean deleteFiles;
    //Represents a directory containing multiple subdirectories
    private File[] listOfFiles;

    private boolean isMultipleMode;

    TwinOrganizer(GUILabelManagement guiLabelManagement) {
        this.guiLabelManagement = guiLabelManagement;
        mapPaperTitleToFolder = new HashMap<>();
        mapFolderToPaperTitle = new HashMap<>();
        downloadedPDFstoReport = new HashMap<>();
        paperTitleToCitedTwin = TwinFileReader.getCitingPaperToTwinID();
    }

    TwinOrganizer() {
        mapPaperTitleToFolder = new HashMap<>();
        mapFolderToPaperTitle = new HashMap<>();
        downloadedPDFstoReport = new HashMap<>();
        paperTitleToCitedTwin = TwinFileReader.getCitingPaperToTwinID();
    }

    /**
     * Initializes the GUI
     */
    private void initialize() {
        //Check if there are any errors in the excel file
        if (TwinFileReader.getErrors().size() != 0) {
            //Notify the user that there are errors in his file
            guiLabelManagement.setAlertPopUp("Important: Your Excel/CSV file contains rows with errors that will be " +
                    "ignored.");
        }
        //Set up GUI
        guiLabelManagement.clearOutputPanel();
        Text outputText = new Text("Organizing the files...");
        outputText.setStyle("-fx-font-size: 16");
        //Add the progress indicator and outputText to the output panel
        guiLabelManagement.setProgressIndicator(0);
        guiLabelManagement.setNodeToAddToOutputPanel(guiLabelManagement.getProgressIndicatorNode());
        guiLabelManagement.setNodeToAddToOutputPanel(outputText);
        System.out.println("GUI SET");

    }

    /**
     * Puts paper that cite a given twin in the same folder
     */
    private void organizeTheFiles() {
        int i = 0;
        //Check if directory exists, if not create it
        File directory = new File("./OrganizedFiles");
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdir();
        }
        File couldNotOrganizeDir = new File("./OrganizedFiles/CouldNotOrganize");
        if (!couldNotOrganizeDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            couldNotOrganizeDir.mkdir();
        }
        System.out.println("Number of files to organize: " + mapPaperTitleToFolder.size());
        try {
            for (String paperTitle : mapPaperTitleToFolder.keySet()) {
                String folderName = mapPaperTitleToFolder.get(paperTitle);
                File file = files[0];
                //Get the source folder

                File srcFolder = new File(file.getParent() + "/" + folderName);
                //Get  all the non txt file (this are the downloaded versions for a given paper)
                ArrayList<File> srcFiles = new ArrayList<>();
                try {
                    for (File temp : srcFolder.listFiles()) {
                        if (!temp.getName().contains("ArticleName")) {
                            srcFiles.add(temp);
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
                File destination;

                if (paperTitleToCitedTwin.get(paperTitle) == null) {
                    //If there is no mapping for this file
                    int version = 0;
                    File destinationFolder = new File("./OrganizedFiles/CouldNotOrganize/" + folderName);
                    try {
                        for (File src : srcFiles) {
                            String path = destinationFolder.getPath() + '_' + version + ".pdf";
                            destination = new File(path);
                            Files.copy(src.toPath(), destination.toPath(), REPLACE_EXISTING);
                            version++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        guiLabelManagement.setAlertPopUp(e.getMessage());
                    }
                    i++;
                    guiLabelManagement.setProgressIndicator(i / ((double) mapPaperTitleToFolder.size()));
                } else {
                    for (Object twinIDObject : paperTitleToCitedTwin.get(paperTitle)) {
                        int twinID = (int) twinIDObject;
                        //Put it in a folder with the same twin id
                        int version = 0;
                        try {
                            File destinationFolder = new File("./OrganizedFiles/" + twinID + "/" + folderName);
                            //noinspection ResultOfMethodCallIgnored
                            new File("./OrganizedFiles/" + twinID).mkdirs();
                            for (File src : srcFiles) {
                                String path = destinationFolder.getPath() + '_' + version + ".pdf";
                                destination = new File(path);
                                Files.copy(src.toPath(), destination.toPath(), REPLACE_EXISTING);
                                version++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            guiLabelManagement.setAlertPopUp(e.getMessage());
                        }
                        i++;
                        guiLabelManagement.setProgressIndicator(i / ((double) mapPaperTitleToFolder.size()));
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }

    }


    /***
     * Stores the folder to title map into the user preferences
     */
    private void storeFolderToTitleMapping() {
        //Store the mapping from folder to title
        Gson gson = new Gson();
        String hashMapString = gson.toJson(mapFolderToPaperTitle);
        //Split the string as long as it is longer that the MAX MAX_VALUE_LENGTH
        int i = 0;
        if (hashMapString.length() > Preferences.MAX_VALUE_LENGTH) {
            int maxLength = Preferences.MAX_VALUE_LENGTH;
            Matcher m = Pattern.compile(".{1," + maxLength + "}").matcher(hashMapString);
            while (m.find()) {
                //Store the shorter substrings
                UserPreferences.storeFolderToTitle("folderNameToTitleName_" + i, m.group());
                i++;
            }
        } else {
            UserPreferences.storeFolderToTitle("folderNameToTitleName_0", hashMapString);

        }
    }


    /**
     * Copies a folder from one location to another
     */
    void copyFolder(File src, File dest) throws IOException {

        if (src.isDirectory()) {

            //if directory not exists, create it
            if (!dest.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dest.mkdirs();
                System.out.println("Directory copied from "
                        + src + "  to " + dest);
            }

            //list all the directory contents
            String files[] = src.list();

            if (files != null) {
                for (String file : files) {
                    //construct the src and dest file structure
                    File srcFile = new File(src, file);
                    File destFile = new File(dest, file);
                    //recursive copy
                    copyFolder(srcFile, destFile);
                }
            }

        } else {
            //if file, then copy it
            //Check first if dest already exist
            if (!dest.exists()) {
                InputStream in = new FileInputStream(src);
                //Use bytes stream to support all file types
                OutputStream out = new FileOutputStream(dest);

                byte[] buffer = new byte[1024];

                int length;
                //copy the file content in bytes
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();
                //If the user chose to delete the files, then we delete it after we are done copying it
                if (deleteFiles) {
                    //noinspection ResultOfMethodCallIgnored
                    src.delete();
                }
                System.out.println("File copied from " + src + " to " + dest);
            }
        }
    }


    @Override
    protected Object call() {
        initialize();
        try {
            if (isMultipleMode) {
                //Get the location of each DownloadedPDfs and its respective report file
                getAllDownloadedPDFsAndReports(new File(listOfFiles[0].getParent()).toPath());

                organizeMultiple();
            } else {
                organizeTheFiles();
            }
            //Store the result
            storeFolderToTitleMapping();
            //Update GUI
            guiLabelManagement.clearOutputPanel();
            Text outputText = new Text("All files have been organized!");
            outputText.setStyle("-fx-font-size: 24");
            outputText.setTextAlignment(TextAlignment.CENTER);
            //Add the progress indicator and outputText to the output panel
            guiLabelManagement.setNodeToAddToOutputPanel(outputText);
        } catch (Exception | Error error1) {
            guiLabelManagement.setAlertPopUp(error1.getMessage());
        }
        return null;
    }

    /**
     * Organizes multiple DownloadedPDFs
     */
    private void organizeMultiple() {
        //Iterate through each DownloadedPDFs folder
        for (File downloadedPDFs : downloadedPDFstoReport.keySet()) {
            File report = downloadedPDFstoReport.get(downloadedPDFs);

            //Set up the files
            files = downloadedPDFs.listFiles();
            setReport(report);

            organizeTheFiles();


        }
    }


    /**
     * Goes through all the subdirectories finding the location of the DownloadedPDFs folder and its respective
     * report file
     */
    private void getAllDownloadedPDFsAndReports(Path start) {
        final File[] downloadedPDFs = new File[1];
        final File[] report = new File[1];
        try (Stream<Path> walk = Files.walk(start, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
            walk.forEach((path) -> {

                if (path.getFileName().toString().equals("DownloadedPDFs")) {
                    downloadedPDFs[0] = path.toFile();
                }
                if (path.getFileName().toString().equals("Report.txt")) {
                    report[0] = path.toFile();
                    if (downloadedPDFstoReport.containsKey(downloadedPDFs[0])) {
                        guiLabelManagement.setAlertPopUp(downloadedPDFs[0].getPath() + " appears more than once");
                    }
                    downloadedPDFstoReport.put(downloadedPDFs[0], report[0]);
                    System.out.println(downloadedPDFs[0]);
                    System.out.println(report[0]);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    /**
     * Parses Report.txt
     *
     * @param report Report.txt
     */
    void setReport(File report) {

        try {
            //Parse the entire report and map file name to folder name
            Scanner scanner = new Scanner(new FileInputStream(report));
            boolean isDownloaded = false;
            boolean isValid = false;
            String paperTitle = null;
            while (scanner.hasNextLine()) {

                String line = scanner.nextLine();
                //If the paper is downloaded, then get the name
                if (line.contains("Paper downloaded")) {
                    isDownloaded = true;
                    paperTitle = line;
                    paperTitle = paperTitle.replaceAll("-Paper downloaded(\\(searchForCitedBy\\))?: ", "");
                    paperTitle = paperTitle.replaceAll("-Paper downloaded(\\(searchForTheArticle\\))?: ", "");
                    paperTitle = paperTitle.replaceAll("\\(Selected in SW.*", "");
                    paperTitle = paperTitle.replaceAll("\"", "");

                    while (paperTitle.endsWith(" ")) {
                        paperTitle = paperTitle.substring(0, paperTitle.lastIndexOf(" "));
                    }
                    //If it downloaded more than 0 pdfs, then is valid
                } else if (!line.contains("Number of PDFs downloaded: 0/") && !isValid && isDownloaded) {
                    //Has at least 1 pdf
                    isValid = true;
                } else {
                    //If its both valid and downloaded, then store its location so we can move the file later
                    if (isValid) {
                        String folder = line;
                        folder = folder.replaceAll(".*Folder path: ", "");
                        folder = folder.replaceAll("\"", "");
                        mapPaperTitleToFolder.put(paperTitle, folder);
                        mapFolderToPaperTitle.put(folder, paperTitle);
                        isValid = false;
                        isDownloaded = false;
                    } else {
                        isDownloaded = false;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }
    }

    void setDownloadedPDFs(File[] files) {
        this.files = files;
    }

    /**
     * True if the program should delete the original location of the files after organizing them, false otherwise
     *
     * @param deleteFiles boolean
     */
    void setDeleteFiles(boolean deleteFiles) {
        this.deleteFiles = deleteFiles;
    }

    void setDownloadedPDFsMultiple(File[] listOfFiles) {
        this.listOfFiles = listOfFiles;

    }

    void setMultipleMode(boolean multipleMode) {
        isMultipleMode = multipleMode;
    }


}


