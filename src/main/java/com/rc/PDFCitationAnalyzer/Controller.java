package com.rc.PDFCitationAnalyzer;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;


import javafx.event.ActionEvent;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.Security;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by rafaelcastro on 5/16/17.
 * Controls the view and retrieves information from the parser
 */


public class Controller implements Initializable {
    private Logger log;
    private File twinFile1;
    private File twinFile2;
    private File[] comparisonFiles;
    private Window window;
    private TreeMap<Integer, ArrayList<Object>> dataGathered;
    private GUILabelManagement guiLabelManagement = new GUILabelManagement();

    private Text outputText;

    @FXML
    private ProgressIndicator progressIndicator;
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
    private PDFComparator comparator;
    private TwinOrganizer twinOrganizer;
    private PDFCounter pdfCounter;
    private boolean organizeDuplicates;
    private MultipleFilesSetup multipleFilesSetup;

    public Controller() {
        guiLabelManagement.getAlertPopUp().addListener((observable, oldValue, newValue) -> displayAlert(newValue));
        guiLabelManagement.getProgressIndicator().addListener((observable, oldValue, newValue) ->
                updateProgressIndicator(newValue.doubleValue()));
    }

    void setTwinFile1(File twinFile1) {
        this.twinFile1 = twinFile1;
    }

    void setTwinFile2(File twinFile2) {
        this.twinFile2 = twinFile2;
    }

    JFXButton getSetFolderButton() {
        return setFolder;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateStatus("Ready to use.");
        titleLabel.getStyleClass().add("title-label");
        Security.addProvider(new BouncyCastleProvider());
        Platform.runLater(() ->{
            progressIndicator = new ProgressIndicator();
            progressIndicator.setStyle("-fx-alignment: center;" +
                    "-fx-progress-color: #990303");
            progressIndicator.setMinHeight(190);
            progressIndicator.setMinWidth(526);
        });

    }

    /**
     * Call when the user clicks on Get PDFs Titles
     *
     * @param e Event
     */
    @FXML
    void getTitlesOnClick(Event e) {
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        informationPanel("Please select the folder that contains only the PDF(s) file(s) that you want, in order to " +
                "extract the title(s)");
        openFolder("titles");
    }



    /**
     * Call when the user clicks on Multiple Pairs of Twins
     *
     * @param e Event
     */
    @FXML
    void multiplePairsOnClick(Event e) {
        Node node = (Node) e.getSource();
        getOutputPanel().getChildren().clear();

        window = node.getScene().getWindow();
        informationPanel("Please select the excel file containing the multiple pairs of twins. The file should " +
                "include, in the following order: pairID, pair#, longID, Title, Year, Authors" +
                ".");
        openFile("excel");
        getOutputPanel().getChildren().clear();
        Text text = new Text("Please Wait! \nProcessing file...");
        text.setStyle("-fx-font-size: 24");
        text.setWrappingWidth(400);
        text.setTextAlignment(TextAlignment.CENTER);
        Platform.runLater(() -> getOutputPanel().getChildren().add(text));
        informationPanel("Please select a directory that contains multiple folders, where each folder contains" +
                " a pair of folders to analyze.");
        openFolder("multipleComparison");


    }


    /**
     * Call when the user clicks on Count Number of PDF(s)
     *
     * @param e Event
     */
    @FXML
    void countNumberOfPDFSOnClick(Event e) {
        Node node = (Node) e.getSource();
        getOutputPanel().getChildren().clear();

        window = node.getScene().getWindow();
        informationPanel("Please select the directory that contains the PDFs.\nThe program can count all the PDFs, " +
                "or the number of folders that contain a PDF.");
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        JFXButton totalNumber = new JFXButton("Count total number of PDFs");
        JFXButton numberOfFolders = new JFXButton("Count number of folders\nthat contain a PDF");
        numberOfFolders.setTextAlignment(TextAlignment.CENTER);

        vBox.getChildren().addAll(totalNumber, numberOfFolders);

        this.pdfCounter = new PDFCounter(this, guiLabelManagement);
        totalNumber.setOnAction(event -> {
            pdfCounter.setIsUniqueCount(false);
            openFolder("counter");

        });
        numberOfFolders.setOnAction(event -> {
            pdfCounter.setIsUniqueCount(true);
            openFolder("counter");
        });
        Platform.runLater(() -> getOutputPanel().getChildren().add(vBox));

    }

    /**
     * Call when the user clicks on Get PDFs Titles
     *
     * @param e Event
     */
    @FXML
    void comparePDFsOnClick(Event e) {
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        informationPanel("Please select the 2 folders that contain the PDFs that you want to compare. If you " +
                "have compared this two folders before, this will overwrite the previous output.");
        getOutputPanel().getChildren().clear();
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        JFXButton firstDirButton = new JFXButton("Select the first directory");
        JFXButton secondDirButton = new JFXButton("Select the second directory");
        Text or = new Text("OR");
        or.setStyle("-fx-font-size: 22");
        or.setWrappingWidth(400);
        or.setTextAlignment(TextAlignment.CENTER);
        JFXButton multipleComparisonButton = new JFXButton("Compare multiple pairs");

        secondDirButton.setDisable(true);
        vBox.getChildren().addAll(firstDirButton, secondDirButton, or, multipleComparisonButton);

        //Display alert
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Please select an option");
        alert.setHeaderText(null);
        alert.setContentText("Do you want to save the files that appear in both folders (duplicates) in a new " +
                "location?");
        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No");
        alert.getButtonTypes().setAll(yes, no);

        //Wait for user input
        Optional<ButtonType> result = alert.showAndWait();
        this.organizeDuplicates = result.get() == yes;


        firstDirButton.setOnAction(event -> {
            openFolder("comparison");
            firstDirButton.setDisable(true);
            secondDirButton.setDisable(false);
            multipleComparisonButton.setDisable(true);
        });
        secondDirButton.setOnAction(event -> {
            multipleComparisonButton.setDisable(true);
            openFolder("comparison");
        });
        multipleComparisonButton.setOnAction(event -> {
            firstDirButton.setDisable(true);
            secondDirButton.setDisable(true);
            informationPanel("Please select a directory that contains multiple folders, where each folder contains" +
                    " a pair of folders to analyze. If you have compared a directory with the same name before, this " +
                    "will overwrite the data");
            openFolder("comparisonMultiple");


        });
        Platform.runLater(() -> getOutputPanel().getChildren().add(vBox));

    }

    /**
     * Organize the download files as pairs of twins
     *
     * @param e Event
     */
    @FXML
    void organizeTwinsOnClick(Event e) {
        //Display all the GUI
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        informationPanel("If two folders represent twin papers, then this function will put them under the same " +
                "folder based on their twin ID. If you have used this method before, it will only organize the new " +
                "files (the files that are not in the directory \"OrganizedFiles\").");
        getOutputPanel().getChildren().clear();
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        JFXButton directory = new JFXButton("Select the file containing the downloaded PDFs");
        JFXButton report = new JFXButton("Select the Report.txt");
        JFXButton excel = new JFXButton("Select the excel file containing the twin pairs");

        //Block the other buttons until the user sets the directory
        excel.setDisable(true);
        report.setDisable(true);

        vBox.getChildren().addAll(directory, report, excel);
        directory.setOnAction(event -> {
            openFolder("downloadedPDFs");
            report.setDisable(false);
            directory.setDisable(true);
        });
        report.setOnAction(event -> {
            openFile("report");
            report.setDisable(true);
            excel.setDisable(false);
        });
        excel.setOnAction(event -> openFile("CSV"));
        Platform.runLater(() -> getOutputPanel().getChildren().add(vBox));

    }

    /**
     * Sets a single PDF file to be analyzed
     *
     * @param e Event
     */
    @FXML
    void setFileOnClick(Event e) {
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        openFile("PDF");
    }

    /**
     * Handles the logic to upload a single file into the program.
     */
    private void openFile(String type) {
        Platform.runLater(() -> {
            twinFiles.setSelected(false);
            FileChooser fileChooser = new FileChooser();
            switch (type) {
                case "PDF":
                    configureFileChooser(fileChooser, "PDF files (*.pdf)", "*.pdf");
                    break;
                case "report":
                    configureFileChooser(fileChooser, "TXT files (*.txt)", "*.txt");
                    break;
                case "CSV":
                    configureFileChooser(fileChooser, "CSV files (*.csv)", "*.csv");
                    break;
                default:
                    configureFileChooser(fileChooser, "Excel file (*.xlsx)", "*.xlsx");
                    break;
            }
            File file = fileChooser.showOpenDialog(window);
            if (file == null) {
                informationPanel("Please upload a file.");
                singleFile.setSelected(false);
            } else if (!file.exists() || !file.canRead()) {
                displayAlert("There was an error opening one of the files");
                singleFile.setSelected(false);
            } else {
                updateStatus("File has been submitted.");
                switch (type) {
                    case "PDF":
                        SetFiles setFiles = new SetFiles();
                        setFiles.setSingleFile(this, file);
                        updateStatus("File has been set.");
                        break;
                    case "report":
                        twinOrganizer.setReport(file);
                        break;
                    case "CSV":
                        twinOrganizer.setCSV(file);
                        //Run the twin organizer once we have the csv file
                        Thread t = new MyThreadFactory().newThread(twinOrganizer);
                        t.start();
                        break;
                    default:
                        this.multipleFilesSetup = new MultipleFilesSetup(this, guiLabelManagement);
                        multipleFilesSetup.setUpFile(file);
                        break;
                }

            }
        });
    }

    /**
     * Call when the user clicks on the Twin Article button.
     *
     * @param e Event
     */
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
            configureFileChooser(fileChooser, "PDF files (*.pdf)", "*.pdf");
            List<File> files = fileChooser.showOpenMultipleDialog(window);
            if (files == null) {
                informationPanel("Please upload a file.");
                twinFiles.setSelected(false);
            } else if (files.size() > 2) {
                displayAlert("You cannot upload more than 2 files");
                twinFiles.setSelected(false);
            } else if (files.size() < 2) {
                displayAlert("You need 2 files");
                twinFiles.setSelected(false);
            } else if (!files.get(0).exists() || !files.get(0).canRead() || !files.get(1).exists() || !files.get(1)
                    .canRead()) {
                displayAlert("There was an error opening one of the files");
                twinFiles.setSelected(false);
            } else if (files.get(0).length() < 1 || files.get(1).length() < 1) {
                displayAlert("One of the files is empty");
                twinFiles.setSelected(false);
            } else {
                updateStatus("File has been submitted.");
                SetFiles setFiles = new SetFiles();
                setFiles.setTwinFiles(this, files.get(0), files.get(1), guiLabelManagement);
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
        openFolder("filesToAnalyze");
    }

    /**
     * Handles the logic to upload a folder into the program.
     *
     * @param type The type of use that the folder will have.
     */
    private void openFolder(String type) {
        Platform.runLater(() -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(window);
            if (selectedDirectory == null) {
                informationPanel("Please upload a file.");
            } else {
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
                        ArrayList<File> workingFiles = new ArrayList<File>();
                        for (File curr : listOfFiles) {
                            if (!curr.getName().equals(".DS_Store")) {
                                if (!curr.exists() || !curr.canRead()) {
                                    displayAlert(curr.getName() + " is not a valid file");
                                } else {
                                    workingFiles.add(curr);
                                }
                            }
                        }
                        listOfFiles = new File[workingFiles.size()];
                        listOfFiles = workingFiles.toArray(listOfFiles);
                        openFolderHelper(type, listOfFiles);

                    }
                }
            }
        });
    }

    /**
     * Sets the file according to the type
     *
     * @param type        type of use that the folder will have
     * @param listOfFiles list of files obtained from the folder
     */
    private void openFolderHelper(String type, File[] listOfFiles) {
        switch (type) {
            case "counter":
                //Counts the number of PDFs in a directory
                pdfCounter.setDirectory(listOfFiles);
                Thread t0 = new MyThreadFactory().newThread(pdfCounter);
                t0.start();
                pdfCounter = null;
                break;
            case "titles":
                //Extracts all the titles and creates an excel file
                TitleFinder tf = new TitleFinder(this, listOfFiles, guiLabelManagement);
                Thread t = new MyThreadFactory().newThread(tf);
                t.start();
                break;
            case "comparison":
                //Compares all the titles and checks for duplicates among two different directories.
                if (this.comparator == null) {
                    comparator = new PDFComparator(this, guiLabelManagement);
                    comparator.setOrganize(organizeDuplicates);
                }
                comparator.setDirectory(listOfFiles);
                if (comparator.isReady()) {
                    Thread t2 = new MyThreadFactory().newThread(comparator);
                    t2.start();
                    comparator = null;
                }
                break;
            case "comparisonMultiple":
                //Compares all the titles and checks for duplicates among two different directories.
                if (this.comparator == null) {
                    comparator = new PDFComparator(this, guiLabelManagement);
                }
                comparator.setOrganize(organizeDuplicates);
                comparator.setDirectoryMultiple(listOfFiles);
                Thread t2 = new MyThreadFactory().newThread(comparator);
                t2.start();
                comparator = null;
                break;
            case "downloadedPDFs":
                //Stores the downloaded PDFs that will be organized based on twin papers
                twinOrganizer = new TwinOrganizer(this, guiLabelManagement);
                twinOrganizer.setDownloadedPDFs(listOfFiles);
                updateStatus("The folder has been set.");
                break;
            case "multipleComparison":
                //Stores the folders that will be analyzed
                multipleFilesSetup.setUpFolder(listOfFiles);
                Thread t3 = new MyThreadFactory().newThread(multipleFilesSetup);
                t3.start();
                break;
            default:
                //Setup files to be analyzed
                comparisonFiles = listOfFiles;
                updateStatus("The folder has been set.");
                analyzeData.setDisable(false);
                break;
        }
    }


    @FXML
    void analyzeDataOnClick() {
        getOutputPanel().getChildren().clear();
        MyTask task = new MyTask();
        Thread t = new MyThreadFactory().newThread(task);
        t.start();
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
    private void displayAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });

    }

    /**
     * Updates the progress indicator
     *
     * @param currProgress double from 0 to 1 with the current progress
     */
    void updateProgressIndicator(Double currProgress) {
        Platform.runLater(() -> {
            progressIndicator.setProgress(currProgress);
        });

    }

    /**
     * Updates the output of the FileAnalyzer
     *
     * @param output message to output
     */
    void updateProgressOutput(String output) {
        Platform.runLater(() -> outputText.setText(output));
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
     * <p>
     * Todo: Add it as part of guilabelmanagement
     *
     * @param message String with the message to output
     */
    void updateStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
        });
    }


    /**
     * VBox, which holds the output panel
     *
     * @return vBox
     */
    VBox getOutputPanel() {
        return outputPanel;
    }


    private void outputToFile() {
        FileOutput output = new FileOutput();
        try {
            //Todo create thread to do this
            output.writeOutputToFile(dataGathered, "Report.xlsx");
            updateStatus("The report has been created! ");

        } catch (IOException e) {
            displayAlert("There was an error trying to open the file. Make sure the file exists. ");
        }
    }

    ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }


    class MyTask extends Task<Void> {

        @Override
        protected Void call() throws Exception {
            updateStatus("Analyzing...");
            Platform.runLater(() -> {
                analyzeData.setDisable(true);
                outputResults.setDisable(true);
            });
            outputText = new Text("Extracting the titles...");
            outputText.setStyle("-fx-font-size: 16");
            //Add the progress indicator and outputText to the output panel
            Platform.runLater(() -> getOutputPanel().getChildren().addAll(progressIndicator, outputText));
            Thread.sleep(2000);

            //Add listener
            guiLabelManagement.getOutput().addListener((observable, oldValue, newValue) ->
                    updateProgressOutput(newValue));

            FileAnalyzer fileAnalyzer = new FileAnalyzer(comparisonFiles, twinFile1, twinFile2, guiLabelManagement);

            try {
                fileAnalyzer.analyzeFiles();
            } catch (Error e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e.getMessage());
            }
            System.out.println("Done Analyzing");
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
                e.printStackTrace();
            } catch (InterruptedException e) {
                // Shouldn't happen, we're invoked when computation is finished
                throw new AssertionError(e);
            }
            Platform.runLater(() -> analyzeData.setDisable(false));
        }
    }


}
