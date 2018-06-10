package com.rc.PDFCitationAnalyzer;

import com.apple.eawt.Application;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.Guard;
import java.security.Security;
import java.util.*;
import java.util.concurrent.Executors;

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
    private SimpleTwinAnalysis simpleTwinAnalysis;


    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private JFXRadioButton twinArticlesAnalysis;
    @FXML
    private JFXRadioButton singleArticleAnalysis;
    @FXML
    private JFXRadioButton multiplePairsAnalysis;
    @FXML
    private VBox outputPanel;
    @FXML
    private Label statusLabel;
    @FXML
    private JFXButton analyzeData;
    @FXML
    private JFXButton outputResults;
    @FXML
    private JFXButton organizeTwins;
    @FXML
    private JFXButton countNumberOfPDFS;
    @FXML
    private JFXButton setFolder;
    @FXML
    private JFXButton getTitles;
    @FXML
    private JFXButton showInstructionsMultipleTwins;
    @FXML
    private JFXButton configureMultipleTwinExcelFile;
    @FXML
    private JFXButton uploadMultipleTwinFile;
    @FXML
    private JFXButton comparePDFs;
    @FXML
    private JFXButton recoverBackup;
    @FXML
    private JFXButton compileResults;
    @FXML
    private ImageView image;


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
        bindGUI();
        //Set up the GUI for the start screen
        guiLabelManagement.setStatus("Ready to use.");
        progressOutputText.setStyle("-fx-font-size: 16; -fx-text-alignment: center");
        loadIcon();
        //To read PDFs that have security settings
        Security.addProvider(new BouncyCastleProvider());
        //Read the user preferences, if any, and if so, change the GUI to reflect them.
        checkUserPreferences();

    }

    /**
     * Bind all the GUI elements to the different Properties inside of the GUILabelManagement obj
     */
    private void bindGUI() {
        guiLabelManagement.getAlertPopUp().addListener((observable, oldValue, newValue) -> displayAlert(newValue));
        guiLabelManagement.getProgressIndicator().addListener((observable, oldValue, newValue) ->
                updateProgressIndicator(newValue.doubleValue()));
        guiLabelManagement.storeProgressIndicator(getProgressIndicator());
        guiLabelManagement.getDisableFolderButton().addListener((observable, oldValue, newValue) ->
                enableFolderButton(newValue));
        guiLabelManagement.getNodesToAddToOutputPanel().addListener((observable, oldValue, newValue) ->
                addElementsToOutputPanel(newValue));
        guiLabelManagement.getInformationPanel().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                informationPanel(newValue);
            }
        });
        guiLabelManagement.getStatus().addListener((observable, oldValue, newValue) -> updateStatus(newValue));
        guiLabelManagement.getTwinFilesAnalysisDeselected().addListener((observable, oldValue, newValue) ->
                deselectTwinFilesAnalysis());
        guiLabelManagement.getSingleFileAnalysisDeselected().addListener((observable, oldValue, newValue) ->
                deselectSingleFileAnalysis());
        guiLabelManagement.getAnalyzeDataButton().addListener((observable, oldValue, newValue) ->
                disableAnalyzeDataButton(newValue));
        guiLabelManagement.getOuputResultsButton().addListener((observable, oldValue, newValue) ->
                disableOutputResultsButton(newValue));
        guiLabelManagement.getChangeConfigureExcelFileText().addListener((observable, oldValue, newValue) ->
                updateConfigurationButtonText());
        guiLabelManagement.getChangeUploadExcelFileText().addListener((observable, oldValue, newValue) ->
                updateUploadExcelFileText());
        guiLabelManagement.getChangeRecoverBackupText().addListener((observable, oldValue, newValue) ->
                updateRecoverBackupText());
        guiLabelManagement.getChangeRecoverBackupTextWithoutListeners().addListener((observable, oldValue, newValue) ->
                updateRecoverBackupText());
        guiLabelManagement.getChangeSaveProgressText().addListener((observable, oldValue, newValue) ->
                updateSaveProgressText());
        guiLabelManagement.getSaveProgressButton().addListener((observable, oldValue, newValue) ->
                disableSaveProgressButton(newValue));

    }

    /**
     * Enables or disables all the buttons in the GUI.
     */
    private void disableGUI(boolean disable) {
        singleArticleAnalysis.setDisable(disable);
        twinArticlesAnalysis.setDisable(disable);
        multiplePairsAnalysis.setDisable(disable);
        showInstructionsMultipleTwins.setDisable(disable);
        configureMultipleTwinExcelFile.setDisable(disable);
        uploadMultipleTwinFile.setDisable(disable);
        organizeTwins.setDisable(disable);
        countNumberOfPDFS.setDisable(disable);
        getTitles.setDisable(disable);
        comparePDFs.setDisable(disable);
        recoverBackup.setDisable(disable);
        compileResults.setDisable(disable);
    }

    /**
     * Loads the application icon (Only works for mac)
     */
    private void loadIcon() {
        if (System.getProperty("os.name").contains("Mac")) {
            URL s = getClass().getClassLoader().getResource("icon.png");
            if (s != null) {
                Application.getApplication().setDockIconImage(
                        new ImageIcon(s).getImage());
            }


        }
    }

    /**
     * Checks if the user has any preferences, and if so, it modifies the GUI
     */
    private void checkUserPreferences() {
        UserPreferences.readPreferences(guiLabelManagement);
        //If there are user configurations, change the button
        if (UserPreferences.getExcelConfiguration().size() != 0) {
            guiLabelManagement.updateConfigureExcelFileText();
        }
        if (!UserPreferences.getExcelLocation().isEmpty()) {
            GUILabelManagement temp = guiLabelManagement;
            this.multipleFilesSetup = new MultipleFilesSetup(temp);
            //Read the file and check if it works
            Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(() -> {
                TwinFileReader.setUpFile(new File(UserPreferences
                        .getExcelLocation()), true, guiLabelManagement);
                //Show the instructions after setting up the file
                showInstructions();
                disableGUI(false);
            }).start());
        } else {
            //If the user has no preferences, then just show the instructions
            showInstructions();
            disableGUI(false);
        }
    }

    /**
     * Displays the main instructions in the Output Panel
     */
    private void showInstructions() {
        Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(() -> {
            guiLabelManagement.clearOutputPanel();
            VBox vBox = new VBox(10);
            vBox.setAlignment(Pos.CENTER);
            Label header = new Label("Instructions to Analyze Multiple Pairs of Twins");
            header.setStyle("-fx-font-size: 18");
            ArrayList<Label> labels = new ArrayList<>();
            Label step0 = new Label("Step 0: Configure the format of the Excel/CSV file containing the multiple pairs" +
                    " of twins, click on 'Configure Twin File'.");
            Label step1 = new Label("Step 1: Upload the Excel(.xlsx)/CSV file, click on 'Upload Twin File'.");
            Label step2 = new Label("Step 2: If you haven't organized the files inside of the DownloadedPDFs " +
                    "folder, click on 'Arrange Twin Files'");
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
        }).start());
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
        //Verify that the user has configured the excel file structure and that it has upload it
        if (UserPreferences.getExcelLocation().isEmpty()) {
            guiLabelManagement.setAlertPopUp("Please upload the Excel/CSV file containing the multiple pairs first!");
        } else {
            if (this.multipleFilesSetup == null) {
                GUILabelManagement temp = guiLabelManagement;
                this.multipleFilesSetup = new MultipleFilesSetup(temp);
            }

            Node node = (Node) e.getSource();
            window = node.getScene().getWindow();
            //Ask the user how many pairs they want to analyze of the current file
            guiLabelManagement.clearOutputPanel();
            int currNumberOfPairs = TwinFileReader.getTwinIDToPaper().size();
            Label instructions = new Label("Your current file has " + currNumberOfPairs + " " +
                    "pairs.\n" +
                    "Which pairs do you want to analyze?");
            instructions.setStyle("-fx-text-alignment: center; -fx-font-size: 15");

            //Set up the Left Range GUI
            Label leftLabel = new Label("Start analysis from TwinID:");
            leftLabel.setStyle("-fx-text-alignment: center; -fx-font-size: 15");
            JFXTextField leftRange = new JFXTextField();
            leftRange.setPromptText("Start");
            leftRange.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 15");

            //Do the same for the right side
            Label rightLabel = new Label("End analysis at TwinID:");
            rightLabel.setStyle("-fx-text-alignment: center; -fx-font-size: 15");
            JFXTextField rightRange = new JFXTextField();
            rightRange.setPromptText("End");
            rightRange.setStyle("-fx-text-alignment: center;-fx-alignment: center; -fx-font-size: 15");

            GridPane ranges = new GridPane();
            //Setting the padding
            ranges.setPadding(new Insets(10, 10, 10, 10));
            //Setting the vertical and horizontal gaps between the columns
            ranges.setVgap(5);
            ranges.setHgap(15);

            //Setting the Grid alignment
            ranges.setAlignment(Pos.CENTER);

            //Arranging all the nodes in the grid
            ranges.add(leftLabel, 0, 0);
            ranges.add(leftRange, 0, 1);
            ranges.add(rightLabel, 1, 0);
            ranges.add(rightRange, 1, 1);


            //Ask the user for the range of papers they want to analyze
            JFXButton continueButton = new JFXButton("Analyze The Selected Pairs");
            JFXButton analyzeAll = new JFXButton("Analyze All The Pairs");

            VBox vBox = new VBox(10);
            vBox.setAlignment(Pos.CENTER);

            vBox.getChildren().addAll(instructions, ranges, continueButton, analyzeAll);
            guiLabelManagement.setNodeToAddToOutputPanel(vBox);

            //Once the user clicks the button verify the input
            continueButton.setOnAction(event -> {
                if (leftRange.getText().isEmpty() || leftRange.getText().equals(" ") || rightRange.getText().isEmpty
                        () || leftRange.getText().equals(" ")) {
                    guiLabelManagement.setAlertPopUp("Please write the left and right range");
                } else {
                    String leftRangeNum = leftRange.getText();
                    String rightRangeNum = rightRange.getText();

                    //Check if its a letter or a number.
                    if (StringUtils.isNumeric(leftRangeNum) && StringUtils.isNumeric(rightRangeNum)) {
                        //If it is a number, verify that it is an int
                        if (ExcelFileConfiguration.isNotInteger(leftRangeNum) || ExcelFileConfiguration.isNotInteger
                                (rightRangeNum)) {
                            guiLabelManagement.setAlertPopUp("The number you inputted is not an integer!");
                        } else {
                            //Make sure that it is a non negative int
                            int leftRangeInt = Integer.parseInt(leftRangeNum);
                            int rightRangeInt = Integer.parseInt(rightRangeNum);
                            if (leftRangeInt < 1 || rightRangeInt < 1) {
                                guiLabelManagement.setAlertPopUp("The number(s) cannot be less than 1!");

                            } else if (rightRangeInt < leftRangeInt) {
                                guiLabelManagement.setAlertPopUp("The right range has to be >= that the left range!");
                            } else {
                                //Store the range
                                multipleFilesSetup.setRange(true, leftRangeInt, rightRangeInt);
                                //Ask the user if they want to disable alerts
                                disableAlerts();
                            }

                        }

                    }
                    //If it is not a number, throw an error
                    else {
                        guiLabelManagement.setAlertPopUp("Please only write numbers here!");
                    }

                }
            });

            //Once the user clicks the button verify the input
            analyzeAll.setOnAction(event -> {
                multipleFilesSetup.setRange(false, 0, 0);
                disableAlerts();
            });
            guiLabelManagement.setInformationPanel("Please select a directory that contains multiple folders, where " +
                    "each folder represents a Twin and the PDFs inside cite that Twin.\n" +
                    "If you used the Analyzer to organize the files, this folder is called OrganizedFiles.");
        }
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
        //Verify that the user has configured the excel file structure and that it has upload it
        if (UserPreferences.getExcelLocation().isEmpty()) {
            guiLabelManagement.setAlertPopUp("Please upload the Excel/CSV file containing the multiple pairs first!");
        } else {

            //Display all the GUI
            Node node = (Node) e.getSource();
            window = node.getScene().getWindow();
            guiLabelManagement.setInformationPanel("This function arranges all the files inside of DownloadedPDFs " +
                    "based on the twin papers each paper cites. So if a papers 'A' cites the twin pair with ID " +
                    "'2040', " +
                    "this function will put paper 'A' inside a folder named '2040'.\n\n" +
                    "Please make sure that you have uploaded the correct Excel/CSV file!\n" +
                    "If you have used this function before, it will only organize the new files (the files that are " +
                    "not" +
                    " in the directory \"OrganizedFiles\")");
            guiLabelManagement.clearOutputPanel();
            VBox vBox = new VBox(10);
            vBox.setAlignment(Pos.CENTER);
            JFXButton directory = new JFXButton("Select the DownloadedPDFs directory");
            JFXButton report = new JFXButton("Select the Report.txt");
            Label or = new Label("OR");
            JFXButton organizeMultiple = new JFXButton("Organize Multiple DownloadedPDFs directories");

            //Block the other buttons until the user sets the directory
            report.setDisable(true);

            vBox.getChildren().addAll(directory, report, or, organizeMultiple);
            directory.setOnAction(event -> {
                openFolder("downloadedPDFs");
                report.setDisable(false);
                directory.setDisable(true);
                organizeMultiple.setDisable(true);

            });
            report.setOnAction(event -> {
                openFile("report");
                report.setDisable(true);
            });
            organizeMultiple.setOnAction(event -> {
                openFolder("downloadedPDFsMultiple");
                directory.setDisable(true);
                organizeMultiple.setDisable(true);
            });
            guiLabelManagement.setNodeToAddToOutputPanel(vBox);
        }
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
        guiLabelManagement.setInformationPanel("In order to understand the structure of your Excel/CSV file that " +
                "contains the multiple pairs of twins, we need you to indicate the relevant columns to " +
                "the program.");
        ExcelFileConfiguration configuration = new ExcelFileConfiguration(guiLabelManagement);
        Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(configuration).start());
    }

    /**
     * Uploads the excel file containing the multiple pairs of twins
     *
     * @param e Event
     */
    @FXML
    void uploadMultipleTwinFileOnClick(Event e) {
        //Do not let the user upload the file if they have not configured the structure of the excel file first
        if (UserPreferences.getExcelConfiguration().size() == 0) {
            guiLabelManagement.setAlertPopUp("You need to configure the Excel/CSV file first!");
        } else {
            Node node = (Node) e.getSource();
            window = node.getScene().getWindow();
            guiLabelManagement.setInformationPanel("Please select the Excel/CSV file containing the multiple pairs of" +
                    " twins.\nMake sure it is an .xlsx or .csv or .odd file!");
            openFile("excel");
            guiLabelManagement.clearOutputPanel();
            Text text = new Text("Please Wait! \nProcessing file...");
            text.setStyle("-fx-font-size: 24");
            text.setWrappingWidth(400);
            text.setTextAlignment(TextAlignment.CENTER);
            guiLabelManagement.setNodeToAddToOutputPanel(text);
        }
    }

    @FXML
    void recoverBackupOnClick() {
        //If the user is already analyzing, then send notification to the MultipleFileSetup to save the progress
        if (recoverBackup.getText().equals("Save Progress")) {
            guiLabelManagement.changeToSaveProgress();
        } else {
            //Else, if the user presses the 'Recover Backup' button, then start the recovery process
            guiLabelManagement.clearOutputPanel();
            guiLabelManagement.setInformationPanel("This function recovers a previous 'Multiple Pairs of Twins' " +
                    "analysis " +
                    "and restarts it from the last saving point." +
                    "\nPlease select the backup you want to recover in the Backups folder.");
            openFile("backup");
        }

    }

    @FXML
    void compileResultsOnClick() {
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setInformationPanel("This function compiles multiple analysis results into one single " +
                "Excel file.\n" +
                "Please open the directory where all the Excel files that start with 'TwinAnalyzerResults_...' are " +
                "located.");
        openFolder("compile");


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
                    configureFileChooser(fileChooser, "TXT file (*.txt)", "*.txt");
                    break;
                case "backup":
                    configureFileChooser(fileChooser, "Binary file (*.bin)", "*.bin");
                    break;
                default:
                    fileChooser.setTitle("Please select the Twin File");
                    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Excel (*.xlsx) or CSV(*" +
                            ".csv, *.odd) file )", "*.xlsx", "*.csv", "*.odd");
                    fileChooser.getExtensionFilters().add(extFilter);
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
                        twinOrganizer.setMultipleMode(false);
                        //Run the twin organizer once we have the csv file
                        Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread
                                (twinOrganizer).start());
                        break;

                    case "backup":
                        //Tries to retrieve a backup, and it exists, it restarts the download process
                        Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(() -> {
                            //Disable the GUI while it loads
                            disableGUI(true);
                            MultipleFilesSetup backup = Backup.recoverBackup(file, guiLabelManagement);
                            if (backup != null) {
                                guiLabelManagement.setStatus("Restarting ..");
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    this.multipleFilesSetup = null;
                                    guiLabelManagement.changeToSaveProgress();
                                    this.multipleFilesSetup = backup;
                                    GUILabelManagement temp = this.guiLabelManagement;
                                    multipleFilesSetup.startRecovery(temp);
                                    new MyThreadFactory().newThread(backup).start();
                                    this.multipleFilesSetup = null;


                                });
                            } else {
                                guiLabelManagement.setAlertPopUp("The backup file is corrupted!");
                                guiLabelManagement.setStatus("Could not use backup...");
                            }
                            disableGUI(false);
                        }).start());
                        break;

                    default:
                        //This happens when the user is upload the excel file that contains multiple pairs of twins
                        GUILabelManagement temp = guiLabelManagement;
                        this.multipleFilesSetup = new MultipleFilesSetup(temp);
                        //Read the file and check if it works
                        Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(() ->
                                TwinFileReader.setUpFile(file, false,
                                guiLabelManagement)).start());
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
     * Configures the types of files that are allowed to be upload (
     *
     * @param fileChooser the current fileChooser
     */
    private void configureFileChooser(FileChooser fileChooser, String description, String extension) {
        fileChooser.setTitle("Please select the file");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(description, extension);
        fileChooser.getExtensionFilters().add(extFilter);

    }


    /**
     * Used when click on the 'Set Folder To Compare' button
     *
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
                Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(pdfCounter).start());
                break;
            case "titles":
                //Extracts all the titles and creates an excel file
                TitleFinder tf = new TitleFinder(listOfFiles, guiLabelManagement);
                Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(tf).start());
                break;
            case "comparison":
                //Compares all the titles and checks for duplicates among two different directories.
                if (this.comparator == null) {
                    comparator = new PDFComparator(guiLabelManagement);
                    comparator.setOrganize(organizeDuplicates);
                }
                comparator.setDirectory(listOfFiles);
                if (comparator.isReady()) {
                    Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(comparator)
                            .start());
                }
                break;
            case "comparisonMultiple":
                //Compares all the titles and checks for duplicates among multiple directories.
                if (this.comparator == null) {
                    comparator = new PDFComparator(guiLabelManagement);
                }
                comparator.setOrganize(organizeDuplicates);
                comparator.setDirectoryMultiple(listOfFiles);
                Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(comparator).start());
                comparator = null;
                break;
            case "downloadedPDFs":
                //Stores the downloaded PDFs that will be organized based on twin papers
                twinOrganizer = new TwinOrganizer(guiLabelManagement);
                twinOrganizer.setDownloadedPDFs(listOfFiles);
                guiLabelManagement.setStatus("The folder has been set.");
                break;
            case "downloadedPDFsMultiple":
                //Organizes multiple downloadedPDF based on the twin papers that they cite
                twinOrganizer = new TwinOrganizer(guiLabelManagement);
                twinOrganizer.setDownloadedPDFsMultiple(listOfFiles);
                guiLabelManagement.setStatus("The folder has been set.");
                twinOrganizer.setMultipleMode(true);
                guiLabelManagement.setStatus("Organizing multiple DownloadedPDFs...");
                //Run the twin organizer once we have the csv file
                Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread
                        (twinOrganizer).start());
                break;
            case "multipleComparison":
                //Stores the folders that will be analyzed when using the Multiple Pairs of Twins mode
                multipleFilesSetup.setUpFolder(listOfFiles);
                //Make sure that Set Folders to Compare, Analyze Data and Output Results buttons are disabled
                // since the user CANNOT use them in this mode
                guiLabelManagement.disableFolderButton(true);
                guiLabelManagement.disableAnalyzeDataButton(true);
                guiLabelManagement.disableOutputResultButton(true);
                Executors.newSingleThreadExecutor().execute(() -> {
                    //Update the button
                    guiLabelManagement.changeToSaveProgress();
                    //Start analyzing
                    new MyThreadFactory().newThread(multipleFilesSetup).start();
                    //Restart for the next use
                    this.multipleFilesSetup = null;

                });
                break;
            case "compile":
                //Store the folder where all the output files are located
                OutputCompiler outputCompiler = new OutputCompiler(listOfFiles, guiLabelManagement);
                Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(outputCompiler).start());
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

    /**
     * Disables non critical alerts when analyzing a paper (Default is false)
     */
    private void disableAlerts() {
        guiLabelManagement.clearOutputPanel();
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        Label instruction = new Label("Do you want to see all alerts or only critical alerts when analyzing the " +
                "files?");
        instruction.setStyle("-fx-text-alignment: center; -fx-font-size: 15");

        JFXButton all = new JFXButton("See All Alerts");
        JFXButton critical = new JFXButton("See Only Critical Alerts");

        vBox.getChildren().addAll(instruction, all, critical);
        guiLabelManagement.setNodeToAddToOutputPanel(vBox);
        critical.setOnAction(event -> {
            this.multipleFilesSetup.disableAlerts(true);
            openFolder("multipleComparison");
        });
        all.setOnAction(event -> {
            openFolder("multipleComparison");
        });

    }


    @FXML
    void analyzeDataOnClick() {
        guiLabelManagement.clearOutputPanel();
        this.simpleTwinAnalysis = new SimpleTwinAnalysis(guiLabelManagement, progressOutputText, comparisonFiles,
                twinFile1, twinFile2);
        Executors.newSingleThreadExecutor().execute(() -> new MyThreadFactory().newThread(simpleTwinAnalysis).start());
    }

    @FXML
    void outputResultsOnClick() {
        dataGathered = simpleTwinAnalysis.getDataGathered();
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
        if (message != null && !message.isEmpty()) {
            Platform.runLater(() -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText(message);
                    alert.showAndWait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Updates the progress indicator
     *
     * @param currProgress double from 0 to 1 with the current progress
     */
    private void updateProgressIndicator(Double currProgress) {
        if (!currProgress.isNaN() && currProgress >= 0) {
            Platform.runLater(() -> {
                try {
                    progressIndicator.setProgress(currProgress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }


    /**
     * Creates a pop up message that says Loading...
     */
    private void informationPanel(String s) {
        try {
            if (s != null && !s.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information");
                alert.setHeaderText(null);
                alert.setContentText(s);
                alert.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the status label (the bottom dark bar)
     *
     * @param message String with the message to output
     */
    private void updateStatus(String message) {
        if (message != null && !message.isEmpty()) {
            Platform.runLater(() -> {
                try {
                    statusLabel.setText(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }


    /**
     * Adds JavaFX elements into the output panel
     */
    private void addElementsToOutputPanel(ObservableList<Node> elements) {
        if (elements != null && elements.size() > 0) {
            Platform.runLater(() -> {
                try {
                    //Clear current output panel and add the new list with the elements
                    //By doing this, we make sure that everything in sync
                    outputPanel.getChildren().clear();
                    outputPanel.getChildren().addAll(elements);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }


    /**
     * Writes the output file
     */
    private void outputToFile() {
        FileOutput output = new FileOutput();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                output.writeOutputToFile(dataGathered, "Report.xlsx");
                guiLabelManagement.setStatus("The report has been created! ");

            } catch (IOException e) {
                guiLabelManagement.setAlertPopUp("There was an error trying to open the file. Make sure the file " +
                        "exists. ");
            }
        });

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
            outputResults.setDisable(select);
        });
    }

    /**
     * Updates the 'Configure Excel File' text once the user has configured the excel file
     */
    private void updateConfigurationButtonText() {
        Platform.runLater(() -> {
            configureMultipleTwinExcelFile.setText("Modify Configuration");
        });
    }

    /**
     * Updates the 'Upload Excel File' text once the user has uploaded an excel file
     */
    private void updateUploadExcelFileText() {
        Platform.runLater(() -> {
            uploadMultipleTwinFile.setText("Modify Excel/CSV File");
        });
    }

    /**
     * Updates the 'Recover Backup' text once the user is analyzing multiple twins
     */
    private void updateRecoverBackupText() {
        Platform.runLater(() -> recoverBackup.setText("Save Progress"));
    }

    /**
     * Updates the 'Save Progress' text to 'Recover Backup'
     */
    private void updateSaveProgressText() {
        Platform.runLater(() -> recoverBackup.setText("Recover Backup"));
    }

    /**
     * Enables or disables the 'Save Progress' button
     */
    private void disableSaveProgressButton(boolean select) {
        Platform.runLater(() -> recoverBackup.setDisable(select));
    }

}
