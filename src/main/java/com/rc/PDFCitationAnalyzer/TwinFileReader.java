package com.rc.PDFCitationAnalyzer;

import com.jfoenix.controls.JFXButton;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Reads the excel file containing the multiple pairs of Twins
 */
class TwinFileReader {
    private static HashMap<Object, ArrayList<Object>> paperToAuthor = new HashMap<>();
    private static HashMap<Object, ArrayList<Object>> paperToYear = new HashMap<>();
    private static HashMap<Object, ArrayList<Object>> twinIDToPaper = new HashMap<>();
    private static HashMap<Object, ArrayList<Object>> twinIDToCitingPapers = new HashMap<>();
    //Maps the title of the paper to the different pairIDs that it belongs to
    private static HashMap<Object, ArrayList<Object>> citingPaperToTwinID = new HashMap<>();

    private static ArrayList<String> titleCitingList = new ArrayList<>();
    private static ArrayList<Integer> errors;

    /**
     * Stores the excel file that contains the multiple pairs of twins
     */
    static void setUpFile(File file, boolean thereIsADefaultFile, GUILabelManagement guiLabelManagement) {
        //Empty everything
        paperToAuthor = new HashMap<>();
        paperToYear =new HashMap<>();
        twinIDToPaper = new HashMap<>();
        twinIDToCitingPapers = new HashMap<>();
        citingPaperToTwinID = new HashMap<>();
        titleCitingList = new ArrayList<>();
        try {
            errors = TwinFileReader.getAllTwinsInformation(file);

            //If there are any errors, display message to the user
            if (errors.size() != 0) {
                //Convert the list into a comma separated string
                String rowsWithErrors = StringUtils.join(errors, ", ");
                guiLabelManagement.setAlertPopUp("The following rows in the Excel file that you submitted contain " +
                        "errors and will be ignored:\n" +
                        rowsWithErrors + "\n\n" +
                        "Common errors include: \n" +
                        "-One or more cells under the following columns are empty: PairID, Title of Twin1, Title of " +
                        "Twin2, Title that cites Twin1 and Twin2, Author of Twin1, Author of Twin2, Year Twin1 " +
                        "was published, Year Twin2 was published.\n" +
                        "-Both twin papers have the same title.\n" +
                        "-The title of a twin paper is the same as the title of the paper that cites such twin.");
            }
            guiLabelManagement.updateUploadExcelFileText();

        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp("There was a problem reading your excel file.\n" +
                    e.getMessage() + "\n" +
                    "Please solve the error or upload a new file.");
            e.printStackTrace();
            //Delete the excel file from the user preferences
            UserPreferences.removeExcelFile();
            return;
        }
        //If there is not a default file, prompt the user if they want to make the current file the default
        if (!thereIsADefaultFile) {
            //Show GUI asking if user wants to store this as their default excel file
            guiLabelManagement.clearOutputPanel();
            Label instructions = new Label("The excel file has been uploaded!\n\n" +
                    "Do you want to make this your default excel file?");
            instructions.setStyle("-fx-text-alignment: center; -fx-font-size: 15");
            JFXButton yes = new JFXButton("Yes");
            yes.setDefaultButton(true);
            JFXButton no = new JFXButton("No");

            HBox hBox = new HBox(10);
            hBox.setAlignment(Pos.CENTER);
            hBox.getChildren().addAll(yes, no);

            VBox vBox = new VBox(10);
            vBox.setAlignment(Pos.CENTER);

            vBox.getChildren().addAll(instructions, hBox);
            guiLabelManagement.setNodeToAddToOutputPanel(vBox);

            Label nextStep = new Label("Now you can analyze multiple pairs of twins.\n" +
                    "Click on the 'Multiple Pairs of Twins' Analysis mode.");
            nextStep.setStyle("-fx-text-alignment: center");

            yes.setOnAction(e -> {
                guiLabelManagement.setStatus("Saved as default excel file.");
                UserPreferences.storeExcelFile(file, true);
                guiLabelManagement.clearOutputPanel();
                guiLabelManagement.setNodeToAddToOutputPanel(nextStep);

            });
            no.setOnAction(e -> {
                guiLabelManagement.setStatus("Excel file has been uploaded.");
                UserPreferences.storeExcelFile(file, false);
                guiLabelManagement.clearOutputPanel();
                guiLabelManagement.setNodeToAddToOutputPanel(nextStep);


            });

            guiLabelManagement.updateUploadExcelFileText();
        }
    }

    /**
     * Reads the information inside of report.xlsx or any excel file and gets all the twin pair information
     *
     * @return List with all the rows with errors, if any.
     * @throws IOException if it is unable to access the file
     */
    static ArrayList<Integer> getAllTwinsInformation(File file) throws IOException {
        //Read the excel file configuration
        ArrayList<Integer> config = UserPreferences.getExcelConfiguration();
        int pairIdColumn = config.get(0);
        int titleTwin1Column = config.get(1);
        int titleTwin2Column = config.get(2);
        int titleCitingColumn = config.get(3);
        int authorTwin1Column = config.get(4);
        int authorTwin2Column = config.get(5);
        int yearTwin1Column = config.get(6);
        int yearTwin2Column = config.get(7);
        FileInputStream fis = new FileInputStream(file);
        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);
        // Get iterator to all the rows in current sheet
        // Traversing over each row of XLSX file
        ArrayList<Integer> rowsWithErrors = new ArrayList<>();
        int twinID = 0;
        int previousTwinID = 0;

        boolean theRowHasAnError = false;
        for (Row row : mySheet) {
            // For each row, iterate through each columns
            Iterator<Cell> cellIterator = row.cellIterator();

            //pairIdColumn is the first column of the excel file, so we start counting from there
            int i = pairIdColumn;
            String titleTwin1 = "";
            String titleTwin2 = "";
            int yearTwin1 = 0;
            int yearTwin2 = 0;
            String authorsTwin1 = "";
            String authorsTwin2 = "";
            String titleCiting = "";

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();

                if (cell.getCellTypeEnum() == CellType.STRING) {
                    //This skips the first row since this is the header row
                    if (twinID == 0) continue;
                    //Map the title of one of the twin papers to the twin ID
                    if (i == titleTwin1Column) {
                        titleTwin1 = cell.getStringCellValue();

                    }
                    if (i == titleTwin2Column) {
                        titleTwin2 = cell.getStringCellValue();

                    }
                    //Map a twin paper author to its title
                    if (i == authorTwin1Column) {
                        //Make sure the authors are formatted correctly
                        authorsTwin1 = cell.getStringCellValue();
                        authorsTwin1 = formatAuthors(authorsTwin1);
                    }
                    //Map a twin paper author to its title
                    if (i == authorTwin2Column) {
                        //Make sure the authors are formatted correctly
                        authorsTwin2 = cell.getStringCellValue();
                        authorsTwin2 = formatAuthors(authorsTwin2);
                    }

                    //Add to the list of citing papers and map the twin id to the titles that cite it
                    if (i == titleCitingColumn) {
                        titleCiting = cell.getStringCellValue();
                        titleCitingList.add(titleCiting);
                    }


                } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                    if (i == pairIdColumn) {
                        //Get the pairID/twinID
                        twinID = (int) cell.getNumericCellValue();
                    }
                    if (twinID == 0) continue;
                    if (i == yearTwin1Column) {
                        yearTwin1 = (int) cell.getNumericCellValue();

                    }
                    if (i == yearTwin2Column) {
                        yearTwin2 = (int) cell.getNumericCellValue();
                    }

                } else {
                    //This should not happen
                    theRowHasAnError = true;
                }
                i++;
            }
            //Skip the header row
            if (twinID == 0) {
                continue;
            }
            //After getting all the data from the current row, check if the input is correct
            //That is, that none of the relevant inputs are empty and that titleTwin1 != titleTwin2
            //And title citing != (titleTwin1 or titleTwin2)
            if (twinID == 0 || titleTwin1.isEmpty() || titleTwin2.isEmpty() || authorsTwin1.isEmpty() ||
                    authorsTwin2.isEmpty() || yearTwin1 == 0 || yearTwin2 == 0 || titleCiting.isEmpty()) {
                theRowHasAnError = true;
            }
            if (titleTwin1.equals(titleTwin2) || titleTwin1.equals(titleCiting) || titleTwin2.equals(titleCiting)) {
                theRowHasAnError = true;
            }
            //Do not consider rows with errors
            if (theRowHasAnError) {
                int faultyRow = row.getRowNum() + 1;
                System.err.println("The following row is incorrectly formatted: " + faultyRow);
                rowsWithErrors.add(faultyRow);
                theRowHasAnError = false;
            }
            //The data is correct so we map it
            else {
                //If the previous TwinID = current Twin ID, we are still going over the same pair so we do not have
                // to add the information again
                if (twinID != previousTwinID) {
                    previousTwinID = twinID;
                    //Map the twinID to the 2 papers that are part of it
                    addToMap("twinID", twinID, titleTwin1);
                    addToMap("twinID", twinID, titleTwin2);
                    //Map the title of the twin paper to its authors
                    addToMap("author", titleTwin1, authorsTwin1);
                    addToMap("author", titleTwin2, authorsTwin2);
                    //Map the title of the twin paper to the year it was published
                    addToMap("year", titleTwin1, yearTwin1);
                    addToMap("year", titleTwin2, yearTwin2);
                }
                //Map the twinID to the papers that cite the twins.
                addToMap("twinIDToCitingPaper", twinID, titleCiting);
                //Maps the title that cites a twin to the different twins that it cites
                addToMap("citingPaperToTwinID", titleCiting, twinID);
            }
            myWorkBook.close();
            fis.close();
        }
        return rowsWithErrors;

    }

    /**
     * Maps a key to an object depending on the type.
     */
    private static void addToMap(String type, Object key, Object objectToAdd) {
        HashMap<Object, ArrayList<Object>> mapToUse;
        switch (type) {
            case "author":
                mapToUse = paperToAuthor;
                break;
            case "year":
                mapToUse = paperToYear;
                break;
            case "citingPaperToTwinID":
                mapToUse = citingPaperToTwinID;
                break;
            case "twinIDToCitingPaper":
                mapToUse = twinIDToCitingPapers;
                break;
            default:
                mapToUse = twinIDToPaper;
                break;
        }
        if (!mapToUse.containsKey(key)) {
            ArrayList<Object> list = new ArrayList<>();
            list.add(objectToAdd);
            mapToUse.put(key, list);
        } else {
            ArrayList<Object> list = mapToUse.get(key);
            list.add(objectToAdd);
            mapToUse.put(key, list);
        }
    }


    /**
     * Formats the author names correctly (Comma separated list of names. Ex: Rafael Castro, John Ellis)
     *
     * @param authors String with all the authors
     * @return String with the name of the authors
     */
    private static String formatAuthors(String authors) {
        if (authors.contains(",") && !authors.contains(";")) {
            return authors;
        }
        String[] allAuthors = authors.split(";");
        StringBuilder sb2 = new StringBuilder();

        for (String currAuthor : allAuthors) {
            //Remove any white space
            currAuthor = currAuthor.replaceAll("^[ \\t]+|[ \\t]+$", "");
            String[] currAuthorNames = currAuthor.split(",");
            StringBuilder holderOfNames = new StringBuilder();
            for (String name : currAuthorNames) {
                holderOfNames.insert(0, name + " ");
            }
            String namesOfAuthorN = holderOfNames.toString();
            namesOfAuthorN = namesOfAuthorN.replaceAll("^[ \\t]+|[ \\t]+$", "");
            sb2.append(namesOfAuthorN).append(", ");
        }
        return sb2.substring(0, sb2.length() - 2);
    }

    static HashMap<Object, ArrayList<Object>> getPaperToAuthor() {
        return paperToAuthor;
    }

    static HashMap<Object, ArrayList<Object>> getPaperToYear() {
        return paperToYear;
    }

    static HashMap<Object, ArrayList<Object>> getTwinIDToPaper() {
        return twinIDToPaper;
    }

    static HashMap<Object, ArrayList<Object>> getTwinIDToCitingPapers() {
        return twinIDToCitingPapers;
    }

    static HashMap<Object, ArrayList<Object>> getCitingPaperToTwinID() {
        return citingPaperToTwinID;
    }

    static ArrayList<String> getTitleCitingList() {
        return titleCitingList;
    }

    static ArrayList<Integer> getErrors() {
        return errors;
    }
}


