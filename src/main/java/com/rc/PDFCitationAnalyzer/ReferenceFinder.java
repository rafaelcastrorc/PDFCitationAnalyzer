package com.rc.PDFCitationAnalyzer;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rafaelcastro on 6/28/17.
 * Finds the way a given twin paper is referenced in the bibliography of an article
 */
class ReferenceFinder {

    private String parsedText;
    private boolean pattern2Used;

    ReferenceFinder () {

    }
    ReferenceFinder(String parsedText, File fileToParse, boolean pattern2Used) throws IOException {
        this.pattern2Used = pattern2Used;
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);

        //Parse again the doc, but only the section where the bibliography could be located
        PDFParser parser = new PDFParser(new RandomAccessBufferedFileInputStream(fileToParse));
        parser.parse();
        COSDocument cosDoc = parser.getDocument();

        PDFTextStripper pdfStripper = new PDFTextStripper();
        PDDocument pdDoc = new PDDocument(cosDoc);

        //Get number of pages
        int numberOfPages = pdDoc.getNumberOfPages();

        if (numberOfPages < 4) {
            //If there are less than 4 pages, then parse the entire paper.
            this.parsedText = parsedText;

        }
        else if (numberOfPages < 20) {
            //Parse the bottom half of the paper
            if (numberOfPages % 2 != 0) {
                pdfStripper.setStartPage((numberOfPages - 1) / 2);
                pdfStripper.setEndPage(numberOfPages);
                this.parsedText = pdfStripper.getText(pdDoc);
                //If it is odd
            } else {
                //If it is even
                pdfStripper.setStartPage(numberOfPages / 2);
                pdfStripper.setEndPage(numberOfPages);
                this.parsedText = pdfStripper.getText(pdDoc);
            }
        }
        else if (numberOfPages > 56 && numberOfPages < 60 ) {
            pdfStripper.setStartPage(numberOfPages  - 40);
            pdfStripper.setEndPage(numberOfPages);
            this.parsedText = pdfStripper.getText(pdDoc);
        }
        else if (numberOfPages < 150){
                pdfStripper.setStartPage(numberOfPages/2);
                pdfStripper.setEndPage(numberOfPages);
                this.parsedText = pdfStripper.getText(pdDoc);
            }
        else {
            pdfStripper.setStartPage(numberOfPages - 100);
            pdfStripper.setEndPage(numberOfPages);
            this.parsedText = pdfStripper.getText(pdDoc);
        }
        }


    /**
     * Gets the full reference used in a given paper to cite a given twin paper.
     * Works in the following cases:
     * For the case when the citations are numbered.
     * Ex: 1. Jacobson MD, Weil M, Raff MC: Programmed cell death in animal development. Cell 1997, 88:347-354.
     * For the case when the citations are not numbered.
     * Ex: Jacobson MD, Weil M, Raff MC: Programmed cell death in animal development. Cell 1997, 88:347-354.
     * And many more..
     * //Todo: Write more
     * <p>
     * Case 1 - for more than 1 author and Case 2 for only 1 or in case of et al
     *
     * @param allAuthorsRegex - Regex based on the names of the authors of a given twin paper.
     * @param authors         - authors of a given twin paper.
     * @param yearPublished   - year the paper was published
     * @return string with the reference used in the paper. Starts with author name and finishes with the year the
     * paper was published.
     */
    String getReference(String allAuthorsRegex, String authors, String mainAuthorRegex, int yearPublished) throws
            IllegalArgumentException {
        //Pattern use to capture the citation. Starts with the author name and ends with the year the paper was
        // published.
        //String patternCase1 = "[^.\\n]*(\\d+(\\.( ).*))*(" + allAuthorsRegex + ")([^;)])*?((\\b((18|19|20)\\d{2}( )
        // ?([A-z])*(,( )?(((18|19|20)\\d{2}([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";
        String patternCase1 = "[^.\\n]*(\\d+(\\.( ).*))*(" + allAuthorsRegex + ")([^;)])*?((\\b((18|19|20)\\d{2}( )?" +
                "([A-z])*(,( )?(((18|19|20)\\d{2}([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";
        String onlyOneAuthorPattern = "[^.\\n]*(\\d+(\\.( ).*))*((" + mainAuthorRegex + "))([^;)])*?((\\b((" +
                yearPublished + ")( )?([A-z])*(,( )?(((" + yearPublished + ")([A-z])*)|[A-z]))*)\\b)|unpublished " +
                "data|data not shown)";
        //If there is only one author, use the only one author pattern
        int numOfAuthors = authors.split(",").length;
        if (numOfAuthors < 2) {
            //Swap patterns if there is only one author.
            patternCase1 = onlyOneAuthorPattern;
        }
        Pattern pattern1 = Pattern.compile(patternCase1);
        Matcher matcher1 = pattern1.matcher(parsedText);
        Comp comparator = new Comp();
        TreeSet<String> result = new TreeSet<>(Collections.reverseOrder(comparator));
        System.out.println("Pattern 1: " + patternCase1);

        while (matcher1.find()) {
            String nResult = matcher1.group();
            result.add(nResult);

        }
        if (result.isEmpty()) {
            //If no reference was found, try searching just with the main author name. This regex is less restrictive
            //Since we are only searching for main author, we will include the year paper was published
            String patternCase2 = "[^.\\n]*(\\d+(\\.( ).*))*((" + mainAuthorRegex + ")(.* et al)?)([^√])*?(((\\b)?((" +
                    yearPublished + ")( )?([A-z])*(,( )?(((" + yearPublished + ")([A-z])*)|[A-z]))*)\\b)" +
                    "|unpublished data|data not shown)";
            System.out.println("Pattern 2: " + patternCase2);
            Pattern pattern2 = Pattern.compile(patternCase2);
            Matcher matcher2 = pattern2.matcher(parsedText);

            String nResult = null;
            while (matcher2.find()) {
                pattern2Used = true;
                //Add the new result
                nResult = matcher2.group();
                result.add(nResult);
            }
            if (result.isEmpty()) {
                return "";
            } else if (result.size() > 1) {
                //If there is more than 1 result, get the last result and form it correctly
                if (nResult.split(" ").length > 50) {
                    //We possible grabbed more than one numbered reference, so we select just the last one.
                    Pattern pattern3 = Pattern.compile("[^.\\n]*(\\d+(\\.( ).*))*((" + mainAuthorRegex + ")(.* et al)" +
                            "?).*((\\n).*){0,4}((\\b((" + yearPublished + ")( )?([A-z])*(,( )?(((" + yearPublished + ")" +
                            "([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)");
                    Matcher matcher3 = pattern3.matcher(nResult);
                    if (matcher3.find()) {
                        result.remove(nResult);
                        nResult = matcher3.group();
                        result.add(nResult);
                    }
                }
                result =  solveReferenceTies(result, authors, String.valueOf(yearPublished));
                return result.first();
            } else {
                String resultToReturn = result.first();
                if (resultToReturn.split(" ").length > 50) {
                    //We possible grabbed more than one numbered reference, so we select just the last one.
                    Pattern pattern3 = Pattern.compile("[^.\\n]*(\\d+(\\.( ).*))*((" + mainAuthorRegex + ")(.* et al)" +
                            "?).*((\\n).*){0,4}((\\b((" + yearPublished + ")( )?([A-z])*(,( )?(((" + yearPublished + ")" +
                            "([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)");
                    Matcher matcher3 = pattern3.matcher(resultToReturn);
                    if (matcher3.find()) {
                        resultToReturn = matcher3.group();
                    }
                }
                return resultToReturn;
            }
        }
        if (result.size() > 1) {
            result = solveReferenceTies(result, authors, String.valueOf(yearPublished));
        }
        return result.first();
    }

    /**
     * Uses Levenshtein Distance to solve reference ties by using the names to find the most similar reference
     *
     * @param result  - list with all the possible references
     * @param authors - names of the authors of a given twin paper
     * @return arrayList with one element, which is the correct reference.
     */
    TreeSet<String> solveReferenceTies(TreeSet<String> result, String authors, String year) throws
            IllegalArgumentException {
        TreeSet<String> newResult = new TreeSet<>();
        String possibleResult = "";
        int smallest = Integer.MAX_VALUE;

        for (String s : result) {
            if (!s.contains(year)) {
                continue;
            }
            int newDistance = StringUtils.getLevenshteinDistance(s, authors);
            if (newDistance == smallest) {
                Logger log = Logger.getInstance();
                log.writeToLogFile("ERROR: There was an error solving the tie");
                log.newLine();
                //Ties should not happen so throw an error
                throw new IllegalArgumentException("ERROR: THERE WAS AN ERROR FINDING THE CITATION IN THIS PAPER, " +
                        "PLEASE INCLUDE MORE THAN 3 AUTHORS' NAMES FOR EACH OF THE TWIN PAPERS" +
                        "\nIf the error persist, please inform the developer.");
            }

            if (newDistance < smallest) {
                smallest = newDistance;
                possibleResult = s;
            }
        }
        newResult.add(possibleResult);
        return newResult;
    }

    class Comp implements Comparator<String> {
        public int compare(String o1, String o2) {
            return Integer.compare(o1.length(), o2.length());
        }
    }

    boolean pattern2Used() {
        return pattern2Used;
    }

}
