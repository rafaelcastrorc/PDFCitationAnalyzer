package com.rc.PDFCitationAnalyzer;

import javafx.concurrent.Task;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Compiles the output result of multiple output files into a single one
 */
public class OutputCompiler extends Task {

    private GUILabelManagement guiLabelManagement;
    private File[] files;
    private TreeMap<Integer, ArrayList<Object>> dataGathered;
    private String newOutputFileName;

    OutputCompiler(File[] files, GUILabelManagement guiLabelManagement) {
        this.files = files;
        this.guiLabelManagement = guiLabelManagement;
        this.dataGathered = new TreeMap<>();
        ArrayList<Object> header = new ArrayList<>();
        //Headers for the general output file
        header.add("Twin Paper ID");
        header.add("Total Number of Files Analyzed");
        header.add("Files that Did Not Contain One or Both Twins");
        header.add("Average Adjacent-Cit Rate");
        dataGathered.put(0, header);
    }


    @Override
    protected Object call() throws Exception {
        setupGUI();
        getFiles();
        createNewOutputFile();
        finish();

        return null;
    }


    private void setupGUI() {
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setProgressIndicator(0);
        guiLabelManagement.setNodeToAddToOutputPanel(guiLabelManagement.getProgressIndicatorNode());
        guiLabelManagement.setStatus("Compiling results...");

    }

    /**
     * Finds all the files that need to be analyed and extracts the relevant information
     */
    private void getFiles() {
        //First search for all the files that start with TwinAnalyzer and are .xlsx files
        ArrayList<File> filesToCompile = new ArrayList<>();
        for (File file : files) {
            //Make sure its not a faulty excel file
            if (file.getName().startsWith("~$") || file.getName().contains("Compiled_")) continue;
            if (file.getName().contains("TwinAnalyzerResults")) {
                String ext = FilenameUtils.getExtension(file.getName());
                if (ext.equals("xlsx")) {
                    filesToCompile.add(file);
                }

            }
        }
        int i = 0;
        //Row 0 is occupied by the header
        int rowsProcessed = 1;
        //Now read all the files and store the non empty rows
        for (File file : filesToCompile) {
            try {
                FileInputStream fis = new FileInputStream(file);
                // Finds the workbook instance for XLSX file
                XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
                // Return first sheet from the XLSX workbook
                XSSFSheet mySheet = myWorkBook.getSheetAt(0);
                // Get iterator to all the rows in current sheet
                // Traversing over each row of XLSX file
                boolean isFirst = true;
                for (Row row : mySheet) {
                    if (isFirst) {
                        isFirst = false;
                        continue;
                    }
                    Iterator<Cell> cellIterator = row.cellIterator();
                    ArrayList<Object> cells = new ArrayList<>();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        if (cell.getCellTypeEnum() == CellType.STRING) {
                            cells.add(cell.getStringCellValue());
                        } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                            cells.add(cell.getNumericCellValue());
                        } else  {
                            guiLabelManagement.setAlertPopUp("There was an error reading file: " + file.getName());
                        }
                    }
                    //Store the current cell with all of its rows
                    dataGathered.put(rowsProcessed, cells);
                    rowsProcessed++;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
            double progress = i / (1.0 * filesToCompile.size());
            guiLabelManagement.setProgressIndicator(progress);
        }

    }

    /**
     * Creates a new output file with all the rows
     */
    private void createNewOutputFile() {
        Text text = new Text("Creating new output file...");
        text.setStyle("-fx-font-size: 20");
        text.setWrappingWidth(400);
        text.setTextAlignment(TextAlignment.CENTER);
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setNodeToAddToOutputPanel(text);

        //Create the new file
        FileOutput output = new FileOutput();
        String currDate = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        this.newOutputFileName = "Compiled_TwinAnalyzerResults_" + currDate + ".xlsx";
        try {
            output.writeOutputToFile(dataGathered, newOutputFileName);
        } catch (IOException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }

    }

    /**
     * Displays message to user telling the compilation finished
     */
    private void finish() {
        Text text = new Text("The new output file was created!\n" +
                "The name is:\n" +
                newOutputFileName);
        text.setStyle("-fx-font-size: 20");
        text.setWrappingWidth(400);
        text.setTextAlignment(TextAlignment.CENTER);
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setNodeToAddToOutputPanel(text);
        guiLabelManagement.setStatus("Done!");

    }


}
