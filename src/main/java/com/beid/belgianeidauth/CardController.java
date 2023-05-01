package com.beid.belgianeidauth;

import com.beid.belgianeidauth.model.Client;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Optional;

public class CardController {
    @FXML
    private Label statusLabel;
    @FXML
    private Button connectBank;
    @FXML
    private Circle circle;

    private final Client client;
    public ObjectProperty<Paint> circleColor = new SimpleObjectProperty<>(Color.RED);

    public CardController() {
        this.client = new Client();
    }

    public void initialize() {
        statusLabel.textFillProperty().bind(circleColor);
        circle.fillProperty().bind(circleColor);

        if (!client.isCardLoaded()) {
            Thread loadCardThread = new Thread(() -> {
                client.loadCard();
                Platform.runLater(() -> statusLabel.setText("Card detected!"));
            });
            loadCardThread.setDaemon(true);
            loadCardThread.start();
        } else {
            statusLabel.setText("Card detected!");
        }

        Thread checkCardThread = new Thread(() -> {
            while (true) {
                boolean isCardLoaded = client.isCardLoaded();
                Platform.runLater(() -> {
                    if (isCardLoaded) {
                        statusLabel.setText("Card detected");
                        connectBank.setDisable(false);
                        circleColor.setValue(Color.GREEN);
                    } else {
                        statusLabel.setText("Card not detected");
                        connectBank.setDisable(true);
                        circleColor.setValue(Color.RED);
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        checkCardThread.setDaemon(true);
        checkCardThread.start();
    }

    @FXML
    public void connectToBank() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Banking credentials");
        dialog.setHeaderText("Enter client number and PIN");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField clientNumber = new TextField();
        clientNumber.setPromptText("Client number");
        PasswordField pin = new PasswordField();
        pin.setPromptText("PIN");

        grid.add(new Label("Client number:"), 0, 0);
        grid.add(clientNumber, 1, 0);
        grid.add(new Label("PIN:"), 0, 1);
        grid.add(pin, 1, 1);

        // Disable the OK button until both fields are not empty
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        clientNumber.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty() || pin.getText().trim().isEmpty());
        });
        pin.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty() || clientNumber.getText().trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Wait for the user to input both fields or cancel the dialog
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            System.out.println(clientNumber.getText());
            System.out.println(pin.getText());

            Client.getBankAuthorization(clientNumber.getText(), pin.getText());
        }
    }

}
