package com.rc.PDFCitationAnalyzer;

import javafx.concurrent.Task;
import javafx.scene.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

/**
 * Task class used when analyzing a pair of twins.
 * Only for Single Article or Twin Articles Mode!
 */
class SimpleTwinAnalysis extends Task<Void> {

    private TreeMap<Integer, ArrayList<Object>> dataGathered;
    private GUILabelManagement guiLabelManagement;
    private Text progressOutputText;
    private File[] comparisonFiles;
    private File twinFile1;
    private File twinFile2;

    SimpleTwinAnalysis(GUILabelManagement guiLabelManagement, Text progressOutputText, File[] comparisonFiles,
                       File twinFile1, File twinFile2) {
        this.guiLabelManagement = guiLabelManagement;
        this.progressOutputText = progressOutputText;
        this.comparisonFiles = comparisonFiles;
        this.twinFile1 = twinFile1;
        this.twinFile2 = twinFile2;
    }



    @Override
    protected Void call() {
        guiLabelManagement.setStatus("Analyzing...");
        //Disable the buttons while the data is processed
        guiLabelManagement.disableFolderButton(true);
        guiLabelManagement.disableAnalyzeDataButton(true);
        guiLabelManagement.disableOutputResultButton(true);
        //Change the progress output
        progressOutputText.setStyle("-fx-font-size: 16; -fx-text-alignment: center");
        progressOutputText.setText("Analyzing...");
        //Add the progress indicator and progressOutputText to the output panel
        guiLabelManagement.setNodeToAddToOutputPanel(guiLabelManagement.getProgressIndicatorNode());
        guiLabelManagement.setNodeToAddToOutputPanel(progressOutputText);
        FileAnalyzer fileAnalyzer = new FileAnalyzer(comparisonFiles, twinFile1, twinFile2, guiLabelManagement);
        fileAnalyzer.setOutputText(progressOutputText);

        try {
            fileAnalyzer.analyzeFiles();
            System.out.println("Done Analyzing");
            guiLabelManagement.clearOutputPanel();
            progressOutputText.setText("Done Analyzing\n" +
                    "Press 'Output Results' to see the result of the analysis.\n" +
                    "The output file name will be 'Report.xlsx'");
            guiLabelManagement.setNodeToAddToOutputPanel(progressOutputText);
            dataGathered = fileAnalyzer.getDataGathered();
            guiLabelManagement.disableFolderButton(false);
            guiLabelManagement.disableAnalyzeDataButton(false);
            guiLabelManagement.disableOutputResultButton(false);

        } catch (Exception e) {
            //In case there is an error, notify the user
            e.printStackTrace();
            guiLabelManagement.setAlertPopUp(e.getMessage());
            guiLabelManagement.clearOutputPanel();
            progressOutputText.setText("There was a problem analyzing the files.\nMake sure you have inputted all" +
                    " the information correctly.");

            guiLabelManagement.setNodeToAddToOutputPanel(progressOutputText);
        }
        return null;
    }


    /**
     * Use this to catch any exceptions
     */
    @Override
    protected void done() {
        try {
            if (!isCancelled()) get();
        } catch (ExecutionException e) {
            // Exception occurred, deal with it
            System.out.println("Exception: " + e.getCause());
            e.printStackTrace();
        } catch (InterruptedException e) {
            // Shouldn't happen, we're invoked when computation is finished
            throw new AssertionError(e);
        }
    }


    TreeMap<Integer, ArrayList<Object>> getDataGathered() {
        return dataGathered;
    }
}