package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by rafaelcastro on 7/25/17.
 * Compares two directories that contain PDF files by extracting the titles of each file.
 * Outputs a Comparison.xlsx file.
 */
class PDFComparator extends Task {
    private final Controller controller;
    private final GUILabelManagement guiLabelManagement;
    private File[] directory1;
    private File[] directory2;

    PDFComparator(Controller controller, GUILabelManagement guiLabelManagement) {
        this.controller = controller;
        this.guiLabelManagement = guiLabelManagement;
    }

    /**
     * Stores a directory to use for comparison.
     *
     * @param listOfFiles Files that are part of the directory
     */
    void setDirectory(File[] listOfFiles) {
        if (directory1 == null) {
            directory1 = listOfFiles;
            controller.updateStatus("Directory 1 has been setup");
        } else {
            directory2 = listOfFiles;
            controller.updateStatus("Directory 2 has been setup");

        }
    }

    /**
     * Checks of both directories have been set.
     *
     * @return true if both directories have been set, false otherwise.
     */
    boolean isReady() {
        return directory1 != null && directory2 != null;
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

        guiLabelManagement.getOutput().addListener((observable, oldValue, newValue) ->
                controller.updateProgressOutput(newValue));

        guiLabelManagement.setProgressIndicator(0);

        Text outputText = new Text("Comparing the files...");
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
     * Compares two directories of PDF files based on their titles.
     */
    private void compare() {
        ArrayList<Object> header = new ArrayList<>();
        //Headers of the excel output file
        header.add("Title");
        header.add("Path of the 1st File");
        header.add("Path of the 2nd File");


        TreeMap<String, String> map = new TreeMap<>();
        TreeMap<Integer, ArrayList<Object>> duplicates = new TreeMap<>();
        duplicates.put(0, header);
        DocumentParser documentParser;
        int i = 0;
        for (File file : directory1) {
            if (file.getName().contains("pdf")) {
                try {
                    documentParser = new DocumentParser(file, false, true);
                    String possibleTitle = documentParser.getTitle();
                    map.put(possibleTitle, file.getPath());
                    documentParser.close();

                } catch (IOException e2) {
                    controller.displayAlert("Unable to parse this file: " + file.getName());
                }
            }
            i++;
            guiLabelManagement.setProgressIndicator(i / ((double) directory1.length + directory2.length));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

        for (File file : directory2) {
            if (file.getName().contains("pdf")) {
                try {
                    documentParser = new DocumentParser(file, false, true);
                    String possibleTitle = documentParser.getTitle();
                    if (map.keySet().contains(possibleTitle) && !possibleTitle.equals("No title found")) {
                        ArrayList<Object> list = new ArrayList<>();
                        list.add(possibleTitle);
                        list.add(map.get(possibleTitle));
                        list.add(file.getPath());
                        duplicates.put(i, list);
                    }
                    documentParser.close();

                } catch (IOException e2) {
                    controller.displayAlert("Unable to parse this file: " + file.getName());
                }
            }
            i++;
            guiLabelManagement.setProgressIndicator(i / ((double) directory1.length + directory2.length));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        FileOutput fileOutput = new FileOutput();
        String fileName = "";
        try {
            //Get parent folder name
            File parentDir1 =directory1[0].getParentFile().getParentFile();
            File parentDir2 =directory2[0].getParentFile().getParentFile();
            if (parentDir1.getName().equals(parentDir2.getName())) {
                fileName =  "Comparison_"+ parentDir1.getName()+".xlsx";
            }
            else {
                fileName =  "Comparison_"+ parentDir1.getName()+"&"+parentDir2.getName()+".xlsx";
            }
            //Output the titles into an excel file
            fileOutput.writeOutputToFile(duplicates, fileName);
        } catch (Exception e) {
            controller.displayAlert(e.getMessage());
        }

        //Once the program is done update GUI
        Platform.runLater(() -> controller.getOutputPanel().getChildren().clear());
        Text outputText = new Text("Possible duplicates: " + (duplicates.size() - 1) + "\n"+fileName+" has been " +
                "created!");
        outputText.setStyle("-fx-font-size: 24");
        outputText.setWrappingWidth(400);
        outputText.setTextAlignment(TextAlignment.CENTER);
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().addAll(outputText));

    }

    @Override
    protected Object call() throws Exception {
        controller.updateStatus("Comparing the files..");
        initialize();
        compare();
        controller.updateStatus("Done");
        directory1 = null;
        directory2 = null;

        return null;
    }


}
