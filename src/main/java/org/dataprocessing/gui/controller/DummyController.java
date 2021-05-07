package org.dataprocessing.gui.controller;

import io.datafx.controller.ViewController;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;

/**
 * A dummy controller
 *
 * @author Nicholas Curl
 */
@ViewController(value = "/fxml/dummy.fxml", title = "Data Processing")
public class DummyController {

    @FXMLViewFlowContext
    private ViewFlowContext context;

}
