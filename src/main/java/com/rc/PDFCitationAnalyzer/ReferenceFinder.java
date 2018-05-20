package com.rc.PDFCitationAnalyzer;

import org.apache.commons.lang3.StringUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rafaelcastro on 6/28/17.
 * Finds the way a given twin paper is referenced in the bibliography of an article.
 */
class ReferenceFinder {

    private COSDocument cosDoc;
    private PDDocument pdDoc;
    private String parsedText;
    private boolean pattern2Used;
    private String authors;
    private String titleOfTwinPaper;

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

    void setAuthorsAndTitleOfCurrentPaper(String authors, String titleOfTwinPaper) {
        this.authors = authors;
        this.titleOfTwinPaper = titleOfTwinPaper;
    }


    /**
     * Gets the full reference used in a given paper to cite a given twin paper.
     * Works in the following cases:
     * For the case when the citations are numbered.
     * Ex: 1. Jacobson MD, Weil M, Raff MC: Programmed cell death in animal development. Cell 1997, 88:347-354.
     * For the case when the citations are not numbered.
     * Ex: Jacobson MD, Weil M, Raff MC: Programmed cell death in animal development. Cell 1997, 88:347-354.
     * And many more..
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

                for (String reference : result) {
                    //Simplify every captured reference. This makes sure that we are capturing the right reference
                    String simplifiedRef = simplifyReference(reference, pattern2, mainAuthorRegex, yearPublished);
                    //Store the values that we want to change only if valid
                    referencesToRemove.add(reference);
                    if (simplifiedRef != null) {
                        referencesToAdd.add(simplifiedRef);
                    }

                }
                //Remove the old references
                for (String referenceToRemove : referencesToRemove) {
                    result.remove(referenceToRemove);
                }
                //Add the new (simplified) references
                result.addAll(referencesToAdd);

                result = removeDuplicates(result);
                //After formatting the last captured reference correctly, compare it to the other captured references
                result = solveReferenceTies(result, authors, String.valueOf(yearPublished));
                //The first result will be the most similar to the reference that we are looking for
                return result.first();
            } else {
                //There is only one captured reference
                String resultToReturn = result.first();

                //Simplify the reference

                resultToReturn = simplifyReference(resultToReturn, pattern2, mainAuthorRegex, yearPublished);
                if (resultToReturn == null) {
                    resultToReturn = "";
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
     * Removes any duplicates in the citations found
     *
     * @return
     */
    private TreeSet<String> removeDuplicates(TreeSet<String> originalResults) {
        HashSet<String> resultsToRemove = new HashSet<>();
        HashSet<String> verified = new HashSet<>();

        for (String comparisonString : originalResults) {
            //Mark it as verified
            verified.add(comparisonString);
            //Check if it is a substring
            for (String curr : originalResults) {
                //Make sure we have not verified this string already
                if (verified.contains(curr)) {
                    continue;
                }
                String longerStr = comparisonString;
                String shorterStr = curr;
                //Check which one is longer
                if (comparisonString != null && comparisonString.length() < curr.length()) {
                    longerStr = curr;
                    shorterStr = comparisonString;
                }
                //Remove the longer string
                if (longerStr != null && longerStr.contains(shorterStr)) {
                    resultsToRemove.add(longerStr);
                    break;
                }
            }
        }
        //Remove all strings that contain substrings from the original results
        originalResults.removeAll(resultsToRemove);
        return originalResults;
    }

    /**
     * Simplifies a reference if it is too long, which means that we PROBABLY capture more than 1 reference so we
     * need to remove the leading and trailing text, and split the reference if necessary (only possible if its a
     * numbered ref)
     *
     * @param originalCitation the citation that we want to simplify
     * @param citationRegex    The citation pattern that we originally used to capture this citation
     * @param mainAuthorRegex  The regex that captures the main author
     * @param yearPublished    The year the paper was published
     * @return null if the captured reference is wrong (we captured the wrong reference). Else it returns the
     * simplified reference.
     */
    private String simplifyReference(String originalCitation, Pattern citationRegex, String mainAuthorRegex, int
            yearPublished) {
        //Try separating the reference into multiple references if its a numbered ref.
        ArrayList<String> referencesFound = separateMultipleReferences(originalCitation, mainAuthorRegex,
                yearPublished);

        String simplifiedReference = null;
        if (referencesFound.size() > 1) {
            //This happens if, after splitting the reference, there was more than 1 valid citation found
            System.err.println("More than 1 simplified reference found");
            for (String currCitation : referencesFound) {
                System.out.println(currCitation);
            }
        }
        //Go through each of the citations and try to remove any leading text
        for (String currCitation : referencesFound) {
            simplifiedReference = currCitation;
            boolean canSimplify = true;
            //Remove any leading text
            while (canSimplify) {
                //Handle whitespace
                currCitation = currCitation.replaceAll("^[ \\t]+|[ \\t]+$", "");
                //Remove the first line of the citation
                currCitation = currCitation.substring(currCitation.indexOf('\n') + 1);
                if (currCitation.isEmpty() || !currCitation.contains("\n")) {
                    canSimplify = false;
                } else {
                    //See if it still matches the original regex  (to make sure we do not remove the text of the actual
                    // citation)
                    Matcher citationMatcher = citationRegex.matcher(currCitation);
                    if (citationMatcher.find()) {
                        //If so, update the simplified reference
                        currCitation = citationMatcher.group();
                        simplifiedReference = currCitation;
                    } else {
                        canSimplify = false;
                    }
                }

            }
        }

        //Try to see if the simplified ref is numbered, and if so, update it
        if (simplifiedReference != null) {
            String numberedRef = checkIfRefIsNumbered(simplifiedReference, mainAuthorRegex);
            if (!numberedRef.isEmpty()) {
                simplifiedReference = numberedRef;
            }
        }

        if (simplifiedReference != null) {
            //Finally check that the length of the simplified string does not exceed the length of the title of the
            // paper
            // + the names of all the authors + the year published
            int authorsLength = authors.split(" ").length;
            int yearLength = 1;
            int titleLength = titleOfTwinPaper.split(" ").length;
            int extraLength = 15;
            int totalLength = authorsLength + titleLength + yearLength + extraLength;
            if (totalLength < simplifiedReference.split(" ").length) {
                return null;
            }
        }
        return simplifiedReference;
    }

    /**
     * Checks if the current captured reference is numbered.
     * Ex: 21. Rafael Castro...
     *
     * @return A numbered reference, if there is one. If not it returns an empty reference.
     */
    private String checkIfRefIsNumbered(String originalCitation, String mainAuthorRegex) {
        //First check if the references start with a number
        Pattern isRefNumberedPattern = Pattern.compile("(^(([\\[(])|(w x))?\\d+[A-z]?)|(^(w)?\\d+[A-z]?(x )?)");
        Matcher areRefNumberedMatcher = isRefNumberedPattern.matcher(originalCitation);
        if (areRefNumberedMatcher.find()) {
            //If we found it, then we are done
            return originalCitation;
        }
        Pattern authorPattern = Pattern.compile(mainAuthorRegex);
        String modifiedCitation = originalCitation;
        String temporaryCitation = modifiedCitation;
        //If there does not seem to be a reference number,  try to check if there is text before a number.
        //To do so, we remove leading text and continue simplifying as long as the regex matches
        boolean canContinueSimplifying = true;
        while (canContinueSimplifying) {
            //Handle whitespace
            temporaryCitation = temporaryCitation.replaceAll("^[ \\t]+|[ \\t]+$", "");

            //Remove the leading text from the citation until we get to a number
            Pattern leadingTextPattern = Pattern.compile("[^\\d]*");
            Matcher leadingTextMatcher = leadingTextPattern.matcher(temporaryCitation);
            if (leadingTextMatcher.find()) {
                String textToRemove = leadingTextMatcher.group();
                if (textToRemove.isEmpty()) {
                    canContinueSimplifying = false;
                }
                temporaryCitation = StringUtils.replaceOnce(temporaryCitation, textToRemove, "");
            } else {
                canContinueSimplifying = false;
            }

            //Now, after removing the leasing text, see if it still matches to the author pattern (to make sure we do
            // not remove the text of the actual citation)
            Matcher authorMatcher = authorPattern.matcher(temporaryCitation);
            if (authorMatcher.find()) {
                //If so, update the simplified citation
                modifiedCitation = temporaryCitation;
            } else {
                canContinueSimplifying = false;
            }
        }
        //Check if we were able to find a numbered ref at all
        if (originalCitation.equals(modifiedCitation)) {
            //If they are the same, it means that no numbered reference was found
            return "";
        } else {
            return modifiedCitation;
        }


    }

    /**
     * If we captured a reference that is numbered, and the captured references contains multiple references (which
     * is an error), this method separates it into the multiple references that it contains.
     * If it cannot separate it, it just returns the original reference
     */
    private ArrayList<String> separateMultipleReferences(String reference, String mainAuthorRegex, int
            yearPublished) {
        ArrayList<String> referencesFound = new ArrayList<>();
        StringBuilder currentReference = new StringBuilder();
        //We only use the main author pattern for efficiency purposes
        Pattern mainAuthorPattern = Pattern.compile(mainAuthorRegex);
        //Remove any leading and trailing space
        reference = reference.replaceAll("^[ \\t]+|[ \\t]+$", "");

        //This pattern checks if the line starts with a number
        String numberedLinePatternStr = "(^(([\\[(])|(w x))?\\d+[A-z]?)|(^(w)?\\d+[A-z]?(x )?)";
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

        //Check first that the reference is numbered, if not there is nothing we can do to simplify this reference.
        // Also, if the reference is only 1 line we cannot split it so we just return
        Matcher checkIfReferenceIsNumbered = numberedLinePattern.matcher(reference);
        if (!checkIfReferenceIsNumbered.find() || !reference.contains("\n")) {
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
                    if (mainAuthorMatcher.find() && currentReference.toString().contains(String.valueOf
                            (yearPublished))) {
                        referencesFound.add(currentReference.toString());
                    }
                    newParagraph = true;
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
     *
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
     * If there are multiple captured references that match all the previous checks, this method compares each one and
     * returns the one that is the most similar to inputted authors names
     *
     * @param result  - list with all the possible references
     * @param authors - names of the authors of a given twin paper
     * @return arrayList with one element, which is the correct reference.
     */
    TreeSet<String> solveReferenceTies(TreeSet<String> result, String authors, String year) throws
            IllegalArgumentException {
        TreeSet<String> newResult = new TreeSet<>();
        String possibleResult = "";
        int mostPoints = -1;

        for (String currRef : result) {
            //First check that the current citation contains the year the paper was published, if not ignore it
            if (!currRef.contains(year)) {
                continue;
            }

            //Check how many authors are cited and it what order, and give points to the string (more is better)
            int currentPoints = assignPointsToReference(currRef, authors);
            //If it has more points than the current highest, replace the old one
            if (currentPoints > mostPoints) {
                mostPoints = currentPoints;
                possibleResult = currRef;
            } else {

                //If the possible result and the current citation have the same number of points, then this can be an
                // problem since there might not be an objective way to decide
                if (currentPoints == mostPoints) {
                    //First try checking if one is a subset of the other. So check if the new one is a subset of the
                    // older one, and if so, return the new one
                    if (possibleResult.contains(currRef)) {
                        possibleResult = currRef;
                    }
                    //Else if the old one is not a subset of the new one, then they are not subset of each other and
                    // thus we have a tie, which might be a problem
                    else if (!currRef.contains(possibleResult)) {
                        //Calculate Levenshtein Distance as a last resort
                        int prevDistance = calculateLevenshteinDistance(possibleResult, authors);
                        int currDistance = calculateLevenshteinDistance(currRef, authors);
                        if (prevDistance > currDistance) {
                            //If the new distance is smaller, save it
                            possibleResult = currRef;
                        }
                        //If the distances are the same, there is nothing we can do to solve this tie
                        if (prevDistance == currDistance) {
                            //Ties should not happen so return an error
                            Logger log = Logger.getInstance();
                            log.writeToLogFile("ERROR: There was an error solving the tie");
                            log.newLine();
                            System.err.println("ERROR: THERE WAS AN ERROR FINDING THE CITATION IN THIS PAPER, " +
                                    "PLEASE INCLUDE MORE THAN 3 AUTHORS' NAMES FOR EACH OF THE TWIN PAPERS" +
                                    "\nIf the error persist, please inform the developer.");
                        }
                    }
                }
            }


        }
        newResult.add(possibleResult);
        return newResult;
    }

    /**
     * As a last resource, calculate the Levenshtein Distance (smaller is better)
     */
    private Integer calculateLevenshteinDistance(String citation, String authors) {
        //Reverse the names to LastName FirstName
        authors = reverseNames(authors);
        LevenshteinDistance distanceCalc = new LevenshteinDistance();
        //Take only the authors of the current reference if the current reference is longer than all the authors
        // of the paper (which means that the current reference probably has the title as well and we do need it
        // for comparison purposes)
        if (citation.split(" ").length > authors.split(" ").length + 5) {
            int newLength = authors.split(" ").length + 5;
            StringBuilder sb = new StringBuilder();
            for (String word : citation.split(" ")) {
                if (newLength <= 0) {
                    break;
                }
                sb.append(word).append(" ");
                newLength--;
            }
            citation = sb.toString();
        }
        return distanceCalc.apply(citation, authors);

    }

    /**
     * Assigns points to the current reference based on how similar it is to the name of the inputted authors.
     * (The higher, the more similar)
     *
     * @param currentReference Reference that will be analyzed
     * @return Number of points
     */
    private int assignPointsToReference(String currentReference, String authors) {
        int numOfPoints = 0;
        String modifiedReference = currentReference;
        int numOfAuthors = Arrays.asList(authors.split("\\s*,\\s*")).size();
        for (int i = 0; i < numOfAuthors; i++) {
            //Generate a regex for each for the current author
            String currAuthorRegexStr = generateAuthorNameRegex(authors, i);
            String currAuthorRegexStrInCaps = generateAuthorNameRegex(authors.toUpperCase(), i);
            //This pattern matches everything up to the current author name
            Pattern currAuthorPattern = Pattern.compile("[^«]*(" + currAuthorRegexStr + ")|(" +
                    currAuthorRegexStrInCaps +
                    ")");
            //Check if the author name is present in the modified reference (the modified reference keeps track of
            // the order in which the authors appear)
            Matcher currAuthorMatcher = currAuthorPattern.matcher(modifiedReference);
            if (currAuthorMatcher.find()) {
                numOfPoints++;
                //Delete everything that comes before this name (to ensure that the names are in the right order)
                String textToRemove = currAuthorMatcher.group();
                modifiedReference = StringUtils.replaceOnce(modifiedReference, textToRemove, "");
            } else{
                //If the earlier authors are not present, the latter won't be either
                break;
            }
        }

        return numOfPoints;
    }

    /**
     * Generates a regex based on an author's name
     *
     * @param authors     Authors of the current twin
     * @param authorToGet Number of the author you need the regex for (in order of appearance)
     * @return String with the regex
     */
    private String generateAuthorNameRegex(String authors, int authorToGet) {
        //Generates all possible references that could be found in a bibliography based on authors names
        //Ex: Xu Luo, X Luo, X. Luo, Luo X. Luo X
        //Splits string by authors names
        List<String> holder = Arrays.asList(authors.split("\\s*,\\s*"));
        ArrayList<String> authorsNames = new ArrayList<>(holder);
        //Format the author names so that we can use it in the regex
        formatAuthorNamesForRegex(authorsNames);

        int authorCounter = 0;
        //Do the regex based only on the author name we are interested in retrieving
        String temp = authorsNames.get(authorToGet);
        authorsNames = new ArrayList<>();
        authorsNames.add(temp);
        StringBuilder authorsRegex = new StringBuilder();
        for (String currAuthor : authorsNames) {
            if (authorCounter == 0) {
                authorsRegex.append("(");
            }
            if ((authorsNames.size() > 1) && (authorCounter < authorsNames.size())) {
                if (authorCounter > 0) {
                    authorsRegex.append("|(");
                } else {
                    //if it is starting parenthesis
                    authorsRegex.append("(");
                }
            }
            if (authorsNames.size() == 1) {
                authorsRegex.append("(");
            }
            String[] splited = currAuthor.split("\\s+");
            StringBuilder possibleCombinations = new StringBuilder();
            possibleCombinations.append("(");
            for (int i = 0; i < splited.length; i++) {
                if (i > 0) {
                    possibleCombinations.append('|');
                }
                possibleCombinations.append("\\b").append(splited[i]).append("\\b");
            }
            possibleCombinations.append("))");
            authorsRegex.append(possibleCombinations.toString());
            authorCounter++;
        }

        authorsRegex.append(")");


        // We search using and for both names of the same author, with the first name as optional. We also include
        // capitalized first and last name.
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
                    //The last name should NOT be optional and in this case we include it in the result
                    newAuthorName.append("(.*").append("(").append(s).append(")").append(")");
                }
                counter++;
            }
        }
        return newAuthorName.toString();


    }

    /**
     * Formats the author names correctly so that they can be used to build a regular expression
     *
     * @param authorsNames ArrayList with all the author names
     */
    static void formatAuthorNamesForRegex(ArrayList<String> authorsNames) {
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
