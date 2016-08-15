package org.subshare.updater.gui.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public abstract class ConsolePane extends GridPane {
	private static final Logger logger = LoggerFactory.getLogger(ConsolePane.class);

	private BooleanProperty done = new SimpleBooleanProperty(this, "done");

	@FXML
	private Label headerLabel;

	@FXML
	private TextArea consoleTextArea;

	@FXML
	private Button okButton;

	private static final AtomicInteger nextId = new AtomicInteger();
	private final int id = nextId.getAndIncrement();

	private final Object instanceMutex = this;
	private StringBuilder buffer = new StringBuilder();

	private final Timer updateUiTimer = new Timer("ConsolePane[" + id + "].updateTimer", true);

	@SuppressWarnings("unused")
	private final Object finalizer = new Object() {
		@Override
		protected void finalize() throws Throwable {
			updateUiTimer.cancel();
		}
	};

	public ConsolePane() {
		loadFxml();
		okButton.disableProperty().bind(done.not());

		final TimerTask updateUiTimerTask = new TimerTask() {
			@Override
			public void run() {
				try {
					updateUi();
				} catch (Exception x) {
					logger.error("updateTimerTask.run: " + x, x);
				}
			}
		};

		updateUiTimer.schedule(updateUiTimerTask, 0, 500);
	}

	public void setHeaderText(boolean error, String text) {
		final String iconName;
		if (error)
			iconName = "ERROR_24x24.png";
		else
			iconName = "INFO_24x24.png";

		headerLabel.setGraphic(new ImageView(new Image(ConsolePane.class.getResource(iconName).toString())));
		headerLabel.setText(text);
	}

	private void loadFxml() {
		final FXMLLoader fxmlLoader = new FXMLLoader(
				ConsolePane.class.getResource(ConsolePane.class.getSimpleName() + ".fxml"), //$NON-NLS-1$
				Messages.RESOURCE_BUNDLE);

		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);

		try {
			fxmlLoader.load();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isDone() {
		return done.get();
	}
	public void setDone(boolean done) {
		this.done.set(done);
	}
	public BooleanProperty doneProperty() {
		return done;
	}

	@FXML
	protected abstract void okButtonClicked(final ActionEvent event);

	public void appendError(final Throwable error) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		error.printStackTrace(pw); pw.flush(); pw.close();
		final String errorStackTrace = sw.toString();

		println("\n\n\n*** ERROR ***");
		println(errorStackTrace);
	}

	public void print(final String message) {
		synchronized (instanceMutex) {
			buffer.append(message);
		}
	}

	private void updateUi() {
		final String bufferStr;
		synchronized (instanceMutex) {
			bufferStr = buffer.toString();
			buffer = new StringBuilder();
		}
		if (bufferStr.length() > 0) {
			Platform.runLater(() -> consoleTextArea.appendText(bufferStr));
		}
	}

	public void println(final String message) {
		print(message + '\n');
	}
}
