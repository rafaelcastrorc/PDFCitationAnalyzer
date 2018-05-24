package com.rc.PDFCitationAnalyzer;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Scanner;

/**
 * Use it to obtain the relevant columns location from the excel file that contains the multiple pairs of twins.
 */
class ExcelFileConfiguration extends Task {

    private GUILabelManagement guiLabelManagement;
    private int pairIdColumn;
    private int titleTwin1Column;
    private int titleTwin2Column;
    private int titleCitingColumn;
    private int authorTwin1Column;
    private int authorTwin2Column;
    private int yearTwin1Column;
    private int yearTwin2Column;
    private HashSet<Integer> columns = new HashSet<>();

    ExcelFileConfiguration(GUILabelManagement guiLabelManagement) {
        this.guiLabelManagement = guiLabelManagement;
    }


    @Override
    protected Object call() {
        initialize();
        return null;
    }

    /**
     * Sets up the start GUI
     */
    private void initialize() {
        guiLabelManagement.setStatus("Configuring Excel/CSV file structure.");
        guiLabelManagement.clearOutputPanel();
        Label disclaimer = new Label("In order to start, please make sure that the following columns are present in" +
                " your excel/CSV file:\n" +
                "1. PairID/TwinID\n" +
                "2. Title of Twin 1\n" +
                "3. Title of Twin 2\n" +
                "4. Title that cites Twin 1 and Twin 2\n" +
                "5. Author of Twin 1\n" +
                "6. Year Twin 1 was published\n" +
                "7. Author of Twin 2\n" +
                "8. Year Twin 2 was published");
        disclaimer.setStyle("-fx-text-alignment: center; -fx-font-size: 13");
        Label instructions = new Label(("If your Excel/CSV file contains all the 8 columns specified above, press " +
                "continue."));
        instructions.setStyle("-fx-text-alignment: center; -fx-font-size: 13");


        JFXButton continueButton = new JFXButton("Continue");
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(disclaimer, instructions, continueButton);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);

        continueButton.setOnAction(e -> {
            //Move to the next step
            askUserForPairIDColumn();
        });
    }

    /**
     * Asks the user for the pairID column number
     */
    private void askUserForPairIDColumn() {
        //Set up the GUI
        guiLabelManagement.clearOutputPanel();
        Label instructions = new Label("Write the number or letter(s) of the column where the 'PairId/TwinId' column " +
                "is located.\n" +
                "Also, make sure that the PairID/TwinID column is the first column of the entire file. THIS IS " +
                "VERY IMPORTANT\n\n" +
                "Press Continue once your are done.");
        instructions.setStyle("-fx-text-alignment: center; -fx-font-size: 14");
        JFXTextField textInput = new JFXTextField();
        textInput.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 14");
        JFXButton continueButton = new JFXButton("Continue");
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(instructions, textInput, continueButton);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);
        continueButton.setOnAction(event -> {
            if (textInput.getText().isEmpty() || textInput.getText().equals(" ")) {
                guiLabelManagement.setAlertPopUp("Please write the column number/letter");
            } else {
                String pairIdColumnText = textInput.getText();
                //Check if its a letter or a number.
                if (StringUtils.isNumeric(pairIdColumnText)) {
                    //If it is a number, verify that it is an int
                    if (isNotInteger(pairIdColumnText)) {
                        guiLabelManagement.setAlertPopUp("The number you inputted is not an integer!");
                    } else {
                        //Make sure that it is a non negative int
                        pairIdColumn = Integer.parseInt(pairIdColumnText);
                        if (pairIdColumn < 0) {
                            guiLabelManagement.setAlertPopUp("The number cannot be negative!");

                        } else if (pairIdColumn > 1) {
                            //Make sure that it is the first column
                            guiLabelManagement.setAlertPopUp("This has to be the first column!");
                        } else {
                            columns.add(pairIdColumn);
                            //Number is a positive integer so move to the next step
                            askUserForTitle1AndTitle2Columns();
                        }

                    }

                }
                //If it is not a number, convert it to one
                else {
                    pairIdColumn = toNumber(pairIdColumnText.toUpperCase());
                    //Make sure is not negative
                    if (pairIdColumn < 0) {
                        guiLabelManagement.setAlertPopUp("Your input is not right!\n" +
                                "Make sure that it is either a positive integer or its only letters with no spaces " +
                                "or symbols.");

                    } else if (pairIdColumn > 1) {
                        //Make sure that it is the first column
                        guiLabelManagement.setAlertPopUp("This has to be the first column!");
                    } else {
                        columns.add(pairIdColumn);
                        askUserForTitle1AndTitle2Columns();
                    }
                }

            }
        });
    }


    /**
     * Asks the user for the column numbers of the titles of Twin 1 & Twin 2
     */
    private void askUserForTitle1AndTitle2Columns() {
        //Set up the GUI
        guiLabelManagement.clearOutputPanel();
        Label instructions1 = new Label("Write the number or letter(s) of the column where the 'Title of Twin 1' " +
                "column" +
                " is located");
        Label instructions2 = new Label("Write the number or letter(s) of the column where the 'Title of Twin 2' " +
                "column" +
                " is located.\n" +
                "Make sure that the titles come before the authors and years published. Press Continue once your are " +
                "done.");
        instructions1.setStyle("-fx-text-alignment: center; -fx-font-size: 14");
        instructions2.setStyle("-fx-text-alignment: center; -fx-font-size: 14");

        JFXTextField textInput1 = new JFXTextField();
        JFXTextField textInput2 = new JFXTextField();

        textInput1.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 14");
        textInput2.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 14");

        JFXButton continueButton = new JFXButton("Continue");
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(instructions1, textInput1, instructions2, textInput2, continueButton);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);
        continueButton.setOnAction(event -> {
            if (textInput1.getText().isEmpty() || textInput1.getText().equals(" ") || textInput2.getText().isEmpty()
                    || textInput2.getText().equals(" ")) {
                guiLabelManagement.setAlertPopUp("Please write the column number/letter");
            } else {
                String titleTwin1ColumnText = textInput1.getText();
                String titleTwin2ColumnText = textInput2.getText();

                //Check if its a letter or a number.
                if (StringUtils.isNumeric(titleTwin1ColumnText) && StringUtils.isNumeric(titleTwin2ColumnText)) {
                    //If it is a number, verify that it is an int
                    if (isNotInteger(titleTwin1ColumnText) || isNotInteger(titleTwin2ColumnText)) {
                        guiLabelManagement.setAlertPopUp("The number you inputted is not an integer!");
                    } else {
                        //Make sure that it is a non negative int
                        titleTwin1Column = Integer.parseInt(titleTwin1ColumnText);
                        titleTwin2Column = Integer.parseInt(titleTwin2ColumnText);

                        if (titleTwin1Column < 0 || titleTwin2Column < 0) {
                            guiLabelManagement.setAlertPopUp("The number cannot be negative!");

                        } else {
                            //Make sure that the numbers are not the same
                            if (titleTwin1Column == titleTwin2Column) {
                                guiLabelManagement.setAlertPopUp("The columns cannot be the same.");
                            } else if (columns.contains(titleTwin1Column) || columns.contains(titleTwin2Column)) {
                                guiLabelManagement.setAlertPopUp("You have already declared this column before");
                            } else {
                                columns.add(titleTwin1Column);
                                columns.add(titleTwin2Column);
                                //Both numbers a positive integer so move to the next step
                                askUserForCitingPaperColumn();
                            }
                        }
                    }
                }
                //If it is not a number, convert it to one
                else {
                    titleTwin1Column = toNumber(titleTwin1ColumnText.toUpperCase());
                    titleTwin2Column = toNumber(titleTwin2ColumnText.toUpperCase());

                    //Make sure is not negative
                    if (titleTwin1Column < 0 || titleTwin2Column < 0) {
                        guiLabelManagement.setAlertPopUp("Your input is not right!\n" +
                                "Make sure that it is either a positive integer or its only letters with no spaces " +
                                "or symbols.");

                    } else {
                        //Make sure that the numbers are not the same
                        if (titleTwin1Column == titleTwin2Column) {
                            guiLabelManagement.setAlertPopUp("The columns cannot be the same.");
                        } else if (columns.contains(titleTwin1Column) || columns.contains(titleTwin2Column)) {
                            guiLabelManagement.setAlertPopUp("You have already declared this column before");
                        } else {
                            columns.add(titleTwin1Column);
                            columns.add(titleTwin2Column);
                            //Move to the next step
                            askUserForCitingPaperColumn();
                        }
                    }
                }

            }
        });
    }

    /**
     * Asks the user for the Citing Paper column number
     */
    private void askUserForCitingPaperColumn() {
        //Set up the GUI
        guiLabelManagement.clearOutputPanel();
        Label instructions = new Label("Write the number or letter(s) of the column where the 'Title that cites Twin " +
                "1 and Twin 2' column is located.\n\n" +
                "Press Continue once your are done.");
        instructions.setStyle("-fx-text-alignment: center; -fx-font-size: 14");
        JFXTextField textInput = new JFXTextField();
        textInput.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 14");
        JFXButton continueButton = new JFXButton("Continue");
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(instructions, textInput, continueButton);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);
        continueButton.setOnAction(event -> {
            if (textInput.getText().isEmpty() || textInput.getText().equals(" ")) {
                guiLabelManagement.setAlertPopUp("Please write the column number/letter");
            } else {
                String titleCitingColumnText = textInput.getText();
                //Check if its a letter or a number.
                if (StringUtils.isNumeric(titleCitingColumnText)) {
                    //If it is a number, verify that it is an int
                    if (isNotInteger(titleCitingColumnText)) {
                        guiLabelManagement.setAlertPopUp("The number you inputted is not an integer!");
                    } else {
                        //Make sure that it is a non negative int
                        titleCitingColumn = Integer.parseInt(titleCitingColumnText);
                        if (titleCitingColumn < 0) {
                            guiLabelManagement.setAlertPopUp("The number cannot be negative!");
                        } else if (columns.contains(titleCitingColumn)) {
                            guiLabelManagement.setAlertPopUp("You have already declared this column before");
                        } else {
                            columns.add(titleCitingColumn);
                            //Number is a positive integer so move to the next step
                            askUserForAuthor1AndAuthor2();
                        }
                    }
                }
                //If it is not a number, convert it to one
                else {
                    titleCitingColumn = toNumber(titleCitingColumnText.toUpperCase());
                    //Make sure is not negative
                    if (titleCitingColumn < 0) {
                        guiLabelManagement.setAlertPopUp("Your input is not right!\n" +
                                "Make sure that it is either a positive integer or its only letters with no spaces " +
                                "or symbols.");
                    } else if (columns.contains(titleCitingColumn)) {
                        guiLabelManagement.setAlertPopUp("You have already declared this column before");
                    } else {
                        columns.add(titleCitingColumn);
                        askUserForAuthor1AndAuthor2();
                    }
                }

            }
        });

    }

    /**
     * Asks the user for the column numbers of the authors of Twin 1 & Twin 2
     */
    private void askUserForAuthor1AndAuthor2() {
        //Set up the GUI
        guiLabelManagement.clearOutputPanel();
        Label instructions1 = new Label("Write the number or letter(s) of the column where the 'Author of Twin 1' " +
                "column" +
                " is located");
        Label instructions2 = new Label("Write the number or letter(s) of the column where the 'Author of Twin 2' " +
                "column" +
                " is located.\n\n" +
                "Press Continue once your are done.");
        instructions1.setStyle("-fx-text-alignment: center; -fx-font-size: 14");
        instructions2.setStyle("-fx-text-alignment: center; -fx-font-size: 14");

        JFXTextField textInput1 = new JFXTextField();
        JFXTextField textInput2 = new JFXTextField();

        textInput1.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 14");
        textInput2.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 14");

        JFXButton continueButton = new JFXButton("Continue");
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(instructions1, textInput1, instructions2, textInput2, continueButton);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);
        continueButton.setOnAction(event -> {
            if (textInput1.getText().isEmpty() || textInput1.getText().equals(" ") || textInput2.getText().isEmpty()
                    || textInput2.getText().equals(" ")) {
                guiLabelManagement.setAlertPopUp("Please write the column number/letter");
            } else {
                String authorTwin1ColumnText = textInput1.getText();
                String authorTwin2ColumnText = textInput2.getText();

                //Check if its a letter or a number.
                if (StringUtils.isNumeric(authorTwin1ColumnText) && StringUtils.isNumeric(authorTwin2ColumnText)) {
                    //If it is a number, verify that it is an int
                    if (isNotInteger(authorTwin1ColumnText) || isNotInteger(authorTwin2ColumnText)) {
                        guiLabelManagement.setAlertPopUp("The number you inputted is not an integer!");
                    } else {
                        //Make sure that it is a non negative int
                        authorTwin1Column = Integer.parseInt(authorTwin1ColumnText);
                        authorTwin2Column = Integer.parseInt(authorTwin2ColumnText);

                        if (authorTwin1Column < 0 || authorTwin2Column < 0) {
                            guiLabelManagement.setAlertPopUp("The number cannot be negative!");

                        } else {
                            //Make sure that the numbers are not the same
                            if (authorTwin1Column == authorTwin2Column) {
                                guiLabelManagement.setAlertPopUp("The columns cannot be the same.");
                            } else if (columns.contains(authorTwin1Column) || columns.contains(authorTwin2Column)) {
                                guiLabelManagement.setAlertPopUp("You have already declared this column before");
                            } else {
                                columns.add(authorTwin1Column);
                                columns.add(authorTwin2Column);
                                //Both numbers a positive integer so move to the next step
                                askUserForYear1AndYear2();
                            }
                        }
                    }
                }
                //If it is not a number, convert it to one
                else {
                    authorTwin1Column = toNumber(authorTwin1ColumnText.toUpperCase());
                    authorTwin2Column = toNumber(authorTwin2ColumnText.toUpperCase());

                    //Make sure is not negative
                    if (authorTwin1Column < 0 || authorTwin2Column < 0) {
                        guiLabelManagement.setAlertPopUp("Your input is not right!\n" +
                                "Make sure that it is either a positive integer or its only letters with no spaces " +
                                "or symbols.");

                    } else {
                        //Make sure that the numbers are not the same
                        if (authorTwin1Column == authorTwin2Column) {
                            guiLabelManagement.setAlertPopUp("The columns cannot be the same.");
                        } else if (columns.contains(authorTwin1Column) || columns.contains(authorTwin2Column)) {
                            guiLabelManagement.setAlertPopUp("You have already declared this column before");
                        } else {
                            columns.add(authorTwin1Column);
                            columns.add(authorTwin2Column);
                            //Move to the next step
                            askUserForYear1AndYear2();
                        }
                    }
                }

            }
        });
    }

    /**
     * Asks the user for the column numbers of the years Twin 1 & Twin 2 were published
     */
    private void askUserForYear1AndYear2() {
        //Set up the GUI
        guiLabelManagement.clearOutputPanel();
        Label instructions1 = new Label("Write the number or letter(s) of the column where the 'Year Twin 1 was " +
                "published' column is located");
        Label instructions2 = new Label("Write the number or letter(s) of the column where the 'Year Twin 2 was " +
                "published' column is located.\n\n" +
                "Press Continue once your are done.");
        instructions1.setStyle("-fx-text-alignment: center; -fx-font-size: 14");
        instructions2.setStyle("-fx-text-alignment: center; -fx-font-size: 14");

        JFXTextField textInput1 = new JFXTextField();
        JFXTextField textInput2 = new JFXTextField();

        textInput1.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 14");
        textInput2.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 14");

        JFXButton continueButton = new JFXButton("Continue");
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(instructions1, textInput1, instructions2, textInput2, continueButton);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);
        continueButton.setOnAction(event -> {
            if (textInput1.getText().isEmpty() || textInput1.getText().equals(" ") || textInput2.getText().isEmpty()
                    || textInput2.getText().equals(" ")) {
                guiLabelManagement.setAlertPopUp("Please write the column number/letter");
            } else {
                String yearTwin1ColumnText = textInput1.getText();
                String yearTwin2ColumnText = textInput2.getText();

                //Check if its a letter or a number.
                if (StringUtils.isNumeric(yearTwin1ColumnText) && StringUtils.isNumeric(yearTwin2ColumnText)) {
                    //If it is a number, verify that it is an int
                    if (isNotInteger(yearTwin1ColumnText) || isNotInteger(yearTwin2ColumnText)) {
                        guiLabelManagement.setAlertPopUp("The number you inputted is not an integer!");
                    } else {
                        //Make sure that it is a non negative int
                        yearTwin1Column = Integer.parseInt(yearTwin1ColumnText);
                        yearTwin2Column = Integer.parseInt(yearTwin2ColumnText);

                        if (yearTwin1Column < 0 || yearTwin2Column < 0) {
                            guiLabelManagement.setAlertPopUp("The number cannot be negative!");

                        } else {
                            //Make sure that the numbers are not the same
                            if (yearTwin1Column == yearTwin2Column) {
                                guiLabelManagement.setAlertPopUp("The columns cannot be the same.");
                            } else if (columns.contains(yearTwin1Column) || columns.contains(yearTwin2Column)) {
                                guiLabelManagement.setAlertPopUp("You have already declared this column before");
                            } else {
                                columns.add(yearTwin1Column);
                                columns.add(yearTwin2Column);
                                //Both numbers a positive integer so move to the next step
                                finish();
                            }
                        }
                    }
                }
                //If it is not a number, convert it to one
                else {
                    yearTwin1Column = toNumber(yearTwin1ColumnText.toUpperCase());
                    yearTwin2Column = toNumber(yearTwin2ColumnText.toUpperCase());

                    //Make sure is not negative
                    if (yearTwin1Column < 0 || yearTwin2Column < 0) {
                        guiLabelManagement.setAlertPopUp("Your input is not right!\n" +
                                "Make sure that it is either a positive integer or its only letters with no spaces " +
                                "or symbols.");

                    } else {
                        //Make sure that the numbers are not the same
                        if (yearTwin1Column == yearTwin2Column) {
                            guiLabelManagement.setAlertPopUp("The columns cannot be the same.");
                        } else if (columns.contains(yearTwin1Column) || columns.contains(yearTwin2Column)) {
                            guiLabelManagement.setAlertPopUp("You have already declared this column before");
                        } else {
                            columns.add(yearTwin2Column);
                            columns.add(yearTwin1Column);
                            //Move to the next step
                            finish();
                        }
                    }
                }

            }
        });
    }

    /**
     * Ask the user if they want to save this configuration, if so save it in Preferences.
     */
    private void finish() {
        guiLabelManagement.clearOutputPanel();
        Label instructions = new Label("Everything has been set up!\n\n" +
                "Do you want to make this your default configuration?");
        instructions.setStyle("-fx-text-alignment: center; -fx-font-size: 14");
        JFXButton yes = new JFXButton("Yes");
        yes.setDefaultButton(true);
        JFXButton no = new JFXButton("No");

        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(yes, no);

        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);

        vBox.getChildren().addAll(instructions, hBox);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);

        Label nextStep = new Label("Now upload your excel/CSV file.\n" +
                "Make sure its an .xlsx or .csv or .odd file.");
        nextStep.setStyle("-fx-text-alignment: center");
        //Store the configuration
        String configuration = pairIdColumn + "," + titleTwin1Column + "," + titleTwin2Column + "," +
                titleCitingColumn + "," + authorTwin1Column + "," + authorTwin2Column + "," + yearTwin1Column +
                "," + yearTwin2Column;
        yes.setOnAction(e -> {
            guiLabelManagement.setStatus("Saved as default configuration.");
            UserPreferences.storeExcelConfiguration(configuration, true);
            guiLabelManagement.clearOutputPanel();
            guiLabelManagement.setNodeToAddToOutputPanel(nextStep);

        });
        no.setOnAction(e -> {
            guiLabelManagement.setStatus("Done configuring Excel/CSV file.");
            guiLabelManagement.clearOutputPanel();
            UserPreferences.storeExcelConfiguration(configuration, false);
            guiLabelManagement.setNodeToAddToOutputPanel(nextStep);


        });

        guiLabelManagement.updateConfigureExcelFileText();
    }


    /**
     * Converts a column letter(s) to a number
     * For instance, column C will be converted to 3
     *
     * @param letters String
     */
    private int toNumber(String letters) {
        int number = 0;
        for (int i = 0; i < letters.length(); i++) {
            number = number * 26 + (letters.charAt(i) - ('A' - 1));
        }
        return number;
    }

    /**
     * Checks if a String represents an integer (base 10)
     *
     * @return false if its not an int, true otherwise
     */
     static boolean isNotInteger(String s) {
        Scanner sc = new Scanner(s.trim());
        if (!sc.hasNextInt(10)) return true;
        // we know it starts with a valid int, now make sure
        // there's nothing left!
        sc.nextInt(10);
        return sc.hasNext();
    }
}
