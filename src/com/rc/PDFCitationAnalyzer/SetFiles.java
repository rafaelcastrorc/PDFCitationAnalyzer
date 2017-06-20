package com.rc.PDFCitationAnalyzer;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rafaelcastro on 6/19/17.
 */
class SetFiles {


    private Controller controller;
    private File file2;
    private ExecutorService executorService;
    private String infoFile1;
    private File file1;

    /**
     * Sets the metadata information of the twin files
     */
    void setTwinFiles(Controller controller, File file1, File file2) {
        this.file1 = file1;
        this.file2 = file2;
        this.controller = controller;
        controller.informationPanel("Not all pdf files are formatted the same. The program will try to " +
                "get the necessary data, but you will have to verify its accuracy");

        this.executorService = Executors.newSingleThreadExecutor(new MyThreadFactory());

        Worker task = new Worker(file1, 1);
        executorService.submit(task);

    }

    public void setSingleFile(Controller controller, File file) {
        this.controller = controller;
        controller.informationPanel("Not all pdf files are formatted the same. The program will try to " +
                "get the necessary data, but you will have to verify its accuracy");
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
            controller.displayAlert(e.getMessage());
        }

        Platform.runLater(() -> {
            Label instructions = new Label("Is this information correct for file \"" + file.getName() + "\" ?");
            Label currentInfo = new Label(FileFormatter.getCurrentInfo());
            currentInfo.setStyle("-fx-text-alignment: center");
            controller.getOutputPanel().getChildren().clear();
            HBox hBox = new HBox(10);
            hBox.setAlignment(Pos.CENTER);
            JFXButton no = new JFXButton("No");
            JFXButton yes = new JFXButton("Yes");
            hBox.getChildren().addAll(no, yes);
            controller.getOutputPanel().getChildren().addAll(instructions, currentInfo, hBox);
            controller.getOutputPanel().setSpacing(10);


            yes.setOnAction(e -> {
                //If user clicks yes, then we are done and we set that as the file
                if (num == 0) {
                    file1 = file;
                    file2 = file;
                }
                else if (num == 1) {
                    file1 = file;
                } else {
                    file2 = file;
                }
                finish(num);
            });


            no.setOnAction(e -> {
                //if user clicks no, he has 2 options, either to let the program try to get the right information, or to
                //input the information manually.
                controller.getOutputPanel().getChildren().clear();
                Label instructions2 = new Label("Select an option:");
                JFXButton letProgramAnalyze = new JFXButton("Let the program try to get the right information");
                JFXButton inputManually = new JFXButton("I want to input the information manually");
                controller.getOutputPanel().getChildren().addAll(instructions2, letProgramAnalyze, inputManually);
                controller.getOutputPanel().setSpacing(10);


                //Let the program get the info
                letProgramAnalyze.setOnAction(event -> getAnalyzedTitle(file, num));

                //Let the user put the info
                inputManually.setOnAction(event -> inputInfoManually(file, num));

            });
        });

    }

    private void finish(int num) {
        if (num ==0) {
            controller.getOutputPanel().getChildren().clear();
            controller.updateStatus("File has been set.");
            infoFile1 = FileFormatter.getCurrentInfo();
            Label currentInfoFile1 = new Label("File information:\n"+ infoFile1);
            currentInfoFile1.setStyle("-fx-text-alignment: center");
            controller.getOutputPanel().getChildren().addAll(currentInfoFile1);
            controller.getSetFolder().setDisable(false);
            try {
                FileFormatter.closeFile();
            } catch (IOException e2) {
                controller.displayAlert(e2.getMessage());
            }
            controller.setTwinFile1(file1);
            controller.setTwinFile2(file2);



        }
        else if (num == 1) {
            controller.updateStatus("Twin 1 has been set");
            infoFile1 = FileFormatter.getCurrentInfo();
            try {
                FileFormatter.closeFile();
            } catch (IOException e2) {
                controller.displayAlert(e2.getMessage());
            }
            Worker task = new Worker(file2, 2);
            executorService.submit(task);
        } else {
            String infoFile2 = FileFormatter.getCurrentInfo();
            try {
                FileFormatter.closeFile();
            } catch (IOException e2) {
                controller.displayAlert(e2.getMessage());
            }

            controller.updateStatus("Both twin files have been set!");
            controller.getOutputPanel().getChildren().clear();
            Label currentInfoFile1 = new Label("Twin 1\n"+ infoFile1);
            currentInfoFile1.setStyle("-fx-text-alignment: center");
            Label currentInfoFile2 = new Label("Twin 2\n"+ infoFile2);
            currentInfoFile2.setStyle("-fx-text-alignment: center");

            controller.getOutputPanel().getChildren().addAll(currentInfoFile1, currentInfoFile2);


            controller.getSetFolder().setDisable(false);

        }
    }


    void getAnalyzedTitle(File file, int num) {
        //Program will try to find the right information based on context cues
        controller.updateStatus("Loading...");
        DocumentParser documentParser = null;
        try {
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e2) {
            controller.displayAlert("There was an error parsing the file");
        }

        System.out.println(documentParser.smallestFont);
        System.out.println(documentParser.largestFont);

        controller.getOutputPanel().getChildren().clear();
        Label instructions = new Label("Is this the title of the document?");
        Label possibleTitle = new Label(documentParser.getTitle());
        possibleTitle.setStyle("-fx-text-alignment: center");
        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        JFXButton no = new JFXButton("No");
        JFXButton yes = new JFXButton("Yes");
        hBox.getChildren().addAll(no, yes);
        controller.getOutputPanel().getChildren().addAll(instructions, possibleTitle, hBox);

        //If the user clicks no, then we let him manually input the information
        no.setOnAction(event -> inputInfoManually(file, num));

        //If the information is correct, we save it into the file
        DocumentParser finalDocumentParser = documentParser;
        yes.setOnAction(event -> {
            FileFormatter.addTitle(possibleTitle.getText());
            getAnalyzedAuthors(finalDocumentParser, file, num);

        });


    }

    void getAnalyzedAuthors(DocumentParser documentParser, File file, int num) {
        String possAuthors = "";
        try {
            //Get the possible authors names
            possAuthors = documentParser.getAuthors();
        } catch (IOException e) {
            controller.displayAlert(e.getMessage());
        }
        controller.getOutputPanel().getChildren().clear();
        Label instructions = new Label("Are these the authors of the document:");
        Label possibleTitle = new Label(possAuthors);
        possibleTitle.setStyle("-fx-text-alignment: center");
        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        JFXButton no = new JFXButton("No");
        JFXButton yes = new JFXButton("Yes");
        hBox.getChildren().addAll(no, yes);
        controller.getOutputPanel().getChildren().addAll(instructions, possibleTitle, hBox);

        no.setOnAction(event -> inputAuthorManually(num));

        String finalPossAuthors = possAuthors;
        yes.setOnAction(event -> {
            FileFormatter.addAuthors(finalPossAuthors);
            setYear(documentParser, num);
        });
    }


    //Ask for year the doc was published
    private void setYear(DocumentParser documentParser, int num) {
        if (documentParser == null) {
            manuallyWriteYear(num);
        } else {
            //Ask program to analyze the info
            //Manually input info
            controller.getOutputPanel().getChildren().clear();
            final String[] year = {documentParser.getYear()};
            if (year[0] == null || year[0].isEmpty()) {
                controller.getOutputPanel().getChildren().clear();
                manuallyWriteYear(num);
            } else {
                Label instruction = new Label("Is this the year the article was published?");
                Label yearLabel = new Label(year[0]);
                HBox hBox = new HBox(10);
                hBox.setAlignment(Pos.CENTER);
                JFXButton no = new JFXButton("No");
                JFXButton yes = new JFXButton("Yes");
                hBox.getChildren().addAll(no, yes);

                controller.getOutputPanel().getChildren().addAll(instruction, yearLabel, hBox);

                no.setOnAction(event -> manuallyWriteYear(num));
                yes.setOnAction(event -> {
                    FileFormatter.addYear(year[0]);
                    try {
                        documentParser.close();
                    } catch (IOException e) {
                        controller.displayAlert(e.getMessage());
                    }
                    finish(num);
                });
            }
        }
    }

    private void manuallyWriteYear(int num) {
        //Manually input info
        controller.getOutputPanel().getChildren().clear();
        Label instruction = new Label("Please write the year the paper was published:");
        JFXTextField textInput = new JFXTextField();
        textInput.setStyle("-fx-text-alignment: center;" +
                "-fx-alignment: center");
        JFXButton submit = new JFXButton("Submit");
        controller.getOutputPanel().getChildren().addAll(instruction, textInput, submit);

        submit.setOnAction(event -> {
            String yearStr = textInput.getText();
            try {
                Integer.valueOf(yearStr);
                FileFormatter.addYear(yearStr);
                finish(num);

            } catch (NumberFormatException e) {
                controller.displayAlert("Please only write numbers here");
            }

        });
    }


    private void inputInfoManually(File file, int num) {
        controller.getOutputPanel().getChildren().clear();
        Label instructions = new Label("Please write the title of the document. You don't need to write more than the first 10 words. \n" +
                "Ex: Apoptosis control by death and decoy receptors");
        instructions.setStyle("-fx-text-alignment: center;" +
                "-fx-spacing: 5");
        JFXTextField textInput = new JFXTextField();
        textInput.setStyle("-fx-text-alignment: center;" +
                "-fx-alignment: center");
        JFXButton submit = new JFXButton("Submit");
        controller.getOutputPanel().getChildren().addAll(instructions, textInput, submit);
        submit.setOnAction(event -> {
            if (textInput.getText().isEmpty()) {
                controller.displayAlert("Please write the title");
            } else {
                FileFormatter.addTitle(textInput.getText());
                inputAuthorManually(num);
            }
        });

    }

    void inputAuthorManually(int num) {
        controller.getOutputPanel().getChildren().clear();
        Label instructions = new Label("Please write the names of the authors, as it appears on the paper, but without special" +
                " symbols. Please include at least 3 authors." +
                "\n\nIf there are more than 3, write only the first 3 in the EXACT order that they appear on the paper. SEPARATE them with ','" +
                "\nEx: Elizabeth Slee, Mary Harte, Ruth Kluck");
        instructions.setStyle("-fx-text-alignment: center;" +
                "-fx-spacing: 5");
        JFXTextField textInput = new JFXTextField();
        textInput.setStyle("-fx-text-alignment: center;" +
                "-fx-alignment: center");
        JFXButton submit = new JFXButton("Submit");
        controller.getOutputPanel().getChildren().addAll(instructions, textInput, submit);
        submit.setOnAction(event -> {
            if (textInput.getText().isEmpty()) {
                controller.displayAlert("Please write the name of the authors");
            } else {
                FileFormatter.addAuthors(textInput.getText());
                setYear(null, num);
            }
        });
    }




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


            return null;
        }
    }
}






