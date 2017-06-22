package com.rc.PDFCitationAnalyzer;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rafaelcastro on 6/20/17.
 */
public class FileAnalyzer {
    private final Controller controller;
    private final File[] comparisonFiles;
    private final File twinFile1;
    private final File twinFile2;
    private TreeMap<Integer, ArrayList<Object>> dataGathered;
    private Text output = new Text();
    private int xA;
    private int xB;
    private int xC;


    FileAnalyzer(Controller controller, File[] comparisonFiles, File twinFile1, File twinFile2, TreeMap<Integer, ArrayList<Object>> dataGathered) {
        this.controller = controller;
        this.comparisonFiles = comparisonFiles;
        this.twinFile1 = twinFile1;
        this.twinFile2 = twinFile2;

        ScrollPane scrollPane = (ScrollPane) controller.getOutputPanel().getChildren().get(0);
        output= (Text) scrollPane.getContent();
        Platform.runLater(() -> scrollPane.setContent(output));
    }

    FileAnalyzer(){
        twinFile1 = null;
        twinFile2 = null;
        comparisonFiles = new File[0];
        controller = null;
    }

    void analyzeFiles() {

        //Error checking to enforce that user has completed step 1 and 2
        if (comparisonFiles == null) {
            controller.displayAlert("You have not set the folder with the files that will be analyzed");
        } else if (twinFile1 == null && twinFile2 == null) {
            controller.displayAlert("You have not set the twin files");
        } else {

            dataGathered = new TreeMap<>();

            //Goes through each document inside of the folder
            for (int i = 0; i < comparisonFiles.length; i++) {
                File curr = comparisonFiles[i];
                if (!curr.getName().equals(".DS_Store")) {
                    try {
                        DocumentParser parser = new DocumentParser(curr, true, false);

                        //For Twin1
                        FileFormatter.setFile(twinFile1);
                        //Retrieve the authors of the paper
                        String authorsNamesTwin1 = FileFormatter.getAuthors();
                        //Get a regex with all possible name combinations of the authors
                        String regexReadyTwin1 = generateReferenceRegex(authorsNamesTwin1, true, false);
                        //Get regex for just the main author (in case reference only contains his name)
                        String mainAuthorTwin1 = generateReferenceRegex(authorsNamesTwin1, false, false);
                        //Gets the citation for twin1 found in this paper
                        String citationTwin1 = "";
                        //Get the year the twin file was published
                        int yearPublished = FileFormatter.getYear();
                        try {
                            citationTwin1 = parser.getReference(regexReadyTwin1, authorsNamesTwin1, mainAuthorTwin1, yearPublished);
                        } catch (Exception e) {
                            controller.displayAlert(e.getMessage());
                        }
                        FileFormatter.closeFile();

                        //For Twin2
                        FileFormatter.setFile(twinFile2);
                        String authorsNamesTwin2 = FileFormatter.getAuthors();
                        String RegexReadyTwin2 = generateReferenceRegex(authorsNamesTwin2, true, false);
                        String mainAuthorTwin2 = generateReferenceRegex(authorsNamesTwin2, false, false);
                        String citationTwin2 = "";
                        int yearPublished2 = FileFormatter.getYear();
                        try {
                            citationTwin2 = parser.getReference(RegexReadyTwin2, authorsNamesTwin2, mainAuthorTwin2, yearPublished2);
                        } catch (Exception e) {
                            controller.displayAlert(e.getMessage());
                        }
                        FileFormatter.closeFile();

                        System.out.println(citationTwin1); //delete
                        System.out.println(citationTwin2);

                        //Number of times A and B are each cited
                        xA = 0;
                        xB = 0;
                        //Number of times A and B are cited together
                        xC = 0;

                        if (citationTwin1.isEmpty() || citationTwin2.isEmpty()) {
                            //If one of the papers is not cited, do not process anything else
                            System.out.println("One of the papers is not cited so result is 0");
                        }

                        if (citationTwin1.isEmpty() && citationTwin2.isEmpty()) {
                            addToOutput(curr, i, 0.0, 0.0, 0.0);
                            parser.close();
                            continue;

                        }

                        String testCitation = citationTwin1;

                        if (citationTwin1.isEmpty()) {
                            testCitation = citationTwin2;
                        }
                        //Check if the references are numbered
                        StringBuilder number = new StringBuilder();
                        for (Character c : testCitation.toCharArray()) {
                            if (c == '.' || c == ' ') {
                                break;
                            }
                            number.append(c);

                        }
                        boolean areRefNumbered = false;
                        String referenceNumberOfTwin = number.toString();
                        Pattern areRefNumberedPattern = Pattern.compile("^([\\[(])?\\d+[A-z]?");
                        Matcher areRefNumberedMatcher = areRefNumberedPattern.matcher(referenceNumberOfTwin);
                        if (areRefNumberedMatcher.find()) {
                            areRefNumbered = true;
                        }

                        //Gets all citations of curr doc
                        ArrayList<String> citationsCurrDoc = parser.getInTextCitations(areRefNumbered);


                        if (areRefNumbered) {
                            getNumberedRef(citationTwin1, citationTwin2, citationsCurrDoc);


                        } else {
                            //Do it based on authors and year
                            //Gets the year that appears on the reference of the twin
                            //todo: use instead the output the user gives . Could use a system of frequencies, most frequent year?
                            String yearTwin1 = "";
                            if (!citationTwin1.isEmpty()) {

                                yearTwin1 = getYear(citationTwin1);
                            }
                            String yearTwin2 = "";
                            if (!citationTwin2.isEmpty()) {
                                yearTwin2 = getYear(citationTwin2);
                            }


                            //Generates a regex based online on the first author of the paper
                            String authorRegexTwin1 = generateReferenceRegex(authorsNamesTwin1, false, true);
                            String authorRegexTwin2 = generateReferenceRegex(authorsNamesTwin2, false, true);


                            for (String citation : citationsCurrDoc) {
                                boolean aFound = false, bFound = false;

                                //If citations matches pattern, return the pattern and compare the year
                                String containsCitationResult = "";
                                if (!citationTwin1.isEmpty()) {
                                    containsCitationResult = containsCitation(citation, authorRegexTwin1, authorsNamesTwin1, citationTwin1);
                                }

                                if (!containsCitationResult.isEmpty() && containsYear(containsCitationResult, yearTwin1)) {
                                    xA = xA + 1;
                                    aFound = true;
                                    System.out.println("--Citation is valid Twin1");
                                }

                                containsCitationResult = "";
                                if (!citationTwin2.isEmpty()) {
                                    containsCitationResult = containsCitation(citation, authorRegexTwin2, authorsNamesTwin2, citationTwin2);
                                }
                                if (!containsCitationResult.isEmpty() && containsYear(containsCitationResult, yearTwin2)) {
                                    xB = xB + 1;
                                    bFound = true;
                                    System.out.println("--Citation is valid Twin2");
                                }
                                if (aFound && bFound) {
                                    xC = xC + 1;
                                }
                            }
                        }

                        addToOutput(curr, i, (double) xA, (double) xB, (double) xC);
                        parser.close();

                    } catch (IOException e) {
                        controller.displayAlert("ERROR: There was an error parsing document " + curr.getName());
                        addToOutput(curr, i, 0.0, 0.0, 0.0);

                    }
                    System.out.println(dataGathered);
                }
            }

        }

    }

    void addToOutput(File currDocName, int currDocNumber, Double xA, Double xB, Double xC) {
        //Calculation for finding percentage
        //rN=xC/[(xA+xB)/2]
        Double rN;
        if (xA + xB == 0) {
            rN = 0.0;
        } else {
            rN = (xC / ((xA + xB) / 2.0)) * 100;
        }
        ArrayList<Object> list = new ArrayList<>();
        list.add(currDocName.getName());
        list.add(xA);
        list.add(xB);
        list.add(xC);
        list.add(rN);
        dataGathered.put(currDocNumber, list);
        Platform.runLater(() -> output.setText("RESULT: Document " + currDocName.getName() + " Cites both papers together " + rN + "%\n" + output.getText()));

    }


    String generateReferenceRegex(String authors, boolean usesAnd, boolean isInText) {
        //Generates all possible references that could be found in a bibliography based on authors names
        //Ex: Xu Luo, X Luo, X. Luo, Luo X. Luo X
        //Splits string by authors names

        List<String> holder = Arrays.asList(authors.split("\\s*,\\s*"));
        ArrayList<String> authorsNames = new ArrayList<>(holder);
        for (int i = 0; i < authorsNames.size(); i++) {
            //Remove single characters like A. or X. or L because it can make the regex produce the wrong result
            String author = authorsNames.get(i);
            author = author.replaceAll("( )?[A-z](\\.)( )?", " ");
            author = author.replaceAll("^[ \\t]+|[ \\t]+$", "");
            authorsNames.remove(i);
            if (!author.isEmpty()) {
                authorsNames.add(i, author);
            }
        }
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
            String[] splited = currAuthor.split("\\s+");
            StringBuilder possibleCombinations = new StringBuilder();

            possibleCombinations.append("(");
            for (int i = 0; i < splited.length; i++) {
                if (i > 0 && i < splited.length) {
                    possibleCombinations.append('|');
                }
                possibleCombinations.append("\\b").append(splited[i]).append("\\b");
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
            //In case we are getting the reference, but we only need one author, them  we search using and for both names of the same author
            String author = authorsRegex.toString();
            author = author.replaceAll("\\(", "");
            author = author.replaceAll("\\)", "");
            String[] mainAuthorName = author.split("\\|");
            //If there is only one author return
            if (mainAuthorName.length == 1) {
                return author;
            }
            StringBuilder newAuthorName = new StringBuilder();
            int counter = 0;
            for (String s : mainAuthorName) {
                if (!s.isEmpty()) {
                    if (counter == 0) {
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


    private String getYear(String citationTwin1) {
        String pattern = "(\\b((18|19|20)\\d{2}([A-z])*(,( )?(((18|19|20)\\d{2}([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown";
        Pattern yearPattern = Pattern.compile(pattern);
        Matcher matcher = yearPattern.matcher(citationTwin1);
        String answer = "";
        if (matcher.find()) {
            //Year if the first 4 digit number to appear
            answer = matcher.group();
        }
        return answer;
    }


    String containsCitation(String citation, String authorRegex, String authorNamesTwin, String citationTwin) {

        //Pattern 1, where in text citations are separated by semi colon
        String patternWithSemicolon = (authorRegex) + "([^;)])*((\\b((18|19|20)\\d{2}([A-z])*((,|( and))( )?(((18|19|20)\\d{2}([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";
        //Pattern 2, where in text citation are separated by comma
        String pattern = (authorRegex) + "([^,)])*((\\b((18|19|20)\\d{2}([A-z])*((,|( and))( )?(((18|19|20)\\d{2}([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";

        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher = pattern1.matcher(citation);
        ArrayList<String> results = new ArrayList<>();

        while (matcher.find()) {
            String answer = matcher.group();
            System.out.println("Found the following in-text citation case 2.1: " + answer);
            if (!answer.equals(citationTwin)) {
                //Makes sure that the in text citation did not match the reference citation.
                results.add(answer);
            }
        }
        if (results.size() == 0) {
            Pattern pattern2 = Pattern.compile(patternWithSemicolon);
            Matcher matcher2 = pattern2.matcher(citation);
            while (matcher2.find()) {
                String answer = matcher2.group();
                System.out.println("Found the following in-text citation case 2.2: " + answer);
                if (!answer.equals(citationTwin)) {
                    //Makes sure that the in text citation did not match the reference citation.
                    results.add(answer);
                }
            }
        }
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            //If for the same in-text citation the program finds more than 1 valid, then it has to solve the tie.
            System.out.println("In-text citation with tie " + citation);
            return solveReferenceTies(results, authorNamesTwin).get(0);
        } else {
            return "";
        }
    }


    //Based on Levenshtein Distance, uses the authors names to find the most similar citation
    private ArrayList<String> solveReferenceTies(ArrayList<String> result, String authors) {
        ArrayList<String> newResult = new ArrayList<>();
        int smallest = Integer.MAX_VALUE;

        for (String s : result) {

            int newDistance = StringUtils.getLevenshteinDistance(s, authors);
            if (newDistance == smallest) {
                //Do it based on the first authors name only
                controller.displayAlert("There was an error finding one of the citations, please report to developer");
            }
            if (newDistance < smallest) {
                smallest = newDistance;
                newResult.add(0, s);
            }
        }
        return newResult;
    }


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
            pattern = "((\\b" + yearAndLetterString + "\\b)|(\\b(" + yearString + "( )?([A-z]=?[^" + letterString + "])*((,( )?([A-z]=?[^" + letterString + "])*)*(( )?)" + letterString + ")+)\\b)|(unpublished data)|(data not shown))";
        }
        Pattern yearPattern = Pattern.compile(pattern);
        Matcher matcher = yearPattern.matcher(citation);
        return matcher.find();
    }



    //Reference are in this format
    //4. Stewart, John 2010.

    //Can parse the following cases:
    //Case 1: When in text citations are numbers between brackets
    //Ex: [4, 5] or  [5]
    //Case 2: When in text citations are numbers, but in the format of superscript
    //Ex: word^(5,6)
    public void getNumberedRef(String citationTwin1, String citationTwin2, ArrayList<String> citationsCurrDoc) {
        StringBuilder number1 = new StringBuilder();
        for (Character c : citationTwin1.toCharArray()) {
            if (c == '.' || c == ' ') {
                break;
            }
            number1.append(c);
        }
        String referenceNumberOfTwin1 = number1.toString();
        Pattern areRefNumberedPattern = Pattern.compile("^([\\[(])?\\d+[A-z]?");
        Matcher areRefNumberedMatcher = areRefNumberedPattern.matcher(referenceNumberOfTwin1);
        if (areRefNumberedMatcher.find()) {
            referenceNumberOfTwin1 = areRefNumberedMatcher.group();
        }
        referenceNumberOfTwin1 = referenceNumberOfTwin1.replaceAll("\\[", "");
        referenceNumberOfTwin1 = referenceNumberOfTwin1.replaceAll("]", "");


        StringBuilder number2 = new StringBuilder();
        for (Character c : citationTwin2.toCharArray()) {
            if (c == '.' || c == ' ') {
                break;
            }
            number2.append(c);
        }
        String referenceNumberOfTwin2 = number2.toString();
        areRefNumberedMatcher = areRefNumberedPattern.matcher(referenceNumberOfTwin2);
        if (areRefNumberedMatcher.find()) {
            referenceNumberOfTwin2 = areRefNumberedMatcher.group();
        }
        referenceNumberOfTwin2 = referenceNumberOfTwin2.replaceAll("\\[", "");
        referenceNumberOfTwin2 = referenceNumberOfTwin2.replaceAll("]", "");
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

            if (!citationTwin1.isEmpty() && matcher1.find()) {
                xA = xA + 1;
                aFound = true;
                System.out.println("-Citation is valid Twin 1");

            }
            if (!citationTwin2.isEmpty() && matcher2.find()) {
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

    public TreeMap<Integer,ArrayList<Object>> getDataGathered() {
        return dataGathered;
    }
}
