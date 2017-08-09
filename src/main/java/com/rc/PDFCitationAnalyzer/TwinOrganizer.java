package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rafaelcastro on 7/26/17.
 * Organizes the downloaded files by pairs of twins
 */
public class TwinOrganizer extends Task {

    private Controller controller;
    private GUILabelManagement guiLabelManagement;
    private File[] files;
    private HashMap<String, Integer> mapTwinNameToID;
    private HashMap<String, String> mapTwinNameToFolder;
    private boolean deleteFiles;

    TwinOrganizer(Controller controller, GUILabelManagement guiLabelManagement) {
        this.controller = controller;
        this.guiLabelManagement = guiLabelManagement;
    }

    TwinOrganizer() {
    }

    /**
     * Initializes the GUI
     */
    private void initialize() {
        //Set up GUI
        Platform.runLater(() -> controller.getOutputPanel().getChildren().clear());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        guiLabelManagement.setProgressIndicator(0);
        Text outputText = new Text("Organizing the files...");
        outputText.setStyle("-fx-font-size: 16");
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().addAll(controller.getProgressIndicator(),
                outputText));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Organizes folders that represent twin papers under the same folder based on their ID
     */
    void organizeTheFiles() {
        int i = 0;
        //Check if directory exists
        File directory = new File("./OrganizedFiles");
        if (!directory.exists()) {
            directory.mkdir();
        }
        File couldNotOrganizeDir = new File("./OrganizedFiles/CouldNotOrganize");
        if (!couldNotOrganizeDir.exists()) {
            couldNotOrganizeDir.mkdir();
        }
        System.out.println("Number of files to organize: " +mapTwinNameToFolder.size() );
        try {
            for (String twinName : mapTwinNameToFolder.keySet()) {
                String folderName = mapTwinNameToFolder.get(twinName);
                File file = files[0];
                //Get the source folder
                File src = new File(file.getParent() + "/" + folderName);
                File destination;

                if (mapTwinNameToID.get(twinName) == null) {
                    //If there is no mapping for this file
                    String path = "./OrganizedFiles/CouldNotOrganize/"+folderName;
                    destination = new File("./OrganizedFiles/CouldNotOrganize/"+path);
                } else {
                    //Put it in a folder with the same twin id
                    String path = "./OrganizedFiles/" + mapTwinNameToID.get(twinName)+"/"+folderName;
                    destination = new File(path);
                }
                try {
                    copyFolder(src, destination);
                } catch (Exception e) {
                    e.printStackTrace();
                    guiLabelManagement.setAlertPopUp(e.getMessage());
                }
                i++;
                guiLabelManagement.setProgressIndicator(i / ((double) mapTwinNameToFolder.size()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }


        //Update GUI
        Platform.runLater(() -> controller.getOutputPanel().getChildren().clear());
        Text outputText = new Text("All files have been organized!");
        outputText.setStyle("-fx-font-size: 24");
        outputText.setTextAlignment(TextAlignment.CENTER);
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().addAll(outputText));
    }

     void copyFolder(File src, File dest) throws IOException{

        if(src.isDirectory()){

            //if directory not exists, create it
            if(!dest.exists()){
                dest.mkdirs();
                System.out.println("Directory copied from "
                        + src + "  to " + dest);
            }

            //list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                //construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                //recursive copy
                copyFolder(srcFile,destFile);
            }

        }else{
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
                    src.delete();
                }
                System.out.println("File copied from " + src + " to " + dest);
            }
        }
    }


    @Override
    protected Object call() throws Exception {
        initialize();
        organizeTheFiles();
        return null;
    }

    /**
     * Sets the csv file containing the twin pairs
     *
     * @param csv CSV file
     */
    void setCSV(File csv) {
        mapTwinNameToID = new HashMap<>();
        //Parse the CSV, get the Twin papers with their respective ID
        try {
            Scanner scanner = new Scanner(csv);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //Get the number id of the twin, which should be the first number of the string
                Pattern idPattern = Pattern.compile("\\d*");
                Matcher idMatcher = idPattern.matcher(line);
                if (!idMatcher.find()) {
                    guiLabelManagement.setAlertPopUp("CSV file is not formatted correctly. Could not find ID for one of the " +
                            "twins.");
                    return;
                }
                String holder = idMatcher.group();
                if (holder.equals("")) continue;
                int id = Integer.valueOf(holder);
                //Get the name of the twin, which should be the last sequence of characters between " "
                String nameOfTwin = null;
                Pattern twinNamePattern = Pattern.compile("\"[^\"]*\"");
                Matcher twinNameMatcher = twinNamePattern.matcher(line);
                while (twinNameMatcher.find()) {
                    nameOfTwin = twinNameMatcher.group();
                }
                if (nameOfTwin == null) {
                    guiLabelManagement.setAlertPopUp("CSV file is not formatted correctly. Could not find ID for one of the " +
                            "twins.");
                    return;
                }
                nameOfTwin = nameOfTwin.replaceAll("\"", "");

                mapTwinNameToID.putIfAbsent(nameOfTwin, id);
            }
        } catch (FileNotFoundException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }
    }


    /**
     * Set the Report.txt and parse it
     *
     * @param report Report.txt
     */
    void setReport(File report) {
        mapTwinNameToFolder = new HashMap<>();
        try {
            //Parse the entire report and map file name to folder name
            Scanner scanner = new Scanner(new FileInputStream(report));
            boolean isDownloaded = false;
            boolean isValid = false;
            String twinName = null;
            while (scanner.hasNextLine()) {

                String line = scanner.nextLine();
                //If the paper is downloaded, then get the name
                if (line.contains("Paper downloaded(searchForCitedBy)")) {
                    isDownloaded = true;
                    twinName = line;
                    twinName = twinName.replaceAll("-Paper downloaded\\(searchForCitedBy\\): ", "");
                    twinName = twinName.replaceAll("\\(Selected in SW.*", "");
                    twinName = twinName.replaceAll("\"", "");

                    while (twinName.endsWith(" ")) {
                        twinName = twinName.substring(0, twinName.lastIndexOf(" "));
                    }
                    //If it downloaded more than 0 pdfs, then is valid
                } else if (!line.contains("Number of PDFs downloaded: 0/") && !isValid && isDownloaded) {
                    //Has at least 1 pdf
                    isValid = true;
                } else {
                    //If its both valid and downloaded, then add it to the map
                    if (isValid && isDownloaded) {
                        String folder = line;
                        folder = folder.replaceAll(".*Folder path: ", "");
                        folder = folder.replaceAll("\"", "");
                        mapTwinNameToFolder.put(twinName, folder);
                        isValid = false;
                        isDownloaded = false;
                    } else {
                        isValid = false;
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
     * @param deleteFiles boolean
     */
    void setDeleteFiles(boolean deleteFiles) {
        this.deleteFiles = deleteFiles;
    }
}
