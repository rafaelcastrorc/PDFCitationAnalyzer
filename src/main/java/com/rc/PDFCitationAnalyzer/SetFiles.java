package com.rc.PDFCitationAnalyzer;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rafaelcastro on 6/19/17.
 * Setups a pair of twin files correctly by asking the user for input.
 * The point of doing this is to get the relevant data to be able to analyze other files and see if they cite
 * this twin file(s)
 */
class SetFiles {
    private GUILabelManagement guiLabelManagement;
    private File file2;
    private ExecutorService executorService;
    private String infoFile1;
    private Controller controller;
    private File file1;
    private boolean done = false;

    /**
     * Sets the metadata information of the twin files
     */
    void setTwinFiles(Controller controller, File file1, File file2, GUILabelManagement guiLabelManagement) {
        this.controller = controller;
        this.file1 = file1;
        this.file2 = file2;
        this.guiLabelManagement = guiLabelManagement;
        guiLabelManagement.setInformationPanel("This function checks if the selected pair of PDF files are in fact " +
                "twin articles/papers.\n\n" +
                "Because not all pdf files are formatted correctly, it is necessary to gather all the relevant " +
                "data of the paper that you selected.\n" +
                "The program will try to get the necessary data, but you will have to verify its accuracy");

        this.executorService = Executors.newSingleThreadExecutor(new MyThreadFactory());

        Worker task = new Worker(file1, 1);
        executorService.submit(task);

    }

    /**
     * Used when analyzing a single article. Sets the metadata for the article
     */
    void setSingleFile(Controller controller, File file, GUILabelManagement guiLabelManagement) {
        this.controller = controller;
        this.file1 = file;
        this.file2 = file;
        this.guiLabelManagement = guiLabelManagement;
        guiLabelManagement.setInformationPanel("This function is mainly used for testing.\n" +
                "It checks if the selected article is cited in a set of papers." +
                "\n\n" +
                "Because not all pdf files are formatted correctly, it is necessary to gather all the relevant " +
                "data of the paper that you selected.\n" +
                "The program will try to get the necessary data, but you will have to verify its accuracy");
        this.executorService = Executors.newSingleThreadExecutor(new MyThreadFactory());
        Worker task = new Worker(file, 0);
        executorService.submit(task);

    }


    /**
     * Helper method to set the twin files
     *
     * @param file file to set
     * @param num  1 if its the first twin, 2 if its the second twin
     * @return File
     */
    private void setTwinFileHelper(File file, int num) {
        try {
            FileFormatter.setFile(file);
        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }

        Platform.runLater(() -> {
            Label instructions = new Label("Is this information correct for file \"" + file.getName() + "\" ?");
            Label currentInfo = new Label(FileFormatter.getCurrentInfo());
            currentInfo.setStyle("-fx-text-alignment: center");
            instructions.setTextAlignment(TextAlignment.CENTER);
            guiLabelManagement.clearOutputPanel();
            HBox hBox = new HBox(10);
            hBox.setAlignment(Pos.CENTER);
            JFXButton no = new JFXButton("No");
            JFXButton yes = new JFXButton("Yes");
            hBox.getChildren().addAll(no, yes);

            //Add everything into a vBox
            VBox vBox = new VBox(10);
            vBox.setAlignment(Pos.CENTER);
            vBox.getChildren().addAll(instructions, currentInfo, hBox);
            guiLabelManagement.setNodeToAddToOutputPanel(vBox);

            guiLabelManagement.setOutputPanelSpacing(10);


            yes.setOnAction(e -> {
                //If user clicks yes, then we are done and we set that as the file
                if (num == 0) {
                    file1 = file;
                    file2 = file;
                } else if (num == 1) {
                    file1 = file;
                } else {
                    file2 = file;
                }
                finish(num);
            });


            no.setOnAction(e -> {
                //if user clicks no, he has 2 options, either to let the program try to get the right information, or to
                //input the information manually.
                guiLabelManagement.clearOutputPanel();
                Label instructions2 = new Label("Select an option:");
                JFXButton letProgramAnalyze = new JFXButton("Let the program try to get the right information");
                JFXButton inputManually = new JFXButton("I want to input the information manually");
                VBox vBox2 = new VBox(10);
                vBox2.setAlignment(Pos.CENTER);
                vBox2.getChildren().addAll(instructions2, letProgramAnalyze, inputManually);
                guiLabelManagement.setNodeToAddToOutputPanel(vBox2);
                guiLabelManagement.setOutputPanelSpacing(10);


                //Let the program get the info
                letProgramAnalyze.setOnAction(event -> getAnalyzedTitle(file, num));
                //Let the user put the info
                inputManually.setOnAction(event -> inputInfoManually(num));

            });
        });

    }

    /**
     * By this point, we have all the necessary metadata of the twin paper to start checking if other papers cite it
     *
     * @param num 0 if it is Single Article Mode, 1 or more for Twin Article mode
     */
    private void finish(int num) {
        //If it is Single Article Mode
        if (num == 0) {
            //Display the gathered data
            guiLabelManagement.clearOutputPanel();
            guiLabelManagement.setStatus("File has been set.");
            infoFile1 = FileFormatter.getCurrentInfo();
            Label currentInfoFile1 = new Label("File information:\n" + infoFile1);
            Label nextStep = new Label("Now click on 'Set Folder to Compare'");
            nextStep.setTextAlignment(TextAlignment.CENTER);
            currentInfoFile1.setStyle("-fx-text-alignment: center");
            guiLabelManagement.setNodeToAddToOutputPanel(currentInfoFile1);
            guiLabelManagement.setNodeToAddToOutputPanel(nextStep);

            guiLabelManagement.disableFolderButton(false);
            try {
                FileFormatter.closeFile();
            } catch (IOException e2) {
                guiLabelManagement.setAlertPopUp(e2.getMessage());
            }
            controller.setTwinFile1(file1);
            controller.setTwinFile2(file2);

        }
        //If it is Twin Article Mode
        else if (num == 1) {
            guiLabelManagement.setStatus("Twin 1 has been set");
            infoFile1 = FileFormatter.getCurrentInfo();
            try {
                FileFormatter.closeFile();
            } catch (IOException e2) {
                guiLabelManagement.setAlertPopUp(e2.getMessage());
            }
            Worker task = new Worker(file2, 2);
            executorService.submit(task);
        } else {
            //We have already configured the 1st file of Twin Article Mode, so now we configure the second one
            String infoFile2 = FileFormatter.getCurrentInfo();
            try {
                FileFormatter.closeFile();
            } catch (IOException e2) {
                guiLabelManagement.setAlertPopUp(e2.getMessage());
            }

            //Set the twin files
            controller.setTwinFile1(file1);
            controller.setTwinFile2(file2);
            //Display the information
            guiLabelManagement.setStatus("Both twin files have been set!");
            guiLabelManagement.clearOutputPanel();
            Label currentInfoFile1 = new Label("Twin 1\n" + infoFile1);
            currentInfoFile1.setStyle("-fx-text-alignment: center");
            Label currentInfoFile2 = new Label("Twin 2\n" + infoFile2);
            currentInfoFile2.setStyle("-fx-text-alignment: center");
            Label nextStep = new Label("Now click on 'Set Folder to Compare'");
            nextStep.setTextAlignment(TextAlignment.CENTER);
            guiLabelManagement.setNodeToAddToOutputPanel(currentInfoFile1);
            guiLabelManagement.setNodeToAddToOutputPanel(currentInfoFile2);
            guiLabelManagement.setNodeToAddToOutputPanel(nextStep);

            guiLabelManagement.disableFolderButton(false);

        }
        done = true;
    }


    /**
     * Using context clues, the program tries to extract the title of the paper.
     */
    private void getAnalyzedTitle(File file, int num) {
        //Program will try to find the right information based on context cues
        guiLabelManagement.setStatus("Loading...");
        DocumentParser documentParser;
        try {
            //Analyze the paper
            documentParser = new DocumentParser(file, false, true);
            System.out.println(documentParser.smallestFont);
            System.out.println(documentParser.largestFont);
            guiLabelManagement.clearOutputPanel();
            //Display the relevant information
            Label instructions = new Label("Is this the title of the document?");
            Label possibleTitle = new Label(documentParser.getTitle());
            possibleTitle.setStyle("-fx-text-alignment: center");
            HBox hBox = new HBox(10);
            hBox.setAlignment(Pos.CENTER);
            JFXButton no = new JFXButton("No");
            JFXButton yes = new JFXButton("Yes");
            hBox.getChildren().addAll(no, yes);
            //Add everything to vBox
            VBox vBox = new VBox(10);
            vBox.setAlignment(Pos.CENTER);
            vBox.getChildren().addAll(instructions, possibleTitle, hBox);
            guiLabelManagement.setNodeToAddToOutputPanel(vBox);


            //If the user clicks no, then we let him manually input the information
            no.setOnAction(event -> inputInfoManually(num));

            //If the information is correct, we save it into the file
            DocumentParser finalDocumentParser = documentParser;
            yes.setOnAction(event -> {
                FileFormatter.addTitle(possibleTitle.getText());
                getAnalyzedAuthors(finalDocumentParser, num);

            });
        } catch (IOException e2) {
            guiLabelManagement.setAlertPopUp("There was an error parsing the file");
        }


    }

    /**
     * Using context clues, the analyzer will try to capture the authors names
     */
    private void getAnalyzedAuthors(DocumentParser documentParser, int num) {
        String possAuthors = "";
        try {
            //Get the possible authors names
            possAuthors = documentParser.getAuthors();
            possAuthors = authorNamesValidator(possAuthors);
        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }
        //Display information
        guiLabelManagement.clearOutputPanel();
        Label instructions = new Label("Are these the authors of the document:");
        Label possibleTitle = new Label(possAuthors);
        possibleTitle.setStyle("-fx-text-alignment: center");
        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        JFXButton no = new JFXButton("No");
        JFXButton yes = new JFXButton("Yes");
        hBox.getChildren().addAll(no, yes);

        //Add everything to vBox
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(instructions, possibleTitle, hBox);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);


        no.setOnAction(event -> inputAuthorManually(num));

        String finalPossAuthors = possAuthors;
        yes.setOnAction(event -> {
            FileFormatter.addAuthors(finalPossAuthors);
            setYear(documentParser, num);
        });
    }


    /**
     * Ask the user if they want to manually input the year or let the program try to retrieve it.
     */
    private void setYear(DocumentParser documentParser, int num) {
        if (documentParser == null) {
            manuallyWriteYear(num);
        } else {
            //Ask program to analyze the info
            //Manually input info
            guiLabelManagement.clearOutputPanel();
            final String[] year = {documentParser.getYear()};
            if (year[0] == null || year[0].isEmpty()) {
                guiLabelManagement.clearOutputPanel();
                manuallyWriteYear(num);
            } else {
                Label instruction = new Label("Is this the year the article was published?");
                Label yearLabel = new Label(year[0]);
                HBox hBox = new HBox(10);
                hBox.setAlignment(Pos.CENTER);
                JFXButton no = new JFXButton("No");
                JFXButton yes = new JFXButton("Yes");
                hBox.getChildren().addAll(no, yes);

                VBox vBox = new VBox(10);
                vBox.setAlignment(Pos.CENTER);
                vBox.getChildren().addAll(instruction, yearLabel, hBox);
                guiLabelManagement.setNodeToAddToOutputPanel(vBox);


                no.setOnAction(event -> manuallyWriteYear(num));
                yes.setOnAction(event -> {
                    FileFormatter.addYear(year[0]);
                    try {
                        documentParser.close();
                    } catch (IOException e) {
                        guiLabelManagement.setAlertPopUp(e.getMessage());
                    }
                    finish(num);
                });
            }
        }
    }

    private void manuallyWriteYear(int num) {
        //Manually input info
        guiLabelManagement.clearOutputPanel();
        Label instruction = new Label("Please write the year the paper was published:");
        JFXTextField textInput = new JFXTextField();
        textInput.setStyle("-fx-text-alignment: center;" +
                "-fx-alignment: center");
        JFXButton submit = new JFXButton("Submit");

        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(instruction, textInput, submit);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);


        submit.setOnAction(event -> {
            String yearStr = textInput.getText();
            try {
                Integer.valueOf(yearStr);
                FileFormatter.addYear(yearStr);
                finish(num);

            } catch (NumberFormatException e) {
                guiLabelManagement.setAlertPopUp("Please only write numbers here");
            }

        });
    }


    /**
     * Displays a text input for the use to write the title of the document
     */
    private void inputInfoManually(int num) {
        guiLabelManagement.clearOutputPanel();
        Label instructions = new Label("Please write the title of the document. You don't need to write more than the" +
                " first 10 words. \n" +
                "Ex: Apoptosis control by death and decoy receptors");
        instructions.setStyle("-fx-text-alignment: center;" +
                "-fx-spacing: 5");
        JFXTextField textInput = new JFXTextField();
        textInput.setStyle("-fx-text-alignment: center;" +
                "-fx-alignment: center");
        JFXButton submit = new JFXButton("Submit");

        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(instructions, textInput, submit);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);


        submit.setOnAction(event -> {
            if (textInput.getText().isEmpty()) {
                guiLabelManagement.setAlertPopUp("Please write the title");
            } else {
                FileFormatter.addTitle(textInput.getText());
                inputAuthorManually(num);
            }
        });

    }

    /**
     * Displays a text input for the user to write the names of the authors
     */
    private void inputAuthorManually(int num) {
        guiLabelManagement.clearOutputPanel();
        Label instructions = new Label("Please write the names of the authors, as it appears on the paper, but " +
                "without special" +
                " symbols. Please include at least 3 authors." +
                "\n\nIf there are more than 3, write only the first 3 in the EXACT order that they appear on the " +
                "paper. SEPARATE them with ','" +
                "\nEx: Elizabeth Slee, Mary Harte, Ruth Kluck");
        instructions.setStyle("-fx-text-alignment: center;" +
                "-fx-spacing: 5");
        JFXTextField textInput = new JFXTextField();
        textInput.setStyle("-fx-text-alignment: center;" +
                "-fx-alignment: center");
        JFXButton submit = new JFXButton("Submit");

        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(instructions, textInput, submit);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);


        submit.setOnAction(event -> {
            if (textInput.getText().isEmpty()) {
                guiLabelManagement.setAlertPopUp("Please write the name of the authors");
            } else {
                String authors = authorNamesValidator(textInput.getText());
                FileFormatter.addAuthors(authors);
                setYear(null, num);
            }
        });
    }


    /**
     * Formats the author names correctly.
     */
    private String authorNamesValidator(String ans) {
        ans = ans.replaceAll("[\\n\\r]", "");
        ans = ans.replaceAll("[^A-z\\s-.,]", "");
        ans = ans.replaceAll("^[ \\t]+|[ \\t]+$", "");
        ans = ans.replaceAll(",\\.", "");
        ans = ans.replaceAll("\\b and", ",");
        ans = ans.replaceAll(" and\\b", "");
        ans = ans.replaceAll(" {2,}", " ");
        ans = ans.replaceAll(",,", ",");
        ans = ans.replaceAll(", ,", ",");

        String[] ansArray = ans.split(",");
        if (ansArray.length > 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                if (i == 2) {
                    sb.append(ansArray[i]);
                } else {
                    sb.append(ansArray[i]).append(", ");

                }
            }
            ans = sb.toString();
        }


        while (ans.endsWith(",")) {
            ans = ans.substring(0, ans.lastIndexOf(","));
        }
        while (ans.endsWith(".")) {
            ans = ans.substring(0, ans.lastIndexOf("."));
        }

        return ans;
    }

    /**
     * Task class use for setting up the files without pausing the GUI.
     */
    public class Worker extends Task<Void> {


        private final File file;
        private final int i;

        Worker(File file, int i) {
            this.file = file;
            this.i = i;
        }

        @Override
        protected Void call() throws Exception {
            setTwinFileHelper(file, i);
            while (!done) {
                Thread.sleep(500);
            }
            return null;
        }

        @Override
        protected void done() {
        }
    }

}






