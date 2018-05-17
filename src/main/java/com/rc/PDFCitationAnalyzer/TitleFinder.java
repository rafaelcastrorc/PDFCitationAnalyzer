package com.rc.PDFCitationAnalyzer;

import javafx.concurrent.Task;
import javafx.scene.text.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by rafaelcastro on 7/24/17.
 * Finds the title of a PDF and creates a report with the title(s) found.
 * Can be used to analyze multiple PDFs.
 */
public class TitleFinder extends Task<Void> {
    private File[] listOfPDFs;
    private final GUILabelManagement guiLabelManagement;

    TitleFinder(File[] listOfPDFs, GUILabelManagement guiLabelManagement) {
        this.listOfPDFs = listOfPDFs;
        this.guiLabelManagement = guiLabelManagement;
    }


    /**
     * Extracts all the titles and outputs an excel file with the titles.
     * The name of the output is Titles.xlsx.
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
                    guiLabelManagement.setAlertPopUp("There was an error parsing the file");
                }
            }
            i++;
            guiLabelManagement.setProgressIndicator(i / (double) listOfPDFs.length);


        }
        //Output the titles into an excel file
        FileOutput fileOutput = new FileOutput();
        try {
            fileOutput.writeTitlesToFile(titles, "Titles.xlsx");
        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }

        guiLabelManagement.clearOutputPanel();
        Text outputText = new Text("Titles.xlsx has been created!");
        outputText.setStyle("-fx-font-size: 24");
        //Add the progress indicator and outputText to the output panel
        guiLabelManagement.setNodeToAddToOutputPanel(outputText);


    }

    /**
     * Initializes the GUI
     */
    private void initialize() {
        //Set up GUI
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setProgressIndicator(0);
        Text outputText = new Text("Analyzing the files...");
        outputText.setStyle("-fx-font-size: 16");
        //Add the progress indicator and outputText to the output panel
        guiLabelManagement.setNodeToAddToOutputPanel(guiLabelManagement.getProgressIndicatorNode());
        guiLabelManagement.setNodeToAddToOutputPanel(outputText);

    }

    @Override
    protected Void call() {
        guiLabelManagement.setStatus("Analyzing...");
        initialize();
        getTitles();
        guiLabelManagement.setStatus("Done analyzing");
        return null;
    }


}
