package com.rc.PDFCitationAnalyzer;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by rafaelcastro on 5/27/17.
 * Test cases for DocumentParser class
 * For testing purposes:
 * Test2.pdf = numerical references with in-text citations numbered formatted between [] - 1.pdf
 * Test3.pdf = numerical references with in-text citations numbered formatted as superscript - 3.pdf
 * Test4.pdf = references without numbers with in text citations between parenthesis - 2.pdf
 * Test5.pdf = references without numbers with in text citations between parenthesis - 4.pdf
 * Test6.pdf = numerical references with in-text citations numbered formatted between [] - 5.pdf
 * Test7.pdf = references without numbers with in text citations between parenthesis. Very old pdf - 7.pdf

 */
class DocumentParserTest {

    private DocumentParser documentParser;
    private FileAnalyzer fileAnalyzer = new FileAnalyzer();


    @org.junit.jupiter.api.Test
    void testInvalidDocument() throws IOException {
        //File does not exist
        File file = new File("./testingFiles/Test999.pdf");
        Throwable exception = assertThrows(IOException.class, () -> {
            new DocumentParser(file, true, false);
        });
        assertEquals("ERROR: File does not exist", exception.getMessage());
    }

    @org.junit.jupiter.api.Test
    void testGetNumericalReferenceAuthorDoesNotAppear() throws IOException {
        //Get the reference and include the number
        File file = new File("./testingFiles/Test2.pdf");
        documentParser = new DocumentParser(file, true, false);
        //Names have to be separated with comma.
        String author = "Rafael Castro";
        String authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        String mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        String result = documentParser.getReference(authorRegex, author, mainAuthor, 2000);
        assertEquals("", result);
    }

    @org.junit.jupiter.api.Test
    void testGetNumericalReference() throws IOException {
        //Get the reference and include the number
        File file = new File("./testingFiles/Test2.pdf");
        documentParser = new DocumentParser(file, true, false);
        //Names have to be separated with comma.
        String author = "Woo M, Hakem R, Soengas MS, Duncan GS, Shahinian A";
        String authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        String mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        String result = documentParser.getReference(authorRegex, author, mainAuthor, 2000);
        assertEquals("57. Woo M, Hakem R, Soengas MS, Duncan GS, Shahinian A, Kagi D,\n" +
                "• Hakem A, McCurrach M, Khoo W, Kaufman SA et al.: Essential\n" +
                "contribution of caspase-3/CPP32 to apoptosis and its associated\n" +
                "nuclear changes. Genes Dev 1998", result);

        author = "Steller H";
        authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        result = documentParser.getReference(authorRegex, author, mainAuthor, 1995);
        assertEquals("1. Steller H: Mechanisms and genes of cellular suicide. Science\n" +
                "1995", result);
        documentParser.close();

        file = new File("./testingFiles/Test3.pdf");
        documentParser = new DocumentParser(file, true, false);
        author = "Thome M, Hofmann K, Burns K, Martinon F,";
        authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        result = documentParser.getReference(authorRegex, author, mainAuthor, 1998);
        assertEquals("37. Thome M, Hofmann K, Burns K, Martinon F, Bodmer JL, Mattmann C and\n" +
                "Tschopp J (1998", result);
        documentParser.close();

        file = new File("./testingFiles/Test6.pdf");
        documentParser = new DocumentParser(file, true, false);
        author = "Karbownik M, Tan D, Manchester LC, Reiter RJ.";
        authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        result = documentParser.getReference(authorRegex, author, mainAuthor, 2000);
        assertEquals("80. Karbownik M, Tan D, Manchester LC, Reiter RJ. Renal\n" +
                "toxicity of the carcinogen delta-aminolevulinic acid: anti-\n" +
                "oxidant effects of melatonin. Cancer Lett 2000", result);
        documentParser.close();


    }

    @org.junit.jupiter.api.Test
    void testGetReferenceNonNumerical() throws IOException {
        //Get the reference and include the number
        File file = new File("./testingFiles/Test4.pdf");
        try {
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Last name of author appears twice
        String author = "Li P., D. Nijhawan, I. Budihardjo";
        String authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        String mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        String result = documentParser.getReference(authorRegex, author, mainAuthor, 1997);
        assertEquals("Li, P., D. Nijhawan, I. Budihardjo, S.M. Srinivasula, M. Ahmad, E.S. Alnemri,\n" +
                "and X. Wang. 1997", result);

        //Year contains lettter
        author = "Kluck R.M., E. Bossy-Wetzel, D.R. Green";
        authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        result = documentParser.getReference(authorRegex, author, mainAuthor, 1997);
        assertEquals("Kluck, R.M., E. Bossy-Wetzel, D.R. Green, and D.D. Newmeyer. 1997a", result);
        documentParser.close();


        file = new File("./testingFiles/Test5.pdf");
        try {
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        author = "Zou, H., Henzel, W.J., Liu, X., Lutschg, A., and Wang, X.";
        authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        result = documentParser.getReference(authorRegex, author, mainAuthor, 1997);
        assertEquals("Zou, H., Henzel, W.J., Liu, X., Lutschg, A., and Wang, X. (1997", result);

        //Liu appears three times
        author = "Liu, Zou, Slaughter, Wang";
        authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        result = documentParser.getReference(authorRegex, author, mainAuthor, 1997);
        assertEquals("). Bcl-2 hetero-Liu, X., Zou, H., Slaughter, C., and Wang, X. (1997", result);
        documentParser.close();



        file = new File("./testingFiles/Test7.pdf");
        try {
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Kerr appears 4 times, each with different years but is the only author
        author = "KERR J. F. R.";
        authorRegex = fileAnalyzer.generateReferenceRegex(author, true, false);
        mainAuthor = fileAnalyzer.generateReferenceRegex(author, false, false);
        result = documentParser.getReference(authorRegex, author, mainAuthor, 1969);
        assertEquals("KERR, J. F. R. (1969", result);
        documentParser.close();


    }

    @org.junit.jupiter.api.Test
    void testGetInTextCitationsNumeric() throws IOException {
        //Get the in-text citations when they are expressed as numbers between []
        File file = new File("./testingFiles/Test2.pdf");
        try {
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> result = documentParser.getInTextCitations(true);
         assertEquals(66, result.size());
        //Simple
        assertTrue(result.contains("[5]"));
        //Two
        assertTrue(result.contains("[1,2]"));
        //three
        assertTrue(result.contains("[9,11,12]"));
        //Special symbol
        assertTrue(result.contains("[3,4••]"));
        //Has dash [8-10]
        assertTrue(result.contains("8,9,10"));
        //Has dash and normal [24,26,29–32]
        assertTrue(result.contains("24,26,29,30,31,32"));
        //Does not exist
        assertFalse(result.contains("[240]"));
        documentParser.close();
    }

    @org.junit.jupiter.api.Test
    void testGetInTextCitationsNumericSuperScript() throws IOException {
        //Get the in-text citations when they are expressed as numbers between []
        File file = new File("./testingFiles/Test3.pdf");
        try {
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> result = documentParser.getInTextCitations(true);

        assertEquals(121, result.size());
        //Simple
        assertTrue(result.contains("3"));
        assertTrue(result.contains("28"));
        //Two
        assertTrue(result.contains("1,2"));
        //three
        assertTrue(result.contains("12,22,23"));
        //Has dash 7-9
        assertTrue(result.contains("7,8,9"));

        //Different font
        assertTrue(result.contains("76,77,78,79,80,81,82"));

        //Invalid
        assertFalse(result.contains("283"));
        assertFalse(result.contains("283"));
        assertFalse(result.contains("341"));

        //Does not exist
        assertFalse(result.contains("3000"));
        documentParser.close();
    }

    @org.junit.jupiter.api.Test
    void testSolveReferenceTies() throws IOException {
        File file = new File("./testingFiles/Test1.pdf");

        ReferenceFinder rf = new ReferenceFinder();

        //Basic case
        TreeSet<String> possibilities = new TreeSet<>();
        possibilities.add("Rafael Castro 2010");
        possibilities.add("Jose Castro 2010");
        TreeSet<String> answer = rf.solveReferenceTies(possibilities, "Rafael Castro.", "2010");
        assertEquals(1, answer.size());
        assertEquals("Rafael Castro 2010", answer.pollFirst());

        possibilities.add("Rafael Castro");
    }


    @org.junit.jupiter.api.Test
    void testInTextCitationContainsDash() throws IOException {
        //The method prints white space at beginning of string.
        File file = new File("./testingFiles/Test1.pdf");
        try {
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String answer = documentParser.inTextCitationContainsDash("[15–20]");
        assertEquals("15,16,17,18,19,20", answer);
        answer = documentParser.inTextCitationContainsDash("[5, 15–20]");
        assertEquals("5,15,16,17,18,19,20", answer);
        answer = documentParser.inTextCitationContainsDash("15–20");
        assertEquals("15,16,17,18,19,20", answer);
        documentParser.close();
    }

    @org.junit.jupiter.api.Test
    void getInTextCitationsBetweenParenthesis() throws IOException {
        //Get the in-text citations when they are expressed as numbers between []
        File file = new File("./testingFiles/Test4.pdf");
        try {
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> result = documentParser.getInTextCitations(false);



        assertEquals(71, result.size());

        assertTrue(result.contains("(Yuan et al., 1993;\n" +
                "Gagliardini et al., 1994; Kumar et al., 1994; Lazebnik et al.,\n" +
                "1994; Wang et al., 1994; Nicholson et al., 1995; Tewari et\n" +
                "al., 1995; Kuida et al., 1996)"));
        assertTrue(result.contains("(Martin and Green,\n" +
                "1995)"));
        assertTrue(result.contains("(Liu et al., 1996; Kluck et al., 1997a,b;\n" +
                "Deveraux et al., 1998; Pan et al., 1998a)"));
        assertTrue(result.contains("(Zou et al.,\n" +
                "1997)"));
        assertTrue(result.contains("(Martin et al., 1995b, 1996)"));

        assertTrue(result.contains("(Slee, E.A., and\n" +
                "S.J. Martin, data not shown)"));
        documentParser.close();


        file = new File("./testingFiles/Test5.pdf");
        try {
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        result = documentParser.getInTextCitations(false);

        assertEquals(139, result.size());
        assertTrue(!result.contains("1999)"));
        assertTrue(result.contains("(Esposti et al., 2001)"));
        assertTrue(result.contains("(Li et al., 1998; Luo et al., 1998)"));

        documentParser.close();

        file = new File("./testingFiles/Test7.pdf");
        try {
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        result = documentParser.getInTextCitations(false);
        assertEquals(80, result.size());
        int i =0;
        for (String s : result) {
            System.out.println(i + s);
            System.out.println();
            i++;

        }
        assertFalse(result.contains("(1972b)"));
        assertTrue(result.contains("(Iversen, 1967; Refsum and Berdal,\n" +
                "1967; Steel, 1967; Frindel, Malaise and\n" +
                "Tubiana, 1968; Laird, 1969; Clifton and\n" +
                "Yatvin, 1970; Weinstein and Frost, 1970;\n" +
                "Lala, 1971, 1972)"));
        assertTrue(result.contains("(Fig. 20 and 23; Currie\n" +
                "et al., 1972; Kerr and Searle, 1972a and\n" +
                "b)"));

        documentParser.close();

    }


    @org.junit.jupiter.api.Test
    void testGetTitle() throws IOException {
        //Gets the title, based on analysis of text and font size

        //Case 1: Document has no text
        File file = new File("./testingFiles/Test1.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String answer = documentParser.getTitle();
        documentParser.close();
        assertEquals("No title found", answer);

        //Case 2: Files with titles
        //Testing file 2.pdf
        file = new File("./testingFiles/Test2.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        answer = documentParser.getTitle();
        documentParser.close();
        assertEquals("Apoptosis control by death and decoy receptors", answer);

        //Testing file 4.pdf
        file = new File("./testingFiles/Test4.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        answer = documentParser.getTitle();
        documentParser.close();
        assertEquals("Ordering the Cytochrome c–initiated Caspase Cascade: Hierarchical Activation of " +
                "Caspases-2, -3, -6, -7, -8, and -10 in a Caspase-9–dependent Manner", answer);

        //Testing twin A.pdf
        file = new File("./A.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        answer = documentParser.getTitle();
        documentParser.close();
        assertEquals("Bid, a Bcl2 Interacting Protein, Mediates Cytochrome c Release from Mitochondria in Response to Activation of Cell Surface Death Receptors", answer);

        //Testing file 3.pdf
        file = new File("./testingFiles/Test3.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        answer = documentParser.getTitle();
        documentParser.close();
        assertEquals("Caspase structure, proteolytic substrates, and function during apoptotic cell death", answer);

        //Testing file 7.pdf
        file = new File("./testingFiles/Test6.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        answer = documentParser.getTitle();
        documentParser.close();
        assertEquals("Melatonin mitigates mitochondrial malfunction", answer);

    }


    @org.junit.jupiter.api.Test
    void testGetAuthors() throws IOException {
        //to get authors, it is necessary to first get the title

        //Case 1: Document has no text
        File file = new File("./testingFiles/Test1.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        documentParser.getTitle();
        String answer = documentParser.getAuthors();
        assertEquals("No authors found", answer);
        documentParser.close();


        //Case 2: Files with titles

        file = new File("./testingFiles/Test2.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        documentParser.getTitle();
        answer = documentParser.getAuthors();
        assertEquals("Avi Ashkenazi, Vishva M Dixit", answer);
        documentParser.close();


        file = new File("./testingFiles/Test3.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        documentParser.getTitle();
        answer = documentParser.getAuthors();
        assertEquals("DW Nicholson", answer);
        documentParser.close();


        file = new File("./testingFiles/Test4.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        documentParser.getTitle();
        answer = documentParser.getAuthors();
        assertEquals("Elizabeth A. Slee, Mary T. Harte, Ruth M. Kluck", answer);
        documentParser.close();

        file = new File("./testingFiles/B.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        documentParser.getTitle();
        answer = documentParser.getAuthors();
        assertEquals("Honglin Li, Hong Zhu, Chi-jie Xu, Junying Yuan", answer);
        documentParser.close();


        file = new File("./A.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        documentParser.getTitle();
        answer = documentParser.getAuthors();
        assertEquals("Xu Luo, Imawati Budihardjo, Hua Zou", answer);
        documentParser.close();


    }


    @org.junit.jupiter.api.Test
    void testGetSuperScriptSize() {
        File file = new File("./testingFiles/Test1.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, true, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String answer;
        //Test 1: PDF with no text body
        HashMap<Float, Integer> map = new HashMap<>();
        map.put((float) 2, 4);
        documentParser.getSuperScriptSize(map, 0);
        assertEquals(Float.POSITIVE_INFINITY, documentParser.textBodySize);

        //Test 3: PDF with text body
        map = new HashMap<>();
        map.put((float) 8, 2);
        documentParser.getSuperScriptSize(map, 0);
        assertEquals((float) 8, documentParser.textBodySize);

        //Just one size
        map = new HashMap<>();
        map.put((float) 8, 2);
        map.put((float) 7, 100);
        map.put((float) 6, 100);
        map.put((float) 5, 40);
        answer = documentParser.getSuperScriptSize(map, 5);
        assertEquals((float) 7, documentParser.textBodySize);
        assertEquals("6.0|5.0", answer);


        //Two possible sizes
        map.put((float) 5, 50);
        answer = documentParser.getSuperScriptSize(map, 5);
        assertEquals((float) 7, documentParser.textBodySize);
        assertEquals("6.0|5.0", answer);

        //Three possible sizes
        map.put((float) 4, 100);
        answer = documentParser.getSuperScriptSize(map, 4);
        assertEquals((float) 7, documentParser.textBodySize);
        assertEquals("6.0|4.0|5.0", answer);

    }

    @org.junit.jupiter.api.Test
    void testFormatSuperScript() {
        File file = new File("./testingFiles/Test1.pdf");
        try {
            //Does not need to parse the entire doc and needs format
            documentParser = new DocumentParser(file, true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Case with no text
        String answer = documentParser.formatSuperScript("");
        assertEquals("", answer);

        //Case where there is only y1 and prefix
        answer = documentParser.formatSuperScript(".{|6.0&10.0|}1234");
        assertEquals("1234", answer);

        answer = documentParser.formatSuperScript(" {|6.0&10.0|}1234");
        assertEquals("1234", answer);

        answer = documentParser.formatSuperScript("hajh{|6.0&10.0|}1234");
        assertEquals("1234", answer);

        answer = documentParser.formatSuperScript("haj{|6.0&10.0|}1234");
        assertEquals("1234", answer);

        answer = documentParser.formatSuperScript("HmL{|6.0&10.0|}1234");
        assertEquals("", answer);

        answer = documentParser.formatSuperScript("H20{|6.0&10.0|}1234");
        assertEquals("", answer);




        //Case where y1 < y2
        answer = documentParser.formatSuperScript(".{|6.0&10.0|}1234{|8.0&12.0|");
        assertEquals("1234", answer);

        //Case where  y2 > y1
        answer = documentParser.formatSuperScript("{|6.0&24.0|}1234{|8.0&12.0|");
        assertEquals("", answer);

        //Case where  y1 = y2
        answer = documentParser.formatSuperScript("{|6.0&10.0|}1234{|8.0&10.0|");
        assertEquals("", answer);


        //Case where  y1 = y2
        documentParser.formatSuperScript("");

        //Case where there is no y2 or prefix
        documentParser.formatSuperScript("");

    }



    @org.junit.jupiter.api.Test
    void testGetYear() {
        fail("Not yet implemented");
    }




}