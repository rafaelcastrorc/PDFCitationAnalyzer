package com.rc.PDFCitationAnalyzer;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by rafaelcastro on 7/20/17.
 * Calculates multiple pairs of twin papers
 */
public class MultipleFilesSetup extends Task {
    private HashMap<Object, ArrayList<Object>> paperToAuthor = new HashMap<>();
    private HashMap<Object, ArrayList<Object>> paperToYear = new HashMap<>();
    private HashMap<Object, ArrayList<Object>> twinIDToPaper = new HashMap<>();
    private HashMap<Object, ArrayList<Object>> twinIDToCitingPapers = new HashMap<>();
    private ArrayList<String> titleCitingList = new ArrayList<>();
    private Text outputText;
    private GUILabelManagement guiLabelManagement;
    private File[] foldersToAnalyze;
    private TreeMap<Integer, ArrayList<Object>> comparisonResults;


    MultipleFilesSetup(GUILabelManagement guiLabelManagement) {
        this.guiLabelManagement = guiLabelManagement;

    }

    /**
     * Stores the excel file that contains the multiple pairs of twins
     */
    void setUpFile(File file, boolean thereIsADefaultFile) {
        File excelFile = file;
        try {
            ArrayList<Integer> errors = getAllTwinsInformation(excelFile);
            //If there are any errors, display message to the user
            if (errors.size() != 0) {
                //Convert the list into a comma separated string
                String rowsWithErrors = StringUtils.join(errors, ", ");
                guiLabelManagement.setAlertPopUp("The following rows in the Excel file that you submitted contain " +
                        "errors and will be ignored:\n" +
                                rowsWithErrors +"\n\n" +
                                "Common errors include: \n" +
                        "-One or more cells under the following columns are empty: PairID, Title of Twin1, Title of " +
                        "Twin2, Title that cites Twin1 and Twin2, Author of Twin1, Author of Twin2, Year Twin1 " +
                        "was published, Year Twin2 was published.\n" +
                        "-Both twin papers have the same title.\n" +
                        "-The title of a twin paper is the same as the title of the paper that cites such twin.");
            }
            guiLabelManagement.updateUploadExcelFileText();

        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp("There was a problem reading your excel file.\n" +
                    e.getMessage()+"\n" +
                    "Please solve the error or upload a new file.");
            e.printStackTrace();
            //Delete the excel file from the user preferences
            UserPreferences.removeExcelFile();
            return;
        }
        //If there is not a default file, prompt the user if they want to make the current file the default
        if (!thereIsADefaultFile) {
            //Show GUI asking if user wants to store this as their default excel file
            guiLabelManagement.clearOutputPanel();
            Label instructions = new Label("The excel file has been uploaded!\n\n" +
                    "Do you want to make this your default excel file?");
            instructions.setStyle("-fx-text-alignment: center; -fx-font-size: 15");
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

            Label nextStep = new Label("Now you can analyze multiple pairs of twins.\n" +
                    "Click on the 'Multiple Pairs of Twins' Analysis mode.");
            nextStep.setStyle("-fx-text-alignment: center");

            yes.setOnAction(e -> {
                guiLabelManagement.setStatus("Saved as default excel file.");
                UserPreferences.storeExcelFile(file, true);
                guiLabelManagement.clearOutputPanel();
                guiLabelManagement.setNodeToAddToOutputPanel(nextStep);

            });
            no.setOnAction(e -> {
                guiLabelManagement.setStatus("Excel file has been uploaded.");
                UserPreferences.storeExcelFile(file, false);
                guiLabelManagement.clearOutputPanel();
                guiLabelManagement.setNodeToAddToOutputPanel(nextStep);


            });

            guiLabelManagement.updateUploadExcelFileText();
        }
    }

    void setUpFolder(File[] foldersToAnalyze) {
        this.foldersToAnalyze = foldersToAnalyze;
    }


    /**
     * Maps a key to an object depending on the type.
     */
    private void addToMap(String type, Object key, Object objectToAdd) {
        HashMap<Object, ArrayList<Object>> mapToUse;
        switch (type) {
            case "author":
                mapToUse = paperToAuthor;
                break;
            case "year":
                mapToUse = paperToYear;
                break;
            case "citingPaper":
                mapToUse = twinIDToCitingPapers;
                break;
            default:
                mapToUse = twinIDToPaper;
                break;
        }
        if (!mapToUse.containsKey(key)) {
            ArrayList<Object> list = new ArrayList<>();
            list.add(objectToAdd);
            mapToUse.put(key, list);
        } else {
            ArrayList<Object> list = mapToUse.get(key);
            list.add(objectToAdd);
            mapToUse.put(key, list);
        }
    }


    /**
     * Reads the information inside of report.xlsx or any excel file and gets all the twin pair information
     * @return List with all the rows with errors, if any.
     * @throws IOException if it is unable to access the file
     */
    private ArrayList<Integer> getAllTwinsInformation(File file) throws IOException {
        //Read the excel file configuration
        ArrayList<Integer> config = UserPreferences.getExcelConfiguration();
        int pairIdColumn = config.get(0);
        int titleTwin1Column = config.get(1);
        int titleTwin2Column = config.get(2);
        int titleCitingColumn = config.get(3);
        int authorTwin1Column = config.get(4);
        int authorTwin2Column = config.get(5);
        int yearTwin1Column = config.get(6);
        int yearTwin2Column = config.get(7);
        FileInputStream fis = new FileInputStream(file);
        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);
        // Get iterator to all the rows in current sheet
        // Traversing over each row of XLSX file
        ArrayList<Integer> rowsWithErrors = new ArrayList<>();
        int twinID = 0;
        int previousTwinID = 0;

        boolean theRowHasAnError = false;
        for (Row row : mySheet) {
            // For each row, iterate through each columns
            Iterator<Cell> cellIterator = row.cellIterator();

            //pairIdColumn is the first column of the excel file, so we start counting from there
            int i = pairIdColumn;
            String titleTwin1 = "";
            String titleTwin2 = "";
            int yearTwin1 = 0;
            int yearTwin2 = 0;
            String authorsTwin1 = "";
            String authorsTwin2 = "";
            String titleCiting = "";

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();

                if (cell.getCellTypeEnum() == CellType.STRING) {
                    //This skips the first row since this is the header row
                    if (twinID == 0) continue;
                    //Map the title of one of the twin papers to the twin ID
                    if (i == titleTwin1Column) {
                        titleTwin1 = cell.getStringCellValue();

                    }
                    if (i == titleTwin2Column) {
                        titleTwin2 = cell.getStringCellValue();

                    }
                    //Map a twin paper author to its title
                    if (i == authorTwin1Column) {
                        //Make sure the authors are formatted correctly
                        authorsTwin1 = cell.getStringCellValue();
                        authorsTwin1 = formatAuthors(authorsTwin1);
                    }
                    //Map a twin paper author to its title
                    if (i == authorTwin2Column) {
                        //Make sure the authors are formatted correctly
                        authorsTwin2 = cell.getStringCellValue();
                        authorsTwin2 = formatAuthors(authorsTwin2);
                    }

                    //Add to the list of citing papers and map the twin id to the titles that cite it
                    if (i == titleCitingColumn) {
                        titleCiting = cell.getStringCellValue();
                        titleCitingList.add(titleCiting);
                    }


                } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                    if (i == pairIdColumn) {
                        //Get the pairID/twinID
                        twinID = (int) cell.getNumericCellValue();
                    }
                    if (twinID == 0) continue;
                    if (i == yearTwin1Column) {
                        yearTwin1 = (int) cell.getNumericCellValue();

                    }
                    if (i == yearTwin2Column) {
                        yearTwin2 = (int) cell.getNumericCellValue();
                    }

                } else {
                    //This should not happen
                    theRowHasAnError = true;
                }
                i++;
            }
            //Skip the header row
            if (twinID == 0) {
                continue;
            }
            //After getting all the data from the current row, check if the input is correct
            //That is, that none of the relevant inputs are empty and that titleTwin1 != titleTwin2
            //And title citing != (titleTwin1 or titleTwin2)
            if (twinID == 0 || titleTwin1.isEmpty() || titleTwin2.isEmpty() || authorsTwin1.isEmpty() ||
                    authorsTwin2.isEmpty() || yearTwin1 == 0 || yearTwin2 == 0 || titleCiting.isEmpty()) {
                theRowHasAnError = true;
            }
            if (titleTwin1.equals(titleTwin2) || titleTwin1.equals(titleCiting) || titleTwin2.equals(titleCiting)) {
                theRowHasAnError = true;
            }
            //Do not consider rows with errors
            if (theRowHasAnError) {
                int faultyRow = row.getRowNum() + 1;
                System.err.println("The following row is incorrectly formatted: " + faultyRow);
                rowsWithErrors.add(faultyRow);
                theRowHasAnError = false;
            }
            //The data is correct so we map it
            else {
                //If the previous TwinID = current Twin ID, we are still going over the same pair so we do not have
                // to add the information again
                if (twinID != previousTwinID) {
                    previousTwinID = twinID;
                    //Map the twinID to the 2 papers that are part of it
                    addToMap("twinID", twinID, titleTwin1);
                    addToMap("twinID", twinID, titleTwin2);
                    //Map the title of the twin paper to its authors
                    addToMap("author", titleTwin1, authorsTwin1);
                    addToMap("author", titleTwin2, authorsTwin2);
                    //Map the title of the twin paper to the year it was published
                    addToMap("year", titleTwin1, yearTwin1);
                    addToMap("year", titleTwin2, yearTwin2);
                }
                //Map the twinID to the papers that cite the twins
                addToMap("citingPaper", twinID, titleCiting);
            }
            myWorkBook.close();
            fis.close();
        }
        return rowsWithErrors;

    }

    /**
     * Formats the author names correctly (Comma separated list of names. Ex: Rafael Castro, John Ellis)
     *
     * @param authors String with all the authors
     * @return String with the name of the authors
     */
    private String formatAuthors(String authors) {
        if (authors.contains(",") && !authors.contains(";")) {
            return authors;
        }
        String[] allAuthors = authors.split(";");
        StringBuilder sb2 = new StringBuilder();

        for (String currAuthor : allAuthors) {
            //Remove any white space
            currAuthor = currAuthor.replaceAll("^[ \\t]+|[ \\t]+$", "");
            String[] currAuthorNames = currAuthor.split(",");
            StringBuilder holderOfNames = new StringBuilder();
            for (String name : currAuthorNames) {
                holderOfNames.insert(0, name + " ");
            }
            String namesOfAuthorN = holderOfNames.toString();
            namesOfAuthorN = namesOfAuthorN.replaceAll("^[ \\t]+|[ \\t]+$", "");
            sb2.append(namesOfAuthorN).append(", ");
        }
        return sb2.substring(0, sb2.length() - 2);
    }

    /**
     * After we are done analyzing all the files, output the result file
     * @param mainDirName Name of the current parent directory
     */
    private void finish(String mainDirName) {
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setStatus("Done analyzing all the twins!");
        //Output result
        FileOutput fileOutput = new FileOutput();
        try {
            fileOutput.writeOutputToFile(comparisonResults, "TwinAnalyzerResults_" + mainDirName + ".xlsx");
        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }

        //Update GUI
        guiLabelManagement.clearOutputPanel();

        Text outputText = new Text("All files have been analyzed.\n" +
                "Please go to 'TwinAnalyzerResults_" + mainDirName + ".xlsx'\n" +
                "to see the overall result of the analysis");
        outputText.setStyle("-fx-font-size: 18;-fx-text-alignment: center");
        outputText.setWrappingWidth(400);
        outputText.setTextAlignment(TextAlignment.CENTER);
        //Add the outputText to the output panel
        guiLabelManagement.setNodeToAddToOutputPanel(outputText);

    }


    /**
     * Initializes the GUI
     */
    private void initialize() {
        //Set up GUI
        guiLabelManagement.clearOutputPanel();
        File file = new File("./Analysis");
        if (!file.exists()) {
            file.mkdir();
        }

        guiLabelManagement.getProgressOutput().addListener((observable, oldValue, newValue) ->
                outputText.setText(newValue));

        guiLabelManagement.setProgressIndicator(0);

        this.outputText = new Text("Analyzing the files...");
        outputText.setStyle("-fx-font-size: 16; -fx-text-alignment: center;-fx-wrap-text: 400px");

        //Add listener to the output text, so that we can use it inside of file analyzer
        guiLabelManagement.getProgressOutput().addListener((observable, oldValue, newValue) ->
                outputText.setText(newValue));
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        guiLabelManagement.setProgressIndicator(0);
        box.getChildren().addAll(guiLabelManagement.getProgressIndicatorNode(), outputText);
        guiLabelManagement.setNodeToAddToOutputPanel(box);

    }

    /**
     * Analyze all the files
     */
    private void analyze() {
        String mainDirName;
        File file0 = foldersToAnalyze[0];
        mainDirName = file0.getParentFile().getName();
        ArrayList<Object> header = new ArrayList<>();
        //Headers for the general output file
        header.add("Twin Paper ID");
        header.add("Total Number of Files Analyzed");
        header.add("Files that Did Not Contain One or Both Twins");
        header.add("Average Adjacent-Cit Rate");

        this.comparisonResults = new TreeMap<>();
        comparisonResults.put(0, header);
        int x = 0;
        boolean malformed = false;
        FileAnalyzer fileAnalyzer = null;
        //Each file represents a twinID
        for (File file : foldersToAnalyze) {
            if (file.isDirectory()) {
                //Check if the folder is a directory and that it contains one folder inside
                File[] files = file.listFiles();
                int twinId = 0;
                try {
                    twinId = Integer.valueOf(file.getName());
                } catch (NumberFormatException e) {
                    malformed = true;
                }
                if (files != null && files.length != 0) {
                    int numOfDirectories = 0;
                    for (File possDir : files) {
                        if (possDir.isDirectory()) {
                            numOfDirectories++;
                            break;
                        }
                    }
                    if (numOfDirectories == 0) {
                        String paper1, paper2, author1, author2;
                        int year1, year2;

                        if (twinIDToPaper.containsKey(twinId)) {
                            //Configure the twin files that will be used to analyze the current directory
                            List<Object> list = twinIDToPaper.get(twinId);
                            paper1 = (String) list.get(0);
                            paper2 = (String) list.get(1);

                            author1 = (String) paperToAuthor.get(paper1).get(0);
                            author2 = (String) paperToAuthor.get(paper2).get(0);
                            year1 = (int) paperToYear.get(paper1).get(0);
                            year2 = (int) paperToYear.get(paper2).get(0);
                            TwinFile twinFile1 = new TwinFile(twinId, paper1, year1, author1);
                            TwinFile twinFile2 = new TwinFile(twinId, paper2, year2, author2);

                            printTwinInfo(twinId, paper1, paper2, author1, author2,  year1, year2);

                            //Analyze the files
                            fileAnalyzer = new FileAnalyzer(files, twinFile1, twinFile2,
                                    guiLabelManagement);

                            try {
                                outputText.setText("Analyzing twin pair: " + twinId);
                                //Analyze the files
                                fileAnalyzer.analyzeFiles();
                                outputText.setText("Done analyzing twin pair " + twinId);
                                //Once its done analyzing, output the results in report file for this specific twin
                                FileOutput output = new FileOutput();
                                ArrayList<Object> headers = getReportHeaders();
                                TreeMap<Integer, ArrayList<Object>> dataGathered = fileAnalyzer.getDataGathered();
                                dataGathered.put(0, headers);
                                output.writeOutputToFile(dataGathered, "Analysis/Report_" + twinId + ".xlsx");
                                outputText.setText("Report created for file ");
                                //Then add it to the overall output file
                                addToOverallOutputFile(file, dataGathered);
                            } catch (Exception e) {
                                guiLabelManagement.setAlertPopUp(e.getMessage());
                                //Add it to the general output
                                ArrayList<Object> error = new ArrayList<>();
                                error.add(file.getName());
                                error.add("Error While Analyzing");
                                error.add("Error While Analyzing");
                                error.add(e.getMessage());
                                comparisonResults.put(comparisonResults.size(), error);
                            }
                        } else {
                            malformed = true;
                        }
                    } else {
                        malformed = true;
                    }
                } else {
                    malformed = true;
                }
            } else {
                malformed = true;
            }
            if (malformed) {
                ArrayList<Object> list = new ArrayList<>();
                list.add(file.getName());
                list.add("Folder was not formatted correctly");
                list.add("Folder was not formatted correctly");
                list.add("Folder was not formatted correctly");
                comparisonResults.put(comparisonResults.size(), list);
            }
            malformed = false;
            x++;
            double progress = x / ((double) foldersToAnalyze.length);
            guiLabelManagement.setProgressIndicator(progress);

        }
        finish(mainDirName);
    }


    /**
     * @return Returns all the headers used by the report file
     */
    private ArrayList<Object> getReportHeaders() {
        ArrayList<Object> headers = new ArrayList<>();
//                                headers.add("Paper Title");
        headers.add("File name");
        headers.add("Number cites A");
        headers.add("Number cites B");
        headers.add("Number cites A&B");
        headers.add("Adjacent-Cit Rate");
        headers.add("Error");
        return headers;
    }

    /**
     * Prints all the relevant information of the twins.
     */
    private void printTwinInfo(int twinId, String paper1, String paper2, String author1, String author2, int year1,
                               int year2) {

        Logger.getInstance().newLine();
        Logger.getInstance().writeToLogFile("---------------------ANALYZING TWIN " + twinId + "\n" +
                paper1 + " " + author1 + " " + year1 + "\n" +
                paper2 + " " + author2 + " " + year2);

        System.out.println();
        System.out.println("-------------------------------------------------------------");
        System.out.println("Analyzing twin: " +twinId);
        System.out.println("Twin 1: " + paper1);
        System.out.println("Authors 1: " + author1);
        System.out.println("Year 1: " + year1);
        System.out.println();
        System.out.println("Twin 2: " + paper2);
        System.out.println("Authors 2: " + author2);
        System.out.println("Year 2: " + year2);
        System.out.println("-------------------------------------------------------------");

    }

    /**
     * Adds the current Twin result analysis to the overall output file
     */
    private void addToOverallOutputFile(File currFile, TreeMap<Integer, ArrayList<Object>> dataGathered) {
        ArrayList<Object> output = new ArrayList<>();

        ArrayList<Double> adjCitationRateList = new ArrayList<>();
        int numOfNAs = 0;

        //Name of the directory
        output.add(currFile.getName());

        //Go through the captured data and store only the adj citation rate.
        // To do this, we only care about the 4
        for (int i : dataGathered.keySet()) {
            //Ignore headers
            if (i == 0) continue;
            List list = dataGathered.get(i);
            //The adj citation is at the 4th index
            Object adjCitation = list.get(4);
            if (adjCitation instanceof String) {
                //In this case we have an N/A
                numOfNAs++;
            } else  {
                adjCitationRateList.add((Double) adjCitation);
            }

        }
        //Count the total number of files analyzed
        output.add(dataGathered.size() - 1);

        //Number of N/As (Files that could not be process for some reason)
        output.add(numOfNAs);

        //Calculate the average joint citation rate across all papers (Without considering any N/As).
        int i = 0;
        Double result = 0.0;
        for (Double adjCitationRate : adjCitationRateList) {
            result = ((result * i) + adjCitationRate)/(i+1);
            i++;
        }
        output.add(result);

        comparisonResults.put(comparisonResults.size(), output);


    }


    @Override
    protected Object call() {
        guiLabelManagement.setStatus("Analyzing files...");
        initialize();
        guiLabelManagement.setStatus("Analyzing files...");
        analyze();
        guiLabelManagement.setStatus("Done");
        return null;
    }

    /**
     * Use this to catch any exceptions
     */
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
    }


}
