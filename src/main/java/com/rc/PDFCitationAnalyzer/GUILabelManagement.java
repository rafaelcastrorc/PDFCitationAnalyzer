package com.rc.PDFCitationAnalyzer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Manages the different objects that the controller is listening to, to update the main GUI.
 */
class GUILabelManagement {

    private StringProperty alertPopUp = new SimpleStringProperty();
    private DoubleProperty progressIndicator = new SimpleDoubleProperty();
    private StringProperty output = new SimpleStringProperty();



    StringProperty getAlertPopUp() {
        return alertPopUp;
    }

    DoubleProperty getProgressIndicator() {
        return progressIndicator;
    }

    StringProperty getOutput() {
        return output;
    }



    /**
     * Sets a pop up alert
     *
     * @param alertPopUp String with message to display
     */
    void setAlertPopUp(String alertPopUp) {
        this.alertPopUp.set(alertPopUp);
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
     * Sets the output displayed in the status label
     *
     * @param output String with message to display
     */
    void setOutput(String output) {
        this.output.set(output);
    }


}