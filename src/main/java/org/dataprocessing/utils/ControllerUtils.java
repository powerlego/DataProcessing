package org.dataprocessing.utils;

import com.jfoenix.controls.JFXAlert;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.RequiredFieldValidator;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.Map;

/**
 * Utility class for controllers
 *
 * @author Nicholas Curl
 */
public class ControllerUtils extends Utils {

    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger();
    /**
     * The instance of the Utils class
     */
    private static final Utils utils = Utils.getInstance();
    /**
     * The instance of this class
     */
    private static final ControllerUtils instance = new ControllerUtils();
    /**
     * The main window
     */
    private final Window window = getWindow();

    /**
     * Gets this class's instance
     *
     * @return This class's instance
     */
    public static ControllerUtils getInstance() {
        return instance;
    }

    /**
     * Prompts the user to specify the company name to associate with the file
     *
     * @param companyNames The map of files and its associated company name
     * @param file         The file to specify the company name
     */
    public void companyName(Map<File, String> companyNames, File file) {
        JFXAlert alert = new JFXAlert(window);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setHideOnEscape(false);
        alert.setOverlayClose(false);
        JFXDialogLayout layout = new JFXDialogLayout();
        layout.setHeading(new Label("Please enter the company name."));
        JFXTextField field = new JFXTextField();
        RequiredFieldValidator validator = new RequiredFieldValidator("Company Name is Required");
        validator.setIcon(new FontIcon("fas-exclamation-triangle"));
        field.setValidators(validator);
        layout.setOnMousePressed(event -> {
            if (!field.equals(event.getSource())) {
                field.getParent().requestFocus();
            }
        });
        field.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (utils.isBlankString(field.getText())) {
                    field.validate();
                } else {
                    field.resetValidation();
                }
            }
        });
        field.setPromptText("Enter the Company Name.");
        layout.setBody(field);
        JFXButton accept = new JFXButton("ACCEPT");
        accept.getStyleClass().add("dialog-accept");
        accept.setDefaultButton(true);
        accept.setOnAction(event -> {
            if (utils.isBlankString(field.getText())) {
                field.validate();
            } else {
                companyNames.put(file, field.getText());
                alert.hideWithAnimation();
            }
        });
        layout.requestFocus();
        layout.setActions(accept);
        alert.setContent(layout);
        alert.showAndWait();
    }
}
