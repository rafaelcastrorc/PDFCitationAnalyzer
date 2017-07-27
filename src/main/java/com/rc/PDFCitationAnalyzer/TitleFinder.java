package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.text.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by rafaelcastro on 7/24/17.
 * Finds the title of a PDF and creates a report with the title found. Can be used for multiple PDFs/
 */
public class TitleFinder extends Task<Void> {
    private Controller controller;
    private File[] listOfPDFs;
    private final GUILabelManagement guiLabelManagement;
    private final ProgressIndicator progressIndicator;

    TitleFinder(Controller controller, File[] listOfPDFs, GUILabelManagement guiLabelManagement, ProgressIndicator
            progressIndicator) {
        this.controller = controller;
        this.listOfPDFs = listOfPDFs;
        this.guiLabelManagement = guiLabelManagement;
        this.progressIndicator = progressIndicator;
    }


    /**
     * Extracts all the titles and outputs an excel file with the titles. The file name is Titles.xlsx.
     */
    private void getTitles() {
        DocumentParser documentParser;
        //Extract all the titles
        ArrayList<String> titles = new ArrayList<>();
        int i = 0;
        for (File file : listOfPDFs) {
            if (file.getName().contains("pdf")) {
                try {
                    documentParser = new DocumentParser(file, false, true);
                    String possibleTitle = documentParser.getTitle();
                    titles.add(possibleTitle);
                    documentParser.close();

                } catch (IOException e2) {
                    controller.displayAlert("There was an error parsing the file");
                }
            }
            i++;
            guiLabelManagement.setProgressIndicator(i / (double) listOfPDFs.length);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
        //Output the titles into an excel file
        FileOutput fileOutput = new FileOutput();
        try {
            fileOutput.writeTitlesToFile(titles, "Titles.xlsx");
        } catch (IOException e) {
            controller.displayAlert(e.getMessage());
        }

        Platform.runLater(() -> controller.getOutputPanel().getChildren().clear());
        Text outputText = new Text("Titles.xlsx has been created!");
        outputText.setStyle("-fx-font-size: 24");
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().addAll(outputText));


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
        guiLabelManagement.getOutput().addListener((observable, oldValue, newValue) ->
                controller.updateProgressOutput(newValue));

        progressIndicator.setStyle("-fx-alignment: center;" +
                "-fx-progress-color: #990303");
        progressIndicator.setMinHeight(190);
        progressIndicator.setMinWidth(526);
        Text outputText = new Text("Analyzing the files...");
        outputText.setStyle("-fx-font-size: 16");
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().addAll(progressIndicator, outputText));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    protected Void call() throws Exception {
        controller.updateStatus("Analyzing...");

        initialize();
        getTitles();
        controller.updateStatus("Done analyzing");
        return null;
    }


}
