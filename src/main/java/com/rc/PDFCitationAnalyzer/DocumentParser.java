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
 * Created by rafaelcastro on 5/15/17.
 * Used to parse PDFs files by converting them to plain text.
 * It uses different context clues to extract the relevant information from the document
 */
class DocumentParser {
    private final Logger log;
    private final RandomAccessBufferedFileInputStream inputStream;
    private COSDocument cosDoc;
    private PDDocument pdDoc;
    private String parsedText = "";
    private String formattedParsedText = "";
    float largestFont;
    float smallestFont;
    private HashMap<Float, Integer> fontSizes;
    private File file;
    float textBodySize;
    private String possibleAuthorsNames;
    int numberOfPages = 0;
    private HashMap<String, Integer> mapNumericCitationToFreq;

    private boolean pattern2Used = false;

    boolean getAreRefNumbered() {
        return areRefNumbered;
    }

    private boolean areRefNumbered = false;

    boolean isPattern2Used() {
        return pattern2Used;
    }

    /**
     * @param fileToParse    - pdf document that needs to be parsed.
     * @param parseEntireDoc - true if you want to parse the entire file, false to parse only the first page
     * @param getFormat      - true if you want to get the format of the text, false if you only want the plain text
     * @throws IOException - If there is an error reading the file
     */
    DocumentParser(File fileToParse, boolean parseEntireDoc, boolean getFormat) throws IOException {
        //Ignore all the logging information of pdfbox
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);

        this.file = fileToParse;
        this.log = Logger.getInstance();
        PDFTextStripper pdfStripper;
        if (!fileToParse.exists() || !fileToParse.canRead()) {
            throw new IOException("ERROR: File does not exist");
        }
        this.inputStream = new RandomAccessBufferedFileInputStream(fileToParse);

        PDFParser parser = new PDFParser(inputStream);
        log.newLine();
        if (!getFormat) {
            log.writeToLogFile("*******FILENAME " + fileToParse.getName());
            System.out.println("*******FILENAME " + fileToParse.getName());
        }
        try {
            parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            try {
                cosDoc = parser.getDocument();
            } catch (IOException e) {
                //Try again
                parser.parse();
                cosDoc = parser.getDocument();
            }

            //To keep track of the fonts.
            largestFont = 0;
            smallestFont = Float.POSITIVE_INFINITY;
            fontSizes = new HashMap<>();

            if (getFormat) {
                pdfStripper = new PDFTextStripper() {
                    float prevFontSize = 0;
                    String prevFont = "";

                    //Modifies the way the text is parsed by including font size
                    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {

                        StringBuilder builder = new StringBuilder();

                        for (TextPosition position : textPositions) {
                            float baseSize = position.getFontSizeInPt();
                            String baseFont = "";

                            boolean isDifferentFontSize = baseSize != prevFontSize;
                            boolean isDifferentFont = isDifferentFontSize;
                            if (!parseEntireDoc) {
                                //If we are getting the title, we need to consider the fonts
                                baseFont = position.getFont().getName();
                                if (baseFont == null || prevFont == null) {
                                    continue;
                                }
                                isDifferentFont = !baseFont.equals(prevFont);
                            }


                            if (isDifferentFontSize || isDifferentFont) {
                                if (!parseEntireDoc) {
                                    builder.append("{|").append(baseSize).append("&").append(baseFont).append("&")
                                            .append

                                                    (position.getYDirAdj()).append("|}");
                                } else {
                                    //Format {|textSize&yPosition|}
                                    builder.append("{|").append(baseSize).append("&").append(position.getYDirAdj())
                                            .append("|}");
                                }
                                prevFontSize = baseSize;
                                prevFont = baseFont;

                                if (smallestFont > baseSize) {
                                    smallestFont = baseSize;
                                }
                                if (largestFont < baseSize) {
                                    largestFont = baseSize;
                                }
                                if (fontSizes.get(baseSize) == null) {
                                    fontSizes.put(baseSize, 1);
                                } else {
                                    int prev = fontSizes.get(baseSize);
                                    fontSizes.put(baseSize, prev + 1);
                                }
                            }
                            builder.append(position.getUnicode());
                        }

                        writeString(builder.toString());
                    }
                };
            } else {
                pdfStripper = new PDFTextStripper();
            }

            pdDoc = new PDDocument(cosDoc);
            pdfStripper.setStartPage(1);
            if (parseEntireDoc) {
                try {
                    pdDoc.getNumberOfPages();
                } catch (IllegalArgumentException e) {
                    throw new IOException("There was a problem parsing this file");
                }
                pdfStripper.setEndPage(pdDoc.getNumberOfPages());
            } else {
                pdfStripper.setEndPage(1);
            }
            if (getFormat) {
                this.formattedParsedText = pdfStripper.getText(pdDoc);
            } else {
                this.parsedText = pdfStripper.getText(pdDoc);
            }
//        if  (!getFormat) {
//            getText(getFormat); //delete
//        }

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }


    /**
     * Gets the plain text from a pdf document. Removes all formatting if desired
     *
     * @return string with all the text
     */
    private String getText(boolean isFormatted) {
        if (isFormatted) {
            System.out.print(formattedParsedText);// delete
            return formattedParsedText;
        } else {
            System.out.print(parsedText);// delete
            return parsedText;
        }
    }


    /**
     * Gets all the in-text citation of a given pdf document
     *
     * @param areRefNumbered - are the references numbered in the bibliography.
     *                       Ex. 1. Ellis, John ... 2010.
     * @return ArrayList with all the citations
     */
    ArrayList<String> getInTextCitations(boolean areRefNumbered) throws IOException, NumberFormatException {
        //Case 1: For the case where in-text citations are displayed as numbers
        //Ex: [1] or [4,5] or [4,5•] or [4•] or [4-20]
        mapNumericCitationToFreq = new HashMap<>();
        ArrayList<String> result1 = getInTextCitationsCase1("");
        int numberOfRefNeeded = 50;

        //Check the type of in text numeric citation () or between [] and count them, to make sure we are not
        // counting invalid citations (There should only be one type).
        int parenthesisCounter = 0;
        int bracketCounter = 0;
        int result1Size = 0;
        for (String s : result1) {
            if (s.contains("(")) {
                parenthesisCounter++;
            } else if (s.contains("[")) {
                bracketCounter++;
            }
            result1Size++;
        }
        if (parenthesisCounter > bracketCounter) {
            result1Size = result1Size - bracketCounter;

        } else
            result1Size = result1Size - parenthesisCounter;

        result1 = verifyIfInvalidCaptured(result1);


        //If there are less than 10 references or there are no references at all, but ref are numbered, then try
        // finding superscript numbered refs
        if (areRefNumbered && (result1.isEmpty() || result1Size < 450)) {
            //First it needs to get the text formatted
            DocumentParser parsedDoc = null;
            numberOfRefNeeded = 80;
            try {
                parsedDoc = new DocumentParser(file, true, true);
            } catch (IOException e) {
                throw new IOException("There was an error parsing the file " + file.getName());
            }
            this.formattedParsedText = parsedDoc.formattedParsedText;
            String superScriptSize = getSuperScriptSize(parsedDoc.fontSizes, parsedDoc.smallestFont);
            parsedDoc.close();
            ArrayList<String> result1Prev = result1;

            try {
                mapNumericCitationToFreq = new HashMap<>();
                result1 = getInTextCitationsCase1(superScriptSize);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                result1 = new ArrayList<>();
            }
            result1 = verifyIfInvalidCaptured(result1);
            if (result1.size() < result1Size) {
                System.out.println("Uses numbered ref");
                //If there were more results before, then use that one
                result1 = result1Prev;
            } else {
                System.out.println("Uses superscript ref");
            }
        }


        //If there are less than 50 numbered in-text citations, and the references are numbered, then try
        // doing case 2.
        if (result1.size() < numberOfRefNeeded || !areRefNumbered) {
            this.areRefNumbered = false;
            return getInTextCitationsCase2(result1, areRefNumbered);
        }
        this.areRefNumbered = true;
        return result1;
    }

    /**
     * Verifies if the same numeric citation appears 95% of the time, which means that is invalid.
     *
     * @param result ArrayList containing all the numeric references
     */
    private ArrayList<String> verifyIfInvalidCaptured(ArrayList<String> result) {
        int numberOfCitations = mapNumericCitationToFreq.size();
        int mostCited = 0;
        String mostCitedCitation = "";
        for (String citation : mapNumericCitationToFreq.keySet()) {
            int curr = mapNumericCitationToFreq.get(citation);
            if (curr > mostCited) {
                mostCited = curr;
                mostCitedCitation = citation;
            }
        }
        //If this is true, remove all the instances ot the faulty reference
        if ((mostCited / (double) numberOfCitations) > 0.95) {
            while (result.contains(mostCitedCitation)) {
                result.remove(mostCitedCitation);
            }
        }
        return result;

    }


    /**
     * Formats a number citation that contains a dash or ± (which is also a dash)
     * Ex: [20-23] It needs to be displayed as [20,21,22,23]
     * Ex: [20,23-25, 27] needs to be displayed as [20, 23, 24, 25, 27]
     *
     * @param citationWithDash - citation that contains the dash
     * @return string with the citation formatted correctly
     */
    String inTextCitationContainsDash(String citationWithDash) {
        if (citationWithDash.contains("refs.")) {
            citationWithDash = citationWithDash.replaceAll("refs\\.( )?", "");
        }
        if (citationWithDash.equals("+/–")) return "";
        //Make sure that there is a number before and after the dash. (So we reject '4- '
        Matcher itsValidDash = Pattern.compile("\\d+.*[-–±].*\\d+").matcher(citationWithDash);
        if (!itsValidDash.find()) {
            return "";
        }
        //Remove empty space and new line characters
        citationWithDash = citationWithDash.replaceAll(" ", "");
        citationWithDash = citationWithDash.replaceAll("\\u0004", "");
        citationWithDash = citationWithDash.replaceAll("\n", "");
        //Keep track of all the numbers found
        ArrayList<Integer> numbersFound = new ArrayList<>();
        //Matches a citation that contains a dash
        Matcher citationWithDashMatcher = Pattern.compile("[0-9]+(?:[-–±]?[0-9]+)?(?:[,;][0-9]+(?:[-–±][0-9]+)?)*")
                .matcher(citationWithDash);
        //Check if there is a valid citation inside the parenthesis (there can only be one)
        int count = 0;
        String tempCitation = "";
        while (citationWithDashMatcher.find()) {
            count++;
            if (count > 1) {
                return "";
            }
            tempCitation = citationWithDashMatcher.group();

        }
        if (count == 0) return "";
        //If there is only one match
        Matcher separateNums = Pattern.compile("([0-9]+)(?:[-–±]([0-9]+))?(?=([,;])|$)").matcher(tempCitation);
        while (separateNums.find()) {
            try {
                //If there is a dash
                if (separateNums.group(2) != null) {
                    int start = Integer.parseInt(separateNums.group(1));
                    //Retrieve every individual number in the rage that the dash represents
                    int end = Integer.parseInt(separateNums.group(2));
                    while (start <= end) {
                        numbersFound.add(start);
                        start++;
                    }
                } else {
                    //If its just a number
                    int num = Integer.parseInt(separateNums.group(1));
                    numbersFound.add(num);
                }

            } catch (NumberFormatException e) {
                return "";
            }
        }

        //Build the new numbered citation
        citationWithDash = " ";
        int counter = 0;
        StringBuilder citationWithDashBuilder = new StringBuilder(citationWithDash);
        for (
                int i : numbersFound)

        {
            if (counter > 0) {
                citationWithDashBuilder.append(',').append(i);
            } else {
                citationWithDashBuilder.append(i);
            }
            counter++;
        }

        citationWithDash = citationWithDashBuilder.toString();
        citationWithDash = citationWithDash.replaceAll("^\\s+", "");
        return citationWithDash;
    }

    /**
     * Closes the file that is being parsed.
     */
    void close() throws IOException {
        try {
            log.writeToLogFile("Closing file");
            log.newLine();
            inputStream.close();
            pdDoc.close();
            cosDoc.close();
        } catch (IOException e) {
            log.writeToLogFile("There was a problem closing the file");
            log.newLine();
            throw new IOException("ERROR: There was an error closing the file");
        }

    }

    /**
     * Gets the title based on a text-analysis of the first page, by finding the string with the largest font that
     * has more than 3 characters
     *
     * @return String with the title of the document
     */
    String getTitle() {
        String result = "";
        //While the title is less than 3 chars, continue looping until find the right title
        TreeMap<Float, Integer> orderedFonts = new TreeMap<>(Collections.reverseOrder());
        this.possibleAuthorsNames = "";
        for (float fontSize : fontSizes.keySet()) {
            orderedFonts.put(fontSize, fontSizes.get(fontSize));
        }
        float titleFont = largestFont;
        while (result.length() < 6) {
            String pattern = "(\\{\\|" + titleFont + ")([^{])*";
            Pattern pattern1 = Pattern.compile(pattern);
            Matcher matcher = pattern1.matcher(formattedParsedText);
            if (matcher.find()) {
                result = matcher.group();
                Pattern patternForAuthorsNames = Pattern.compile(pattern + "\\{\\|\\d*(\\.)" +
                        "?\\d*&[^}]*}[^{]*\\{\\|\\d*(\\.)?\\d*(\\.)?\\d*&[^}]*}[^{]*");
                Matcher matcherForAuthor = patternForAuthorsNames.matcher(formattedParsedText);
                if (matcherForAuthor.find()) {
                    possibleAuthorsNames = matcherForAuthor.group();
                }
            }
            if (result.isEmpty()) {
                return "No title found";
            }

            result = result.replaceAll("(\\{\\|\\d*(\\.)?\\d*&[^|}]*\\|})", " ");
            result = result.replace("\n", " ").replace("\r", " ");
            result = result.replaceAll("^[ \\t]+|[ \\t]+$", "");
            orderedFonts.remove(orderedFonts.firstKey());
            try {
                titleFont = orderedFonts.firstKey();
            } catch (NoSuchElementException e) {
                return "No title found";
            }

        }
        return result;
    }


    /**
     * Gets the authors of the paper by analyzing the text. getTitle() needs to be called before using this method.
     *
     * @return String with all the author names
     * @throws IOException Unable to parse the file
     */
    String getAuthors() throws IOException {
        //Authors are normally right under the title of the paper.
        //So we consider the two lines that come after the title
        if (possibleAuthorsNames.isEmpty()) {
            return "No authors found";
        }
        //The index 2 and 3  contain the two possible author strings.
        String[] result = possibleAuthorsNames.split("\\{");

        for (int i = result.length - 2; i < result.length; i++) {
            possibleAuthorsNames = result[i];

            possibleAuthorsNames = possibleAuthorsNames.replaceAll("\\|\\d*(\\.)?\\d*&[^}]*}", "");
            possibleAuthorsNames = possibleAuthorsNames.replaceAll("[\\n\\r]", "");

            //Remove any special characters
            possibleAuthorsNames = possibleAuthorsNames.replaceAll("[^A-z\\s-.,]", "");
            //Remove any leading or trailing space
            possibleAuthorsNames = possibleAuthorsNames.replaceAll("^[ \\t]+|[ \\t]+$", "");
            //Remove ,. exact match
            possibleAuthorsNames = possibleAuthorsNames.replaceAll(",\\.", "");
            //Change and with no comma before to just comma
            possibleAuthorsNames = possibleAuthorsNames.replaceAll("\\b and", ",");
            //Remove the word and
            possibleAuthorsNames = possibleAuthorsNames.replaceAll("\\band\\b", "");
            //Remove more than 1 space together
            possibleAuthorsNames = possibleAuthorsNames.replaceAll(" {2,}", " ");


            //Remove trailing comma because it can produce errors and make sure that the string is not too long

            //Count the number of spaces in the string to make sure it is not too long or too short.
            //If it is too long, it probably means that we have a string that do not contain the names of the authors
            //If it is too short, it means that we only retrieved one name.
            String[] numOfSpaces = possibleAuthorsNames.split("\\s");
            if (numOfSpaces.length < 15 && numOfSpaces.length > 2) {
                //remove comma
                while (possibleAuthorsNames.endsWith(",")) {
                    possibleAuthorsNames = possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf(","));
                }
                while (possibleAuthorsNames.endsWith(".")) {
                    possibleAuthorsNames = possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf("."));
                }
                return possibleAuthorsNames;
            }

            //Try to find more than one author, if not just return the first one.
            if (numOfSpaces.length <= 2) {
                while (possibleAuthorsNames.endsWith(",")) {
                    possibleAuthorsNames = possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf(","));
                }
                DocumentParser dp = new DocumentParser(file, false, false);
                Pattern authorsPattern = Pattern.compile(possibleAuthorsNames + "(.*)?");
                Matcher matcher2 = authorsPattern.matcher(dp.parsedText);
                if (matcher2.find()) {
                    possibleAuthorsNames = matcher2.group();
                    //Format correctly
                    possibleAuthorsNames = possibleAuthorsNames.replaceAll("[\\n\\r]", "");
                    possibleAuthorsNames = possibleAuthorsNames.replaceAll("[^A-z\\s-.,]", "");
                    possibleAuthorsNames = possibleAuthorsNames.replaceAll("^[ \\t]+|[ \\t]+$", "");
                    possibleAuthorsNames = possibleAuthorsNames.replaceAll(",\\.", "");
                    possibleAuthorsNames = possibleAuthorsNames.replaceAll("\\b and", ",");
                    possibleAuthorsNames = possibleAuthorsNames.replaceAll("\\band\\b", "");
                    possibleAuthorsNames = possibleAuthorsNames.replaceAll(" {2,}", " ");


                    dp.close();

                    while (possibleAuthorsNames.endsWith(",")) {
                        possibleAuthorsNames = possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf(","));
                    }
                    while (possibleAuthorsNames.endsWith(".")) {
                        possibleAuthorsNames = possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf("."));
                    }
                    return possibleAuthorsNames;


                } else {
                    dp.close();
                    return possibleAuthorsNames;
                }
            }


        }
        return "No authors found";
    }


    /**
     * Helper method. Gets all the in text citations that are written as numbers, including superscripts.
     *
     * @param superScriptSize empty string if there no superscript, if not the size that the superscript
     * @return ArrayList with all the citations
     */
    private ArrayList<String> getInTextCitationsCase1(String superScriptSize) {
        ArrayList<String> result = new ArrayList<>();
        Matcher matcher;

        //If there is no super script size
        String textBetweenParenthesis = "";
        if (superScriptSize.isEmpty()) {
            //If there is no superscript, accept in-text references that use numbers. Ex: [1], (1a), (10, 15)
            //Accepts ( or [].
            //For it to accept a numeric reference, it checks that before such reference there is at
            //least one word followed by any characters (this makes sure that the numeric reference is not just some
            // number between parenthesis standing on its own). So for it to be valid, the numeric citation has to be
            // part of a sentence. ([A-z]+[^)\]x\n]*\n?[^)\]x\n]*)
            // For instance, this regex will capture: 'and they found that (19)'
            // And it will ignore strings that start as: '1(201)' or  '(201)'  or '[29]'
//            String patternCase1 = "(\\(|\\[|w)( )?(tBID;)?( ?refs\\. ?)?\\d+([a-z])?(•|\u0004)*(( )?(–|-)( )?\\d+" +
//                    "([a-z])?(•|\u0004)*)*(( )?,( |\\n)*\\d+([a-z])?(•|\u0004)*((( )?(–|-)( )?\\d+([a-z])?)
// (•|\u0004)" +
//                    "*)*)*( )?(\\)|]|x)";
            String textBeforeParenthesis = "([A-z]+[^)\\]x\\n.]*\\n?[^)\\]x\\n]*)";
            textBetweenParenthesis = "(\\(|\\[|w)( )?(tBID;)?( ?refs\\. ?)?\\d+" +
                    "([a-z])?(•|\u0004)*(( )?(–|-)( )?\\d+([a-z])?(•|\u0004)*)*(( )?,( |\\n)*\\d+([a-z])?(•|\u0004)*(" +
                    "(( )?(–|-)( )?\\d+([a-z])?)(•|\u0004)*)*)*( )?(\\)|]|x)";
            Pattern pattern1 = Pattern.compile(textBeforeParenthesis + textBetweenParenthesis);
            matcher = pattern1.matcher(parsedText);

        } else {
            //If there could be superScript in-text citations
            String pattern = "(([^A-z])(\\{\\|(" + superScriptSize + ")&(\\d*(\\.)?\\d*)\\|})(?!\\)|\\" +
                    "(|(-|–)|[A-z]|\\d*\\.|,)([^A-z(}])*)|(([A-z]{2,})(\\{\\|(" + superScriptSize + ")&(\\d*(\\.)" +
                    "?\\d*)\\|})(?!\\)|\\(|(-|–)|[A-z]|\\d*\\.|,)([^A-z(}\\n])*)";
            Pattern pattern1 = Pattern.compile(pattern);
            matcher = pattern1.matcher(formattedParsedText);
        }

        while (matcher.find()) {
            String answer = matcher.group();
            //If we are analyzing a citation without superscripts, then keep anything between the parenthesis
            if (superScriptSize.isEmpty()) {
                Pattern betweenParenthesisPattern = Pattern.compile(textBetweenParenthesis);
                Matcher betweenParenthesisMatcher = betweenParenthesisPattern.matcher(answer);
                if (betweenParenthesisMatcher.find()) {
                    answer = betweenParenthesisMatcher.group();
                }
            }
            //Format the reference correctly
            answer = answer.replaceAll("w", "[");
            answer = answer.replaceAll("x", "]");

            log.writeToLogFile("Found CASE 1 " + answer);
            log.newLine();
            answer = formatSuperScript(answer);
            //If citation contains a '–', it needs to be modified
            if (answer.contains("–") || answer.contains("±") || answer.contains("-")) {
                answer = inTextCitationContainsDash(answer);
            }

            if (!answer.isEmpty()) {
                //If pattern is just a year (2009) do not admit. Or if it is a symbol with a number (#9) or (9+)
                Pattern doNotAccept = Pattern.compile("\\(([(0-9)]{4})\\)|([#©†=η]\\d*)|(\\d*\\+)|(^(·|," +
                        "|\u0003/\u0003|\u0002)$)");
                Matcher invalid = doNotAccept.matcher(answer);

                if (!invalid.find()) {
                    String[] numberOfResults = answer.split(",");
                    //We only take into account in-text citation with no more than 50 numbers. So (1-50) is Valid and
                    // (1 - 100) is not because that is usually not a citation
                    if (numberOfResults.length <= 50) {
                        //Finally check that there is a number in the current citation
                        Matcher isNumber = Pattern.compile("[0-9]").matcher(answer);
                        if (isNumber.find()) {
                            //Count the frequency this reference appers in the currently captured references
                            if (mapNumericCitationToFreq.containsKey(answer)) {
                                int curr = mapNumericCitationToFreq.get(answer);
                                mapNumericCitationToFreq.put(answer, curr + 1);

                            } else {
                                mapNumericCitationToFreq.put(answer, 1);
                            }
                            result.add(answer);
                        }
                    }
                }
            }
        }

        if (result.isEmpty()) {
            log.writeToLogFile("WARNING - Could not find in-text citations for document - CASE 1");
            log.newLine();
        }
        return result;
    }


    /**
     * Gets all the in text citations that are written between parenthesis.
     * Ex: (Castro 2012)
     *
     * @param result1        All the in text citations found using case 1
     * @param areRefNumbered True if the program uses numeric in text citations, false otherwise
     * @return ArrayList with all the citations
     */
    private ArrayList<String> getInTextCitationsCase2(ArrayList<String> result1, boolean areRefNumbered) {
        ArrayList<String> result2 = new ArrayList<>();
        String patternCase2 = "(\\(|（|\\[)[^)]*\\n*(unpublished data|Fig\\. ([0-9-]*(\\n| |;( |\\n)\\D*)(and \\d*)*)" +
                "*|data not shown|\\d{4})([a-zA-Z]((,|(( |\\n)and))( |\\n)?[a-zA-Z])*)*((,|( |\\n)and)( |\\n)" +
                "(unpublished data|data not shown|\\d{4}(\\n)?([a-zA-Z]((,|(( |\\n)and))( |\\n)?[a-zA-Z])*)*))*((;|," +
                "|；)\\D*\\d{4}([a-zA-Z]((,|(( |\\n)and))( |\\n)?[a-zA-Z])*)*((,|(( |\\n)and))( |\\n)(unpublished " +
                "data|data not shown|\\d{4}))*)*([])）])";
        Pattern pattern2 = Pattern.compile(patternCase2);
        Matcher matcher2 = pattern2.matcher(parsedText);

        while (matcher2.find()) {
            String answer = matcher2.group();
            log.writeToLogFile("Found CASE 2 " + answer);
            log.newLine();
            //Make sure that the citation is not just a numbered reference
            // Ex of rejected input: [1a] Rafael Castro, ... , ...  (2010)
            Pattern isNumericRef = Pattern.compile("(^(([\\[(])|(w x))\\d+[A-z]?)(([])])|(x w))");
            Matcher isNumericRefMatcher = isNumericRef.matcher(answer);
            if (isNumericRefMatcher.find()) {
                String textToRemove = isNumericRefMatcher.group();
                //Remove the numeric page
                String tempAns = StringUtils.replaceOnce(answer, textToRemove, "");
                //Try checking if there if there is still a citation
                Matcher matcherTemp = pattern2.matcher(tempAns);
                if (matcherTemp.find()) {
                    //Update the answer
                    answer = matcherTemp.group();
                } else {
                    //If there is no valid citation, just ignore it
                    continue;
                }
            }
            //Make sure that answer is not only the year (2010) or year and letter (2010a)
            Pattern validCitationCase2 = Pattern.compile("[^(0-9) ][A-z]");
            Matcher validation = validCitationCase2.matcher(answer);
            if (validation.find()) {
                //Make sure that the answer is not just a month between parenthesis Ex: (January)
                Pattern monthAndYear = Pattern.compile("(January \\d{4}\\)$)|(February \\d{4}\\)$)|(March \\d{4}\\)$)" +
                        "|(April \\d{4}\\)$)|(May \\d{4}\\)$)|(June \\d{4}\\)$)|(July \\d{4}\\)$)|(August \\d{4}\\)$)" +
                        "|" +
                        "(September \\d{4}\\)$)|(October \\d{4}\\)$)|(November \\d{4}\\)$)|(December \\d{4}\\)$)");
                Matcher monthAndYearMatcher = monthAndYear.matcher(answer);
                if (!monthAndYearMatcher.find()) {
                    result2.add(answer);
                }
            }
        }


        if (result2.isEmpty()) {
            log.writeToLogFile("WARNING - Could not find in-text citations for document - CASE 2");
            log.newLine();
        }

        if (result1.isEmpty() && result2.isEmpty()) {
            System.err.println("ERROR - Could not find in-text citations in this document " + file.getName()); //delete
            log.writeToLogFile("ERROR - Could not find in-text citations in this document");
            log.newLine();  //Throw error
            return new ArrayList<>();
        }

        if (areRefNumbered && result1.size() > result2.size()) {
            this.areRefNumbered = true;
            return result1;
        }
        System.out.println("Uses standard citations");
        return result2;

    }

    /**
     * Based on the number of times each font is used, get all the possible font sizes that could be used to write
     * superscripts.
     * This method also computes the body size of the text, by assuming that it is the most frequent text size, and
     * that it is >=7.0
     * It also assumes that the superscript size is less than 7.0
     *
     * @param fontSizes    map from font size to number of times it is used.
     * @param smallestFont smallest font size in the entire text
     * @return string with all the possible font sizes that could be used to write superscripts.
     */
    String getSuperScriptSize(HashMap<Float, Integer> fontSizes, float smallestFont) {
        TreeMap<Integer, List<Float>> frequencies = new TreeMap<>(Collections.reverseOrder());
        StringBuilder superScriptSize = new StringBuilder();

        //Order the frequencies in descending order. Map<Number of times used, Size of font>
        for (float size : fontSizes.keySet()) {
            int numberOfTimes = fontSizes.get(size);
            if (frequencies.get(numberOfTimes) == null) {
                ArrayList<Float> sizesList = new ArrayList<>();
                sizesList.add(size);
                frequencies.put(numberOfTimes, sizesList);
            } else {
                List<Float> sizesList = frequencies.get(numberOfTimes);
                sizesList.add(size);
                frequencies.put(numberOfTimes, sizesList);
            }
        }
        textBodySize = Float.POSITIVE_INFINITY;

        int highestFreq = frequencies.firstKey();

        boolean first = true;
        boolean found = false;
        //Important: The following lines of code are based on what I have found through extensive testing, but there is
        // always some edge case that might have to be considered.
        //We iterate through all the fonts, starting with the most frequently used, to try to determine the size of
        // the body (We assumed that it is <=7pts. Once we have the size of the body, anything lower will be the
        // superscript size (with some caveats)
        for (int numberOfTimesUSed : frequencies.keySet()) {
            if (first) {
                for (float size : frequencies.get(numberOfTimesUSed)) {
                    //If the paper is very short, then the highest frequency might be a small number, so we have
                    // separate conditions for it
                    if (highestFreq < 30) {
                        if (size >= 7.0 && (!(fontSizes.containsKey((float) 12.0) && fontSizes.get((float) 12.0) >
                                20) && !(fontSizes.containsKey((float) 10.0) && fontSizes.get((float) 10.0) > 20) &&
                                !(fontSizes.containsKey((float) 9.0) && fontSizes.get((float) 9.0) > 20))) {
                            textBodySize = size;
                            first = false;
                        }
                    } else {
                        if (size >= 7.0 && (!(fontSizes.containsKey((float) 12.0) && fontSizes.get((float) 12.0) >
                                60) && !(fontSizes.containsKey((float) 10.0) && fontSizes.get((float) 10.0) > 60)
                                && !(fontSizes.containsKey((float) 11.0) && fontSizes.get((float) 11.0) > 60)
                                && !(fontSizes.containsKey((float) 9.0) && fontSizes.get((float) 9.0) >= 59))) {
                            //For this to be the text body size, the highest frequency is 7pts or larger, and the
                            // other larger fonts are used less than X times. We check if the other fonts exist and
                            // have a frequency of greater than X because the most frequent font is not necessarily
                            // the text body size. (It might be the 2nd or 3rd most frequent)
                            textBodySize = size;
                            first = false;
                        }
                    }
                }

            }
            for (float size : frequencies.get(numberOfTimesUSed)) {
                if (highestFreq < 30) {
                    if (numberOfTimesUSed < 10) {
                        //If it happens less than 50 times, we have already considered everything we needed so we break
                        break;
                    }
                    //It can only be a superscript if:
                    //-It is at least same size as the smallest text in the doc
                    //-It is smaller than the text body size
                    //-It is smaller than or equal to 8.0
                    found = isFound(smallestFont, superScriptSize, found, size);
                } else {
                    if (numberOfTimesUSed < 25) {
                        //If it happens less than 40 times, we have already considered everything we needed so we break
                        break;
                    }
                    //It can only be a superscript if:
                    //-It is at least same size as the smallest text in the doc
                    //-It is smaller than the text body size
                    //-It is smaller than or equal to 8.0
                    //-It was used at least 50 times
                    found = isFound(smallestFont, superScriptSize, found, size);
                }
            }
        }

        return superScriptSize.toString();
    }

    /**
     * Checks if the current font matches the criteria to be a superscript. If so, we append it to the currently
     * found super script sizes (There can be more than 1 super script size)
     */
    private boolean isFound(float smallestFont, StringBuilder superScriptSize, boolean found, float size) {
        if (smallestFont <= size && size < textBodySize && size <= 8.0) {
            if (!found) {
                superScriptSize.append(size);
                found = true;
            } else {
                superScriptSize.append("|").append(size);

            }
        }
        return found;
    }


    /**
     * Returns a a valid superscript. If y2 > y1, then is valid. If empty, ignore. If there is no y2, then check the
     * word before the possible superscript
     *
     * @param possibleSuperScript String that could contain a superscript
     * @return string with correctly formatted superscript, or an empty string if it is not
     */
    String formatSuperScript(String possibleSuperScript) {
        //Get the front of the string, if there exist any
        boolean containsNewLine = false;
        if (possibleSuperScript.contains("\n")) {
            containsNewLine = true;
            possibleSuperScript = possibleSuperScript.replaceAll("\\n", "");

        }
        Pattern pattern = Pattern.compile("(^[^{]*)");
        Matcher matcher = pattern.matcher(possibleSuperScript);
        String prefix = "";
        if (matcher.find()) {
            prefix = matcher.group();
        }
        //3 cases
        //If y2 and y1 exist, then just base it on that
        //If y2 does not exist, base it on prefix
        //If y2 does not exist and prefix does not exist, return true/
        String[] result = possibleSuperScript.split("\\{");
        boolean first = true;
        StringBuilder y1 = new StringBuilder();
        StringBuilder y2 = new StringBuilder();

        String possibleResult = possibleSuperScript;

        //Remove formatting
        possibleResult = possibleResult.replaceAll(".*\\{\\|\\d*(\\.)?\\d*&\\d*(\\.)?\\d*\\|}", "");
        possibleResult = possibleResult.replaceAll("\\{\\|\\d*(\\.)?\\d*&\\d*(\\.)?\\d*\\|", "");

        possibleResult = possibleResult.replaceAll("}", "").replaceAll("\\s", "");


        if (result.length == 1) {
            return possibleResult;
        }
        for (String s : result) {
            if (s.equals(prefix)) {
                //Ignore if it is the prefix
                continue;
            }
            s = s.replaceAll("\\|\\d*(\\.)?\\d*&", "");
            for (Character c : s.toCharArray()) {
                if (Character.isDigit(c) || c == '.') {
                    if (first) {
                        y1.append(c);
                    } else {
                        y2.append(c);
                    }

                } else break;
            }
            first = false;

        }
        //If there is no y2
        if (y2.toString().isEmpty() || result.length == 3) {
            if (prefix.length() <= 3 && prefix.length() > 0) {
                //If first or last char is Caps, or the last one, then it is probably an abreviation & isn't not
                // going to be the prefix of a citation
                //Get the first character and last character
                Character firstChar = prefix.charAt(0);
                Character lastChar = prefix.charAt(prefix.length() - 1);
                //Add here other invalid prefixes!!!!!
                Pattern invalidPrefixes = Pattern.compile("\\bCa\\b");
                Matcher invalidPrefixesMatcher = invalidPrefixes.matcher(prefix);
                //If first or last is mayus, or the prefix is invalid and it does not contain a dash, then return ""
                if ((Character.isUpperCase(firstChar) || Character.isUpperCase(lastChar) || Character.isDigit
                        (lastChar) ||
                        invalidPrefixesMatcher.find()) && (!possibleResult.contains("-") && !possibleResult.contains
                        ("–") && possibleResult
                        .contains("±") || (Character.isUpperCase(firstChar) && (Character.isUpperCase(lastChar) ||
                        Character.isDigit(lastChar))))) {
                    return "";
                }

            }
            if (y2.toString().isEmpty()) {
                //If there is no y2, but prefix is valid, return result
                return possibleResult;
            }
        }
        if (containsNewLine) {
            if (Float.valueOf(y2.toString()) >= Float.valueOf(y1.toString())) {
                return possibleResult;
            } else return "";
        }
        //Y2 is higher than y1, which means that y1 is a superscript.
        if (Float.valueOf(y2.toString()) > Float.valueOf(y1.toString())) {
            return possibleResult;
        } else return "";

    }

    String getYear() {
        return null;
    }

    /**
     * Finds the way a given twin paper is referenced in the bibliography of an article
     *
     * @param allAuthorRegex            - Regex based on the names of the authors of a given twin paper.
     * @param authorsOfTwinPaper        - authors of a given twin paper.
     * @param mainAuthorRegex           - regex for the main author of the paper
     * @param yearTwinPaperWasPublished - year the paper was published
     * @param titleOfTwinPaper          - title of current paper
     * @return string with the reference used in the paper. Starts with author name and finishes with the year the
     */

    String getReference(String allAuthorRegex, String authorsOfTwinPaper, String mainAuthorRegex, int
            yearTwinPaperWasPublished, String titleOfTwinPaper) throws IllegalArgumentException, IOException {

        ReferenceFinder referenceFinder = new ReferenceFinder(parsedText, file, pattern2Used);
        referenceFinder.setAuthorsAndTitleOfCurrentPaper(authorsOfTwinPaper, titleOfTwinPaper);
        String reference = referenceFinder.getReference(allAuthorRegex, authorsOfTwinPaper, mainAuthorRegex,
                yearTwinPaperWasPublished);

        //Check the max length the reference can be
        int authorsLength = authorsOfTwinPaper.split(" ").length;
        int yearLength = 1;
        int titleLength = titleOfTwinPaper.split(" ").length;
        int extraLength = 20;
        int totalLength = authorsLength + titleLength + yearLength + extraLength;
        if (totalLength < reference.split(" ").length) {
            System.err.println("There is possibly an error in this reference!!");
            throw new IllegalArgumentException("The captured reference is too long (usually an error)");
        }
        pattern2Used = referenceFinder.pattern2Used();
        referenceFinder.close();
        return reference;
    }


}