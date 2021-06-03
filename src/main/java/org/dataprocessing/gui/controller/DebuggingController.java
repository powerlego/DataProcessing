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
import org.dataprocessing.backend.tasks.KitWriter;
import org.dataprocessing.utils.CustomExecutors;
import org.dataprocessing.utils.Utils;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;

/**
 * @author Nicholas Curl
 */
@ViewController(value = "/fxml/debugging.fxml", title = "Data Processor")
public class DebuggingController {
    private static final Logger          logger = LogManager.getLogger(DebuggingController.class);
    private static final SqlServer       server = SqlServer.getInstance();
    private static final Utils           utils  = Utils.getInstance();
    @FXMLViewFlowContext
    private              ViewFlowContext context;
    @FXML
    private              JFXSpinner      progSpin;
    @FXML
    private              Label           progress1;
    @FXML
    private              JFXButton       process;
    @FXML
    private              StackPane       root;
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
        ExecutorService executor = CustomExecutors.newFixedThreadPool(20);
        process.setOnAction(action -> {
            progSpin.getStyleClass().remove("custom-spinner-success");
            progSpin.getStyleClass().remove("custom-spinner-cancel");
            server.connectToServer();
            KitWriter kitWriter = new KitWriter();
            //VendorList vendorList = new VendorList(Paths.get("./mapped data/"));
            DoubleBinding totalProgress = Bindings.createDoubleBinding(() -> (
                                                                               Math.max(0, kitWriter.getTotalProgress())
                                                                       ),
                                                                       kitWriter.totalProgressProperty()
            );
            progSpin.progressProperty().bind(totalProgress);
            complete = Bindings.createBooleanBinding(() -> (Math.abs(1.0 - totalProgress.get()) <= 5e-5),
                                                     totalProgress
            );
            kitWriter.map(executor);
            ObservableList<String> styleList = progSpin.getStyleClass();
            context.register("StyleClasses", styleList);
            complete.addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        utils.shutdownExecutor(executor, logger);
                        progSpin.getStyleClass().add("custom-spinner-success");
                        complete.removeListener(this);
                    }
                }
            });
        });
    }
}
