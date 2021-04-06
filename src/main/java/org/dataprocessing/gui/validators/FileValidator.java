package org.dataprocessing.gui.validators;

import com.jfoenix.validation.base.ValidatorBase;
import javafx.beans.DefaultProperty;
import javafx.scene.control.TextInputControl;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validates that a file exists
 *
 * @author Nicholas Curl
 */
@DefaultProperty(value = "icon")
public class FileValidator extends ValidatorBase {

    public FileValidator() {
        setMessage("Must be a valid file path.");
    }

    public FileValidator(String message) {
        super(message);
    }

    @Override
    protected void eval() {
        if (srcControl.get() instanceof TextInputControl) {
            evalTextInputField();
        }
    }

    private void evalTextInputField() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        String text = textField.getText();
        try {
            Path path = Paths.get(text);
            hasErrors.set(!path.toFile().exists());
        } catch (InvalidPathException e) {
            hasErrors.set(true);
        }
    }
}
