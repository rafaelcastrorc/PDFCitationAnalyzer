package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.util.Objects;

/**
 * Created by rafaelcastro on 8/1/17.
 * Counts the number of PDF files in a directory.
 * Has 3 modes:
 * 1. Count all the PDF files in a directory (including subdirectories)
 * 2. Count all the folders that contain at least 1 PDF file.
 * 3. Count all the successful downloads (can only be used on the DownloadedPDFs directory)
 */
class PDFCounter extends Task {
    private final GUILabelManagement guiLabelManagement;
    private File[] directory;
    private int counter = 0;
    private int counterOfSuccessful = 0;
    private boolean isUniqueCount;
    private Text outputText = new Text();
    private String directoryName;
    private boolean isSuccessful;

    PDFCounter(GUILabelManagement guiLabelManagement) {
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
            guiLabelManagement.setStatus("The directory has been setup");
            directoryName = listOfFiles[0].getParentFile().getPath();
        }
    }

    /**
     * Initializes the GUI
     */
    private void initialize() {
        //Set up GUI
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setProgressIndicator(0);
        //Update GUI
        guiLabelManagement.clearOutputPanel();
        Platform.runLater(() -> {
            this.outputText.setText("Total number of PDFs: " + counter);
            outputText.setStyle("-fx-font-size: 20");
            outputText.setWrappingWidth(400);
            outputText.setTextAlignment(TextAlignment.CENTER);
            guiLabelManagement.setNodeToAddToOutputPanel(outputText);
        });
    }

    /**
     * Counts all PDFs in a given directory. Uses recursion.
     */
    private void countAllPDFs(File[] files) {
        boolean containsPDF = false;
        boolean containsTXT = false;
        try {
            for (File file : files) {
                if (file.isDirectory()) {
                    countAllPDFs(Objects.requireNonNull(file.listFiles()));
                } else {
                    if (file.getName().contains(".pdf")) {
                        containsPDF = true;
                        counter++;
                        if (isUniqueCount) {
                            break;
                        }
                    } else {
                        //If it is processed, then there should be a txt file
                        if (file.getName().contains(".txt")) {
                            containsTXT = true;
                        }
                    }

                    if (containsPDF && containsTXT) {
                        counterOfSuccessful++;
                    }
                    Platform.runLater(() -> {
                        if (!isUniqueCount && !isSuccessful) {
                            outputText.setText("Total number of PDFs: " + counter);
                        } else if (isSuccessful) {
                            outputText.setText("Total number of successful downloads: " + counterOfSuccessful);
                        } else {
                            outputText.setText("Total number of folders containing PDFs: " + counter);
                        }
                    });


                }
            }
        } catch (StackOverflowError | NullPointerException e) {
            guiLabelManagement.setAlertPopUp("Unable to count the number of PDFs in this directory");
        }
    }


    @Override
    protected Object call() {
        guiLabelManagement.setStatus("Counting the PDFs...");
        initialize();
        countAllPDFs(directory);
        //Display a message depending on the mode selected
        Platform.runLater(() -> {
            outputText.setTextAlignment(TextAlignment.CENTER);
            if (!isUniqueCount && !isSuccessful) {
                outputText.setText("Total number of PDFs: " + counter + "\nPath: " + directoryName);
            } else if (!isUniqueCount) {
                outputText.setText("Total number of successful downloads: " + counterOfSuccessful + "\nPath: " +
                        directoryName);
            } else {
                outputText.setText("Total number of folders containing PDFs: " + counter + "\nPath: " + directoryName);
            }
        });
        guiLabelManagement.setStatus("Done");
        directory = null;

        return null;
    }

    /**
     * Set to true if you are counting the number of directories that contain a PDF
     */
    void setIsUniqueCount(boolean isUniqueCount) {
        this.isUniqueCount = isUniqueCount;
    }

    /**
     * Set to true if you are counting the number of directories that contain a PDF and a txt file, thus a successful
     * download.
     */
    void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }
}
