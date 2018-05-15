package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by rafaelcastro on 7/20/17.
 * Calculates multiple pairs of twin papers
 */
public class MultipleFilesSetup extends Task {
    private File excelFile;
    private HashMap<Object, ArrayList<Object>> paperToAuthor = new HashMap<>();
    private HashMap<Object, ArrayList<Object>> paperToYear = new HashMap<>();
    private HashMap<Object, ArrayList<Object>> twinIDToPaper = new HashMap<>();
    private Text outputText;
    private Controller controller;
    private GUILabelManagement guiLabelManagement;
    private File[] foldersToAnalyze;
    private TreeMap<Integer, ArrayList<Object>> comparisonResults;


    MultipleFilesSetup(Controller controller, GUILabelManagement guiLabelManagement) {
        this.controller = controller;
        this.guiLabelManagement = guiLabelManagement;

    }

    void setUpFile(File file) {
        this.excelFile = file;
    }

    void setUpFolder(File[] foldersToAnalyze) {
        this.foldersToAnalyze = foldersToAnalyze;
    }


    private void setupTitleList() {
        try {
            readFile(excelFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToMap(String type, Object key, Object objectToAdd) {
        HashMap<Object, ArrayList<Object>> mapToUse;
        switch (type) {
            case "author":
                mapToUse = paperToAuthor;
                break;
            case "year":
                mapToUse = paperToYear;
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
     * Reads the information inside of report.xlsx or any excel file and get all the twin pair information
     *
     * @throws IOException if it is unable to access the file
     */
    private void readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);
        // Get iterator to all the rows in current sheet
        // Traversing over each row of XLSX file
        int twinID = 0;
        for (Row row : mySheet) {
            // For each row, iterate through each columns
            Iterator<Cell> cellIterator = row.cellIterator();

            int i = 0;
            String titleTwin1 = "";
            String titleTwin2 = "";
            int yearTwin1;
            int yearTwin2;
            String authorsTwin1;
            String authorsTwin2;

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();

                if (cell.getCellTypeEnum() == CellType.STRING) {
                    if (twinID == 0) continue;
                    //Map the title of one of the twin papers to the twin ID
                    if (i == 4) {
                        titleTwin1 = cell.getStringCellValue();
                        addToMap("twinID", twinID, titleTwin1);
                    }
                    if (i == 5) {
                        titleTwin2 = cell.getStringCellValue();
                        addToMap("twinID", twinID, titleTwin2);

                    }
                    //Map a twin paper author to its title
                    if (i == 7) {
                        //Make sure the authors are formatted correctly
                        authorsTwin1 = cell.getStringCellValue();
                        authorsTwin1 = formatAuthors(authorsTwin1);
                        addToMap("author", titleTwin1, authorsTwin1);
                    }
                    //Map a twin paper author to its title
                    if (i == 9) {
                        //Make sure the authors are formatted correctly
                        authorsTwin2 = cell.getStringCellValue();
                        authorsTwin2 = formatAuthors(authorsTwin2);
                        addToMap("author", titleTwin2, authorsTwin2);
                    }

                } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                    if (i == 0) {
                        //Get the pairID/twinID
                        if (twinID == (int) cell.getNumericCellValue()) break;
                        twinID = (int) cell.getNumericCellValue();
                    }
                    if (twinID == 0) continue;
                    if (i == 8) {
                        yearTwin1 = (int) cell.getNumericCellValue();
                        addToMap("year", titleTwin1, yearTwin1);

                    }
                    if (i == 10) {
                        yearTwin2 = (int) cell.getNumericCellValue();
                        addToMap("year", titleTwin2, yearTwin2);

                    }

                } else {
                    cell.getCellTypeEnum();
                }
                i++;

            }
            myWorkBook.close();
            fis.close();
            outputText.setText("Done reading excel file");

        }

    }

    /**
     * Formats the author names correctly
     *
     * @param authors
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

    private void finish(String mainDirName) {
        Platform.runLater(() -> {
        controller.getOutputPanel().getChildren().clear();
        controller.updateStatus("Done analyzing all the twins");
        controller.getSetFolderButton().setDisable(false);
        });
        //Output result
        FileOutput fileOutput = new FileOutput();
        try {
            fileOutput.writeOutputToFile(comparisonResults, "TwinAnalyzerResults_" + mainDirName + ".xlsx");
        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }

        //Update GUI
        Platform.runLater(() -> controller.getOutputPanel().getChildren().clear());

        Text outputText = new Text("-Done Analyzing");
        outputText.setStyle("-fx-font-size: 18");
        outputText.setWrappingWidth(400);
        outputText.setTextAlignment(TextAlignment.CENTER);
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().addAll(outputText));
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }


    }


    /**
     * Initializes the GUI
     */
    private void initialize() {
        //Set up GUI
        Platform.runLater(() -> controller.getOutputPanel().getChildren().clear());
        File file = new File("./Analysis");
        if (!file.exists()) {
            file.mkdir();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        guiLabelManagement.getOutput().addListener((observable, oldValue, newValue) ->
                outputText.setText(newValue));

        guiLabelManagement.setProgressIndicator(0);

        this.outputText = new Text("Analyzing the files...");
        outputText.setStyle("-fx-font-size: 18");
        //Add listener
        guiLabelManagement.getOutput().addListener((observable, oldValue, newValue) ->
                outputText.setText(newValue));
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(controller.getProgressIndicator(), outputText);
        //Add the progress indicator and outputText to the output panel
        Platform.runLater(() -> controller.getOutputPanel().getChildren().add(box));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Analyze all the files
     */
    private void analyze() {
        String mainDirName;
        File file0 = foldersToAnalyze[0];
        mainDirName = file0.getParentFile().getName();
        ArrayList<Object> header = new ArrayList<>();
        //Headers of the excel output file
        header.add("Twin Paper ID");
        header.add("Result");
        this.comparisonResults = new TreeMap<>();
        comparisonResults.put(0, header);
        int x = 0;
        boolean malformed = false;
        FileAnalyzer fileAnalyzer = null;
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

                            Logger.getInstance().newLine();
                            Logger.getInstance().writeToLogFile("---------------------ANALYZING TWIN " + twinId + "\n" +
                                    paper1 + " "+ author1 + " " + year1 + "\n"+
                                    paper2 + " "+ author2 + " " + year2);
                            System.out.println("---------------------ANALYZING TWIN " + twinId);

                            //Analyze the files
                            fileAnalyzer = new FileAnalyzer(files, twinFile1, twinFile2,
                                    guiLabelManagement);

                            try {
                                outputText.setText("Analyzing twin pair: " + twinId);
                                fileAnalyzer.analyzeFiles();
                                outputText.setText("Done analyzing twin pair " + twinId);
                                FileOutput output = new FileOutput();
                                ArrayList<Object> list2 = new ArrayList<>();
                                list2.add("Paper");
                                list2.add("Number cites A");
                                list2.add("Number cites B");
                                list2.add("Number cites A&B");
                                list2.add("Adjacent-Cit Rate");
                                TreeMap<Integer, ArrayList<Object>> dataGathered = fileAnalyzer.getDataGathered();
                                dataGathered.put(0, list2);
                                output.writeOutputToFile(dataGathered, "Analysis/Report_" + twinId + ".xlsx");
                                outputText.setText("Report created for file ");
                                fileAnalyzer = null;
                            } catch (Error e) {
                                guiLabelManagement.setAlertPopUp(e.getMessage());
                                throw new IllegalArgumentException(e.getMessage());
                            } catch (IOException e) {
                                guiLabelManagement.setAlertPopUp(e.getMessage());
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
                comparisonResults.put(comparisonResults.size(), list);
            }
            malformed = false;
            x++;
            double progress = x / ((double) foldersToAnalyze.length);
            guiLabelManagement.setProgressIndicator(progress);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        finish(mainDirName);
    }


    @Override
    protected Object call() throws Exception {
        controller.updateStatus("Analyzing files...");
        initialize();
        setupTitleList();
        controller.updateStatus("Analyzing files...");
        analyze();
        Thread.sleep(1000);
        controller.updateStatus("Done");

        return null;
    }


}
