package org.dataprocessing;

import com.jfoenix.assets.JFoenixResources;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.svg.SVGGlyph;
import com.jfoenix.svg.SVGGlyphLoader;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.container.DefaultFlowContainer;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.database.SqlServer;
import org.dataprocessing.gui.controller.MainController;
import org.dataprocessing.utils.Utils;

import java.io.IOException;

public class Main extends Application {

    private static final Logger logger = LogManager.getLogger();
    private static final Utils utils = Utils.getInstance();
    private static final SqlServer server = SqlServer.getInstance();
    @FXMLViewFlowContext
    private ViewFlowContext flowContext;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        /*List<String> params = getParameters().getRaw();
        if (params.size() != 0) {
            if (params.get(0).equalsIgnoreCase("debug")) {
                FXMLLoader loader;
                Parent root;
                Scene scene;
                switch (params.get(1)) {
                    case "customerMapping":
                        loader = new FXMLLoader(getClass().getResource("/fxml/debugging.fxml"));
                        root = loader.load();
                        scene = new Scene(root);
                        primaryStage.setScene(scene);
                        utils.setWindow(primaryStage);
                        primaryStage.show();
                    case "sqlDatabase":
                        Flow flow = new Flow(DebuggingController.class);
                        DefaultFlowContainer container = new DefaultFlowContainer();
                        flowContext = new ViewFlowContext();
                        flowContext.register("Stage", primaryStage);
                        flow.createHandler(flowContext).start(container);
                        JFXDecorator decorator = new JFXDecorator(primaryStage, container.getView());
                        decorator.setCustomMaximize(true);
                        decorator.setGraphic(new SVGGlyph(""));
                        primaryStage.setTitle("Data Processing");

                        double width = 800;
                        double height = 600;
                        try {
                            Rectangle2D bounds = Screen.getScreens().get(0).getBounds();
                            width = bounds.getWidth() / 2.5;
                            height = bounds.getHeight() / 1.35;
                        } catch (Exception e) {
                            logger.fatal("Index out of Bounds", e);
                            System.exit(-1);
                        }
                        scene = new Scene(decorator, width, height);
                        final ObservableList<String> stylesheets = scene.getStylesheets();
                        stylesheets.addAll(JFoenixResources.load("/css/jfoenix-fonts.css").toExternalForm(),
                                JFoenixResources.load("/css/jfoenix-design.css").toExternalForm(),
                                Main.class.getResource("/css/styling.css").toExternalForm());
                        primaryStage.setScene(scene);
                        utils.setWindow(primaryStage);
                        primaryStage.show();
                    default:
                        break;
                }
            }
        } else {*/
        Thread thread = new Thread(() -> {
            try {
                SVGGlyphLoader.loadGlyphsFont(Main.class.getResourceAsStream("/fonts/icomoon.svg"),
                        "icomoon.svg");
            } catch (IOException ioExc) {
                logger.fatal("Cannot Load SVG Glyphs", ioExc);
                System.exit(-1);
            }
        });
        thread.start();
        Flow flow = new Flow(MainController.class);
        DefaultFlowContainer container = new DefaultFlowContainer();
        flowContext = new ViewFlowContext();
        flowContext.register("Stage", primaryStage);
        flow.createHandler(flowContext).start(container);
        JFXDecorator decorator = new JFXDecorator(primaryStage, container.getView());
        decorator.setCustomMaximize(true);
        decorator.setGraphic(new SVGGlyph(""));
        primaryStage.setTitle("Data Processing");
        double width = 800;
        double height = 600;
        try {
            Rectangle2D bounds = Screen.getScreens().get(0).getBounds();
            width = bounds.getWidth() / 2.5;
            height = bounds.getHeight() / 1.35;
        } catch (Exception e) {
            logger.fatal("Index out of Bounds", e);
            System.exit(-1);
        }
        Scene scene = new Scene(decorator, width, height);
        final ObservableList<String> stylesheets = scene.getStylesheets();
        stylesheets.addAll(JFoenixResources.load("/css/jfoenix-fonts.css").toExternalForm(),
                JFoenixResources.load("/css/jfoenix-design.css").toExternalForm(),
                Main.class.getResource("/css/styling.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            thread.interrupt();
            server.closeConnection();
            System.exit(0);
        });
        utils.setWindow(primaryStage);
        primaryStage.show();
    }
    //}
}
