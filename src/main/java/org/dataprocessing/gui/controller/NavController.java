package org.dataprocessing.gui.controller;

import com.jfoenix.controls.*;
import com.jfoenix.validation.RequiredFieldValidator;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dataprocessing.backend.mappers.nav.NAVVendor;
import org.dataprocessing.gui.model.NavModel;
import org.dataprocessing.gui.validators.FileValidator;
import org.dataprocessing.utils.*;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The controller for NAV Processing
 *
 * @author Nicholas Curl
 */
@ViewController(value = "/fxml/processing.fxml", title = "Data Processor")
public class NavController {

    /**
     * The instance of the logger
     */
    private static final Logger                  logger          = LogManager.getLogger();
    /**
     * The instance of the Utils class
     */
    private static final Utils                   utils           = Utils.getInstance();
    /**
     * The instance of the FileUtils class
     */
    private static final FileUtils               fileUtils       = FileUtils.getInstance();
    /**
     * The instance of the Alerts class
     */
    private static final Alerts                  alerts          = Alerts.getInstance();
    /**
     * The instance of the NavModel class
     */
    private static final NavModel                model           = NavModel.getInstance();
    /**
     * Instance of the ControllerUtils class
     */
    private static final ControllerUtils         controllerUtils = ControllerUtils.getInstance();
    /**
     * The map containing the file to process and the formats it contains
     */
    private final        Map<File, List<String>> fileFormats;
    /**
     * The map of the file to process and the company name associated to it
     */
    private final        Map<File, String>       companyNames;
    /**
     * The main window
     */
    private final        Window                  window;
    @FXMLViewFlowContext
    private              ViewFlowContext         context;
    @FXML
    private              JFXButton               mainCancelButton;
    @FXML
    private              JFXButton               processButton;
    @FXML
    private              Label                   progress;
    @FXML
    private              JFXProgressBar          progressBar;
    @FXML
    private              JFXTextField            filePath1;
    @FXML
    private              JFXTextField            filePath2;
    @FXML
    private              JFXButton               fileSelect1;
    @FXML
    private              JFXButton               fileSelect2;
    @FXML
    private              StackPane               root;
    @FXML
    private              Label                   processor;

    /**
     * The directory that contains the data to process
     */
    private File dataLocation;
    /**
     * The directory to store the mapped data
     */
    private File storageLocation;

    /**
     * The constructor for this class
     */
    public NavController() {
        fileFormats = new HashMap<>();
        companyNames = new HashMap<>();
        window = utils.getWindow();
        dataLocation = null;
        storageLocation = null;
    }

    /**
     * A helper function that prompts the user to specify the formats of the file
     *
     * @param numSheets The number of sheets in the file
     * @param file      The file to specify the formats
     */
    private void fileFormats(int numSheets, File file) {
        JFXAlert<Void> fileFormat = new JFXAlert<>(window);
        fileFormat.initModality(Modality.APPLICATION_MODAL);
        fileFormat.setOverlayClose(false);
        fileFormat.setHideOnEscape(false);
        JFXDialogLayout layout = new JFXDialogLayout();
        Label label1 = new Label("Select File Templates for: ");
        Path fileName = dataLocation.toPath().relativize(file.toPath());
        Label label2 = new Label(fileName.toString());
        label2.setWrapText(true);
        Label label3 = new Label("This has ");
        Label label4 = new Label(String.valueOf(numSheets));
        Label label5 = new Label(" sheets.");
        HBox hBox1 = new HBox(label1, label2);
        hBox1.setAlignment(Pos.CENTER);
        HBox hBox2 = new HBox(label3, label4, label5);
        hBox2.setAlignment(Pos.CENTER);
        VBox vBox = new VBox(hBox1, hBox2);
        vBox.setAlignment(Pos.CENTER);
        layout.setHeading(vBox);
        JFXCheckBox expense = new JFXCheckBox("Expense Category");
        JFXCheckBox ap = new JFXCheckBox("Open AP (Vendor Bills)");
        JFXCheckBox ar = new JFXCheckBox("Open AR (Invoices)");
        JFXCheckBox purchase = new JFXCheckBox("Open Purchase Order");
        JFXCheckBox sales = new JFXCheckBox("Open Sales Order");
        JFXCheckBox credit = new JFXCheckBox("Open Vendor Credit Purchase Item Line");
        JFXCheckBox balance = new JFXCheckBox("Trial Balance");
        JFXCheckBox vendor = new JFXCheckBox("Vendor");
        VBox vBox1 = new VBox(expense, ap, ar, purchase, sales, credit, balance, vendor);
        vBox1.setAlignment(Pos.CENTER);
        vBox1.setSpacing(5.0);
        layout.setBody(vBox1);
        JFXButton accept = new JFXButton("ACCEPT");
        accept.setDefaultButton(true);
        accept.getStyleClass().add("dialog-accept");
        List<String> formats = new ArrayList<>();
        accept.setOnAction(event -> {
            if (expense.isSelected()) {
                formats.add("expense");
            }
            if (ap.isSelected()) {
                formats.add("ap");
            }
            if (ar.isSelected()) {
                formats.add("ar");
            }
            if (purchase.isSelected()) {
                formats.add("purchase");
            }
            if (sales.isSelected()) {
                formats.add("sales");
            }
            if (credit.isSelected()) {
                formats.add("credit");
            }
            if (balance.isSelected()) {
                formats.add("balance");
            }
            if (vendor.isSelected()) {
                formats.add("vendor");
            }
            if (formats.size() < 1) {
                alerts.alertWindow("Please select a format.", "Please select at least one format for this file.");
            }
            else {
                fileFormats.put(file, formats);
                fileFormat.hideWithAnimation();
            }
        });
        layout.setActions(accept);
        layout.requestFocus();
        fileFormat.setContent(layout);
        fileFormat.showAndWait();
    }

    /**
     * Initializes the NavController
     */
    @PostConstruct
    public void init() {
        //Gets the style classes of the progress bar
        @SuppressWarnings("unchecked")
        ObservableList<String> styles = (ObservableList<String>) context.getRegisteredObject("StyleClasses");
        if (styles != null) {
            styles.addListener((InvalidationListener) observable -> progressBar.getStyleClass().setAll(styles));
        }
        //Checks to see if the gui should be locked
        if (!model.isLocked()) {
            progressBar.setVisible(false);
            model.setProgressBarText("");
            model.setProgress(null);
        }
        processor.setText("NAV Processing");
        //The validators for the file selectors
        FileValidator fileValidator = new FileValidator("Valid Directory Required!");
        fileValidator.setIcon(new FontIcon("fas-exclamation-triangle"));
        RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator("Valid Directory Required!");
        requiredFieldValidator.setIcon(new FontIcon("fas-exclamation-triangle"));
        filePath1.setValidators(requiredFieldValidator, fileValidator);
        filePath2.setValidators(requiredFieldValidator, fileValidator);
        filePath1.setText(model.getFilePath1());
        filePath2.setText(model.getFilePath2());
        if (!utils.isBlankString(model.getFilePath1())) {
            dataLocation = Paths.get(model.getFilePath1()).toFile();
        }
        if (!utils.isBlankString(model.getFilePath2())) {
            storageLocation = Paths.get(model.getFilePath2()).toFile();
        }
        progress.textProperty().bind(model.progressBarTextProperty());
        if (model.getProgress() != null) {
            progressBar.setVisible(true);
            progressBar.progressProperty().bind(model.getProgress().progressProperty());
        }
        root.setOnMousePressed(event -> {
            if (!filePath1.equals(event.getSource()) && !filePath2.equals(event.getSource())) {
                filePath1.getParent().requestFocus();
            }
        });
        fileSelect1.setOnAction(action -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(Paths.get(".").toFile());
            chooser.setTitle("Location of data files...");
            dataLocation = chooser.showDialog(window);
            if (dataLocation == null) {
                alerts.alertWindow("Please select a file directory.",
                                   "Please select a directory that contains the data files."
                );
            }
            else {
                filePath1.setText(dataLocation.getPath());
                model.setFilePath1(dataLocation.getPath());
                filePath1.resetValidation();
            }
        });
        filePath1.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (filePath1.validate()) {
                    dataLocation = Paths.get(filePath1.getText()).toFile();
                    model.setFilePath1(dataLocation.getPath());
                }
            }
        });
        fileSelect2.setOnAction(action -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(Paths.get(".").toFile());
            chooser.setTitle("Location to store mapped data...");
            storageLocation = chooser.showDialog(window);
            if (storageLocation == null) {
                alerts.alertWindow("Please select a file directory.",
                                   "Please select a directory that stores the mapped data files."
                );
            }
            else {
                filePath2.setText(storageLocation.getPath());
                model.setFilePath2(storageLocation.getPath());
                filePath2.resetValidation();
            }
        });
        filePath2.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (filePath2.validate()) {
                    storageLocation = Paths.get(filePath2.getText()).toFile();
                    model.setFilePath2(storageLocation.getPath());
                }
            }
        });
        mainCancelButton.setOnAction(action -> {
            if (model.getProgress() != null) {
                if (model.getProgress().isRunning()) {
                    progressBar.progressProperty().unbind();
                    progressBar.getStyleClass().add("progress-cancel");
                    progress.setText("Canceled");
                    model.setLocked(false);
                    fileFormats.clear();
                    companyNames.clear();
                    model.getProgress().cancel();
                }
            }
        });
        processButton.setOnAction(action -> {
            progressBar.getStyleClass().remove("progress-success");
            progressBar.getStyleClass().remove("progress-cancel");
            if (dataLocation == null) {
                alerts.alertWindow("Please select a file directory.",
                                   "Please select a directory that stores the mapped data files."
                );
            }
            else {
                if (storageLocation == null) {
                    alerts.alertWindow("Please select a file directory.",
                                       "Please select a directory that stores the mapped data files."
                    );
                }
                else {
                    model.setLocked(true);
                    File[] files = dataLocation.listFiles();
                    try {
                        Objects.requireNonNull(files, "File list must not be null.");
                        processFiles(files);
                    }
                    catch (Exception e) {
                        logger.fatal("Exception", e);
                        System.exit(-1);
                    }
                    model.setProgressBarText("Processing...");
                    Task<Void> task = new Task<Void>() {

                        @Override
                        protected Void call() {
                            double progressUpdate = 1.0 / fileFormats.size();
                            double progress = 0.0;
                            Reader converter = new Reader();
                            Path storeLocation = Paths.get(storageLocation.toURI());
                            Path navStoreLocation = storeLocation.resolve("NAV/");
                            for (File dataFile : fileFormats.keySet()) {
                                Path companyFolder = navStoreLocation.resolve(companyNames.get(dataFile) + "/");
                                try {
                                    Files.createDirectories(companyFolder);
                                }
                                catch (IOException e) {
                                    logger.fatal("Unable to create directories.", e);
                                    continue;
                                }
                                progressUpdate = progressUpdate / fileFormats.get(dataFile).size();
                                for (String format : fileFormats.get(dataFile)) {
                                    switch (format) {
                                        case "expense":
                                            alerts.alertWindow("Format", format);
                                            break;
                                        case "ap":
                                            alerts.alertWindow("Format", format);
                                            break;
                                        case "ar":
                                            alerts.alertWindow("Format", format);
                                            break;
                                        case "purchase":
                                            alerts.alertWindow("Format", format);
                                            break;
                                        case "sales":
                                            alerts.alertWindow("Format", format);
                                            break;
                                        case "credit":
                                            alerts.alertWindow("Format", format);
                                            break;
                                        case "balance":
                                            alerts.alertWindow("Format", format);
                                            break;
                                        case "vendor":
                                            NAVVendor navVendor = new NAVVendor();
                                            if (dataFile.getName().contains(".csv")) {
                                                List<List<String>> sheet = converter.readCSV(dataFile);
                                                navVendor.mapVendor(sheet);
                                                List<List<String>> mappedSheet = navVendor.getMapTable();
                                                Path vendorMappedFile = companyFolder.resolve("Vendor Mapped.csv");
                                                fileUtils.writeCSV(vendorMappedFile, mappedSheet);
                                            }
                                            else {
                                                List<List<String>> sheet = converter.readSheet(dataFile, "Vendor");
                                                navVendor.mapVendor(sheet);
                                                List<List<String>> mappedSheet = navVendor.getMapTable();
                                                Path vendorMappedFile = companyFolder.resolve("Vendor Mapped.xlsx");
                                                fileUtils.writeXlsx(vendorMappedFile, mappedSheet);
                                            }
                                    }
                                    progress += progressUpdate;
                                    updateProgress(progress, 1.0);
                                    utils.sleep(10);
                                }
                                utils.sleep(10);
                            }
                            return null;
                        }
                    };
                    task.setOnSucceeded(event -> {
                        progressBar.getStyleClass().add("progress-success");
                        model.setProgressBarText("Completed");
                        model.setLocked(false);
                    });
                    model.setProgress(task);
                    ObservableList<String> styleList = progressBar.getStyleClass();
                    context.register("StyleClasses", styleList);
                    new Thread(task).start();
                    progressBar.setVisible(true);
                    progressBar.progressProperty().bind(task.progressProperty());
                }
            }
        });
        processButton.disableProperty().bind(model.lockedProperty());
        fileSelect1.disableProperty().bind(model.lockedProperty());
        filePath1.disableProperty().bind(model.lockedProperty());
        fileSelect2.disableProperty().bind(model.lockedProperty());
        filePath2.disableProperty().bind(model.lockedProperty());
    }

    /**
     * Helper function to process the files
     *
     * @param files The files to process
     */
    private void processFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                File[] directoryFiles = file.listFiles();
                Objects.requireNonNull(directoryFiles, "Directory file list must not be null.");
                if (directoryFiles.length != 0) {
                    processFiles(directoryFiles);
                }
            }
            else {
                if (file.getName().contains(".csv")) {
                    singleFileFormat(file);
                }
                else {
                    try {
                        Workbook wb = WorkbookFactory.create(file);
                        int numSheets = wb.getNumberOfSheets();
                        wb.close();
                        if (numSheets <= 1) {
                            singleFileFormat(file);
                        }
                        else {
                            fileFormats(numSheets, file);
                        }
                    }
                    catch (IOException e) {
                        logger.fatal("Unable to create workbook", e);
                        System.exit(-1);
                    }
                    catch (EncryptedDocumentException e) {
                        logger.fatal("Document is encrypted", e);
                        System.exit(-1);
                    }
                }
                controllerUtils.companyName(companyNames, file);
            }
        }
    }

    /**
     * A helper function to specify the format of the file
     *
     * @param file The file to specify the format
     */
    private void singleFileFormat(File file) {
        JFXAlert<Void> fileFormat = new JFXAlert<>(window);
        fileFormat.initModality(Modality.APPLICATION_MODAL);
        fileFormat.setOverlayClose(false);
        fileFormat.setHideOnEscape(false);
        JFXDialogLayout layout = new JFXDialogLayout();
        Label label1 = new Label("Select File Templates for: ");
        Path fileName = dataLocation.toPath().relativize(file.toPath());
        Label label2 = new Label(fileName.toString());
        label2.setWrapText(true);
        HBox hBox1 = new HBox(label1, label2);
        hBox1.setAlignment(Pos.CENTER);
        VBox vBox = new VBox(hBox1);
        vBox.setAlignment(Pos.CENTER);
        layout.setHeading(vBox);
        ToggleGroup toggleGroup = new ToggleGroup();
        JFXRadioButton expense = new JFXRadioButton("Expense Category");
        expense.setToggleGroup(toggleGroup);
        JFXRadioButton ap = new JFXRadioButton("Open AP (Vendor Bills)");
        ap.setToggleGroup(toggleGroup);
        JFXRadioButton ar = new JFXRadioButton("Open AR (Invoices)");
        ar.setToggleGroup(toggleGroup);
        JFXRadioButton purchase = new JFXRadioButton("Open Purchase Order");
        purchase.setToggleGroup(toggleGroup);
        JFXRadioButton sales = new JFXRadioButton("Open Sales Order");
        sales.setToggleGroup(toggleGroup);
        JFXRadioButton credit = new JFXRadioButton("Open Vendor Credit Purchase Item Line");
        credit.setToggleGroup(toggleGroup);
        JFXRadioButton balance = new JFXRadioButton("Trial Balance");
        balance.setToggleGroup(toggleGroup);
        JFXRadioButton vendor = new JFXRadioButton("Vendor");
        vendor.setToggleGroup(toggleGroup);
        VBox vBox1 = new VBox(expense, ap, ar, purchase, sales, credit, balance, vendor);
        vBox1.setAlignment(Pos.CENTER);
        vBox1.setSpacing(5.0);
        layout.setBody(vBox1);
        JFXButton accept = new JFXButton("ACCEPT");
        accept.setDefaultButton(true);
        accept.getStyleClass().add("dialog-accept");
        List<String> formats = new ArrayList<>();
        accept.setOnAction(event -> {
            if (expense.isSelected()) {
                formats.add("expense");
            }
            if (ap.isSelected()) {
                formats.add("ap");
            }
            if (ar.isSelected()) {
                formats.add("ar");
            }
            if (purchase.isSelected()) {
                formats.add("purchase");
            }
            if (sales.isSelected()) {
                formats.add("sales");
            }
            if (credit.isSelected()) {
                formats.add("credit");
            }
            if (balance.isSelected()) {
                formats.add("balance");
            }
            if (vendor.isSelected()) {
                formats.add("vendor");
            }
            if (formats.size() < 1) {
                alerts.alertWindow("Please select a format.", "Please select at least one format for this file.");
            }
            else {
                fileFormats.put(file, formats);
                fileFormat.hideWithAnimation();
            }
        });
        layout.setActions(accept);
        layout.requestFocus();
        fileFormat.setContent(layout);
        fileFormat.showAndWait();
    }
}
