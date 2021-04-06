package org.dataprocessing.gui.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
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
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.database.SqlServer;
import org.dataprocessing.backend.mappers.custom.VendorList;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Nicholas Curl
 */
@ViewController(value = "/fxml/debugging.fxml", title = "Data Processor")
public class DebuggingController {
    private static final Logger logger = LogManager.getLogger();
    private static final SqlServer server = SqlServer.getInstance();
    @FXMLViewFlowContext
    private ViewFlowContext context;

    @FXML
    private JFXSpinner progSpin;
    @FXML
    private Label progress1;
    @FXML
    private JFXButton process;
    @FXML
    private StackPane root;
    private BooleanBinding complete;

    @PostConstruct
    public void init() {
        @SuppressWarnings("unchecked")
        ObservableList<String> styles = (ObservableList<String>) context.getRegisteredObject("StyleClasses");
        if (styles != null) {
            styles.addListener((InvalidationListener) observable -> {
                progSpin.getStyleClass().setAll(styles);
                progSpin.applyCss();
            });
            progSpin.getStyleClass().setAll(styles);
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        process.setOnAction(action -> {
            progSpin.getStyleClass().remove("custom-spinner-success");
            progSpin.getStyleClass().remove("custom-spinner-cancel");
            server.connectToServer();

            VendorList vendorList = new VendorList(Paths.get("./mapped data/"));
            DoubleBinding totalProgress = Bindings.createDoubleBinding(() -> (
                            Math.max(0, vendorList.getTotalProgress())
                    ),
                    vendorList.totalProgressProperty()
            );
            progSpin.progressProperty().bind(totalProgress);
            complete = Bindings.createBooleanBinding(() -> (Math.abs(1.0 - totalProgress.get()) <= 5e-5), totalProgress);
            vendorList.map(executor);
            ObservableList<String> styleList = progSpin.getStyleClass();
            context.register("StyleClasses", styleList);
            complete.addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        progSpin.getStyleClass().add("custom-spinner-success");
                        complete.removeListener(this);
                    }
                }
            });
        });


    }


}
