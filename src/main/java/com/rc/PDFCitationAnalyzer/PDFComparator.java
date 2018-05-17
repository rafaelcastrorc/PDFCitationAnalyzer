package com.rc.PDFCitationAnalyzer;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

/**
 * Created by rafaelcastro on 7/25/17.
 * Compares two directories that contain PDF files by extracting the titles of each file.
 * Outputs a Comparison.xlsx file.
 */
class PDFComparator extends Task {
    private final GUILabelManagement guiLabelManagement;
    private File[] directory1;
    private File[] directory2;
    private File[] directoryMultiple;
    //Results gathered from comparing multiple directories
    private TreeMap<Integer, ArrayList<Object>> comparisonResults;
    private double averageSimilarity = 0.0;
    private int numOfComparedFolders = 0;
    private Text outputText;
    //List that holds all the similarity % gathered
    private ArrayList<Double> listOfResults = new ArrayList<>();
    private boolean organize;
    private String duplicatesFolderLocation = "No duplicates found";
    private int j = 1;

    PDFComparator(GUILabelManagement guiLabelManagement) {
        this.guiLabelManagement = guiLabelManagement;
    }

    /**
     * Stores a directory to use for comparison.
     *
     * @param listOfFiles Files that are part of the directory
     */
    void setDirectory(File[] listOfFiles) {
        if (listOfFiles.length < 1) {
            guiLabelManagement.setAlertPopUp("This directory is empty!");
            return;
        }
        if (directory1 == null) {
            directory1 = listOfFiles;
        } else {
            directory2 = listOfFiles;
            guiLabelManagement.setStatus("Directory 2 has been setup");
        }
    }

    /**
     * Checks of both directories have been set.
     *
     * @return true if both directories have been set, false otherwise.
     */
    boolean isReady() {
        return directory1 != null && directory2 != null;
    }


    /**
     * Initializes the GUI
     */
    private void initialize() {
        //Set up GUI
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setProgressIndicator(0);
        this.outputText = new Text("Comparing the files...");
        outputText.setStyle("-fx-font-size: 18");
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);

        box.getChildren().addAll(guiLabelManagement.getProgressIndicatorNode(), outputText);
        //Add the progress indicator and outputText to the output panel
        guiLabelManagement.setNodeToAddToOutputPanel(box);
    }


    /**
     * Compares multiple directories containing pairs of twin papers
     */
    private void compareMultiple() {
        String mainDirName;
        File file0 = directoryMultiple[0];
        mainDirName = file0.getParentFile().getName();
        ArrayList<Object> header = new ArrayList<>();
        //Headers of the excel output file
        header.add("Twin Paper ID");
        header.add("Number of duplicates");
        header.add("Similarity between the two folders");
        this.comparisonResults = new TreeMap<>();
        comparisonResults.put(0, header);
        int x = 0;
        boolean malformed = false;
        for (File file : directoryMultiple) {
            if (file.isDirectory()) {
                //Check if the folder is a directory and that it contains two folders inside
                File[] files = file.listFiles();
                if (files != null && files.length != 0) {
                    int numOfDirectories = 0;
                    File[] dir1 = null;
                    File[] dir2 = null;
                    for (File possDir : files) {
                        if (possDir.isDirectory()) {
                            numOfDirectories++;
                        }
                        if (numOfDirectories == 1) {
                            dir1 = possDir.listFiles();
                        } else if (numOfDirectories == 2) {
                            dir2 = possDir.listFiles();
                        }
                    }
                    //There has to be exactly two directories
                    if (dir2 != null && dir1 != null && dir1.length != 0 && dir2.length != 0 && numOfDirectories == 2) {
                        directory1 = dir1;
                        directory2 = dir2;
                        compare(true, mainDirName);
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
                list.add("Comparison_" + file.getName());
                list.add("Folder was not formatted correctly");
                list.add("N/A");
                comparisonResults.put(comparisonResults.size(), list);
            }
            malformed = false;
            x++;
            double progress = x / ((double) directoryMultiple.length);
            guiLabelManagement.setProgressIndicator(progress);
        }

        //Output result
        FileOutput fileOutput = new FileOutput();
        try {
            fileOutput.writeOutputToFile(comparisonResults, "Comparison_" + mainDirName + ".xlsx");
        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }

        //Calculate mean
        Double[] array = listOfResults.toArray(new Double[0]);
        Arrays.sort(array);
        double median;
        if (array.length % 2 == 0) {
            median = (array[array.length / 2] + (double) array[array.length / 2 - 1]) / 2;
        } else {
            median = array[array.length / 2];
        }

        //Update GUI
        guiLabelManagement.clearOutputPanel();

        Text outputText = new Text("-Average similarity between every pair of folders: " + averageSimilarity +
                "%\n-Median similarity between every pair of folders: " + median + "\n" +
                "-Comparison_" + mainDirName + ".xlsx has been created.\n" +
                "-You can find the comparison results per folder in Comparison_" + mainDirName);
        if (organize) {
            outputText = new Text("-Average similarity between every pair of folders: " + averageSimilarity +
                    "%\n-Median similarity between every pair of folders: " + median + "\n" +
                    "-Comparison_" + mainDirName + ".xlsx has been created.\n" +
                    "-You can find the comparison results per folder in Comparison_" + mainDirName + "\n-Duplicate" +
                    " files can be found at: " + duplicatesFolderLocation);
        }
        outputText.setStyle("-fx-font-size: 15");
        outputText.setWrappingWidth(400);
        outputText.setTextAlignment(TextAlignment.CENTER);
        //Add the progress indicator and outputText to the output panel
        Text finalOutputText = outputText;
        guiLabelManagement.setNodeToAddToOutputPanel(finalOutputText);


    }


    /**
     * Compares two directories of PDF files based on their titles.
     */
    private void compare(boolean isMultiple, String mainDirName) {
        ArrayList<Object> header = new ArrayList<>();
        //Headers of the excel output file
        header.add("Title");
        header.add("Path of the 1st File");
        header.add("Path of the 2nd File");

        //Get the name of the directory that holds both folders
        String currName;
        File parentDir1 = directory1[0].getParentFile().getParentFile();
        File parentDir2 = directory2[0].getParentFile().getParentFile();
        if (parentDir1.getName().equals(parentDir2.getName())) {
            currName = parentDir1.getName();
        } else {
            currName = parentDir1.getName() + "&" + parentDir2.getName();
        }

        //Maps title to file path
        TreeMap<String, String> map = new TreeMap<>();
        //Stores the result in the given format. Title, path 1st file, path 2nd file
        TreeMap<Integer, ArrayList<Object>> duplicates = new TreeMap<>();
        duplicates.put(0, header);
        DocumentParser documentParser;
        int i = 0;
        for (File file : directory1) {
            if (file.getName().contains("pdf")) {
                try {
                    outputText.setText("Parsing file " + file.getName());
                    documentParser = new DocumentParser(file, false, true);
                    String possibleTitle = documentParser.getTitle();
                    map.put(possibleTitle, file.getPath());
                    documentParser.close();

                } catch (Exception e2) {
                    e2.printStackTrace();
                    guiLabelManagement.setAlertPopUp("Unable to parse file: " + file.getPath() + "\n" + e2.getMessage
                            ());
                }
            }
            i++;
            if (!isMultiple) {
                guiLabelManagement.setProgressIndicator(i / ((double) directory1.length + directory2.length));
            }
        }

        for (File file : directory2) {
            if (file.getName().contains("pdf")) {
                try {
                    outputText.setText("Parsing file " + file.getName());
                    documentParser = new DocumentParser(file, false, true);
                    String possibleTitle = documentParser.getTitle();
                    if (map.keySet().contains(possibleTitle) && !possibleTitle.equals("No title found")) {
                        ArrayList<Object> list = new ArrayList<>();
                        list.add(possibleTitle);
                        String filePathFile1 = map.get(possibleTitle);
                        //Add both file paths to the list
                        list.add(filePathFile1);
                        list.add(file.getPath());
                        //Add it to the duplicates map
                        duplicates.put(i, list);
                        //Organize the files
                        if (organize) {
                            organizeSimilarFiles(mainDirName, currName, filePathFile1);
                        }
                    }
                    documentParser.close();

                } catch (Exception e2) {
                    guiLabelManagement.setAlertPopUp("Unable to parse file: " + file.getPath() + "\n" + e2.getMessage
                            ());
                }
            }
            i++;
            if (!isMultiple) {
                guiLabelManagement.setProgressIndicator(i / ((double) directory1.length + directory2.length));
            }

        }

        outputResult(duplicates, isMultiple, mainDirName, currName);
    }

    /**
     * Creates the excel file with the comparison result, where only the duplicate files appear, and updates the GUI
     *
     * @param duplicates  TreeMap with all the results to output to the excel file
     * @param mainDirName Name of the directory that holds multiple folders (for multiple comparison mode)
     * @param fileName    Name of the directory that holds both folders
     */
    private void outputResult(TreeMap<Integer, ArrayList<Object>> duplicates, boolean isMultiple, String mainDirName,
                              String fileName) {
        guiLabelManagement.clearOutputPanel();
        FileOutput fileOutput = new FileOutput();
        //File name of just the parent directory that contains the two directories that are being compared
        String simpleFileName = "";
        //Filename is the name of the excel file that will be created
        fileName = "Comparison_" + fileName + ".xlsx";
        try {
            simpleFileName = fileName;
            if (isMultiple) {
                File comparison = new File("./Comparison_" + mainDirName);
                if (!comparison.exists()) {
                    //Make comparison folder
                    //noinspection ResultOfMethodCallIgnored
                    comparison.mkdir();
                }
                fileName = "./Comparison_" + mainDirName + "/" + fileName;
            }
            //Output the titles into an excel file
            fileOutput.writeOutputToFile(duplicates, fileName);
        } catch (Exception e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }
        int duplicateNum = duplicates.size() - 1;
        double similarity = ((duplicates.size() - 1) / ((directory1.length + directory2.length)
                / 2.0)) * 100;
        if (!isMultiple) {
            //Once the program is done update GUI
            guiLabelManagement.clearOutputPanel();
            Text outputText = new Text("-Possible duplicates: " + duplicateNum + "\n-Similarity " +
                    "between the 2 folders: " + similarity + "%\n-" + fileName + " has been created!");
            if (organize) {
                outputText = new Text("-Possible duplicates: " + duplicateNum + "\n-Similarity " +
                        "between the 2 folders: " + similarity + "%\n-" + fileName + " has been " +
                        "created!" + "\n-Duplicate files can be found at: " + duplicatesFolderLocation);
            }
            outputText.setStyle("-fx-font-size: 15");
            outputText.setWrappingWidth(400);
            outputText.setTextAlignment(TextAlignment.CENTER);
            //Add the progress indicator and outputText to the output panel
            Text finalOutputText = outputText;
            guiLabelManagement.setNodeToAddToOutputPanel(finalOutputText);
        } else {
            //Add to comparison results (the list with all the results for each directory)
            ArrayList<Object> list = new ArrayList<>();
            list.add(simpleFileName);
            list.add(duplicateNum);
            list.add(similarity);
            comparisonResults.put(comparisonResults.size(), list);
            averageSimilarity = ((averageSimilarity * numOfComparedFolders) + similarity) / (numOfComparedFolders + 1);
            listOfResults.add(similarity);
            numOfComparedFolders++;

        }

    }

    /**
     * Saves only the files that cite both twins in a new location
     *
     * @param mainDirName   Name of the directory that holds multiple folders (for multiple comparison mode)
     * @param directoryName Name of the directory that holds both folders
     * @param filePathFile1 File path of the duplicate in the 1st folder
     */
    private void organizeSimilarFiles(String mainDirName, String directoryName, String filePathFile1) {
        TwinOrganizer twinOrganizer = new TwinOrganizer();
        twinOrganizer.setDeleteFiles(false);
        String path;
        String destFolder;
        //If there is no main directory name
        if (mainDirName.equals("")) {
            destFolder = "./Organized_Comparison_" + directoryName;
            path = destFolder + "/" + j + ".pdf";
            this.duplicatesFolderLocation = "Organized_Comparison_" + directoryName;
        } else {
            destFolder = "./Organized_Comparison_" + mainDirName + "/" + directoryName;
            path = destFolder + "/" + j + ".pdf";
            this.duplicatesFolderLocation = "Organized_Comparison_" + mainDirName;
        }
        File dest = new File(path);

        //If the destination folder does not exist create it
        if (!new File(destFolder).exists()) {
            //noinspection ResultOfMethodCallIgnored
            new File(destFolder).mkdirs();
        }
        try {
            twinOrganizer.copyFolder(new File(filePathFile1), dest);
            //delete the second one since they are the same
//            if (deleteFiles) {
//                File file1 = new File(filePathFile1);
//                File file2 = new File(filePathFile2);
//                file1.delete();
//                file2.delete();
//            }

        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }
        j++;

    }

    @Override
    protected Object call() {
        guiLabelManagement.setStatus("Comparing the files...");
        initialize();
        if (directoryMultiple != null) {
            compareMultiple();
        } else {
            compare(false, "");
        }
        guiLabelManagement.setStatus("Done");
        directory1 = null;
        directory2 = null;
        directoryMultiple = null;
        listOfResults = new ArrayList<>();
        return null;
    }


    void setDirectoryMultiple(File[] directoryMultiple) {
        this.directoryMultiple = directoryMultiple;
        guiLabelManagement.setStatus("The directory has been setup");


    }

    /**
     * Organize duplicates into a new location
     * @param organize  Trye if the user wants to move duplicates into a new location
     */
    void setOrganize(boolean organize) {
        this.organize = organize;
    }

}
