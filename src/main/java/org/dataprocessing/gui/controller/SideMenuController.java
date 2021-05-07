package org.dataprocessing.gui.controller;

import com.jfoenix.controls.JFXListView;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import io.datafx.controller.util.VetoException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * The controller for the side bar
 *
 * @author Nicholas Curl
 */
@ViewController(value = "/fxml/SideBar.fxml", title = "Data Processor")
public class SideMenuController {

    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger();

    @FXMLViewFlowContext
    private ViewFlowContext context;

    @FXML
    @ActionTrigger("nav")
    private Label nav;

    @FXML
    @ActionTrigger("por")
    private Label por;

    @FXML
    @ActionTrigger("porDebug")
    private Label porDebug;

    @FXML
    @ActionTrigger("debug")
    private Label debug;

    @FXML
    private JFXListView<Label> sideList;

    @PostConstruct
    public void init() {
        Objects.requireNonNull(context, "context");
        FlowHandler contentFlowHandler = (FlowHandler) context.getRegisteredObject("ContentFlowHandler");
        sideList.propagateMouseEventsToParent();
        sideList.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> {
            new Thread(() -> Platform.runLater(() -> {
                if (newVal != null) {
                    try {
                        contentFlowHandler.handle(newVal.getId());
                    }
                    catch (VetoException | FlowException exc) {
                        logger.fatal("Exception", exc);
                        System.exit(-1);
                    }
                }
            })).start();
        });
        Flow contentFlow = (Flow) context.getRegisteredObject("ContentFlow");
        bindNodeToController(nav, NavController.class, contentFlow, contentFlowHandler);
        bindNodeToController(por, PorController.class, contentFlow, contentFlowHandler);
        bindNodeToController(porDebug, PORDebuggingController.class, contentFlow, contentFlowHandler);
        bindNodeToController(debug, DebuggingController.class, contentFlow, contentFlowHandler);
    }

    private void bindNodeToController(Node node, Class<?> controllerClass, Flow flow, FlowHandler flowHandler) {
        flow.withGlobalLink(node.getId(), controllerClass);
    }
}
