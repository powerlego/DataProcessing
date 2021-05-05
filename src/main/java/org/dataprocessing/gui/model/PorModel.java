package org.dataprocessing.gui.model;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The model for POR Processing
 *
 * @author Nicholas Curl
 */
public class PorModel {
    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger();
    /**
     * The instance of this class
     */
    private static final PorModel instance = new PorModel();
    /**
     * The string property of the progress indicator's text
     */
    private final StringProperty progLabelText;
    /**
     * The boolean property to specify if the gui is locked
     */
    private final BooleanProperty locked;
    /**
     * The list of tasks being executed
     */
    private final List<Task<?>> tasks;
    /**
     * The string property of the path to store the mapped data
     */
    private final StringProperty filePath;
    /**
     * The property of the total progress
     */
    private DoubleBinding totalProgress;
    /**
     * Is the gui cancelable
     */
    private boolean cancelable;

    /**
     * Is the processing canceled
     */
    private boolean canceled;

    /**
     * The constructor of this class
     */
    public PorModel() {
        progLabelText = new SimpleStringProperty();
        locked = new SimpleBooleanProperty();
        tasks = new ArrayList<>();
        filePath = new SimpleStringProperty();
        canceled = false;
    }

    /**
     * Gets the instance of this class
     *
     * @return The instance of this class
     */
    public static PorModel getInstance() {
        return instance;
    }

    /**
     * Add a task to the list of tasks being executed
     *
     * @param task The task to add
     */
    public void addTask(Task<?> task) {
        tasks.add(task);
    }

    /**
     * Adds a list of tasks to the list of tasks being executed
     *
     * @param tasks The list of tasks to add
     */
    public void addTasks(Collection<? extends Task<?>> tasks) {
        this.tasks.addAll(tasks);
    }

    /**
     * Gets the string value of the file path
     *
     * @return The string value
     */
    public String getFilePath() {
        return filePath.get();
    }

    /**
     * Sets the string value of the file path
     *
     * @param filePath The string value to set
     */
    public void setFilePath(String filePath) {
        this.filePath.set(filePath);
    }

    /**
     * Get the list of tasks being executed
     *
     * @return The list of tasks being executed
     */
    public List<Task<?>> getTasks() {
        return tasks;
    }

    /**
     * Is the gui able to cancel
     *
     * @return The boolean value set
     */
    public boolean isCancelable() {
        return cancelable;
    }

    /**
     * Sets if the gui is able to cancel
     *
     * @param cancelable True if the gui is able to cancel, false if not
     */
    public void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }

    /**
     * Is the processing canceled
     *
     * @return True if it is canceled, false otherwise
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Sets the processing to be canceled or not
     *
     * @param canceled The boolean value
     */
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Should the gui be locked
     *
     * @return True if it is locked, false otherwise
     */
    public boolean isLocked() {
        return locked.get();
    }

    /**
     * Sets if the gui should be locked
     *
     * @param locked True if the gui should be locked, false if not
     */
    public void setLocked(boolean locked) {
        this.locked.set(locked);
    }

    /**
     * Gets the locked boolean property
     *
     * @return The boolean property
     */
    public BooleanProperty lockedProperty() {
        return locked;
    }

    /**
     * Gets the progress indicator's text property
     *
     * @return The text property
     */
    public StringProperty progLabelTextProperty() {
        return progLabelText;
    }

    /**
     * Sets the string value of the progress label
     *
     * @param text The string value to set
     */
    public void setProgLabelText(String text) {
        progLabelText.set(text);
    }

    /**
     * Sets the total progress property of the gui
     *
     * @param totalProgress The total progress property
     */
    public void setTotalProgressProperty(DoubleBinding totalProgress) {
        this.totalProgress = totalProgress;
    }

    /**
     * Gets the total progress property of the gui
     *
     * @return The total progress property
     */
    public DoubleBinding totalProgressProperty() {
        return totalProgress;
    }
}
