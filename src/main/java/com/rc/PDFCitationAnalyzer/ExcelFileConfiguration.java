package com.rc.PDFCitationAnalyzer;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Used for obtaining the relevant data columns from the excel file by asking the user for input.
 */
class ExcelFileConfiguration extends Task {

    private GUILabelManagement guiLabelManagement;

    ExcelFileConfiguration(GUILabelManagement guiLabelManagement) {
        this.guiLabelManagement = guiLabelManagement;
    }


    @Override
    protected Object call() throws Exception {
        initialize();
        return null;
    }

    /**
     * Sets up the start GUI
     */
    private void initialize() {
        guiLabelManagement.clearOutputPanel();
        Label disclaimer = new Label("In order to start, please make sure that the following columns are present in" +
                " your excel file:\n" +
                "1. PairID/TwinID\n" +
                "2. Title of Twin 1\n" +
                "3. Title of Twin 2\n" +
                "4. Tile that cites Twin 1 and Twin 2\n" +
                "5. Author of Twin 1\n" +
                "6. Year Twin 1 was published\n" +
                "7. Author of Twin 2\n" +
                "8. Year Twin 2 was published");
        disclaimer.setStyle("-fx-text-alignment: center");
        Label instructions = new Label("If your excel file contains all the 8 columns specified above, press continue");
        instructions.setStyle("-fx-text-alignment: center");
//        JFXTextField textInput = new JFXTextField();
//        textInput.setStyle("-fx-text-alignment: center;" +
//                "-fx-alignment: center");
        JFXButton continueButton = new JFXButton("Continue");
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(disclaimer, instructions, continueButton);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);

        continueButton.setOnAction(e -> {
            //Move to the next step
            askUserForPairIDColumn();
        });

//        submit.setOnAction(event -> {
//            if (textInput.getText().isEmpty()) {
//                guiLabelManagement.setAlertPopUp("Please write the title");
//            } else {
//                FileFormatter.addTitle(textInput.getText());
//                inputAuthorManually(num);
//            }
//        });
    }

    /**
     * Asks the user for the pairID column number
     */
    private void askUserForPairIDColumn() {

    }
}
