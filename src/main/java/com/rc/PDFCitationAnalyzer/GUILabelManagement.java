package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;

import java.io.Serializable;

/**
 * Handles the GUI of the application throughout all the classes.
 */
class GUILabelManagement implements Serializable {

    private transient StringProperty alertPopUp = new SimpleStringProperty();
    private transient DoubleProperty progressIndicator = new SimpleDoubleProperty();
    private transient StringProperty status = new SimpleStringProperty();
    private transient StringProperty informationPanel = new SimpleStringProperty();
    private transient BooleanProperty clearOutputPanel = new SimpleBooleanProperty();
    private transient BooleanProperty disableFolderButton = new SimpleBooleanProperty(true);
    private transient BooleanProperty analyzeDataButton = new SimpleBooleanProperty(true);
    private transient BooleanProperty outputResultsButton = new SimpleBooleanProperty(true);
    private transient BooleanProperty changeConfigureExcelFileText = new SimpleBooleanProperty();
    private transient BooleanProperty changeUploadExcelFileText = new SimpleBooleanProperty();
    private transient BooleanProperty changeRecoverBackupText = new SimpleBooleanProperty();
    private transient BooleanProperty changeSaveProgressText = new SimpleBooleanProperty();
    private transient BooleanProperty twinFilesAnalysisDeselected = new SimpleBooleanProperty();
    private transient BooleanProperty singleFileAnalysisDeselected = new SimpleBooleanProperty();
    private transient ListProperty<Node> nodesToAddToOutputPanel = new SimpleListProperty<>();
    private transient ProgressIndicator progressIndicatorNode;
    private transient IntegerProperty outputPanelSpacing = new SimpleIntegerProperty();
    //Change this depending on the time of machine used. If it is a slow machine, it might take longer for the GUI to
    // render (Note that time is in milliseconds)
    private static final int TIME_TO_WAIT_FOR_GUI = 300;
    private boolean showAlerts = true;


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

    BooleanProperty getChangeRecoverBackupText() {return changeRecoverBackupText;}

    BooleanProperty getChangeSaveProgressText() {return changeSaveProgressText;}

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
        if (showAlerts) {
            //Clear the previous alert (if not it won't show a new alert if it has the same text)
            this.alertPopUp.set("");
            this.alertPopUp.set(alertPopUp);
            waitForGUIToLoad();
        }
    }


    /**
     * Sets the current percentage the progress indicator has loaded from 0 to 1
     *
     * @param loadBar double from 0 to 1
     */
    void setProgressIndicator(double loadBar) {
        this.progressIndicator.set(loadBar);
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
     * Updates the 'Recover Backup' text once the user is analyzing
     */
    void changeToSaveProgress() {
        boolean currVal = changeRecoverBackupText.getValue();
        this.changeRecoverBackupText.set(!currVal);
        waitForGUIToLoad();
    }

    /**
     * Updates the 'Save Progress' text to Recover Backup
     */
    void changeToRecoverBackup() {
        boolean currVal = changeSaveProgressText.getValue();
        this.changeSaveProgressText.set(!currVal);
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
        this.informationPanel.set("");
        this.informationPanel.set(s);
        waitForGUIToLoad();
    }


    /**
     * Waits for the GUI
     */
    static void waitForGUIToLoad() {
        try {
            Thread.sleep(TIME_TO_WAIT_FOR_GUI);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    void disableAlerts() {
        this.showAlerts = false;
    }


}