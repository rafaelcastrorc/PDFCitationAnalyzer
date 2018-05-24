package com.rc.PDFCitationAnalyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

        primaryStage.setTitle("Citation Analyzer by RC");
        Scene loadingScene = new Scene(root);
        loadingScene.getStylesheets().add("https://fonts.googleapis.com/css?family=Roboto:300");
        String css =  getClass().getClassLoader().getResource("Style.css").toExternalForm();
        loadingScene.getStylesheets().add(css);
        primaryStage.setScene(loadingScene);
        primaryStage.setResizable(false);
        primaryStage.show();


    }

}
