package com.beid.belgianeidauth;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class EIDApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("eid-auth.fxml")));
        primaryStage.setTitle("EID Authentication");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/eidLogo.png"))));
        primaryStage.setScene(new Scene(root, 650, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}