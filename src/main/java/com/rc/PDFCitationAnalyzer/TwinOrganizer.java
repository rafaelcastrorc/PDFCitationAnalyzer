package com.rc.PDFCitationAnalyzer;

import javafx.concurrent.Task;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by rafaelcastro on 7/26/17.
 * Organizes the downloaded files based on the twins that each paper cites
 */
public class TwinOrganizer extends Task {

    private GUILabelManagement guiLabelManagement;
    private File[] files;
    //Maps the title of the paper to the different pairIDs that it belongs to
    private HashMap<String, ArrayList<Integer>> paperTitleToCitedTwin;
    //Maps the title of the paper to the name of the folder where its located (inside of DownloadedPDFs)
    private HashMap<String, String> mapPaperTitleToFolder;
    private boolean deleteFiles;

    TwinOrganizer(GUILabelManagement guiLabelManagement) {
        this.guiLabelManagement = guiLabelManagement;
    }

    TwinOrganizer() {
    }

    /**
     * Initializes the GUI
     */
    private void initialize() {
        //Set up GUI
        guiLabelManagement.clearOutputPanel();
        guiLabelManagement.setProgressIndicator(0);
        Text outputText = new Text("Organizing the files...");
        outputText.setStyle("-fx-font-size: 16");
        //Add the progress indicator and outputText to the output panel
        guiLabelManagement.setNodeToAddToOutputPanel(guiLabelManagement.getProgressIndicatorNode());
        guiLabelManagement.setNodeToAddToOutputPanel(outputText);


    }

    /**
     * Puts paper that cite a given twin in the same folder
     */
    private void organizeTheFiles() {
        int i = 0;
        //Check if directory exists, if not create it
        File directory = new File("./OrganizedFiles");
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdir();
        }
        File couldNotOrganizeDir = new File("./OrganizedFiles/CouldNotOrganize");
        if (!couldNotOrganizeDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            couldNotOrganizeDir.mkdir();
        }
        System.out.println("Number of files to organize: " + mapPaperTitleToFolder.size());
        try {
            for (String paperTitle : mapPaperTitleToFolder.keySet()) {
                String folderName = mapPaperTitleToFolder.get(paperTitle);
                File file = files[0];
                //Get the source folder
                File srcFolder = new File(file.getParent() + "/" + folderName);
                //Get  all the non txt file (this are the downloaded versions for a given paper)
                ArrayList<File> srcFiles = new ArrayList<>();
                for (File temp : Objects.requireNonNull(srcFolder.listFiles())) {
                    if (!temp.getName().contains("ArticleName")) {
                        srcFiles.add(temp);
                    }
                }
                File destination;

                if (paperTitleToCitedTwin.get(paperTitle) == null) {
                    //If there is no mapping for this file
                    int version = 0;
                    File destinationFolder = new File("./OrganizedFiles/CouldNotOrganize/" + folderName);
                    try {
                        for (File src : srcFiles) {
                            String path = destinationFolder.getPath() + '_' + version + ".pdf";
                            destination = new File(path);
                            Files.copy(src.toPath(), destination.toPath());
                            version++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        guiLabelManagement.setAlertPopUp(e.getMessage());
                    }
                    i++;
                    guiLabelManagement.setProgressIndicator(i / ((double) mapPaperTitleToFolder.size()));
                } else {
                    for (int twinID : paperTitleToCitedTwin.get(paperTitle)) {
                        //Put it in a folder with the same twin id
                        int version = 0;
                        try {
                            File destinationFolder = new File("./OrganizedFiles/" + twinID + "/" + folderName);
                            //noinspection ResultOfMethodCallIgnored
                            new File("./OrganizedFiles/" + twinID).mkdirs();
                            for (File src : srcFiles) {
                                String path = destinationFolder.getPath() + '_' + version + ".pdf";
                                destination = new File(path);
                                Files.copy(src.toPath(), destination.toPath());
                                version++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            guiLabelManagement.setAlertPopUp(e.getMessage());
                        }
                        i++;
                        guiLabelManagement.setProgressIndicator(i / ((double) mapPaperTitleToFolder.size()));
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }


        //Update GUI
        guiLabelManagement.clearOutputPanel();
        Text outputText = new Text("All files have been organized!");
        outputText.setStyle("-fx-font-size: 24");
        outputText.setTextAlignment(TextAlignment.CENTER);
        //Add the progress indicator and outputText to the output panel
        guiLabelManagement.setNodeToAddToOutputPanel(outputText);
    }

    void copyFolder(File src, File dest) throws IOException {

        if (src.isDirectory()) {

            //if directory not exists, create it
            if (!dest.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dest.mkdirs();
                System.out.println("Directory copied from "
                        + src + "  to " + dest);
            }

            //list all the directory contents
            String files[] = src.list();

            if (files != null) {
                for (String file : files) {
                    //construct the src and dest file structure
                    File srcFile = new File(src, file);
                    File destFile = new File(dest, file);
                    //recursive copy
                    copyFolder(srcFile, destFile);
                }
            }

        } else {
            //if file, then copy it
            //Check first if dest already exist
            if (!dest.exists()) {
                InputStream in = new FileInputStream(src);
                //Use bytes stream to support all file types
                OutputStream out = new FileOutputStream(dest);

                byte[] buffer = new byte[1024];

                int length;
                //copy the file content in bytes
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();
                //If the user chose to delete the files, then we delete it after we are done copying it
                if (deleteFiles) {
                    //noinspection ResultOfMethodCallIgnored
                    src.delete();
                }
                System.out.println("File copied from " + src + " to " + dest);
            }
        }
    }


    @Override
    protected Object call() {
        initialize();
        organizeTheFiles();
        return null;
    }


    /**
     * Reads the information of the file containing the twin pairs
     *
     * @throws IOException if it is unable to access the file
     */
    void readFile(File file) throws IOException {
        paperTitleToCitedTwin = new HashMap<>();
        FileInputStream fis = new FileInputStream(file);
        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);
        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);
        // Get iterator to all the rows in current sheet
        // Traversing over each row of XLSX file
        for (Row row : mySheet) {
            // For each row, iterate through each columns
            Iterator<Cell> cellIterator = row.cellIterator();

            int i = 0;
            int twinID = 0;
            String title;

            //Col 0 is the twinID, Col 6 is titleciting
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();

                if (cell.getCellTypeEnum() == CellType.STRING) {
                    if (i == 6) {
                        title = cell.getStringCellValue();
                        ArrayList<Integer> list;
                        if (!paperTitleToCitedTwin.containsKey(title)) {
                            list = new ArrayList<>();
                            list.add(twinID);
                        } else {
                            list = paperTitleToCitedTwin.get(title);
                            list.add(twinID);
                        }
                        //Ignore the header row
                        if (!title.equals("titleciting")) {
                            paperTitleToCitedTwin.put(title, list);
                        }
                    }
                } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                    if (i == 0) {
                        twinID = (int) cell.getNumericCellValue();
                    }
                }
                i++;

            }

        }

    }


    /**
     * Parses Report.txt
     *
     * @param report Report.txt
     */
    void setReport(File report) {
        mapPaperTitleToFolder = new HashMap<>();
        try {
            //Parse the entire report and map file name to folder name
            Scanner scanner = new Scanner(new FileInputStream(report));
            boolean isDownloaded = false;
            boolean isValid = false;
            String paperTitle = null;
            while (scanner.hasNextLine()) {

                String line = scanner.nextLine();
                //If the paper is downloaded, then get the name
                if (line.contains("Paper downloaded")) {
                    isDownloaded = true;
                    paperTitle = line;
                    paperTitle = paperTitle.replaceAll("-Paper downloaded(\\(searchForCitedBy\\))?: ", "");
                    paperTitle = paperTitle.replaceAll("-Paper downloaded(\\(searchForTheArticle\\))?: ", "");
                    paperTitle = paperTitle.replaceAll("\\(Selected in SW.*", "");
                    paperTitle = paperTitle.replaceAll("\"", "");

                    while (paperTitle.endsWith(" ")) {
                        paperTitle = paperTitle.substring(0, paperTitle.lastIndexOf(" "));
                    }
                    //If it downloaded more than 0 pdfs, then is valid
                } else if (!line.contains("Number of PDFs downloaded: 0/") && !isValid && isDownloaded) {
                    //Has at least 1 pdf
                    isValid = true;
                } else {
                    //If its both valid and downloaded, then store its location so we can move the file later
                    if (isValid) {
                        String folder = line;
                        folder = folder.replaceAll(".*Folder path: ", "");
                        folder = folder.replaceAll("\"", "");
                        mapPaperTitleToFolder.put(paperTitle, folder);
                        isValid = false;
                        isDownloaded = false;
                    } else {
                        isDownloaded = false;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            guiLabelManagement.setAlertPopUp(e.getMessage());
        }
    }

    void setDownloadedPDFs(File[] files) {
        this.files = files;
    }

    /**
     * True if the program should delete the original location of the files after organizing them, false otherwise
     *
     * @param deleteFiles boolean
     */
    void setDeleteFiles(boolean deleteFiles) {
        this.deleteFiles = deleteFiles;
    }
}
