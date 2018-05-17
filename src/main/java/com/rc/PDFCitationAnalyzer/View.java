package com.rc.PDFCitationAnalyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/View.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Citation Analyzer by RC");
        Scene loadingScene = new Scene(root);
        loadingScene.getStylesheets().add("Style.css");
        loadingScene.getStylesheets().add("https://fonts.googleapis.com/css?family=Roboto");
        primaryStage.setScene(loadingScene);
        primaryStage.setResizable(false);
        primaryStage.show();

    }

}
