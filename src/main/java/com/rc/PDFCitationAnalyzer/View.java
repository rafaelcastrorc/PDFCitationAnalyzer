package com.rc.PDFCitationAnalyzer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.URL;

/**
 * Created by rafaelcastro on 6/7/17.
 * Extends Application. Required for JavaFX
 */
public class View extends Application {

    public View() {
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Loads all the necessary components to start the application
        Parent root;
        URL s = getClass().getClassLoader().getResource("View.fxml");
        FXMLLoader loader = new FXMLLoader(s);
        root = loader.load();


        //Only use print statements if we are debugging
        boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
                getInputArguments().toString().contains("jdwp");
        if (!isDebug) {
            PrintStream dummyStream = new PrintStream(new OutputStream(){
                public void write(int b) {
                }
            });
            System.setOut(dummyStream);

        } else {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        }


        primaryStage.setTitle("Citation Analyzer by RC");
        Scene loadingScene = new Scene(root);
        loadingScene.getStylesheets().add("https://fonts.googleapis.com/css?family=Roboto:300");
        //The resources, once in the jar, are in the root folder
        String css =  getClass().getClassLoader().getResource("Style.css").toExternalForm();
        loadingScene.getStylesheets().add(css);
        primaryStage.setScene(loadingScene);
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            //Reset printing
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            Platform.exit();
            System.exit(0);
        });


    }

}
