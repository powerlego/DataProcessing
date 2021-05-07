package org.dataprocessing.gui.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

/**
 * The model for Nav Processing
 *
 * @author Nicholas Curl
 */
public class NavModel {

    /**
     * The instance of this class
     */
    private static final NavModel        instance = new NavModel();
    /**
     * The string property of the path of the data to process
     */
    private final        StringProperty  filePath1;
    /**
     * The string property of the path to store the mapped data
     */
    private final        StringProperty  filePath2;
    /**
     * The boolean property to specify if the gui is locked
     */
    private final        BooleanProperty locked;
    /**
     * The string property of the progress bar's text
     */
    private final        StringProperty  progressBarText;
    /**
     * The boolean property to specify if the gui is not locked
     */
    private final        BooleanProperty notLocked;
    /**
     * The current task being executed
     */
    private              Task<Void>      progress;

    /**
     * The constructor for this class
     */
    public NavModel() {
        filePath1 = new SimpleStringProperty();
        filePath2 = new SimpleStringProperty();
        locked = new SimpleBooleanProperty();
        progressBarText = new SimpleStringProperty();
        notLocked = new SimpleBooleanProperty();
        setLocked(false);
    }

    /**
     * Gets this class's instance
     *
     * @return This class's instance
     */
    public static NavModel getInstance() {
        return instance;
    }

    /**
     * Gets the string value of the string property
     *
     * @return The string value
     */
    public String getFilePath1() {
        return filePath1.get();
    }

    /**
     * Sets the string value of the string property
     *
     * @param filePath1 The value to set
     */
    public void setFilePath1(String filePath1) {
        this.filePath1.set(filePath1);
    }

    /**
     * Gets the string value of the string property
     *
     * @return The string value
     */
    public String getFilePath2() {
        return filePath2.get();
    }

    /**
     * Sets the string value of the string property
     *
     * @param filePath2 The value to set
     */
    public void setFilePath2(String filePath2) {
        this.filePath2.set(filePath2);
    }

    /**
     * Gets the task being executed
     *
     * @return The task being executed
     */
    public Task<Void> getProgress() {
        return progress;
    }

    /**
     * Sets the task being executed
     *
     * @param progress The task being executed
     */
    public void setProgress(Task<Void> progress) {
        this.progress = progress;
    }

    /**
     * Should the gui be locked
     *
     * @return The boolean value
     */
    public boolean isLocked() {
        return locked.get();
    }

    /**
     * Specifies if the gui is locked or not
     *
     * @param locked The boolean specifying if the gui should be locked or not
     */
    public void setLocked(boolean locked) {
        this.locked.set(locked);
        this.notLocked.set(!locked);
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
     * Gets the string property of the progress bar's text
     *
     * @return The string property
     */
    public StringProperty progressBarTextProperty() {
        return progressBarText;
    }

    /**
     * Sets the string value of the progress bar's text
     *
     * @param progressBarText The value to set
     */
    public void setProgressBarText(String progressBarText) {
        this.progressBarText.set(progressBarText);
    }
}
