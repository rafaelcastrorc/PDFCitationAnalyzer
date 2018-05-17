package com.rc.PDFCitationAnalyzer;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
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
 * Handles the GUI logic, and sends commands to the different objects.
 */

public class Controller implements Initializable {
    private File twinFile1;
    private File twinFile2;
    private File[] comparisonFiles;
    private Window window;
    private TreeMap<Integer, ArrayList<Object>> dataGathered;
    private GUILabelManagement guiLabelManagement = new GUILabelManagement();
    private Text progressOutputText = new Text();
    private PDFComparator comparator;
    private TwinOrganizer twinOrganizer;
    private PDFCounter pdfCounter;
    private boolean organizeDuplicates;
    private MultipleFilesSetup multipleFilesSetup;

    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label titleLabel;
    @FXML
    private JFXRadioButton twinArticlesAnalysis;
    @FXML
    private JFXRadioButton singleArticleAnalysis;
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
    @FXML
    private JFXButton showInstructionsMultipleTwins;
    @FXML
    private JFXButton configureMultipleTwinExcelFile;
    @FXML
    private JFXButton uploadMultipleTwinFile;


    public Controller() {
    }

    void setTwinFile1(File twinFile1) {
        this.twinFile1 = twinFile1;
    }

    void setTwinFile2(File twinFile2) {
        this.twinFile2 = twinFile2;
    }

    /**
     * Initializes the JavaFX application
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Bind all the GUI elements to the different Properties inside of the GUILabelManagement obj
        guiLabelManagement.getAlertPopUp().addListener((observable, oldValue, newValue) -> displayAlert(newValue));
        guiLabelManagement.getProgressIndicator().addListener((observable, oldValue, newValue) ->
                updateProgressIndicator(newValue.doubleValue()));
        guiLabelManagement.storeProgressIndicator(getProgressIndicator());
        guiLabelManagement.getDisableFolderButton().addListener((observable, oldValue, newValue) ->
                enableFolderButton(newValue));
        guiLabelManagement.getNodesToAddToOutputPanel().addListener((observable, oldValue, newValue) ->
                addElementsToOutputPanel(newValue));
        guiLabelManagement.getProgressOutput().addListener((observable, oldValue, newValue) ->
                updateProgressOutput(newValue));
        guiLabelManagement.getInformationPanel().addListener((observable, oldValue, newValue) -> informationPanel
                (newValue));
        guiLabelManagement.getStatus().addListener((observable, oldValue, newValue) -> updateStatus(newValue));
        guiLabelManagement.getTwinFilesAnalysisDeselected().addListener((observable, oldValue, newValue) ->
                deselectTwinFilesAnalysis());
        guiLabelManagement.getSingleFileAnalysisDeselected().addListener((observable, oldValue, newValue) ->
                deselectSingleFileAnalysis());
        guiLabelManagement.getAnalyzeDataButton().addListener((observable, oldValue, newValue) ->
                disableAnalyzeDataButton(newValue));
        guiLabelManagement.getOuputResultsButton().addListener((observable, oldValue, newValue) ->
                disableOutputResultsButton(newValue));
        guiLabelManagement.setStatus("Ready to use.");
        titleLabel.getStyleClass().add("title-label");
        changeProgressOutputStyle("-fx-font-size: 16");
        changeProgressOutputStyle("-fx-text-alignment: center");
        //To read PDFs that have security settings
        Security.addProvider(new BouncyCastleProvider());
        showInstructions();
    }

    /**
     * Displays the main instructions in the Output Panel
     */
    private void showInstructions() {
        Platform.runLater(() -> {
            guiLabelManagement.clearOutputPanel();
            VBox vBox = new VBox(10);
            vBox.setAlignment(Pos.CENTER);
            Label header = new Label("Instructions to Analyze Multiple Pairs of Twins");
            header.setStyle("-fx-font-size: 18");
            ArrayList<Label> labels = new ArrayList<>();
            Label step0 = new Label("Step 0: Configure the format of the Excel file containing the multiple pairs of " +
                    "twins.");
            Label step1 = new Label("Step 1: Upload the Excel (.xlsx) file.");
            Label step2 = new Label("Step 2: If you haven't organized the files inside of the DownloadedPDFs folder, " +
                    "click on 'Arrange Twin Files'");
            Label step3 = new Label("Step 3: Click the 'Multiple Pairs of Twins' button");
            labels.add(header);
            labels.add(step0);
            labels.add(step1);
            labels.add(step2);
            labels.add(step3);

            //Format all labels (except for the header label)
            for (int i = 1; i < labels.size(); i++) {
                Label curr = labels.get(i);
                curr.setTextAlignment(TextAlignment.CENTER);
                curr.setStyle("-fx-font-size: 14");
                //Replace it
                labels.set(i, curr);
            }
            vBox.getChildren().addAll(labels);
            guiLabelManagement.setNodeToAddToOutputPanel(vBox);
        });
    }


    /**
     * Call when the user clicks on Get PDFs Titles
     * Retrieves the titles of one or more PDFs.
     *
     * @param e Event
     */
    @FXML
    void getTitlesOnClick(Event e) {
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        guiLabelManagement.setInformationPanel(String.format("This function extracts the titles of 1 or more PDFs and" +
                " outputs the titles into the file 'Titles.xlsx'.\nNote that the PDFs need to be in the same folder" +
                ".\n\nPlease select the folder that contains only the PDF(s) file(s) that you want, in order to " +
                "extract the title(s)."));
        openFolder("titles");
    }


    /**
     * Call when the user clicks on Multiple Pairs of Twins
     * Analyzes multiple pairs of twin articles.
     *
     * @param e Event
     */
    @FXML
    void multiplePairsAnalysisOnClick(Event e) {
        Node node = (Node) e.getSource();

        window = node.getScene().getWindow();
        guiLabelManagement.setInformationPanel("Please select the excel file containing the multiple pairs of twins. " +
                "The file should " +
                "include, in the following order: pairID, pair#, longID, Title, Year, Authors" +
                ".");
        openFile("excel");
        guiLabelManagement.clearOutputPanel();
        Text text = new Text("Please Wait! \nProcessing file...");
        text.setStyle("-fx-font-size: 24");
        text.setWrappingWidth(400);
        text.setTextAlignment(TextAlignment.CENTER);

        guiLabelManagement.setNodeToAddToOutputPanel(text);
        guiLabelManagement.setInformationPanel("Please select a directory that contains multiple folders, where each " +
                "folder contains" +
                " a pair of folders to analyze.");
        openFolder("multipleComparison");


    }


    /**
     * Call when the user clicks on Count Number of PDF(s)
     * Counts the number of PDFs that exist on a given directory. The way the program counts them depends on the
     * user's choice.
     *
     * @param e Event
     */
    @FXML
    void countNumberOfPDFSOnClick(Event e) {
        Node node = (Node) e.getSource();
        guiLabelManagement.clearOutputPanel();

        window = node.getScene().getWindow();
        guiLabelManagement.setInformationPanel("" +
                "This function has 3 modes:\n " +
                "1. Count all the PDFs inside a folder (including subdirectories).\n" +
                "2. Count the number of folders that contain a PDF.\n" +
                "3. Count the number of successful downloads, which can ONLY be used on the DownloadedPDFs directory" +
                ".\n\n" +
                "Please select the directory that contains the PDFs.\n");
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        JFXButton totalNumber = new JFXButton("Count total number of PDFs");
        JFXButton numberOfFolders = new JFXButton("Count number of folders\nthat contain a PDF");
        JFXButton numberOfSuccessful = new JFXButton("Count number of successful downloads");

        numberOfFolders.setTextAlignment(TextAlignment.CENTER);

        vBox.getChildren().addAll(totalNumber, numberOfFolders, numberOfSuccessful);

        this.pdfCounter = new PDFCounter(guiLabelManagement);
        totalNumber.setOnAction(event -> {
            pdfCounter.setIsUniqueCount(false);
            pdfCounter.setSuccessful(false);

            openFolder("counter");

        });
        numberOfFolders.setOnAction(event -> {
            pdfCounter.setIsUniqueCount(true);
            pdfCounter.setSuccessful(false);
            openFolder("counter");
        });

        numberOfSuccessful.setOnAction(event -> {
            pdfCounter.setIsUniqueCount(false);
            pdfCounter.setSuccessful(true);
            openFolder("counter");
        });

        guiLabelManagement.setNodeToAddToOutputPanel(vBox);
    }

    /**
     * Call when the user clicks on Get PDFs Titles
     * Compares two folders containing PDFs, and finds the duplicates.
     *
     * @param e Event
     */
    @FXML
    void comparePDFsOnClick(Event e) {
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        guiLabelManagement.setInformationPanel("" +
                "This function compares 2 directories and checks for possible duplicates.\n" +
                "Note that to compare multiple pairs, each subdirectory must have exactly 2 folders.\n\n" +
                "Please select the 2 folders that contain the PDFs that you want to compare.\n" +
                "If you have compared this two folders before, this will overwrite the previous output.");
        guiLabelManagement.clearOutputPanel();
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
            guiLabelManagement.setInformationPanel("Please select a directory that contains multiple folders, where " +
                    "each folder contains" +
                    " a pair of folders to analyze. If you have compared a directory with the same name before, this " +
                    "will overwrite the data");
            openFolder("comparisonMultiple");


        });
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);

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
        guiLabelManagement.setInformationPanel("If two folders represent twin papers, then this function will put " +
                "them under the same " +
                "folder based on their twin ID. If you have used this method before, it will only organize the new " +
                "files (the files that are not in the directory \"OrganizedFiles\").\nThe excel file should have the" +
                " following columns in this exact order: pairID, PairMember, WOSID cited, and the title.");
        guiLabelManagement.clearOutputPanel();
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        JFXButton directory = new JFXButton("Select the file containing the downloaded PDFs");
        JFXButton report = new JFXButton("Select the Report.txt");
        JFXButton excel = new JFXButton("Select the excel file containing the twin pairs.");

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

        guiLabelManagement.setNodeToAddToOutputPanel(vBox);
    }

    /**
     * Sets a single PDF file to be analyzed.
     * This is the 'Single Article' button of the GUI
     *
     * @param e Event
     */
    @FXML
    void singleArticleAnalysisOnClick(Event e) {
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        openFile("PDF");
    }

    /**
     * Shows the instructions on how to analyze multiple twins
     */
    @FXML
    void showInstructionsMultipleTwinsOnClick() {
        showInstructions();
    }

    /**
     * Asks the user how the excel file containing the multiple pairs of twins is structured.
     *
     * @param e Event
     */
    @FXML
    void configureMultipleTwinExcelFileOnClick(Event e) {
        guiLabelManagement.setInformationPanel("In order to understand how your excel file containing the multiple " +
                "pairs of twins is structured, we need you to indicate the relevant columns to the program");
        ExcelFileConfiguration configuration = new ExcelFileConfiguration(guiLabelManagement);
        Thread t = new MyThreadFactory().newThread(configuration);
        t.start();
    }

    /**
     * Uploads the excel file containing the multiple pairs of twins
     *
     * @param e Event
     */
    @FXML
    void uploadMultipleTwinFileOnClick(Event e) {
        //Todo: Do something
    }

    /**
     * Handles the logic to upload a single file into the program.
     */
    private void openFile(String type) {
        Platform.runLater(() -> {
            guiLabelManagement.deselectTwinFilesAnalysis();
            FileChooser fileChooser = new FileChooser();
            switch (type) {
                case "PDF":
                    configureFileChooser(fileChooser, "Single PDF file (*.pdf)", "*.pdf");
                    break;
                case "report":
                    configureFileChooser(fileChooser, "TXT files (*.txt)", "*.txt");
                    break;
                case "CSV":
                    configureFileChooser(fileChooser, "Excel file (*.xlsx)", "*.xlsx");
                    break;
                default:
                    configureFileChooser(fileChooser, "Excel file (*.xlsx)", "*.xlsx");
                    break;
            }
            File file = fileChooser.showOpenDialog(window);
            if (file == null) {
                guiLabelManagement.setInformationPanel("Please upload a file.");
                guiLabelManagement.deselectSingleFileAnalysis();
            } else if (!file.exists() || !file.canRead()) {
                guiLabelManagement.setAlertPopUp("There was an error opening one of the files");
                guiLabelManagement.deselectSingleFileAnalysis();
            } else {
                guiLabelManagement.setStatus("File has been submitted.");
                switch (type) {
                    case "PDF":
                        SetFiles setFiles = new SetFiles();
                        setFiles.setSingleFile(this, file, guiLabelManagement);
                        guiLabelManagement.setStatus("File has been set.");
                        //Make sure that Set Folders to Compare, Analyze Data and Output Results buttons are disabled
                        // since the user has not verified the data of the PDF yet
                        guiLabelManagement.disableFolderButton(true);
                        guiLabelManagement.disableAnalyzeDataButton(true);
                        guiLabelManagement.disableOutputResultButton(true);

                        break;
                    case "report":
                        twinOrganizer.setReport(file);
                        break;
                    case "CSV":
                        //This happens when the user is trying to organize the PDF files
                        try {
                            twinOrganizer.readFile(file);
                        } catch (IOException e) {
                            guiLabelManagement.setAlertPopUp(e.getMessage());
                        }
                        //Run the twin organizer once we have the csv file
                        Thread t = new MyThreadFactory().newThread(twinOrganizer);
                        t.start();
                        break;
                    default:
                        this.multipleFilesSetup = new MultipleFilesSetup(guiLabelManagement);
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
    void setTwinArticlesAnalysisOnClick(Event e) {
        Node node = (Node) e.getSource();
        window = node.getScene().getWindow();
        analyzeMetadataOfTwinFiles();
    }

    /**
     * Handles the logic to upload twin files into the program.
     */
    private void analyzeMetadataOfTwinFiles() {
        Platform.runLater(() -> {
            guiLabelManagement.deselectSingleFileAnalysis();
            FileChooser fileChooser = new FileChooser();
            configureFileChooser(fileChooser, "Two PDF files (*.pdf)", "*.pdf");
            List<File> files = fileChooser.showOpenMultipleDialog(window);
            if (files == null) {
                guiLabelManagement.setInformationPanel("Please upload a file.");
                guiLabelManagement.deselectTwinFilesAnalysis();
            } else if (files.size() > 2) {
                guiLabelManagement.setAlertPopUp("You cannot upload more than 2 files");
                guiLabelManagement.deselectTwinFilesAnalysis();
            } else if (files.size() < 2) {
                guiLabelManagement.setAlertPopUp("You need 2 files");
                guiLabelManagement.deselectTwinFilesAnalysis();
            } else if (!files.get(0).exists() || !files.get(0).canRead() || !files.get(1).exists() || !files.get(1)
                    .canRead()) {
                guiLabelManagement.setAlertPopUp("There was an error opening one of the files");
                guiLabelManagement.deselectTwinFilesAnalysis();
            } else if (files.get(0).length() < 1 || files.get(1).length() < 1) {
                guiLabelManagement.setAlertPopUp("One of the files is empty");
                guiLabelManagement.deselectTwinFilesAnalysis();
            } else {
                guiLabelManagement.setStatus("File has been submitted.");
                SetFiles setFiles = new SetFiles();
                setFiles.setTwinFiles(this, files.get(0), files.get(1), guiLabelManagement);
                guiLabelManagement.setStatus("File have been set.");
                //Make sure that Set Folders to Compare, Analyze Data and Output Results buttons are disabled
                // since the user has not verified the data of the PDF yet
                guiLabelManagement.disableFolderButton(true);
                guiLabelManagement.disableAnalyzeDataButton(true);
                guiLabelManagement.disableOutputResultButton(true);

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


    /**
     * Used when click on the 'Set Folder To Compare' button
     * @param event Event
     */
    @FXML
    public void setFolderOnClick(ActionEvent event) {
        Node node = (Node) event.getSource();
        window = node.getScene().getWindow();
        guiLabelManagement.setInformationPanel("Make sure that all the files are in the same folder.");
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
                guiLabelManagement.setInformationPanel("Please upload a file.");
            } else {
                if (!selectedDirectory.exists()) {
                    guiLabelManagement.setAlertPopUp("Folder does not exist");
                } else if (!selectedDirectory.canRead()) {
                    guiLabelManagement.setAlertPopUp("Folder cannot be read");
                } else {
                    File[] listOfFiles = selectedDirectory.listFiles();
                    if (listOfFiles == null) {
                        guiLabelManagement.setAlertPopUp("The file need to be inside of a folder");
                    } else if (listOfFiles.length < 1) {
                        guiLabelManagement.setAlertPopUp("There are no files in this folder");
                    } else {
                        ArrayList<File> workingFiles = new ArrayList<>();
                        for (File curr : listOfFiles) {
                            if (!curr.getName().equals(".DS_Store")) {
                                if (!curr.exists() || !curr.canRead()) {
                                    guiLabelManagement.setAlertPopUp(curr.getName() + " is not a valid file");
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
     * Sets the file according to the function that the user wants to use
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
                TitleFinder tf = new TitleFinder(listOfFiles, guiLabelManagement);
                Thread t = new MyThreadFactory().newThread(tf);
                t.start();
                break;
            case "comparison":
                //Compares all the titles and checks for duplicates among two different directories.
                if (this.comparator == null) {
                    comparator = new PDFComparator(guiLabelManagement);
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
                    comparator = new PDFComparator(guiLabelManagement);
                }
                comparator.setOrganize(organizeDuplicates);
                comparator.setDirectoryMultiple(listOfFiles);
                Thread t2 = new MyThreadFactory().newThread(comparator);
                t2.start();
                comparator = null;
                break;
            case "downloadedPDFs":
                //Stores the downloaded PDFs that will be organized based on twin papers
                twinOrganizer = new TwinOrganizer(guiLabelManagement);
                twinOrganizer.setDownloadedPDFs(listOfFiles);
                guiLabelManagement.setStatus("The folder has been set.");
                break;
            case "multipleComparison":
                //Stores the folders that will be analyzed when using the Multiple Pairs of Twins mode
                multipleFilesSetup.setUpFolder(listOfFiles);
                //Make sure that Set Folders to Compare, Analyze Data and Output Results buttons are disabled
                // since the user CANNOT use them in this mode
                guiLabelManagement.disableFolderButton(true);
                guiLabelManagement.disableAnalyzeDataButton(true);
                guiLabelManagement.disableOutputResultButton(true);
                Thread t3 = new MyThreadFactory().newThread(multipleFilesSetup);
                t3.start();
                break;
            default:
                //Stores the files that will be analyzed (on Single Article of Twin Article mode only) to check if
                // they cite the previously selected Twin Article
                comparisonFiles = listOfFiles;
                guiLabelManagement.setStatus("The folder has been set.");
                //Enable the analyze data button
                guiLabelManagement.disableAnalyzeDataButton(false);
                guiLabelManagement.disableOutputResultButton(true);
                //Display message
                guiLabelManagement.clearOutputPanel();
                Label nextStep = new Label("Now click on 'Analyze Data'");
                nextStep.setStyle("-fx-font-size: 16");
                nextStep.setStyle("-fx-text-alignment: center");
                guiLabelManagement.setNodeToAddToOutputPanel(nextStep);
                break;
        }
    }


    @FXML
    void analyzeDataOnClick() {
        guiLabelManagement.clearOutputPanel();
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
            try {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Updates the progress indicator
     *
     * @param currProgress double from 0 to 1 with the current progress
     */
    private void updateProgressIndicator(Double currProgress) {
        Platform.runLater(() -> {
            try {
                progressIndicator.setProgress(currProgress);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        });

    }

    /**
     * Updates the output of the FileAnalyzer (which is the bottom text in the Output Panel)
     *
     * @param output message to output
     */
    private void updateProgressOutput(String output) {
        Platform.runLater(() -> progressOutputText.setText(output));
    }

    /**
     * Changes the style of the progress output
     *
     * @param style string with the desired style
     */
    private void changeProgressOutputStyle(String style) {
        Platform.runLater(() -> progressOutputText.setStyle(style));
    }

    /**
     * Creates a pop up message that says Loading...
     */
    private void informationPanel(String s) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(s);
        alert.showAndWait();
    }

    /**
     * Updates the status label (the bottom dark bar)
     *
     * @param message String with the message to output
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            try {
                statusLabel.setText(message);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Adds JavaFX elements into the output panel
     */
    private void addElementsToOutputPanel(ObservableList<Node> elements) {
        Platform.runLater(() -> {
            try {
                //Clear current output panel and add the new list with the elements
                //By doing this, we make sure that everything in sync
                outputPanel.getChildren().clear();
                outputPanel.getChildren().addAll(elements);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Writes the output file
     */
    private void outputToFile() {
        FileOutput output = new FileOutput();
        try {
            //Todo create thread to do this
            output.writeOutputToFile(dataGathered, "Report.xlsx");
            guiLabelManagement.setStatus("The report has been created! ");

        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp("There was an error trying to open the file. Make sure the file " +
                    "exists. ");
        }
    }

    /**
     * Initializes the ProgressIndicator Node
     *
     * @return ProgressIndicator
     */
    private ProgressIndicator getProgressIndicator() {
        if (progressIndicator == null) {
            progressIndicator = new ProgressIndicator();
            progressIndicator.setStyle("-fx-alignment: center;" +
                    "-fx-progress-color: #990303");
            progressIndicator.setMinHeight(190);
            progressIndicator.setMinWidth(526);
        }
        return progressIndicator;
    }

    /**
     * Marks the 'Twin Articles' radial button as deselected
     */
    private void deselectTwinFilesAnalysis() {
        Platform.runLater(() -> twinArticlesAnalysis.setSelected(false));
    }

    /**
     * Marks the 'Single Article' radial button as deselected
     */
    private void deselectSingleFileAnalysis() {
        Platform.runLater(() -> singleArticleAnalysis.setSelected(false));
    }

    /**
     * Enables or disables the 'Set Folder To Compare' button
     */
    private void enableFolderButton(boolean disable) {
        Platform.runLater(() -> setFolder.setDisable(disable));
    }


    /**
     * Enables or disables the Analyze Data button
     */
    private void disableAnalyzeDataButton(boolean select) {
        Platform.runLater(() -> {
            analyzeData.setDisable(select);
        });
    }

    /**
     * Enables or disables the Output Results button
     */
    private void disableOutputResultsButton(boolean select) {
        Platform.runLater(() -> {
            //Button is originally hidden
            outputResults.setDisable(select);
        });
    }


    /**
     * Task class used when analyzing a pair of twins.
     * Only for Single Article or Twin Articles Mode!!!
     */
    class MyTask extends Task<Void> {

        @Override
        protected Void call() {
            guiLabelManagement.setStatus("Analyzing...");
            //Disable the buttons while the data is processed
            guiLabelManagement.disableAnalyzeDataButton(true);
            guiLabelManagement.disableOutputResultButton(true);
            //Change the progress output
            changeProgressOutputStyle("-fx-font-size: 16");
            changeProgressOutputStyle("-fx-text-alignment: center");
            guiLabelManagement.setProgressOutput("Extracting the titles...");
            //Add the progress indicator and progressOutputText to the output panel
            guiLabelManagement.setNodeToAddToOutputPanel(guiLabelManagement.getProgressIndicatorNode());
            guiLabelManagement.setNodeToAddToOutputPanel(progressOutputText);

            FileAnalyzer fileAnalyzer = new FileAnalyzer(comparisonFiles, twinFile1, twinFile2, guiLabelManagement);

            try {
                fileAnalyzer.analyzeFiles();
            } catch (Error e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e.getMessage());
            }
            System.out.println("Done Analyzing");
            guiLabelManagement.clearOutputPanel();
            guiLabelManagement.setProgressOutput("Done Analyzing\n" +
                    "Press 'Output Results' to see the result of the analysis.\n" +
                    "The output file name will be 'Report.xlsx'");
            guiLabelManagement.setNodeToAddToOutputPanel(progressOutputText);
            dataGathered = fileAnalyzer.getDataGathered();
            guiLabelManagement.disableOutputResultButton(false);
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
            Platform.runLater(() -> guiLabelManagement.disableAnalyzeDataButton(false));
        }
    }


}
