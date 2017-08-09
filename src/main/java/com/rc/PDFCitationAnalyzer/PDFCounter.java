package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.File;

/**
 * Created by rafaelcastro on 8/1/17.
 * Counts the number of PDF files in a directory
 */
class PDFCounter extends Task {


    private final Controller controller;
    private final GUILabelManagement guiLabelManagement;
    private File[] directory;
    private int counter = 0;
    private boolean isUniqueCount;
    private Text outputText;
    private String directoryName;

    PDFCounter(Controller controller, GUILabelManagement guiLabelManagement) {
        this.controller = controller;
        this.guiLabelManagement = guiLabelManagement;
    }

    /**
     * Stores a directory.
     *
     * @param listOfFiles Files that are part of the directory
     */
    void setDirectory(File[] listOfFiles) {
        if (listOfFiles.length < 1) {
            guiLabelManagement.setAlertPopUp("This directory is empty!");
            return;
        }
        if (directory == null) {
            directory = listOfFiles;
            controller.updateStatus("The directory has been setup");
            directoryName = listOfFiles[0].getParentFile().getPath();
        }

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

        //Update GUI
        Platform.runLater(() -> controller.getOutputPanel().getChildren().clear());
        this.outputText = new Text("Total number of PDFs: " + counter);
        outputText.setStyle("-fx-font-size: 22");
        outputText.setWrappingWidth(400);
        outputText.setTextAlignment(TextAlignment.CENTER);
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().addAll(outputText));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Counts all PDFs in a given directory. Uses recursion.
     */
    private void countAllPDFs(File[] files) {
        try {
            for (File file : files) {
                if (file.isDirectory()) {
                    countAllPDFs(file.listFiles());
                } else {
                    if (file.getName().contains(".pdf")) {
                        counter++;
                        if (isUniqueCount) {
                            break;
                        }
                    }

                    if (!isUniqueCount) {
                        outputText.setText("Total number of PDFs: " + counter);
                    } else{
                        outputText.setText("Total number of folders containing PDFs: " + counter);
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }catch (StackOverflowError e){
            guiLabelManagement.setAlertPopUp("Unable to count the number of PDFs in this directory");
        }
    }


    @Override
    protected Object call() throws Exception {
        controller.updateStatus("Counting the PDFs...");
        initialize();
        countAllPDFs(directory);
        if (!isUniqueCount) {
            outputText.setText("Total number of PDFs: " + counter+"\nPath: "+directoryName);
        } else{
            outputText.setText("Total number of folders containing PDFs: " + counter+"\nPath: "+directoryName);
        }
        Thread.sleep(1000);
        controller.updateStatus("Done");
        directory = null;

        return null;
    }


    void setIsUniqueCount(boolean isUniqueCount) {
        this.isUniqueCount = isUniqueCount;
    }

}
