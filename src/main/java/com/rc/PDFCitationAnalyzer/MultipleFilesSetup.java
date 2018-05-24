package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by rafaelcastro on 7/20/17.
 * Calculates multiple pairs of twin papers
 */
public class MultipleFilesSetup extends Task {
    private HashMap<Object, ArrayList<Object>> paperToAuthor;
    private HashMap<Object, ArrayList<Object>> paperToYear;
    private HashMap<Object, ArrayList<Object>> twinIDToPaper;
    private Text outputText;
    private GUILabelManagement guiLabelManagement;
    private File[] foldersToAnalyze;
    private TreeMap<Integer, ArrayList<Object>> comparisonResults;
    private boolean thereIsARange;
    private int end;
    private int start;


    MultipleFilesSetup(GUILabelManagement guiLabelManagement) {
        this.guiLabelManagement = guiLabelManagement;

    }


    void setUpFolder(File[] foldersToAnalyze) {
        this.foldersToAnalyze = foldersToAnalyze;
    }


    /**
     * After we are done analyzing all the files, output the result file
     *
     * @param mainDirName Name of the current parent directory
     */
    private void finish(String mainDirName) {
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setStatus("Done analyzing all the twins!");
        //Output result
        FileOutput fileOutput = new FileOutput();
        try {
            String currDate = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
            fileOutput.writeOutputToFile(comparisonResults, "TwinAnalyzerResults_" + mainDirName + "_" + currDate +
                    ".xlsx");
        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }

        //Update GUI
        guiLabelManagement.clearOutputPanel();

        Platform.runLater(() -> {
            Text outputText = new Text("All files have been analyzed.\n" +
                    "Please go to 'TwinAnalyzerResults_" + mainDirName + ".xlsx'\n" +
                    "to see the overall result of the analysis");
            outputText.setStyle("-fx-font-size: 18;-fx-text-alignment: center");
            outputText.setWrappingWidth(400);
            outputText.setTextAlignment(TextAlignment.CENTER);
            //Add the outputText to the output panel
            guiLabelManagement.setNodeToAddToOutputPanel(outputText);
        });

    }


    /**
     * Initializes the GUI
     */
    private void initialize() {
        //Get all the twin info
        paperToAuthor = TwinFileReader.getPaperToAuthor();
        paperToYear = TwinFileReader.getPaperToYear();
        twinIDToPaper = TwinFileReader.getTwinIDToPaper();
        //Set up GUI
        guiLabelManagement.clearOutputPanel();
        File file = new File("./Analysis");
        if (!file.exists()) {
            file.mkdir();
        }

        guiLabelManagement.setProgressIndicator(0);

        this.outputText = new Text("Analyzing the files...");
        outputText.setStyle("-fx-font-size: 16; -fx-text-alignment: center;-fx-wrap-text: 400px");

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
                        //Check if there is a range, and if so, check that it belongs to the range
                        if (!malformed && thereIsARange) {
                            if (start > twinId || twinId > end) {
                                continue;
                            }
                        }

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

                            printTwinInfo(twinId, paper1, paper2, author1, author2, year1, year2);

                            //Analyze the files
                            fileAnalyzer = new FileAnalyzer(files, twinFile1, twinFile2,
                                    guiLabelManagement);
                            fileAnalyzer.setOutputText(outputText);


                            try {
                                int finalTwinId = twinId;
                                Platform.runLater(() -> outputText.setText("Analyzing twin pair: " + finalTwinId));
                                //Analyze the files
                                fileAnalyzer.analyzeFiles();
                                int finalTwinId1 = twinId;
                                Platform.runLater(() -> outputText.setText("Done analyzing twin pair " + finalTwinId1));
                                //Once its done analyzing, output the results in report file for this specific twin
                                FileOutput output = new FileOutput();
                                ArrayList<Object> headers = getReportHeaders();
                                TreeMap<Integer, ArrayList<Object>> dataGathered = fileAnalyzer.getDataGathered();
                                dataGathered.put(0, headers);
                                //Get the current date
                                String currDate = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
                                output.writeOutputToFile(dataGathered, "Analysis/Report_" + twinId + "_" + currDate +
                                        ".xlsx");
                                Platform.runLater(() -> outputText.setText("Report created for file "));
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
        headers.add("Paper Title");
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
        System.out.println("Analyzing twin: " + twinId);
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
            //The adj citation is at the 5th index
            Object adjCitation = list.get(5);
            if (adjCitation instanceof String) {
                //In this case we have an N/A
                numOfNAs++;
            } else {
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
            result = ((result * i) + adjCitationRate) / (i + 1);
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


    /**
     * Sets the range of twins of the current Twin File List that will be analyzed
     */
    void setRange(boolean thereIsARange, int start, int end) {
        this.thereIsARange = thereIsARange;
        if (thereIsARange) {
            this.start = start;
            this.end = end;
        }
    }


}
