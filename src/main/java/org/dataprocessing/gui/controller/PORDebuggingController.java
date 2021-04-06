package org.dataprocessing.gui.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.database.SqlServer;
import org.dataprocessing.backend.mappers.por.POROpenSales;
import org.dataprocessing.gui.model.PorModel;
import org.dataprocessing.utils.Alerts;
import org.dataprocessing.utils.FileUtils;
import org.dataprocessing.utils.Utils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Nicholas Curl
 */
@ViewController(value = "/fxml/databaseProcessing.fxml", title = "Debugging")
public class PORDebuggingController {
    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger();
    /**
     * The instance of the SqlServer class
     */
    private static final SqlServer server = SqlServer.getInstance();
    /**
     * The instance of the FileUtils class
     */
    private static final FileUtils fileUtils = FileUtils.getInstance();
    /**
     * The instance of the Utils class
     */
    private static final Utils utils = Utils.getInstance();
    /**
     * The instance of the Alerts class
     */
    private static final Alerts alerts = Alerts.getInstance();
    /**
     * The instance of the PorModel class
     */
    private static final PorModel model = PorModel.getInstance();
    /**
     * The main window
     */
    private final Window window;

    @FXMLViewFlowContext
    private ViewFlowContext context;
    @FXML
    private StackPane root;
    @FXML
    private Label processor;
    @FXML
    private Label progLabel;
    @FXML
    private JFXSpinner progSpin;
    @FXML
    private JFXButton processButton;
    @FXML
    private JFXButton mainCancelButton;
    @FXML
    private JFXButton fileSelect;
    @FXML
    private JFXTextField filePath;

    /**
     * The directory to store the mapped data
     */
    private File storageLocation;
    private BooleanBinding complete;

    /**
     * The constructor for this class
     */
    public PORDebuggingController() {
        window = utils.getWindow();
        storageLocation = null;
    }

    /**
     * Initializes the PorController
     */
    @PostConstruct
    public void init() {
        processor.setText("Debugging");
        @SuppressWarnings("unchecked")
        ObservableList<String> styles = (ObservableList<String>) context.getRegisteredObject("StyleClasses");
        if (styles != null) {
            styles.addListener((InvalidationListener) observable -> {
                progSpin.getStyleClass().setAll(styles);
                progSpin.applyCss();
            });
            progSpin.getStyleClass().setAll(styles);
        }
        progLabel.textProperty().bind(model.progLabelTextProperty());
        processButton.disableProperty().bind(model.lockedProperty());
        fileSelect.disableProperty().bind(model.lockedProperty());
        filePath.disableProperty().bind(model.lockedProperty());
        if (model.totalProgressProperty() != null) {
            progSpin.progressProperty().bind(model.totalProgressProperty());
        }
        root.setOnMousePressed(event -> {
            if (!filePath.equals(event.getSource())) {
                filePath.getParent().requestFocus();
            }
        });
        mainCancelButton.setOnAction(event -> {
            if (model.isCancelable()) {
                model.setCanceled(true);
                for (Task<?> task : model.getTasks()) {
                    if (!task.isCancelled() || !task.isDone()) {
                        task.cancel(true);
                    }
                }
                model.getTasks().clear();
                model.setLocked(false);
                model.setProgLabelText("Canceled");
                progSpin.getStyleClass().add("custom-spinner-cancel");
                server.closeConnection();
                model.setCancelable(false);
            }
        });
        if (!utils.isBlankString(model.getFilePath())) {
            storageLocation = Paths.get(model.getFilePath()).toFile();
        }
        filePath.setText(model.getFilePath());
        fileSelect.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(Paths.get(".").toFile());
            chooser.setTitle("Location to store mapped data...");
            storageLocation = chooser.showDialog(window);
            if (storageLocation == null) {
                alerts.alertWindow("Please select a file directory.", "Please select a directory that stores the mapped data files.");
            } else {
                filePath.setText(storageLocation.getPath());
                model.setFilePath(storageLocation.getPath());
                filePath.resetValidation();
            }
        });
        filePath.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (filePath.validate()) {
                    storageLocation = Paths.get(filePath.getText()).toFile();
                    model.setFilePath(storageLocation.getPath());
                }
            }
        });
        processButton.setOnAction(action -> {
            progSpin.getStyleClass().remove("custom-spinner-success");
            progSpin.getStyleClass().remove("custom-spinner-cancel");
            model.setCanceled(false);
            if (!filePath.validate()) {
                alerts.alertWindow("Please select a file directory.", "Please select a directory that stores the mapped data files.");
            } else {
                ExecutorService executor = Executors.newCachedThreadPool();
                server.connectToServer();
                Path storeLocation = Paths.get(storageLocation.toURI());
                Path porStoreLocation = storeLocation.resolve("POR/");
                try {
                    Files.createDirectories(porStoreLocation);
                } catch (IOException e) {
                    logger.fatal("Unable to create directories.", e);
                    System.exit(-1);
                }
                POROpenSales porOpenSales = new POROpenSales(porStoreLocation);
                //POROpenPO porOpenSales = new POROpenPO(porStoreLocation);
                //POROpenAR porOpenSales = new POROpenAR(porStoreLocation);
                //PORItemMaster porOpenSales = new PORItemMaster(porStoreLocation);
                //PORCustomer porOpenSales = new PORCustomer(porStoreLocation);
                model.addTasks(porOpenSales.getTasks());
                DoubleBinding totalProgress = Bindings.createDoubleBinding(() -> (
                                Math.max(0, porOpenSales.getTotalProgress())
                        ),
                        porOpenSales.totalProgressProperty()
                );
                model.setTotalProgressProperty(totalProgress);
                model.setProgLabelText("Processing...");
                model.setLocked(true);
                model.setCancelable(true);
                progSpin.progressProperty().bind(totalProgress);
                complete = Bindings.createBooleanBinding(() -> (Math.abs(1.0 - totalProgress.get()) <= 5e-5), totalProgress);
                porOpenSales.map(executor);
                ObservableList<String> styleList = progSpin.getStyleClass();
                context.register("StyleClasses", styleList);
                complete.addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            progSpin.getStyleClass().add("custom-spinner-success");
                            model.setProgLabelText("Complete");
                            model.setLocked(false);
                            model.setCancelable(false);
                            complete.removeListener(this);
                        }
                    }
                });
            }
        });
    }

    /**
     * Unbinds the elements before closing
     */
    @PreDestroy
    public void onClose() {
        progSpin.progressProperty().unbind();
        progLabel.textProperty().unbind();
        processButton.disableProperty().unbind();
    }
}
