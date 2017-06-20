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
 * Parses a pdf document, retrieves the relevant information.
 */
class DocumentParser {
    private final Logger log;
    private COSDocument cosDoc;
    private PDDocument pdDoc;
    String parsedText = "";
    private String formattedParsedText = "";
    float largestFont;
    float smallestFont;
    private HashMap<Float, Integer> fontSizes;
    private File file;
    float textBodySize;
    private String possibleAuthorsNames;
    float titleFontSize;



    /**
     * Constructor. Takes 1 argument.
     *
     * @param fileToParse    - pdf document that needs to be parsed.
     * @param parseEntireDoc - true if you want to parse the entire file, false to parse only the first page
     * @param getFormat      - true if you want to get the format of the text, false if you only want the plain text
     * @throws IOException - If there is an error reading the file
     */
    DocumentParser(File fileToParse, boolean parseEntireDoc, boolean getFormat) throws IOException {
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);

        this.file = fileToParse;
        this.log = Logger.getInstance();
        PDFTextStripper pdfStripper = null;
        if (!fileToParse.exists() || !fileToParse.canRead() ) {
            throw new IOException("ERROR: File does not exist");
        }
        PDFParser parser = new PDFParser(new RandomAccessBufferedFileInputStream(fileToParse));
        System.out.println("*(******FILENAME "+ fileToParse.getName());
        parser.parse();
        cosDoc = parser.getDocument();

        //To keep track of the fonts
        largestFont = 0;
        smallestFont = Float.POSITIVE_INFINITY;
        fontSizes = new HashMap<>();

        if (getFormat) {
            pdfStripper = new PDFTextStripper() {
                //Modifies the way the text is parsed by including font size
                float prevFontSize = 0;
                String prevFont = "";


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
                            isDifferentFont = !baseFont.equals(prevFont);
                        }



                        if ( isDifferentFontSize || isDifferentFont) {
                            if (!parseEntireDoc) {
                                builder.append("{|").append(baseSize).append("&").append(baseFont).append("&").append(position.getYDirAdj()).append("|}");
                            }
                            else {
                                //Format {|textSize&yPosition|}
                                builder.append("{|").append(baseSize).append("&").append(position.getYDirAdj()).append("|}");
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
            }catch (IllegalArgumentException e){
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

       // getText(getFormat); //delete

    }


    /**
     * Gets the plain text from a pdf document. Removes all formatting
     *
     * @return string with all the text
     */
    protected String getText(boolean isFormatted) {
        if (isFormatted) {
            System.out.print(formattedParsedText);// delete
            return formattedParsedText;
        } else {
            System.out.print(parsedText);// delete
            return parsedText;
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
     *
     * Case 1 - for more than 1 author and Case 2 for only 1 or in case of et al
     *
     * @param allAuthorsRegex - Regex based on the names of the authors of a given twin paper.
     * @param authors     - authors of a given twin paper.
     * @param yearPublished2
     * @return string with the reference used in the paper. Starts with author name and finishes with the year the paper was published.
     */
    String getReference(String allAuthorsRegex, String authors, String mainAuthorRegex, int yearPublished2) throws IllegalArgumentException, IOException {
        //Pattern use to capture the citation. Starts with the author name and ends with the year the paper was published.
        //String patternCase1 = "[^.\\n]*(\\d+(\\.( ).*))*(" + allAuthorsRegex + ")([^;)])*?((\\b((18|19|20)\\d{2}( )?([A-z])*(,( )?(((18|19|20)\\d{2}([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";
        String patternCase1 = "[^.\\n]*(\\d+(\\.( ).*))*(" + allAuthorsRegex + ")([^;)])*?((\\b((18|19|20)\\d{2}( )?([A-z])*(,( )?(((18|19|20)\\d{2}([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";
        String onlyOneAuthorPattern = "[^.\\n]*(\\d+(\\.( ).*))*(("+mainAuthorRegex+"))([^;)])*?((\\b(("+ yearPublished2 +")( )?([A-z])*(,( )?((("+ yearPublished2 +")([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";
        //If there is only one author, go directly to case 2
        int numOfAuthors = authors.split(",").length;
        if (numOfAuthors < 2) {
            //Swap pattenrs
            patternCase1 = onlyOneAuthorPattern;
        }
        System.out.println("Pattern 1 " + patternCase1);
        Pattern pattern1 = Pattern.compile(patternCase1);
        Matcher matcher1 = pattern1.matcher(parsedText);
        Comp compator = new Comp();
        TreeSet<String> result = new TreeSet<>(Collections.reverseOrder(compator));
        log.writeToLogFile("Citations found for paper");
        log.newLine();



        while (matcher1.find()) {
            log.writeToLogFile("Found " + matcher1.group());
            log.newLine();
            String nResult = matcher1.group();
                result.add(nResult);

        }
        if (result.isEmpty()) {
            log.writeToLogFile("No reference found case 1");
            log.newLine();
            //Since we are only searching for main author, we will include the year paper was published
            String patternCase2 = "[^.\\n]*(\\d+(\\.( ).*))*(("+mainAuthorRegex+")(.* et al))([^;)])*?((\\b(("+ yearPublished2 +")( )?([A-z])*(,( )?((("+ yearPublished2 +")([A-z])*)|[A-z]))*)\\b)|unpublished data|data not shown)";
            System.out.println("Pattern 2 " +patternCase2);
            Pattern pattern2 = Pattern.compile(patternCase2);
            Matcher matcher2 = pattern2.matcher(parsedText);

            while (matcher2.find()) {
                String nResult = matcher2.group();
                result.add(nResult);
            }
            if (result.isEmpty()) {
                return "";
            }
            else if (result.size()>1) {
                result = solveReferenceTies(result, authors);
            }
            else {
                return result.first();
            }
        }
        if (result.size() > 1) {
            log.writeToLogFile("There is a tie");
            log.newLine();
            result = solveReferenceTies(result, authors);
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
     TreeSet<String> solveReferenceTies(TreeSet<String> result, String authors) throws IllegalArgumentException {
        TreeSet<String> newResult = new TreeSet<>();
        String possibleResult = "";
        int smallest = Integer.MAX_VALUE;

        for (String s : result) {

            int newDistance = StringUtils.getLevenshteinDistance(s, authors);
            if (newDistance == smallest) {
                log.writeToLogFile("ERROR: There was an error solving the tie");
                log.newLine();
                //Ties should not happen so throw an error
                throw new IllegalArgumentException("ERROR: THERE WAS AN ERROR FINDING THE CITATION IN THIS PAPER, PLEASE INCLUDE MORE THAN 3 AUTHORS' NAMES FOR EACH OF THE TWIN PAPERS" +
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

    /**
     * Gets all the in-text citation of a given pdf document
     * @param areRefNumbered - are the references numbered in the bibliography.
     *                       Ex. 1. Ellis, John ... 2010.
     * @return ArrayList with all the citations
     */
    ArrayList<String> getInTextCitations(boolean areRefNumbered) throws IOException {
        //Case 1: For the case where in-text citations are displayed as numbers
        //Ex: [1] or [4,5] or [4,5•] or [4•] or [4-20]
        ArrayList<String> result1 = getInTextCitationsCase1("");

        //If there are less than 10 references or there are no references at all, but ref are numbered, then try finding superscript numbered refs
        if (areRefNumbered && (result1.isEmpty() || result1.size() < 10)) {
            //First it needs to get the text formatted
            DocumentParser parsedDoc = null;
            try {
                parsedDoc = new DocumentParser(file, true, true);
            } catch (IOException e) {
                throw new IOException("There was an error parsing the file "+file.getName());
            }
            this.formattedParsedText = parsedDoc.formattedParsedText;
            String superScriptSize = getSuperScriptSize(parsedDoc.fontSizes, parsedDoc.smallestFont);

            result1 = getInTextCitationsCase1(superScriptSize);

        }


        //If less than 50 intext citations where found for case 1, then try doing case 2.
        if (result1.size() < 50) {
            ArrayList<String> result2 = new ArrayList<>();
            String patternCase2 = "\\(\\D*(unpublished data|data not shown|\\d{4})([a-zA-Z]((,| and)( )?[a-zA-Z])*)*((,| and)( |\\n)(unpublished data|data not shown|\\d{4}([a-zA-Z]((,| and)( )?[a-zA-Z])*)*))*(;\\D*\\d{4}([a-zA-Z]((,| and)( )?[a-zA-Z])*)*((,| and) (unpublished data|data not shown|\\d{4}))*)*\\)";
            patternCase2 = "\\(\\D*(unpublished data|Fig\\. ([0-9-]*(\\n| |;( |\\n)\\D*)(and \\d*)*)*|data not shown|\\d{4})([a-zA-Z]((,|(( |\\n)and))( |\\n)?[a-zA-Z])*)*((,|( |\\n)and)( |\\n)(unpublished data|data not shown|\\d{4}(\\n)?([a-zA-Z]((,|(( |\\n)and))( |\\n)?[a-zA-Z])*)*))*((;|,)\\D*\\d{4}([a-zA-Z]((,|(( |\\n)and))( |\\n)?[a-zA-Z])*)*((,|(( |\\n)and))( |\\n)(unpublished data|data not shown|\\d{4}))*)*\\)";
            Pattern pattern2 = Pattern.compile(patternCase2);
            Matcher matcher2 = pattern2.matcher(parsedText);


            while (matcher2.find()) {
                String answer = matcher2.group();
                log.writeToLogFile("Found CASE 2 " + answer);
                log.newLine();
                //Make sure that answer is not only the year (2010) or year and letter (2010a)
                Pattern validCitationCase2 = Pattern.compile("[^(0-9) ][A-z]");
                Matcher validation = validCitationCase2.matcher(answer);
                if (validation.find()){
                    result2.add(answer);
                }

            }

            if (result2.isEmpty()) {
                log.writeToLogFile("WARNING - Could not find in-text citations for document - CASE 2");
                log.newLine();
            }

            if (result1.isEmpty() && result2.isEmpty()) {
                System.err.println("ERROR - Could not find in-text citations in this document"); //delete
                log.writeToLogFile("ERROR - Could not find in-text citations in this document");
                log.newLine();
                return new ArrayList<>();
            }

            //Todo: change how this works
            if (result1.size() > result2.size()) {
                //Case 1 is going to be used
                return result1;
            } else {
                //Case 2 is going to be used
                //Might change in the future Todo
                return result2;

            }
        }
        return result1;
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
        int counter = 0;
        ArrayList<String> newAnswer = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean thereIsADash = false;
        for (Character c : citationWithDash.toCharArray()) {
            if (c == '–' || c == '±') {
                newAnswer.add(sb.toString());
                sb = new StringBuilder();
                counter++;
                thereIsADash = true;

            } else {
                if (c == ',') {
                    if (counter > 0 && thereIsADash) {
                        int leftSide = Integer.parseInt(newAnswer.get(counter - 1));
                        int rightSide = Integer.parseInt(sb.toString());
                        while (leftSide < rightSide) {
                            leftSide = leftSide + 1;
                            newAnswer.add(String.valueOf(leftSide));
                            counter++;
                        }
                        thereIsADash = false;
                    } else {
                        newAnswer.add(sb.toString());
                        sb = new StringBuilder();
                        counter++;
                    }


                } else {
                    if (c != ' ' && ((c != '[' && c != ']') && (c != '(' && c != ')'))) {
                        sb.append(c);
                    }
                }
            }
        }
        int leftSide = Integer.parseInt(newAnswer.get(counter - 1));
        int rightSide = Integer.parseInt(sb.toString());
        while (leftSide < rightSide) {
            leftSide = leftSide + 1;
            newAnswer.add(String.valueOf(leftSide));
        }
        citationWithDash = " ";
        int counter2 = 0;
         StringBuilder citationWithDashBuilder = new StringBuilder(citationWithDash);
         for (String s : newAnswer) {
            if (counter2 > 0) {
                citationWithDashBuilder.append(',').append(s);
            } else {
                citationWithDashBuilder.append(s);
            }
            counter2++;
        }
         citationWithDash = citationWithDashBuilder.toString();
         citationWithDash= citationWithDash.replaceAll("^\\s+", "");
        return citationWithDash;
    }


    /**
     * Closes the file that is being parsed.
     */
    void close() throws IOException {
        try {
            log.writeToLogFile("Closing file");
            log.newLine();
            pdDoc.close();
            cosDoc.close();
        } catch (IOException e) {
            log.writeToLogFile("There was a problem closing the file");
            log.newLine();
            throw new IOException("ERROR: There was an error closing the file");

        }

    }


    /**
     * Gets the title based on a text-analysis of the first page, by finding the string with the largest font that has more than 3 characters
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
        while (result.length() < 3) {
            String pattern = "(\\{\\|" + titleFont + ")([^{])*";
            this.titleFontSize = titleFont;
            Pattern pattern1 = Pattern.compile(pattern);
            Matcher matcher = pattern1.matcher(formattedParsedText);
            if (matcher.find()) {
                result = matcher.group();
                Pattern patternForAuthorsNames = Pattern.compile(pattern + "\\{\\|\\d*(\\.)?\\d*&[^}]*}[^{]*\\{\\|\\d*(\\.)?\\d*(\\.)?\\d*&[^}]*}[^{]*");
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
            titleFont = orderedFonts.firstKey();
        }

        return result;
    }


    /**
     * Gets the authors of the paper by analyzing the text. getTitle() needs to be called before using this method.
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

        for (int i= result.length - 2; i < result.length; i++) {
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
                    possibleAuthorsNames =  possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf(","));
                }
                while (possibleAuthorsNames.endsWith(".")) {
                    possibleAuthorsNames =  possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf("."));
                }
                return possibleAuthorsNames;
            }

            //Try to find more than one author, if not just return the first one.
            if (numOfSpaces.length <=2) {
                while (possibleAuthorsNames.endsWith(",")) {
                    possibleAuthorsNames = possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf(","));
                }
                 DocumentParser dp = new DocumentParser(file, false, false);
                 Pattern authorsPattern = Pattern.compile(possibleAuthorsNames+"(.*)?");
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
                         possibleAuthorsNames =  possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf(","));
                     }
                     while (possibleAuthorsNames.endsWith(".")) {
                         possibleAuthorsNames =  possibleAuthorsNames.substring(0, possibleAuthorsNames.lastIndexOf("."));
                     }
                     return possibleAuthorsNames;


                 }
                 else return possibleAuthorsNames;
            }



        }
        return "No authors found";
    }


    /**
     * Helper method. Gets all the in text citations that are written as numbers, including superscripts.
     * @param superScriptSize empty string if there no superscript, if not the size that the superscript
     * @return ArrayList with all the citations
     */
     ArrayList<String> getInTextCitationsCase1(String superScriptSize){
        ArrayList<String> result = new ArrayList<>();
        Matcher matcher;

        //If there is no super script size
        if (superScriptSize.isEmpty()) {
            //If there is no superscript
            //Accepts ( or []
            String patternCase1 = "(\\(|\\[)\\d+(•)*(–\\d+(•)*)*(,( )*\\d+(•)*((–\\d+)(•)*)*)*(\\)|])";
            Pattern pattern1 = Pattern.compile(patternCase1);
            matcher = pattern1.matcher(parsedText);

        } else {

            //If there could be superScript in-text citations

            String pattern = "(([^A-z])(\\{\\|(" + superScriptSize + ")&(\\d*(\\.)?\\d*)\\|})(?!\\)|\\(|-|[A-z]|\\d*\\.|,)([^A-z(}\\n])*)|(([A-z]{2,})(\\{\\|(" + superScriptSize + ")&(\\d*(\\.)?\\d*)\\|})(?!\\)|\\(|-|[A-z]|\\d*\\.|,)([^A-z(}\\n])*)";
            Pattern pattern1 = Pattern.compile(pattern);
            matcher = pattern1.matcher(formattedParsedText);
        }

        while (matcher.find()) {
            String answer = matcher.group();
            log.writeToLogFile("Found CASE 1 " + answer);
            log.newLine();
            answer = formatSuperScript(answer);
            //If citation contains a '–', it needs to be modified
            if (answer.contains("–") || answer.contains("±")) {
                answer = inTextCitationContainsDash(answer);
            }

            if (!answer.isEmpty()) {
                //If pattern is just a year (2009) do not admit
                Pattern doNotAccept = Pattern.compile("\\([(0-9)]{4}\\)");
                Matcher invalid = doNotAccept.matcher(answer);

                if (!invalid.find()) {
                    result.add(answer);

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
     * Based on the number of times each font is used, get all the possible font sizes that could be used to write superscripts.
     * This method also computes the body size of the text, by assuming that it is the most frequent text size, and that it is >=7.0
     * It also assumes that the superscript size is less than 7.0
     * @param fontSizes map from font size to number of times it is used.
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
            }
            else {
                List<Float> sizesList = frequencies.get(numberOfTimes);
                sizesList.add(size);
                frequencies.put(numberOfTimes, sizesList);
            }
        }
        textBodySize = Float.POSITIVE_INFINITY;

        boolean first = true;
        boolean found = false;
        for (int numberOfTimesUSed : frequencies.keySet()) {
            //Normally textbody size is greater than 7
            if (first) {
                for (float size : frequencies.get(numberOfTimesUSed)) {
                    if (size >= 7.0) {
                        textBodySize = size;
                        first = false;
                    }
                }

            }
            for (float size : frequencies.get(numberOfTimesUSed)) {

                if (numberOfTimesUSed < 50) {
                        //If it happens less than 50 times, we have already considered everything we needed so we break
                        break;
                    }
                    //It can only be a superscript if:
                    //-It is at least same size as the smallest text in the doc
                    //-It is smaller than the text body size
                    //-It is smaller than or equal to 7.0
                    //-It was used at least 50 times
                    if (smallestFont <= size && size < textBodySize && size <= 7.0 && numberOfTimesUSed >= 50) {
                        if (!found) {
                            superScriptSize.append(size);
                            found = true;
                        } else {
                            superScriptSize.append("|").append(size);

                        }
                    }
                }
        }

        return superScriptSize.toString();
    }


    /**
     * Returns a a valid superscript. If y2 > y1, then is valid. If empty, ignore. If there is no y2, then check the word before the possible superscript
     * @param possibleSuperScript String that could contain a superscript
     * @return string with correctly formatted superscript, or an empty string if it is not
     */
     String formatSuperScript(String possibleSuperScript) {
        //Get the front of the string, if there exist any
        Pattern pattern = Pattern.compile("(^[^{]*)");
        Matcher matcher = pattern.matcher(possibleSuperScript);
        String prefix = "";
        if (matcher.find()) {
            prefix = matcher.group();
        }
        //3 cases
        //If y2 and y1 exist, then just base it on that
        //If y2 does not exist, base it on prefix
        //If y2 does not exist and prefex does not exist, return true/
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
        for(String s: result) {
            if (s.equals(prefix)) {
                //Ignore if it is the prefix
                continue;
            }
            s = s.replaceAll("\\|\\d*(\\.)?\\d*&", "");
            for (Character c : s.toCharArray()) {
                if (Character.isDigit(c) || c == '.') {
                    if (first) {
                        y1.append(c);
                    }
                    else {
                        y2.append(c);
                    }

                }
                else break;
            }
            first = false;

        }
        //If there is no y2
        if (y2.toString().isEmpty() || result.length == 3) {
            if (prefix.length() <= 3 && prefix.length() > 0) {
                //If first or last char is mayus, or the last one, then it is probably an abreviation and not going to be the prefix of a citation
                //Get the first character and last cha
                Character firstChar = prefix.charAt(0);
                Character lastChar = prefix.charAt(prefix.length()-1);


                //If first or last is mayus, return false, else check lenght
                if (Character.isUpperCase(firstChar) || Character.isUpperCase(lastChar)) {
                    //if (y2.toString().isEmpty()) {//delete
                        return "";
                  //  }
                }

            }
            if (y2.toString().isEmpty()) {
                //If there is no y2, but prefix is valid, return result
                return possibleResult;
            }
        }
        //Y2 is higher than y1, which means that y1 is a superscript.
        if (Float.valueOf(y2.toString()) > Float.valueOf(y1.toString())) {
            return possibleResult;
        }
        else return "";


    }


    public String getYear()
    {
        return null;
    }
}


class Comp implements Comparator<String> {
    public int compare(String o1, String o2) {
        return Integer.compare(o1.length(), o2.length());
    }
}