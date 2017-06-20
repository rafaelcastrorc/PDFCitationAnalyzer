package com.rc.PDFCitationAnalyzer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the different objects that the controller is listening to, to update the main GUI.
 */
class GUILabelManagement {

    private StringProperty alertPopUp = new SimpleStringProperty();
    private StringProperty searchResultLabel = new SimpleStringProperty();
    private DoubleProperty loadBar = new SimpleDoubleProperty();
    private StringProperty output = new SimpleStringProperty();



    StringProperty getAlertPopUp() {
        return alertPopUp;
    }

    StringProperty getSearchResultLabel() {
        return searchResultLabel;
    }

    DoubleProperty getLoadBar() {
        return loadBar;
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
     * Sets what the searchResult label will display
     *
     * @param searchResultLabel String with message to display
     */
    void setSearchResultLabel(String searchResultLabel) {
        this.searchResultLabel.set(searchResultLabel);
    }

    /**
     * Sets the current percentage the progress bar has loaded from 0 to 1
     *
     * @param loadBar double from 0 to 1
     */
    void setLoadBar(double loadBar) {
        this.loadBar.set(loadBar);
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