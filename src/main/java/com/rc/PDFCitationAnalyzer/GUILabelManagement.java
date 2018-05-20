package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;

/**
 * Handles the GUI of the application throughout all the classes.
 */
class GUILabelManagement {

    private StringProperty alertPopUp = new SimpleStringProperty();
    private DoubleProperty progressIndicator = new SimpleDoubleProperty();
    private StringProperty progressOutput = new SimpleStringProperty();
    private StringProperty status = new SimpleStringProperty();
    private StringProperty informationPanel = new SimpleStringProperty();
    private BooleanProperty clearOutputPanel = new SimpleBooleanProperty();
    private BooleanProperty disableFolderButton = new SimpleBooleanProperty(true);
    private BooleanProperty analyzeDataButton = new SimpleBooleanProperty(true);
    private BooleanProperty outputResultsButton = new SimpleBooleanProperty(true);
    private BooleanProperty changeConfigureExcelFileText = new SimpleBooleanProperty(true);
    private BooleanProperty changeUploadExcelFileText = new SimpleBooleanProperty(true);
    private BooleanProperty twinFilesAnalysisDeselected = new SimpleBooleanProperty();
    private BooleanProperty singleFileAnalysisDeselected = new SimpleBooleanProperty();
    private ListProperty<Node> nodesToAddToOutputPanel = new SimpleListProperty<>();
    private ProgressIndicator progressIndicatorNode;
    private IntegerProperty outputPanelSpacing = new SimpleIntegerProperty();
    //Change this depending on the time of machine used. If it is a slow machine, it might take longer for the GUI to
    // render (Note that time is in milliseconds)
    private static final int TIME_TO_WAIT_FOR_GUI = 300;


    StringProperty getAlertPopUp() {
        return alertPopUp;
    }

    DoubleProperty getProgressIndicator() {
        return progressIndicator;
    }

    BooleanProperty getClearOutputPanel() {
        return clearOutputPanel;
    }

    StringProperty getStatus() {
        return status;
    }

    BooleanProperty getDisableFolderButton() {
        return disableFolderButton;
    }

    StringProperty getProgressOutput() {
        return progressOutput;
    }

    StringProperty getInformationPanel() {
        return informationPanel;
    }

    ProgressIndicator getProgressIndicatorNode() {
        return progressIndicatorNode;
    }

    IntegerProperty getOutputPanelSpacing() {
        return outputPanelSpacing;
    }

    BooleanProperty getTwinFilesAnalysisDeselected() {
        return twinFilesAnalysisDeselected;
    }

    BooleanProperty getSingleFileAnalysisDeselected() {
        return singleFileAnalysisDeselected;
    }

    BooleanProperty getAnalyzeDataButton() {
        return analyzeDataButton;
    }

    BooleanProperty getChangeConfigureExcelFileText() {return changeConfigureExcelFileText;}

    BooleanProperty getChangeUploadExcelFileText() {return changeUploadExcelFileText;}


    BooleanProperty getOuputResultsButton() {
        return outputResultsButton;
    }

    ListProperty<Node> getNodesToAddToOutputPanel() {
        return nodesToAddToOutputPanel;
    }


    /**
     * Sets a pop up alert
     *
     * @param alertPopUp String with message to display
     */
    void setAlertPopUp(String alertPopUp) {
        //Clear the previous alert (if not it won't show a new alert if it has the same text)
        this.alertPopUp.set("");
        this.alertPopUp.set(alertPopUp);
        waitForGUIToLoad();
    }


    /**
     * Sets the current percentage the progress indicator has loaded from 0 to 1
     *
     * @param loadBar double from 0 to 1
     */
    void setProgressIndicator(double loadBar) {
        this.progressIndicator.set(loadBar);
        waitForGUIToLoad();

    }

    /**
     * Sets the progressOutput displayed in the status label
     *
     * @param progressOutput String with message to display
     */
    void setProgressOutput(String progressOutput) {
        //Clear the previous output
        this.progressOutput.set("");
        this.progressOutput.set(progressOutput);
        waitForGUIToLoad();
    }


    /**
     * Clears the output panel
     */
    void clearOutputPanel() {
        nodesToAddToOutputPanel.clear();
        waitForGUIToLoad();

    }

    void setOutputPanelSpacing(int spacing) {
        this.outputPanelSpacing.set(10);
    }


    /**
     * Stores the progress indicator Node
     *
     * @param progressIndicator ProgressIndicator Node
     */
    void storeProgressIndicator(ProgressIndicator progressIndicator) {
        progressIndicatorNode = progressIndicator;
        waitForGUIToLoad();

    }


    /**
     * Adds a single node (JavaFX object) to the output panel
     *
     * @param nodeToAddToOutputPanel Single node to add
     */
    void setNodeToAddToOutputPanel(Node nodeToAddToOutputPanel) {
        //Because we are using an observable list, run it in the JavaFX application thread
        Platform.runLater(() -> {
            try {
                ObservableList<Node> currentNodes;
                //Get the current nodes
                if (this.nodesToAddToOutputPanel.size() != 0) {
                    currentNodes = this.nodesToAddToOutputPanel.get();
                    currentNodes.add(nodeToAddToOutputPanel);
                }
                //Create a new list if there are currently no nodes in the output panel
                else {
                    currentNodes = FXCollections.observableArrayList();
                    currentNodes.add(nodeToAddToOutputPanel);
                }
                //Set the new list
                this.nodesToAddToOutputPanel.set(currentNodes);
            } catch (Exception e) {
                e.getMessage();
                e.printStackTrace();
            }
        });
        waitForGUIToLoad();

    }


    /**
     * Sets the status message, which is the bottom text of the Analyzer.
     *
     * @param status Message to display
     */
    void setStatus(String status) {
        this.status.set(status);
        waitForGUIToLoad();

    }

    /**
     * Enables the folder button
     */
    void disableFolderButton(boolean disable) {
        this.disableFolderButton.set(disable);
        waitForGUIToLoad();
    }


    /**
     * Deselects the 'Twin Files; radial button
     */
    void deselectTwinFilesAnalysis() {
        boolean currVal = twinFilesAnalysisDeselected.getValue();
        this.twinFilesAnalysisDeselected.set(!currVal);
        waitForGUIToLoad();
    }


    /**
     * Deselects the 'Single File' radial button
     */
    void deselectSingleFileAnalysis() {
        boolean currVal = singleFileAnalysisDeselected.getValue();
        this.singleFileAnalysisDeselected.set(!currVal);
        waitForGUIToLoad();
    }


    /**
     * Updates the 'Configure Excel File' text once the user has configured the excel file
     */
    void updateConfigureExcelFileText() {
        boolean currVal = changeConfigureExcelFileText.getValue();
        this.changeConfigureExcelFileText.set(!currVal);
        waitForGUIToLoad();
    }

    /**
     * Updates the 'Upload Excel File' text once the user has uploaded an excel file
     */
    void updateUploadExcelFileText() {
        boolean currVal = changeUploadExcelFileText.getValue();
        this.changeUploadExcelFileText.set(!currVal);
        waitForGUIToLoad();
    }


    /**
     * Enables or disables the Analyze Data button
     */
    void disableAnalyzeDataButton(boolean disable) {
        this.analyzeDataButton.set(disable);
        waitForGUIToLoad();
    }

    /**
     * Enables or disables the Analyze Data button
     */
    void disableOutputResultButton(boolean disable) {
        this.outputResultsButton.set(disable);
        waitForGUIToLoad();
    }


    /**
     * Creates a pop up with information for the user.
     */
    void setInformationPanel(String s) {
        this.informationPanel.set(s);
        waitForGUIToLoad();
    }


    /**
     * Waits for the GUI
     */
    private void waitForGUIToLoad() {
        try {
            Thread.sleep(TIME_TO_WAIT_FOR_GUI);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}