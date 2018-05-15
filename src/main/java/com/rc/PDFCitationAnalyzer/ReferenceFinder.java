package com.rc.PDFCitationAnalyzer;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rafaelcastro on 6/28/17.
 * Finds the way a given twin paper is referenced in the bibliography of an article
 */
class ReferenceFinder {

    private COSDocument cosDoc;
    private PDDocument pdDoc;
    private String parsedText;
    private boolean pattern2Used;

    ReferenceFinder() {

    }

    ReferenceFinder(String parsedText, File fileToParse, boolean pattern2Used) throws IOException {
        this.pattern2Used = pattern2Used;
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);

        //Parse again the doc, but only the section where the bibliography could be located
        PDFParser parser = new PDFParser(new RandomAccessBufferedFileInputStream(fileToParse));
        parser.parse();
        cosDoc = parser.getDocument();

        PDFTextStripper pdfStripper = new PDFTextStripper();
        pdDoc = new PDDocument(cosDoc);

        //Get number of pages
        int numberOfPages = pdDoc.getNumberOfPages();

        if (numberOfPages < 4) {
            //If there are less than 4 pages, then parse the entire paper.
            this.parsedText = parsedText;

        } else if (numberOfPages < 20) {
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
        } else if (numberOfPages > 56 && numberOfPages < 60) {
            pdfStripper.setStartPage(numberOfPages - 40);
            pdfStripper.setEndPage(numberOfPages);
            this.parsedText = pdfStripper.getText(pdDoc);
        } else if (numberOfPages < 150) {
            pdfStripper.setStartPage(numberOfPages / 2);
            pdfStripper.setEndPage(numberOfPages);
            this.parsedText = pdfStripper.getText(pdDoc);
        } else {
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
        TreeSet<String> result = new TreeSet<>(Collections.reverseOrder());
        System.out.println("Pattern 1: " + patternCase1);

        while (matcher1.find()) {
            String nResult = matcher1.group();

            result.add(nResult);


        }
        if (result.size() > 1) {
            result = solveReferenceTies(result, authors, String.valueOf(yearPublished));
        }
        //If no reference was found, try searching just with the main author name. This regex is less restrictive
        //Since we are only searching for main author, we will include the year paper was published
        if (result.isEmpty() || (result.size() == 1 && result.first().equals(""))) {
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
            }
            //If there is more than 1 result
            else if (result.size() > 1) {

                ArrayList<String> referencesToRemove = new ArrayList<>();
                ArrayList<String> referencesToAdd = new ArrayList<>();

                //Check if any of the captured results is too long (more than 50 words), if so, we might have
                // possible captured more than 1 reference, so we eliminate any leading text

                for (String reference : result) {
                    if (reference.split(" ").length > 50) {
                        String simplifiedRef = simplifyReference(nResult, pattern2, mainAuthorRegex, yearPublished);
                        //Store the values that we want to change
                        referencesToRemove.add(reference);
                        if (simplifiedRef != null) {
                            referencesToAdd.add(simplifiedRef);
                        }
                    }
                }
                //Remove the old references
                for (String referenceToRemove : referencesToRemove) {
                    result.remove(referenceToRemove);
                }
                //Add the new (simplified) references
                result.addAll(referencesToAdd);

                //After formatting the last captured reference correctly, compare it to the other captured references
                result = solveReferenceTies(result, authors, String.valueOf(yearPublished));
                //The first result will be the most similar to the reference that we are looking for
                return result.first();
            } else {
                //There is only one captured reference
                String resultToReturn = result.first();

                //If we grabbed more than one numbered reference, we select just the last one
                if (resultToReturn.split(" ").length > 50) {
                    resultToReturn = simplifyReference(resultToReturn, pattern2, mainAuthorRegex, yearPublished);
                    if (resultToReturn == null) {
                        resultToReturn = "";
                    }
                }

                return resultToReturn;
            }
        }
        //Check if there are any ties remaining
        if (result.size() > 1) {
            result = solveReferenceTies(result, authors, String.valueOf(yearPublished));
        }
        return result.first();
    }

    /**
     * Simplifies a reference if it is too long, which means that we PROBABLY capture more than 1 reference so we
     * need to remove the leading text and split the reference if necessary
     * @param citation the citation that we want to simplify
     * @param citationRegex The citation pattern that we originally used to capture this citation
     * @param mainAuthorRegex The regex that captures the main author
     * @param yearPublished The year the paper was published
     *
     */
    private String simplifyReference(String citation, Pattern citationRegex, String mainAuthorRegex, int yearPublished) {
        //Try separating the current captured reference in case we captured multiple numbered references
        ArrayList<String> referencesFound = separateMultipleReferences(citation, mainAuthorRegex, yearPublished);
        String simplifiedReference = null;
        for (String currCitation : referencesFound) {
            simplifiedReference = currCitation;
            boolean canSimplify = true;
            //Remove any leading text
            while (canSimplify) {
                //Handle whitespace
                citation = citation.replaceAll("^[ \\t]+|[ \\t]+$", "");

                //Remove the first line of the citation.
                Pattern firstSentencePattern = Pattern.compile("\\d.*");
                Matcher firstSentenceMatcher = firstSentencePattern.matcher(citation);
                if (firstSentenceMatcher.find()) {
                    String firstSentence = firstSentenceMatcher.group();
                    if (firstSentence.isEmpty()) {
                        canSimplify = false;
                    }
                    citation = citation.replace(firstSentence, "");
                } else {
                    canSimplify = false;
                }
                //See if it still matches the original regex  (to make sure we do not remove the text of the actual
                // citation)
                Matcher citationMatcher = citationRegex.matcher(citation);
                if (citationMatcher.find()) {
                    //If so, update the simplified reference
                    citation = citationMatcher.group();
                    simplifiedReference = citation;
                } else {
                    canSimplify = false;
                }
            }
        }

        return simplifiedReference;
    }

    /**
     * If we captured a reference that is numbered, and the captured references contains multiple references (which
     * is an error), this method separates it into the multiple references that it contains.
     */
    private ArrayList<String> separateMultipleReferences(String reference, String mainAuthorRegex, int
            yearPublished) {
        ArrayList<String> referencesFound = new ArrayList<>();
        StringBuilder currentReference = new StringBuilder();
        //We only use the main author pattern for efficiency purposes
        Pattern mainAuthorPattern =  Pattern.compile(mainAuthorRegex);
        //Remove any leading and trailing space
        reference = reference.replaceAll("^[ \\t]+|[ \\t]+$", "");

        //This pattern checks if the line starts with a number
        String numberedLinePatternStr  ="(^(([\\[(])|(w x))?\\d+[A-z]?)|(^(w)?\\d+[A-z]?(x )?)";
        Pattern numberedLinePattern = Pattern.compile(numberedLinePatternStr);
        //This pattern checks if the end of a line has a space || non word character,  and then an enter, which means
        // that it is part of a paragraph
        Pattern partOfParagraphPattern = Pattern.compile("(\\s|\\W)?(\\n)");
        //This pattern matches everything before a numbered reference. For instance if the numbered reference is [1],
        // this will match [
        Pattern patternBeforeNumber = Pattern.compile("^(.*?)(?=\\d+)");
        //This pattern matches everything after a numbered reference but before a space and a word (which is how we
        // are assuming the text of the citation starts. For instance if the numbered reference is [1], this will
        // match ]
        Pattern patternAfterNumber = Pattern.compile("(^.*?)(?=\\s\\w)");

        //This stores how the numbered references is formatted before the number
        String beforeNumberedReference = "";
        //This stores how the numbered references is formatted after the number
        String afterNumberedReference = "";

        //This pattern matches how the number references are formatted in this paper (Created at runtime after
        // reading the first line)
        Pattern numberedReferenceFormatPattern = null;
        //Check first that the reference is numbered, if not there is nothing we can do to simplify this reference
        Matcher checkIfReferenceIsNumbered = numberedLinePattern.matcher(reference);
        if (!checkIfReferenceIsNumbered.find()) {
            referencesFound.add(reference);
            return referencesFound;
        }

        boolean newParagraph = true;
        boolean isFirstLine = true;

        //Scan the entire reference, line by line
        Scanner scanner = new Scanner(reference);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            //If the we are in a new paragraph, check if it starts with a number, if not we stop
            if (newParagraph) {
                String temp = line;
                temp = temp.replaceAll("^[ \\t]+|[ \\t]+$", "");
                Matcher numberedLineMatcher = numberedLinePattern.matcher(temp);
                Matcher numberedReferenceFormatMatcher = null;
                if (numberedReferenceFormatPattern != null) {
                    numberedReferenceFormatMatcher = numberedReferenceFormatPattern.matcher(temp);
                }
                if (numberedLineMatcher.find() && (isFirstLine || matchesNumberedRefFormat(temp, patternBeforeNumber,
                        beforeNumberedReference, numberedLineMatcher, patternAfterNumber, afterNumberedReference))) {
                    //If it is the first line of the originally captured ref, store how this paper numbers the
                    // references.
                    //Ex: 1. or [1] or [1a] or 1, etc.
                    if (isFirstLine) {
                        //Get the first 10 characters. If there are less than 10 chars in the first line, then this
                        // is not a numbered reference so we cannot simplify it
                        if (temp.length() <= 10) {
                            referencesFound.add(reference);
                            return referencesFound;
                        }
                        String first10Chars = temp.substring(0, 10);
                        //Get everything before the numbered ref
                        Matcher beforeNumberMatcher = patternBeforeNumber.matcher(first10Chars);
                        if (beforeNumberMatcher.find()) {
                            beforeNumberedReference = beforeNumberMatcher.group();
                            //Remove the characters
                            first10Chars = first10Chars.substring(beforeNumberedReference.length(), first10Chars
                                    .length());
                        }
                        //Get the numbered reference
                        String numberedRef = numberedLineMatcher.group();
                        //Remove it from the string
                        first10Chars = first10Chars.substring(numberedRef.length(), first10Chars.length());
                        //Get everything after the numbered ref, but not including the actual text
                        Matcher afterNumberMatcher = patternAfterNumber.matcher(first10Chars);
                        if (afterNumberMatcher.find()) {
                            afterNumberedReference = afterNumberMatcher.group();
                        }
                        //Create the regex that matches the numeric references format used by the current paper
                        numberedReferenceFormatPattern = Pattern.compile("("+beforeNumberedReference+ ")" +
                                ""+numberedLinePatternStr +"("+ afterNumberedReference+")");

                        isFirstLine = false;
                    }
                    //Clear the previous reference
                    currentReference = new StringBuilder();
                    currentReference.append(temp);
                    newParagraph = false;
                }
                //We might still be in the previous paragraph, so we check if now it matches
                else {
                    currentReference.append("\n").append(line);
                    Matcher mainAuthorMatcher = mainAuthorPattern.matcher(currentReference.toString());
                    if (mainAuthorMatcher.find() && currentReference.toString().contains(String.valueOf(yearPublished))) {
                        referencesFound.add(currentReference.toString());
                    }
                    newParagraph= true;
                }
            }
            //Check if the line is part of a paragraph
            else {
                currentReference.append("\n").append(line);
                Matcher partOfParagraphMatcher = partOfParagraphPattern.matcher(line);
                //If there is just a new line character, but not a space before, it means that the next line will be
                // a new paragraph
                if (!partOfParagraphMatcher.find()) {
                    newParagraph = true;
                    //Save the current paragraph (which is an entire reference)
                    String newReference = currentReference.toString();
                    //But first check if it matches the regex, if not, we don't use it
                    Matcher mainAuthorMatcher = mainAuthorPattern.matcher(newReference);
                    if (mainAuthorMatcher.find() && newReference.contains(String.valueOf(yearPublished))) {
                        referencesFound.add(newReference);
                    }
                }
            }
        }

        return referencesFound;
    }


    /**
     * Checks if the current numbered string matches the format of the numbered references in this paper
     * For example. If this papers references have the format '1.' this will match a string that starts with '2.' but
     * not one that starts with '2'
     * @return true if it matches, false otherwise
     */
    private boolean matchesNumberedRefFormat(String numberedString, Pattern patternBeforeNumber, String
            beforeNumberedReferenceFormat, Matcher numberedLineMatcher, Pattern patternAfterNumber, String
            afterNumberedReferenceFormat) {
        //If the string has less than 10 characters, then it is not a new numbered reference, its just a string that
        // starts with a number
        if (numberedString.length() <= 10) {
            return false;
        }
        String first10Chars = numberedString.substring(0, 10);
        //Get everything before the numbered ref
        Matcher beforeNumberMatcher = patternBeforeNumber.matcher(first10Chars);
        if (beforeNumberMatcher.find()) {
            String beforeNumberedReference = beforeNumberMatcher.group();
            //Check if it matches the format
            if (!beforeNumberedReference.equals(beforeNumberedReferenceFormat)) return false;
            //Remove the characters
            first10Chars = first10Chars.substring(beforeNumberedReference.length(), first10Chars
                    .length());
        }
        //Get the numbered reference and remove it from the string
        first10Chars = first10Chars.substring(numberedLineMatcher.group().length(), first10Chars.length());
        //Get everything after the numbered ref, but not including the actual text
        Matcher afterNumberMatcher = patternAfterNumber.matcher(first10Chars);
        String afterNumberedReference = "";
        if (afterNumberMatcher.find()) {
            afterNumberedReference = afterNumberMatcher.group();
        }
        //Check if it matches the format, if so, return the result
        return afterNumberedReferenceFormat.equals(afterNumberedReference);

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
        authors = reverseNames(authors);
        TreeSet<String> newResult = new TreeSet<>();
        String possibleResult = "";
        int smallest = Integer.MAX_VALUE;

        LevenshteinDistance distanceCalc = new LevenshteinDistance();
        for (String s : result) {
            String currRef = s;
            if (!s.contains(year)) {
                continue;
            }
            //Take only the authors of the current reference if the current reference is longer than all the authors
            // of the paper (which means that the current reference probably has the title as well and we do need it
            // for comparison purposes)
            if (s.split(" ").length > authors.split(" ").length + 5) {
                int newLength = authors.split(" ").length + 5;
                StringBuilder sb = new StringBuilder();
                for (String word : s.split(" ")) {
                    if (newLength <= 0) {
                        break;
                    }
                    sb.append(word).append(" ");
                    newLength--;
                }
                s = sb.toString();
            }
            int newDistance = distanceCalc.apply(s, authors);

            //If the possible result and the current citation have the same distance, then this can be an problem since
            // there is no objective way to decide
            if (newDistance == smallest) {
                //Check if one is a subset of the other, if so just return the shorter one
                if (possibleResult.contains(currRef)) {
                    possibleResult = currRef;
                } else {
                    Logger log = Logger.getInstance();
                    log.writeToLogFile("ERROR: There was an error solving the tie");
                    log.newLine();
                    //Todo: Ties should not happen so throw an error
                    System.out.println("ERROR: THERE WAS AN ERROR FINDING THE CITATION IN THIS PAPER, " +
                            "PLEASE INCLUDE MORE THAN 3 AUTHORS' NAMES FOR EACH OF THE TWIN PAPERS" +
                            "\nIf the error persist, please inform the developer.");
                }
            }

            if (newDistance < smallest) {
                smallest = newDistance;
                possibleResult = currRef;
            }


        }
        newResult.add(possibleResult);
        return newResult;
    }

    /**
     * Reverse the names of the authors, from fistName lastName, to lastName, firstName
     *
     * @param authors String with all the authors
     * @return String
     */
    private String reverseNames(String authors) {
        if (!authors.contains(",")) return authors;
        StringBuilder reversedAuthors = new StringBuilder();
        String[] authorNamesArray = authors.split(",");
        for (String currAuthor : authorNamesArray) {
            StringBuilder sb = new StringBuilder();
            for (String currAuthorName : currAuthor.split(" ")) {
                sb.insert(0, currAuthorName + " ");
            }
            String namesOfAuthorReversed = sb.toString();
            namesOfAuthorReversed = namesOfAuthorReversed.replaceAll("^[ \\t]+|[ \\t]+$", "");
            reversedAuthors.append(namesOfAuthorReversed).append(", ");
        }
        //Todo" remove trailing ,
        return reversedAuthors.toString().trim();

    }

    /**
     * Closes the file that is being parsed.
     */
    void close() throws IOException {
        try {
            pdDoc.close();
            cosDoc.close();
        } catch (IOException e) {
            throw new IOException("ERROR: There was an error closing the file");
        }

    }

    boolean pattern2Used() {
        return pattern2Used;
    }

}
