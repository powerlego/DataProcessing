package org.dataprocessing.utils;

import com.jfoenix.controls.JFXAlert;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.RequiredFieldValidator;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for displaying alerts
 *
 * @author Nicholas Curl
 */
public class Alerts {

    /**
     * Instance of the logger
     */
    private static final Logger logger   = LogManager.getLogger(Alerts.class);
    /**
     * This class's instance
     */
    private static final Alerts instance = new Alerts();
    /**
     * The instance of the Utils class
     */
    private static final Utils  utils    = Utils.getInstance();

    /**
     * Gets this class's instance
     *
     * @return This class's instance
     */
    public static Alerts getInstance() {
        return instance;
    }

    /**
     * Displays an error window with the given error message
     *
     * @param error The error message to display
     */
    public void errorWindow(String error) {
        alertWindow("Error", error);
    }

    /**
     * Displays an alert window with the given heading and body
     *
     * @param heading The heading of the alert
     * @param body    The body of the alert
     */
    public void alertWindow(String heading, String body) {
        JFXAlert alert = new JFXAlert(utils.getWindow());
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setOverlayClose(false);
        JFXDialogLayout layout = new JFXDialogLayout();
        layout.setMaxWidth(utils.getWindow().getWidth());
        Label label1 = new Label(heading);
        label1.setWrapText(true);
        label1.setAlignment(Pos.CENTER);
        label1.setTextAlignment(TextAlignment.CENTER);
        HBox hBox = new HBox(label1);
        hBox.setAlignment(Pos.CENTER);
        layout.setHeading(hBox);
        Label label2 = new Label(body);
        label2.setWrapText(true);
        label2.setAlignment(Pos.CENTER);
        label2.setTextAlignment(TextAlignment.CENTER);
        HBox hBox1 = new HBox(label2);
        hBox1.setAlignment(Pos.CENTER);
        layout.setBody(hBox1);
        JFXButton closeButton = new JFXButton("Close");
        closeButton.getStyleClass().add("dialog-accept");
        closeButton.setOnAction(event -> alert.hideWithAnimation());
        layout.setActions(closeButton);
        alert.setContent(layout);
        alert.showAndWait();
    }

    /**
     * Displays a text prompt for fixing a string format
     *
     * @param header      The header of this prompt
     * @param format      The requested format of the string
     * @param prompt      The text to display in the prompt
     * @param companyName The company name that is associated with the string to fix
     *
     * @return The fixed string
     */
    public String stringFormatPrompt(String header, String format, String prompt, String companyName) {
        AtomicReference<String> value = new AtomicReference<>();
        JFXAlert alert = new JFXAlert(utils.getWindow());
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setOverlayClose(false);
        alert.setHideOnEscape(false);
        JFXDialogLayout layout = new JFXDialogLayout();
        layout.setMaxWidth(500);
        Label label = new Label(header);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.setWrapText(true);
        Label formatLabel = new Label(format);
        formatLabel.setTextAlignment(TextAlignment.CENTER);
        formatLabel.setAlignment(Pos.CENTER);
        formatLabel.setWrapText(true);
        VBox vBox = new VBox(label, formatLabel);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(5.0);
        Label label1 = new Label("This is for ");
        label1.setAlignment(Pos.CENTER);
        label1.setTextAlignment(TextAlignment.CENTER);
        Label label2 = new Label(companyName);
        label2.setTextAlignment(TextAlignment.CENTER);
        label2.setAlignment(Pos.CENTER);
        label2.setWrapText(true);
        HBox hBox = new HBox(label1, label2);
        hBox.setAlignment(Pos.CENTER);
        VBox vBox1 = new VBox(vBox, hBox);
        vBox1.setAlignment(Pos.CENTER);
        vBox1.setSpacing(10.0);
        layout.setHeading(vBox1);
        JFXTextField field = new JFXTextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-font-size: 16");
        RequiredFieldValidator validator = new RequiredFieldValidator("Required Field");
        validator.setIcon(new FontIcon("fas-exclamation-triangle"));
        field.setValidators(validator);
        field.setAlignment(Pos.CENTER);
        field.setLabelFloat(true);
        layout.setBody(field);
        JFXButton button = new JFXButton("ACCEPT");
        button.getStyleClass().add("dialog-accept");
        button.setDefaultButton(true);
        button.setOnAction(event -> {
            if (field.validate()) {
                alert.hideWithAnimation();
                value.set(field.getText());
            }
        });
        layout.setActions(button);
        alert.setContent(layout);
        alert.showAndWait();
        return value.get();
    }
}
