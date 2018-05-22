package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.scene.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rafaelcastro on 6/20/17.
 * Analyzes a group of files to see if they cite both twins in the same parenthesis.
 */
class FileAnalyzer {
    //Array of files that will be analyzed to see if they cite the twin papers
    private File[] comparisonFiles;
    //Objects that represent the twin files
    private Object twinFile1;
    private Object twinFile2;
    private GUILabelManagement guiLabelManagement;
    //Stores the results of each individual analysis
    private TreeMap<Integer, ArrayList<Object>> dataGathered;
    //xA is the number of times Twin1 is cited
    private Integer xA;
    //xB is the number of times Twin2 is cited
    private Integer xB;
    //xC is the number of times Twin1 & 2 are cited
    private Integer xC;

    //Relevant information of each twin
    private int inputtedYearTwin1;
    private String mainAuthorTwin1Regex;
    private String regexReadyTwin1;
    private String authorsNamesTwin1;
    private String regexReadyTwin2;
    private int inputtedYearTwin2;
    private String mainAuthorTwin2Regex;
    private String authorsNamesTwin2;
    private boolean isMultipleAnalysis;
    private String titleTwin1;

    //Extracted citation of each twin found in a given file
    private String citationTwin1;
    private String citationTwin2;
    private String titleTwin2;

    //Keeps track of all the errors
    private ArrayList<String> errors = new ArrayList<>();
    //True if only 1 reference is numbered, which means we captured the wrong reference
    private boolean thereIsANumberedRefError = false;
    private Text outputText;


    /**
     * Constructor to be used to analyze twin articles
     *
     * @param comparisonFiles    Files that will be analyzed
     * @param twinFile1          Twin1
     * @param twinFile2          Twin2
     * @param guiLabelManagement GuiLabelManagement obj
     */
    FileAnalyzer(File[] comparisonFiles, File twinFile1, File twinFile2, GUILabelManagement guiLabelManagement) {
        this.comparisonFiles = comparisonFiles;
        this.twinFile1 = twinFile1;
        this.twinFile2 = twinFile2;
        this.guiLabelManagement = guiLabelManagement;
        this.isMultipleAnalysis = false;
    }

    /**
     * Used for testing purposes
     */
    FileAnalyzer() {
        this.twinFile1 = null;
        this.twinFile2 = null;
        this.comparisonFiles = new File[0];
        this.isMultipleAnalysis = false;
    }

    /**
     * Used when analyzing multiple pairs of twins
     *
     * @param dir                Directory with all the files to analyze
     * @param paper1             twin1 obj
     * @param paper2             twin2 obj
     * @param guiLabelManagement GuiLabelManagement obj
     */
    FileAnalyzer(File[] dir, TwinFile paper1, TwinFile paper2, GUILabelManagement guiLabelManagement) {
        this.comparisonFiles = dir;
        this.twinFile1 = paper1;
        this.twinFile2 = paper2;
        this.guiLabelManagement = guiLabelManagement;
        this.isMultipleAnalysis = true;
    }

    /**
     * Performs all the computations to check the number of times Twin1 and Twin2 are cited together.
     */
    void analyzeFiles() {

        //Error checking to enforce that user has completed step 1 and 2
        if (comparisonFiles == null) {
            guiLabelManagement.setAlertPopUp("You have not set the folder with the files that will be analyzed");
        } else if (twinFile1 == null && twinFile2 == null) {
            guiLabelManagement.setAlertPopUp("You have not set the twin files");
        } else {

            dataGathered = new TreeMap<>();

            formatTwins();
            //Goes through each document inside of the folder
            for (int i = 0; i < comparisonFiles.length; i++) {
                File curr = comparisonFiles[i];
                if (!curr.getName().equals(".DS_Store")) {
                    try {
                        System.out.println();
                        DocumentParser parser = new DocumentParser(curr, true, false);
                        citationTwin1 = "";
                        citationTwin2 = "";

                        //Reset the errors
                        errors = new ArrayList<>();
                        thereIsANumberedRefError = false;

                        //Find the citation for twin 1 in the current doc
                        try {
                            citationTwin1 = parser.getReference(regexReadyTwin1, authorsNamesTwin1,
                                    mainAuthorTwin1Regex, inputtedYearTwin1, titleTwin1);
                        } catch (Exception e) {
                            e.printStackTrace();
                            guiLabelManagement.setAlertPopUp(e.getMessage());
                            errors.add("There was an error finding the citation for T1 :" + e
                                    .getMessage());
                        }

                        //Find the citation for twin 2 in the current doc
                        try {
                            citationTwin2 = parser.getReference(regexReadyTwin2, authorsNamesTwin2,
                                    mainAuthorTwin2Regex, inputtedYearTwin2, titleTwin2);
                        } catch (Exception e) {
                            e.printStackTrace();
                            guiLabelManagement.setAlertPopUp(e.getMessage());
                            errors.add("There was an error finding the citation for T2 :" + e
                                    .getMessage());

                        }

                        parser.close();
                        System.out.println("Reference found for Twin1: \n" + citationTwin1); //delete
                        System.out.println("\nReference found for Twin2: \n" + citationTwin2 + "\n");

                        //Number of times A and B are each cited
                        xA = 0;
                        xB = 0;
                        //Number of times A and B are cited together
                        xC = 0;

                        if (citationTwin1.isEmpty()) {
                            System.out.println("Twin 1 is not cited");
                            xA = null;
                        }
                        if (citationTwin2.isEmpty()) {
                            System.out.println("Twin 2 is not cited");
                            xB = null;
                        }

                        if (citationTwin1.isEmpty() && citationTwin2.isEmpty()) {
                            System.out.println("None of the papers are cited");
                        }

                        if (citationTwin1.isEmpty() && citationTwin2.isEmpty()) {
                            //If none of the twin papers is cited, then we move to the next article
                            addToOutput(curr, i, null, null, 0.0, errors);
                            if (!isMultipleAnalysis) {
                                guiLabelManagement.setProgressIndicator((i + 1) / (double) comparisonFiles.length);
                            } else {
                                int finalI = i;
                                Platform.runLater(() -> {
                                    outputText.setText("Files analyzed of current twin: " + new
                                            DecimalFormat("#.##").format((100 * ((finalI + 1) / (double)
                                            comparisonFiles.length))) +
                                            "%");
                                });
                            }
                            continue;
                        }
                        //Check if the references are numbered or not in the bibliography
                        boolean areRefNumbered = isThereARefNumber();
                        ArrayList<String> citationsCurrDoc;
                        //Gets all citations of curr doc
                        try {
                            citationsCurrDoc = parser.getInTextCitations(areRefNumbered);
                            if (parser.getAreRefNumbered() && thereIsANumberedRefError) {
                                //If the references are numeric, but we only have the number for 1 of the references,
                                // then we won't be able to find the in-text citations
                                throw new NumberFormatException("Only one of the references is numbered");
                            }
                        } catch (NumberFormatException e) {
                            numericInTextCitationError(i, curr, e);
                            continue;
                        }
                        if (areRefNumbered && parser.getAreRefNumbered()) {
                            analyzeInTextNumberedRef(citationTwin1, citationTwin2, citationsCurrDoc);

                        } else {
                            analyzeInTextRef(citationTwin1, citationTwin2, inputtedYearTwin1, inputtedYearTwin2,
                                    authorsNamesTwin1, authorsNamesTwin2, citationsCurrDoc, parser);

                        }

                        addToOutput(curr, i, xA, xB, (double) xC, errors);
                        if (!isMultipleAnalysis) {
                            guiLabelManagement.setProgressIndicator((i + 1) / (double) comparisonFiles.length);
                        } else {
                            int finalI1 = i;
                            Platform.runLater(() -> {
                                outputText.setText("Files analyzed of current twin: " + new
                                        DecimalFormat("#" +
                                        ".##").format((100 * ((finalI1 + 1) / (double) comparisonFiles.length))) + "%");
                            });
                        }

                    } catch (Exception e) {
                        addParsingError(i, curr, e);
                    }
                }
            }


        }

    }

    /**
     * Adds an error when there is a problem parsing the document
     */
    private void addParsingError(int i, File curr, Exception e) {
        errors.add("Error reading the file: " + e.getMessage());
        e.printStackTrace();
        guiLabelManagement.setAlertPopUp("ERROR: There was an error parsing document. The file is " +
                "probably corrupted or not supported. " + curr.getName() + "\n" + e.getMessage());
        if (!isMultipleAnalysis) {
            guiLabelManagement.setProgressIndicator((i + 1) / (double) comparisonFiles.length);
        } else {
            Platform.runLater(() -> {
                outputText.setText("Files analyzed of current twin: " + new DecimalFormat("#" +
                        ".##").format((100 * ((i + 1) / (double) comparisonFiles.length))) + "%");
                addToOutput(curr, i, null, null, 0.0, errors);
            });
        }
    }

    /**
     * Adds an error when there is a problem finding the in-text citations of a paper that uses numbered references
     */
    private void numericInTextCitationError(int i, File curr, NumberFormatException e) {
        errors.add("There was an error finding the in-text citations: " + e.getMessage());
        Logger.getInstance().newLine();
        Logger.getInstance().writeToLogFile("ERROR: There was an error finding the in-text " +
                "citations for the document " + curr.getName());
        e.printStackTrace();
        guiLabelManagement.setAlertPopUp("ERROR: There was an error finding the in-text " +
                "citations for the document " + curr.getName());
        if (!isMultipleAnalysis) {
            guiLabelManagement.setProgressIndicator((i + 1) / (double) comparisonFiles.length);
        } else {
            Platform.runLater(() -> outputText.setText(("Files analyzed of current twin: " + new
                    DecimalFormat("#.##").format((100 * ((i + 1) / (double) comparisonFiles.length))) +
                    "%")));
            addToOutput(curr, i, null, null, 0.0, errors);

        }
    }

    /**
     * Checks if the references have a number or not.
     *
     * @return True if the reference has a number.
     */
    private boolean isThereARefNumber() {
        ArrayList<String> citationsToTest = new ArrayList<>();
        boolean reference1isNumbered = false, reference2IsNumbered = false;
        //Do not use an empty citation
        if (!citationTwin1.isEmpty()) {
            citationsToTest.add(citationTwin1);
        }
        if (!citationTwin2.isEmpty()) {
            citationsToTest.add(citationTwin2);
        }
        for (int i = 0; i < citationsToTest.size(); i++) {
            String testCitation = citationsToTest.get(i);
            //Remove any leading or trailing white space
            testCitation = testCitation.replaceAll("^[ \\t]+|[ \\t]+$", "");

            //Check both references to see if they have a number.
            Pattern areRefNumberedPattern = Pattern.compile("(^(([\\[(])|(w x))?\\d+[A-z]?)|(^(w)?\\d+[A-z]?(x )?)");
            Matcher areRefNumberedMatcher = areRefNumberedPattern.matcher(testCitation);
            if (areRefNumberedMatcher.find()) {
                if (i == 0) {
                    reference1isNumbered = true;
                } else {
                    reference2IsNumbered = true;
                }
            }
        }
        //If there is only 1 reference, we return the result (this can only happen if the current paper only
        // cites one twin)
        if (citationsToTest.size() == 1) {
            return reference1isNumbered;
        }

        //If both booleans have the same value, then we return
        if (reference1isNumbered == reference2IsNumbered) {
            return reference1isNumbered;
        }
        //If this happens, then 1 ref is numbered and the other is not, which is an eror
        System.err.println("Only 1 of the references is numbered, which is an error!!!");
        thereIsANumberedRefError = true;
        return true;
    }

    /**
     * Formats the twin files with all the necessary data
     */
    private void formatTwins() {
        //If we are analyzing multiple twins, then we use the TwinFile object
        if (twinFile1.getClass() == TwinFile.class) {
            formatTwinFilesForMultipleAnalysis();

        } else {
            //Use the data inputted by the user instead (For single analysis)
            try {
                //For twin 1
                FileFormatter.setFile((File) twinFile1);
                //Retrieve the authors of the paper (only 3)
                this.authorsNamesTwin1 = FileFormatter.getAuthors();
                //Remove accents
                this.authorsNamesTwin1 = StringUtils.stripAccents(this.authorsNamesTwin1);
                //Get a regex with all possible name combinations of the authors
                this.regexReadyTwin1 = generateReferenceRegex(authorsNamesTwin1, true, false);
                //Get regex for just the main author (in case reference only contains his name)
                String mainAuthorTwin1 = generateReferenceRegex(authorsNamesTwin1, false, false);

                String formattedMainAuthor1Regex;
                if (authorsNamesTwin1.toUpperCase().equals(authorsNamesTwin1)) {
                    //Convert it to Mix Case
                    formattedMainAuthor1Regex = generateReferenceRegex(WordUtils.capitalizeFully(authorsNamesTwin1),
                            false, false);
                    this.authorsNamesTwin1 = WordUtils.capitalizeFully(authorsNamesTwin1);

                } else {
                    //Get regex for just the main author (in case reference only contains his name), but in all CAPS
                    formattedMainAuthor1Regex = generateReferenceRegex(authorsNamesTwin1.toUpperCase(), false, false);
                }

                //Combines both regex
                this.mainAuthorTwin1Regex = "(" + mainAuthorTwin1 + ")|(" + formattedMainAuthor1Regex + ")";
                //Gets the citation for twin1 found in this paper
                //Get the year the twin file was published
                this.inputtedYearTwin1 = FileFormatter.getYear();
                this.titleTwin1 = FileFormatter.getTitle();
                FileFormatter.closeFile();

                //For Twin2
                FileFormatter.setFile((File) twinFile2);
                this.authorsNamesTwin2 = FileFormatter.getAuthors();
                this.authorsNamesTwin2 = StringUtils.stripAccents(this.authorsNamesTwin2);
                this.regexReadyTwin2 = generateReferenceRegex(authorsNamesTwin2, true, false);
                String mainAuthorTwin2 = generateReferenceRegex(authorsNamesTwin2, false, false);
                String formattedMainAuthor2Regex;
                if (authorsNamesTwin2.toUpperCase().equals(authorsNamesTwin2)) {
                    formattedMainAuthor2Regex = generateReferenceRegex(WordUtils.capitalizeFully(authorsNamesTwin2),
                            false, false);
                    this.authorsNamesTwin2 = WordUtils.capitalizeFully(authorsNamesTwin2);

                } else {
                    formattedMainAuthor2Regex = generateReferenceRegex(authorsNamesTwin2.toUpperCase(), false, false);
                }
                this.mainAuthorTwin2Regex = "(" + mainAuthorTwin2 + ")|(" + formattedMainAuthor2Regex + ")";
                this.inputtedYearTwin2 = FileFormatter.getYear();
                this.titleTwin2 = FileFormatter.getTitle();


                FileFormatter.closeFile();

            } catch (IOException e) {
                guiLabelManagement.setAlertPopUp(e.getMessage());
            }
        }


    }

    /**
     * Formats the twin files to be used for multiple twin analysis
     */
    private void formatTwinFilesForMultipleAnalysis() {
        //For paper1
        TwinFile twinFile1Obj = (TwinFile) twinFile1;
        this.authorsNamesTwin1 = twinFile1Obj.getAuthors();
        //Remove any special accents from the names
        this.authorsNamesTwin1 = StringUtils.stripAccents(this.authorsNamesTwin1);
        //Generate regex for all the authors names
        this.regexReadyTwin1 = generateReferenceRegex(authorsNamesTwin1, true, false);
        //Get regex for just the main author (in case reference only contains his name)
        String mainAuthorTwin1 = generateReferenceRegex(authorsNamesTwin1, false, false);
        //Then we also create a regex for the author either in ALL CAPS, or Mix Case, depending on how the
        // original main author name is formatted
        String formattedMainAuthor1Regex;
        if (authorsNamesTwin1.toUpperCase().equals(authorsNamesTwin1)) {
            //Convert it to Mix Case
            formattedMainAuthor1Regex = generateReferenceRegex(WordUtils.capitalizeFully(authorsNamesTwin1),
                    false, false);
            //Save the entire author names in mix caps
            this.authorsNamesTwin1 = WordUtils.capitalizeFully(authorsNamesTwin1);
        } else {
            //Get regex for just the main author (in case reference only contains his name), but in all CAPS
            formattedMainAuthor1Regex = generateReferenceRegex(authorsNamesTwin1.toUpperCase(), false, false);
        }
        //Combines both regex
        this.mainAuthorTwin1Regex = "(" + mainAuthorTwin1 + ")|(" + formattedMainAuthor1Regex + ")";
        //Get the year the twin file was published
        this.inputtedYearTwin1 = twinFile1Obj.getYearPublished();
        //Get the title of the paper
        this.titleTwin1 = twinFile1Obj.getPaperName();

        //For paper2
        TwinFile twinFile2Obj = (TwinFile) twinFile2;
        this.authorsNamesTwin2 = twinFile2Obj.getAuthors();
        this.authorsNamesTwin2 = StringUtils.stripAccents(this.authorsNamesTwin2);
        this.regexReadyTwin2 = generateReferenceRegex(authorsNamesTwin2, true, false);
        String mainAuthorTwin2 = generateReferenceRegex(authorsNamesTwin2, false, false);
        String formattedMainAuthor2Regex;
        if (authorsNamesTwin2.toUpperCase().equals(authorsNamesTwin2)) {
            formattedMainAuthor2Regex = generateReferenceRegex(WordUtils.capitalizeFully(authorsNamesTwin2),
                    false, false);
            this.authorsNamesTwin2 = WordUtils.capitalizeFully(authorsNamesTwin2);

        } else {
            formattedMainAuthor2Regex = generateReferenceRegex(authorsNamesTwin2.toUpperCase(), false, false);
        }
        this.mainAuthorTwin2Regex = "(" + mainAuthorTwin2 + ")|(" + formattedMainAuthor2Regex + ")";
        this.inputtedYearTwin2 = twinFile2Obj.getYearPublished();
        this.titleTwin2 = twinFile2Obj.getPaperName();
    }

    /**
     * Analyzes in-text citations to verify if it cites one or both of the twin papers
     *
     * @param citationTwin1     citation of the first twin
     * @param citationTwin2     citation of the second twin
     * @param inputtedYearTwin1 the year twin1 was published
     * @param inputtedYearTwin2 the year twin2 was published
     * @param authorsNamesTwin1 the names of the authors of twin1
     * @param authorsNamesTwin2 the names of the authors of twin2
     * @param citationsCurrDoc  the in-text citations of the curr doc
     * @param parser            Reference to the current DocumentParser obj
     */
    private void analyzeInTextRef(String citationTwin1, String citationTwin2, int inputtedYearTwin1, int
            inputtedYearTwin2, String authorsNamesTwin1, String authorsNamesTwin2, ArrayList<String>
                                          citationsCurrDoc, DocumentParser parser) {
        //Do it based on authors and year
        //Gets the year that appears on the reference of the twin
        String yearTwin1 = "";
        if (!citationTwin1.isEmpty()) {
            yearTwin1 = getYear(citationTwin1);
            if (!yearTwin1.contains(String.valueOf(inputtedYearTwin1))) {
                //If this happens, we have the wrong reference, so we use the right year.
                yearTwin1 = String.valueOf(inputtedYearTwin1);
            }
        }
        String yearTwin2 = "";
        if (!citationTwin2.isEmpty()) {
            yearTwin2 = getYear(citationTwin2);
            if (!yearTwin2.contains(String.valueOf(inputtedYearTwin2))) {
                yearTwin2 = String.valueOf(inputtedYearTwin2);
            }
        }


        //Generates a regex based online on the first author of the paper
        String authorRegexTwin1 = generateReferenceRegex(authorsNamesTwin1, false, true);
        String authorRegexTwin2 = generateReferenceRegex(authorsNamesTwin2, false, true);


        for (String citation : citationsCurrDoc) {
            boolean aFound = false, bFound = false;

            //If citations matches pattern, return the pattern and compare the year
            String containsCitationResult = "";
            if (!citationTwin1.isEmpty()) {
                containsCitationResult = containsCitation(citation, authorRegexTwin1, authorsNamesTwin1,
                        citationTwin1, parser.isPattern2Used(), 1);
            }

            if (!containsCitationResult.isEmpty() && containsYear(containsCitationResult, yearTwin1)) {
                System.out.println(citation);
                xA = xA + 1;
                aFound = true;
                System.out.println("--Citation is valid Twin1");
            }

            containsCitationResult = "";
            if (!citationTwin2.isEmpty()) {
                containsCitationResult = containsCitation(citation, authorRegexTwin2, authorsNamesTwin2,
                        citationTwin2, parser.isPattern2Used(), 2);
            }
            if (!containsCitationResult.isEmpty() && containsYear(containsCitationResult, yearTwin2)) {
                System.out.println(citation);
                xB = xB + 1;
                bFound = true;
                System.out.println("--Citation is valid Twin2");
            }
            if (aFound && bFound) {
                System.out.println("--Twin citation found");
                xC = xC + 1;
            }
        }
    }


    /**
     * Analyzes the in-text citations where the reference are numbered, and the in-text citations as well.
     * Ex reference: 4. Stewart, John 2010 ....
     * Can parse the following cases:
     * Case 1: When in text citations are numbers between brackets
     * Ex: [4, 5] or  [5]
     * Case 2: When in text citations are numbers, but in the format of superscript
     * Ex: word^(5,6)
     *
     * @param citationTwin1    Citation of the first twin
     * @param citationTwin2    Citation of the second twin
     * @param citationsCurrDoc ArrayList with all the in-text citations of the current doc.
     */
    private void analyzeInTextNumberedRef(String citationTwin1, String citationTwin2, ArrayList<String>
            citationsCurrDoc) {
        citationTwin1 = citationTwin1.replaceAll("^[ \\t]+|[ \\t]+$", "");
        citationTwin2 = citationTwin2.replaceAll("^[ \\t]+|[ \\t]+$", "");
        String referenceNumberOfTwin1 = null;
        Pattern areRefNumberedPattern = Pattern.compile("(^(([\\[(])|(w x))?\\d+[A-z]?)|(^(w)?\\d+[A-z]?(x )?)");
        Matcher areRefNumberedMatcher = areRefNumberedPattern.matcher(citationTwin1);
        if (areRefNumberedMatcher.find()) {
            referenceNumberOfTwin1 = areRefNumberedMatcher.group();
        }
        if (referenceNumberOfTwin1 != null) {
            //Format reference number correctly
            referenceNumberOfTwin1 = formatReferenceNumber(referenceNumberOfTwin1);
        }

        String referenceNumberOfTwin2 = null;
        areRefNumberedMatcher = areRefNumberedPattern.matcher(citationTwin2);
        if (areRefNumberedMatcher.find()) {
            referenceNumberOfTwin2 = areRefNumberedMatcher.group();
        }
        if (referenceNumberOfTwin2 != null) {
            referenceNumberOfTwin2 = formatReferenceNumber(referenceNumberOfTwin2);
        }

        System.out.println("Reference number of twin 1: " + referenceNumberOfTwin1);
        System.out.println("Reference number of twin 2: " + referenceNumberOfTwin2);

        String patter1S = "\\b" + referenceNumberOfTwin1 + "\\b";
        String pattern2S = "\\b" + referenceNumberOfTwin2 + "\\b";

        Pattern pattern1 = Pattern.compile(patter1S);
        Pattern pattern2 = Pattern.compile(pattern2S);

        //Get number
        for (String citation : citationsCurrDoc) {
            Matcher matcher1 = null;
            if (!citationTwin1.isEmpty()) {
                matcher1 = pattern1.matcher(citation);
            }
            Matcher matcher2 = null;
            if (!citationTwin2.isEmpty()) {
                matcher2 = pattern2.matcher(citation);
            }

            boolean aFound = false, bFound = false;

            if (matcher1 != null && matcher1.find()) {
                xA = xA + 1;
                aFound = true;
                System.out.println("-Citation is valid Twin 1");

            }
            if (matcher2 != null && matcher2.find()) {
                xB = xB + 1;
                bFound = true;
                System.out.println("-Citation is valid Twin 2");

            }

            //If citation contains both twin files, then increase counter
            if (aFound && bFound) {
                System.out.println("Twin citations found: " + citation);
                xC = xC + 1;
            }
        }

    }


    /**
     * Adds the current result to the output
     *
     * @param currDocName   The name of the pdf file.
     * @param currDocNumber The number of files the program has analyzed so far.
     * @param xA            Number of times Twin1 is cited
     * @param xB            Number of times Twin2 is cited
     * @param xC            Number of times Twin1 and Twin2 are cited together
     * @param errors        List of errors
     */
    private void addToOutput(File currDocName, int currDocNumber, Integer xA, Integer xB, Double xC, ArrayList<String
            > errors) {
        //Calculation for finding percentage
        //rN=xC/[(xA+xB)/2]
        Double rN;
        ArrayList<Object> list;
        //Try to get the current title name based on the name of the file. (The current name of the file is the
        // folder name where it was located before organizing it)
        String titleOfCurrentPaper = "";
        try {
            titleOfCurrentPaper = UserPreferences.getFolderNameToTitleNameMap().get(currDocName.getName().split("_")
                    [0]);
        } catch (Exception ignored) {
        }


        //Add the name of the title
        //Check if xA or xB are null, this means that there is no citation for one or both of the twin articles
        if (xA == null || xB == null) {
            list = new ArrayList<>();

            list.add(titleOfCurrentPaper);
            list.add(currDocName.getName());
            if (xA == null) {
                list.add("N/A");
                Platform.runLater(() -> outputText.setText("Document " + currDocName.getName() + "\ndoes not cite " +
                        "Twin A"));
            } else {
                list.add(xA);
            }
            if (xB == null) {
                list.add("N/A");
                Platform.runLater(() -> outputText.setText("Document " + currDocName.getName() + "\ndoes not cite " +
                        "Twin B"));
            } else {
                list.add(xB);
            }
            //We can't calculate adj citation
            list.add("N/A");
            Platform.runLater(() -> outputText.setText("Document " + currDocName.getName() + "\ndoes not cite " +
                    "both twins"));
            list.add("N/A");

        } else {
            if (xA + xB == 0) {
                rN = 0.0;
            } else {
                rN = (xC / ((xA + xB) / 2.0)) * 100;
            }
            list = new ArrayList<>();
            list.add(titleOfCurrentPaper);
            list.add(currDocName.getName());
            list.add(xA);
            list.add(xB);
            list.add(xC);
            list.add(rN);
            Platform.runLater(() -> outputText.setText("Document " + currDocName.getName() + "\nCites both papers " +
                    "together " + rN + "%"));
        }

        //Process the errors
        StringBuilder sb = new StringBuilder();
        for (String error : errors) {
            sb.append(error);
            sb.append("\n");
        }
        //Add the errors
        list.add(sb.toString());
        //We let the index 0 be empty because that is reserved for the headings of the excel file
        dataGathered.put(currDocNumber + 1, list);
    }


    /**
     * Generates a regex to find one of the twin papers in the bibliography or in text citations
     *
     * @param authors  Authors of the current twin
     * @param usesAnd  true if there are multiple authors
     * @param isInText true if we are looking for in-text citations, false if we are looking for the bibliography
     * @return String with the regex
     */
    String generateReferenceRegex(String authors, boolean usesAnd, boolean isInText) {
        //Generates all possible references that could be found in a bibliography based on authors names
        //Ex: Xu Luo, X Luo, X. Luo, Luo X. Luo X
        //Splits string by authors names
        List<String> holder = Arrays.asList(authors.split("\\s*,\\s*"));
        ArrayList<String> authorsNames = new ArrayList<>(holder);
        ReferenceFinder.formatAuthorNamesForRegex(authorsNames);
        int authorCounter = 0;
        //Do it based on only the first author's name IMPORTANT ASSUMPTION
        if (!usesAnd) {
            String temp = authorsNames.get(0);
            authorsNames = new ArrayList<>();
            authorsNames.add(temp);
        }

        StringBuilder authorsRegex = new StringBuilder();
        for (String currAuthor : authorsNames) {
            if (authorCounter == 0 && !usesAnd) {
                authorsRegex.append("(");
            }
            if ((authorsNames.size() > 1) && (authorCounter < authorsNames.size())) {
                if (usesAnd) {
                    authorsRegex.append("(?=.*");
                } else {
                    if (authorCounter > 0) {
                        authorsRegex.append("|(");
                    } else {
                        //if it is starting parenthesis
                        authorsRegex.append("(");

                    }
                }
            }
            if (authorsNames.size() == 1) {
                authorsRegex.append("(");
            }
            String[] splitted = currAuthor.split("\\s+");
            StringBuilder possibleCombinations = new StringBuilder();

            possibleCombinations.append("(");
            for (int i = 0; i < splitted.length; i++) {
                if (i > 0) {
                    possibleCombinations.append('|');
                }
                possibleCombinations.append("\\b").append(splitted[i]).append("\\b");
            }
            possibleCombinations.append("))");
            authorsRegex.append(possibleCombinations.toString());


            authorCounter++;
        }
        if (!usesAnd) {
            authorsRegex.append(")");

        }

        if (usesAnd || isInText) {
            return authorsRegex.toString();
        } else {
            //In case we are getting the reference, but we only need one author, them  we search using and for both
            // names of the same author, with the first name as optional. We also include capitalized first and last
            // name.
            String author = authorsRegex.toString();
            author = author.replaceAll("\\(", "");
            author = author.replaceAll("\\)", "");
            String[] mainAuthorName = author.split("\\|");
            //If there is only one name return
            if (mainAuthorName.length == 1) {
                return author;
            }
            StringBuilder newAuthorName = new StringBuilder();
            int limit = mainAuthorName.length - 1;
            int counter = 0;
            for (String s : mainAuthorName) {
                if (!s.isEmpty()) {
                    if (counter < limit) {
                        newAuthorName.append("(?=.*").append("(").append(s).append(")").append(")?");
                    } else {
                        newAuthorName.append("(?=.*").append("(").append(s).append(")").append(")");
                    }
                    counter++;
                }
            }
            return newAuthorName.toString();
        }

    }


    /**
     * Gets the year of a given citation
     *
     * @param citationTwin1 The citation that will be used to extract the year
     * @return String with the year
     */
    private String getYear(String citationTwin1) {
        String pattern = "(\\b((18|19|20)\\d{2}([A-z])*(,( )?(((18|19|20)\\d{2}([A-z])*)|[A-z]))*)\\b)|unpublished " +
                "data|data not shown";
        Pattern yearPattern = Pattern.compile(pattern);
        Matcher matcher = yearPattern.matcher(citationTwin1);
        String answer = "";
        if (matcher.find()) {
            //Year if the first 4 digit number to appear
            answer = matcher.group();
        }
        return answer;
    }


    /**
     * Analyzes if a citation cites one of the twin papers
     *
     * @param citation        The citation we are analyzing
     * @param authorRegex     The regex of the main author
     * @param authorNamesTwin All the names of the twin paper
     * @param citationTwin    The citation of the twin paper
     * @param usesPattern2    true if pattern2 was used to find the reference in the bibliography, false otherwise
     * @return string with the valid citation of the twin
     */
    String containsCitation(String citation, String authorRegex, String authorNamesTwin, String citationTwin, boolean
            usesPattern2, int twinNum) {
        //Remove any accents from the citation we are analyzing
        citation = StringUtils.stripAccents(citation);
        //Remove leading and trailing whitespace
        citationTwin = citationTwin.replaceAll("^[ \\t]+|[ \\t]+$", "");
        //Case 2.1, where in text citations are separated by semi colon
        String patternWithSemicolon = (authorRegex) + "([^;)])*((\\b((18|19|20)\\d{2}([A-z])*((,|( and))( )?((" +
                "(18|19|20)\\d{2}([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";
        //Case 2.2, where in text citation are separated by comma
        String pattern = (authorRegex) + "([^,)])*((\\b((18|19|20)\\d{2}([A-z])*((,|( and))( )?(((18|19|20)\\d{2}" +
                "([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";

        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher = pattern1.matcher(citation);
        ArrayList<String> results = new ArrayList<>();

        while (matcher.find()) {
            String answer = matcher.group();
//            System.out.println("Found the following in-text citation case 2.1: " + answer);
            if (!usesPattern2) {
                //In pattern 2, only the main author name appears in the reference, so the reference can look exactly
                // the same as the in-text citation. Therefore, we do not check for contains citation
                if (!citationTwin.contains(answer)) {
                    //Makes sure that the in text citation did not match the reference citation.
                    results.add(answer);
                }
            } else {
                results.add(answer);
            }
        }
        if (results.size() == 0) {
            Pattern pattern2 = Pattern.compile(patternWithSemicolon);
            Matcher matcher2 = pattern2.matcher(citation);
            while (matcher2.find()) {
                String answer = matcher2.group();
//                System.out.println("Found the following in-text citation case 2.2: " + answer);
                if (!usesPattern2) {
                    if (!citationTwin.contains(answer)) {
                        //Makes sure that the in text citation did not match the reference citation.
                        results.add(answer);
                    }
                } else {
                    results.add(answer);
                }
            }
        }
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            //If for the same in-text citation the program finds more than 1 valid, then it has to solve the tie.
            System.out.println("In-text citation with tie " + citation);
            int yearPublished;
            if (twinNum == 1) {
                yearPublished = inputtedYearTwin1;
            } else {
                yearPublished = inputtedYearTwin2;
            }
            return solveReferenceTies(results, authorNamesTwin, String.valueOf(yearPublished)).get(0);
        } else {
            return "";
        }
    }

    /**
     * Based on Levenshtein Distance, uses the authors names to find the most similar citation
     *
     * @param result  The multiple references that were found
     * @param authors The authors of the twin paper
     * @return ArrayList with one element
     */
    private ArrayList<String> solveReferenceTies(ArrayList<String> result, String authors, String year) {
        ArrayList<String> newResult = new ArrayList<>();
        int smallest = Integer.MAX_VALUE;

        for (String s : result) {
            if (!s.contains(year)) {
                continue;
            }
            int newDistance = StringUtils.getLevenshteinDistance(s, authors);
            if (newDistance == smallest) {
                //Do it based on the first authors name only
                guiLabelManagement.setAlertPopUp("There was an error finding one of the citations, please report to " +
                        "developer");
//                logger.writeToLogFile("There was an error finding one of the citations, please report to developer");
//                logger.newLine();
            }
            if (newDistance < smallest) {
                smallest = newDistance;
                newResult.add(0, s);
            }
        }
        return newResult;
    }


    /**
     * Checks if a citation contains a given year.
     *
     * @param citation Citation to be analyzed
     * @param year     year the paper was published
     * @return true if it contains the year, false otherwise
     */
    boolean containsYear(String citation, String year) {
        StringBuilder yearNumber = new StringBuilder();
        StringBuilder yearLetter = new StringBuilder();
        try {
            String yearNumber1 = String.valueOf(Integer.valueOf(year));
            yearNumber.append(yearNumber1);
        } catch (NumberFormatException e) {
            //If year contains a letter, or is unpublished data or data not shown
            switch (year) {
                case "unpublished data":
                    yearNumber.append("0000");
                    break;
                case "data not shown":
                    yearNumber.append("0000");
                    break;
                default:
                    boolean parsingNumber = true;
                    for (Character c : year.toCharArray()) {

                        if (Character.isDigit(c) && parsingNumber) {
                            yearNumber.append(c);
                        } else {
                            parsingNumber = false;
                            if (c != ' ') {
                                yearLetter = yearLetter.append(c);
                            }
                        }

                    }

                    break;
            }
        }
        String yearString = yearNumber.toString();
        String letterString = yearLetter.toString();
        String yearAndLetterString = yearString + letterString;

        String pattern;

        //If there is no letter, do this
        if (letterString.isEmpty()) {
            pattern = "((\\b" + yearAndLetterString + "\\b)|(unpublished data)|(data not shown))";
        } else {
            pattern = "((\\b" + yearAndLetterString + "\\b)|(\\b(" + yearString + "( )?([A-z]=?[^" + letterString +
                    "])*((,( )?([A-z]=?[^" + letterString + "])*)*(( )?)" + letterString + ")+)\\b)|(unpublished " +
                    "data)|(data not shown))";
        }
        Pattern yearPattern = Pattern.compile(pattern);
        Matcher matcher = yearPattern.matcher(citation);
        return matcher.find();
    }

    /**
     * @return TreeMap with all the data gathered.
     */
    TreeMap<Integer, ArrayList<Object>> getDataGathered() {
        return dataGathered;
    }

    /**
     * Formats the reference number of the reference correctly
     *
     * @param referenceNumber The reference number we are analyzing
     * @return reference number correctly formatted
     */
    private String formatReferenceNumber(String referenceNumber) {
        referenceNumber = referenceNumber.replaceAll("\\[", "");
        referenceNumber = referenceNumber.replaceAll("]", "");
        referenceNumber = referenceNumber.replaceAll("w x", "");
        referenceNumber = referenceNumber.replaceAll("w", "");
        referenceNumber = referenceNumber.replaceAll("x", "");
        referenceNumber = referenceNumber.replaceAll("\\(", "");
        return referenceNumber;
    }

    void setOutputText(Text progressOutputText) {
        this.outputText = progressOutputText;
    }
}

