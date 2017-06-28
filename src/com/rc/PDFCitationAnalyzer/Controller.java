package com.rc.PDFCitationAnalyzer;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;


import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rafaelcastro on 5/16/17.
 * Controls the view and retrieves information from the parser
 */


//Todo: Only accept up to three author names, if file cannot be read, and if file cannot find citation
public class Controller implements Initializable {
    private Logger log;
    private File twinFile1;
    ExecutorService executorService = Executors.newSingleThreadExecutor(new MyThreadFactory());
    private File twinFile2;
    private File[] comparisonFiles;
    private Window window;
    private TreeMap<Integer, ArrayList<Object>> dataGathered;

    @FXML
    private Label titleLabel;
    @FXML
    private JFXRadioButton twinFiles;
    @FXML
    private JFXRadioButton singleFile;
    @FXML
    private VBox outputPanel;
    @FXML
    private Label statusLabel;
    @FXML
    private JFXButton analyzeData;
    @FXML
    private JFXButton outputResults;
    @FXML
    private JFXButton setFolder;

    void setTwinFile1(File twinFile1) {
        this.twinFile1 = twinFile1;
    }

    void setTwinFile2(File twinFile2) {
        this.twinFile2 = twinFile2;
    }


    JFXButton getSetFolder() {
        return setFolder;
    }


    public Controller() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateStatus("Ready to use.");
        titleLabel.getStyleClass().add("title-label");

    }

    @FXML
    void setFileOnClick(Event e) {
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        openFile();
    }

    /**
     * Handles the logic to upload a single file into the program.
     */
    private void openFile() {
        Platform.runLater(() -> {
            twinFiles.setSelected(false);
            FileChooser fileChooser = new FileChooser();
            configureFileChooser(fileChooser, "PDF files (*.pdf)","*.pdf");
            File file = fileChooser.showOpenDialog(window);
            if (file == null) {
                informationPanel("Please upload a file.");
                singleFile.setSelected(false);
            }
            else if (!file.exists() || !file.canRead()) {
                displayAlert("There was an error opening one of the files");
                singleFile.setSelected(false);
            }
            else {
                updateStatus("File has been submitted.");
                SetFiles setFiles = new SetFiles();
                setFiles.setSingleFile(this, file);
                updateStatus("File has been set.");

            }
        });
    }

    @FXML
    void setTwinFilesOnClick(Event e) {
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        openTwinFiles();
    }

    /**
     * Handles the logic to upload twin files into the program.
     */
    private void openTwinFiles() {
        Platform.runLater(() -> {
            singleFile.setSelected(false);
            FileChooser fileChooser = new FileChooser();
            configureFileChooser(fileChooser, "PDF files (*.pdf)","*.pdf");
            List<File> files = fileChooser.showOpenMultipleDialog(window);
            if (files == null) {
                informationPanel("Please upload a file.");
                twinFiles.setSelected(false);
            } else if (files.size() > 2) {
                displayAlert("You cannot upload more than 2 files");
                twinFiles.setSelected(false);
            }
            else if (files.size() < 2) {
                displayAlert("You need 2 files");
                twinFiles.setSelected(false);
            }
            else if (!files.get(0).exists() || !files.get(0).canRead() || !files.get(1).exists() || !files.get(1).canRead() ) {
                displayAlert("There was an error opening one of the files");
                twinFiles.setSelected(false);
            } else if (files.get(0).length() < 1 || files.get(1).length() < 1) {
                displayAlert("One of the files is empty");
                twinFiles.setSelected(false);
            } else {
                updateStatus("File has been submitted.");
                SetFiles setFiles = new SetFiles();
                setFiles.setTwinFiles(this, files.get(0), files.get(1));
                updateStatus("File have been set.");

            }
        });
    }

    /**
     * Configures the types of files that are allowed to be upload (.txt or .csv)
     *
     * @param fileChooser the current fileChooser
     */
    private void configureFileChooser(FileChooser fileChooser, String description, String extension) {
        fileChooser.setTitle("Please select the two twin files");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(description, extension);
        fileChooser.getExtensionFilters().add(extFilter);

    }



    @FXML
    public void setFolderOnClick(ActionEvent event) {
        Node node = (Node) event.getSource();
        window = node.getScene().getWindow();
        informationPanel("Make sure that all the files are in the same folder.");
        openFolder();
    }

    /**
     * Handles the logic to upload a folder into the program.
     */
    private void openFolder() {
        Platform.runLater(() -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(window);
            if(selectedDirectory == null){
                informationPanel("Please upload a file.");
            }else {
                if (!selectedDirectory.exists()) {
                    displayAlert("Folder does not exist");
                } else if (!selectedDirectory.canRead()) {
                    displayAlert("Folder cannot be read");
                } else {
                    File[] listOfFiles = selectedDirectory.listFiles();
                    if (listOfFiles == null) {
                        displayAlert("The file need to be inside of a folder");
                    } else if (listOfFiles.length < 1) {
                        displayAlert("There are no files in this folder");
                    } else {
                        for (File curr : listOfFiles) {
                            if (!curr.getName().equals(".DS_Store")) {
                                if (!curr.exists() || !curr.canRead()) {
                                    displayAlert(curr.getName() + " is not a valid file");
                                    break;
                                } else {
                                    comparisonFiles = listOfFiles;
                                    updateStatus("The folder has been set.");
                                    analyzeData.setDisable(false);
                                }
                            }
                        }
                    }
                }
            }
        });
    }


    @FXML
    void analyzeDataOnClick() {
        getOutputPanel().getChildren().clear();

        Thread.UncaughtExceptionHandler h = (th, ex) -> System.out.println("Uncaught exception: " + ex);

        MyTask task = new MyTask();
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(h);

        try {
            thread.start();
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    @FXML
    void outputResultsOnClick() {
        ArrayList<Object> list = new ArrayList<>();
        //Headers of the excel output file
        list.add("Paper");
        list.add("Number cites A");
        list.add("Number cites B");
        list.add("Number cites A&B");
        list.add("Adjacent-Cit Rate");
        dataGathered.put(0, list);
        outputToFile();

    }


    /**
     * Displays a pop up alert message
     *
     * @param message String with the message to display
     */
     void displayAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });

    }

    /**
     * Creates a pop up message that says Loading...
     */
    void informationPanel(String s) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(s);
        alert.showAndWait();
    }

    /**
     * Updates the status label of the Single Article mode
     *
     * @param message String with the message to output
     */
    void updateStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
        });
    }


    VBox getOutputPanel() {
        return outputPanel;
    }


    private void outputToFile() {
        FileOutput output = new FileOutput();
        try {
            output.writeToFile(dataGathered);
            updateStatus("The report has been created! ");

        } catch (IOException e) {
            displayAlert("There was an error trying to open the file. Make sure the file exists. ");
        }
    }


    class MyTask extends Task<Void>{

        @Override
        protected Void call() throws Exception {
            updateStatus("Analyzing...");
            Platform.runLater(() -> {
                analyzeData.setDisable(true);
                outputResults.setDisable(true);});

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setStyle("-fx-alignment: center;" +
                    "-fx-background-color: transparent;" +
                    "-fx-text-alignment: justify");

            Text text = new Text("Analyzing the files...");
            scrollPane.setContent(text);
            Platform.runLater(() -> getOutputPanel().getChildren().add(scrollPane));
            Thread.sleep(2000);
            System.out.println("Method called");
            FileAnalyzer fileAnalyzer = new FileAnalyzer(Controller.this, comparisonFiles, twinFile1, twinFile2);
            try {
                fileAnalyzer.analyzeFiles();
            } catch (Error e) {
                throw new IllegalArgumentException(e.getMessage());
            }
            System.out.println("here");
            dataGathered = fileAnalyzer.getDataGathered();
            outputResults.setDisable(false);
            return null;
        }

        @Override
        protected void done() {
            try {
                if (!isCancelled()) get();
            } catch (ExecutionException e) {
                // Exception occurred, deal with it
                System.out.println("Exception: " + e.getCause());
            } catch (InterruptedException e) {
                // Shouldn't happen, we're invoked when computation is finished
                throw new AssertionError(e);
            }

            Platform.runLater(() -> analyzeData.setDisable(false));
        }
    }



}
