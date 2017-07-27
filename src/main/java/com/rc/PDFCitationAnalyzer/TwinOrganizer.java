package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rafaelcastro on 7/26/17.
 * Organizes the downloaded files by pairs of twins
 */
public class TwinOrganizer extends Task {

    private final Controller controller;
    private final ProgressIndicator progressIndicator;
    private final GUILabelManagement guiLabelManagement;
    private File[] files;
    private File mainFolder;
    private HashMap<String, Integer> mapTwinNameToID;
    private HashMap<String, String> mapTwinNameToFolder;

    TwinOrganizer(Controller controller, GUILabelManagement guiLabelManagement, ProgressIndicator
            progressIndicator) {
        this.controller = controller;
        this.guiLabelManagement = guiLabelManagement;
        this.progressIndicator = progressIndicator;
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
        guiLabelManagement.getProgressIndicator().addListener((observable, oldValue, newValue) ->
                controller.updateProgressIndicator(newValue.doubleValue()));
        progressIndicator.setStyle("-fx-alignment: center;" +
                "-fx-progress-color: #990303");
        progressIndicator.setMinHeight(190);
        progressIndicator.setMinWidth(526);
        Text outputText = new Text("Organizing the files...");
        outputText.setStyle("-fx-font-size: 16");
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().addAll(progressIndicator, outputText));
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
        if (! directory.exists()){
            directory.mkdir();
        }
        File couldNotOrganizeDir = new File("./OrganizedFiles/CouldNotOrganize");
        if (!couldNotOrganizeDir.exists()){
            couldNotOrganizeDir.mkdir();
        }
        for (String twinName : mapTwinNameToFolder.keySet()) {
            String folderName = mapTwinNameToFolder.get(twinName);
            File file = files[0];
            //Get the source folder
            File src = new File(file.getParent()+"/"+folderName);
            File destination;

            if (mapTwinNameToID.get(twinName) == null) {
                //If there is no mapping for this file
                destination = new File("./OrganizedFiles/CouldNotOrganize");
            }
            else {
                //Put it in a folder with the same twin id
                destination = new File("./OrganizedFiles/"+mapTwinNameToID.get(twinName)+"/"+folderName);

            }
            try {
                FileUtils.copyDirectory(src, destination);
            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
            guiLabelManagement.setProgressIndicator(i / ((double) mapTwinNameToFolder.size()));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }


        //Update GUI
        Platform.runLater(() -> controller.getOutputPanel().getChildren().clear());
        Text outputText = new Text("All files have been organized!");
        outputText.setStyle("-fx-font-size: 24");
        outputText.setTextAlignment(TextAlignment.CENTER);
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().addAll(outputText));


    }
    @Override
    protected Object call() throws Exception {
        initialize();
        organizeTheFiles();
        return null;
    }

    /**
     * Sets the csv file containing the twin pairs
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
                    controller.displayAlert("CSV file is not formatted correctly. Could not find ID for one of the " +
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
                    controller.displayAlert("CSV file is not formatted correctly. Could not find ID for one of the " +
                            "twins.");
                    return;
                }
                nameOfTwin = nameOfTwin.replaceAll("\"", "");

                mapTwinNameToID.putIfAbsent(nameOfTwin, id);
            }
        } catch (FileNotFoundException e) {
            controller.displayAlert(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Set the Report.txt and parse it
     * @param report Report.txt
     */
    void setReport(File report) {
        mapTwinNameToFolder  = new HashMap<>();
        try {
            //Parse the entire report and map file name to folder name
            Scanner scanner = new Scanner(new FileInputStream(report));
            boolean isDownloaded = false;
            boolean isValid = false;
            String twinName = null;
            while (scanner.hasNextLine()) {

                String line = scanner.nextLine();
                if (line.contains("Paper downloaded(searchForCitedBy)")){
                    isDownloaded = true;
                    twinName = line;
                    twinName = twinName.replaceAll("-Paper downloaded\\(searchForCitedBy\\): ", "");
                    twinName = twinName.replaceAll("\\(Selected in SW.*", "");
                    twinName = twinName.replaceAll("\"", "");

                    while (twinName.endsWith(" ")) {
                        twinName = twinName.substring(0, twinName.lastIndexOf(" "));
                    }
                }
                else if (!line.contains("Number of PDFs downloaded: 0/") && !isValid && isDownloaded) {
                    //Has at least 1 pdf
                    isValid = true;
                }
                else {
                    if (isValid && isDownloaded) {
                        String folder = line;
                        folder = folder.replaceAll(".*Folder path: ", "");
                        mapTwinNameToFolder.put(twinName, folder);
                        isValid = false;
                        isDownloaded = false;
                    }
                    else {
                        isValid = false;
                        isDownloaded = false;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            controller.displayAlert(e.getMessage());
        }
    }

    void setDownloadedPDFs(File[] files) {
        this.files = files;
    }
}
