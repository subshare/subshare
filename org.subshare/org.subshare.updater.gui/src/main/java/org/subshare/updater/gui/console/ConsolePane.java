package org.subshare.updater.gui.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

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
	private BooleanProperty done = new SimpleBooleanProperty(this, "done");

	@FXML
	private Label headerLabel;

	@FXML
	private TextArea consoleTextArea;

	@FXML
	private Button okButton;

	public ConsolePane() {
		loadFxml();
		okButton.disableProperty().bind(done.not());
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
		if (Platform.isFxApplicationThread())
			_appendError(error);
		else
			Platform.runLater(() -> _appendError(error));
	}

	private void _appendError(final Throwable error) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PrintWriter pw = new PrintWriter(out);
		error.printStackTrace(pw); pw.flush(); pw.close();

		String errorStackTrace = new String(out.toByteArray(), StandardCharsets.UTF_8);
		consoleTextArea.appendText(errorStackTrace);
	}

	public void print(final String message) {
		if (Platform.isFxApplicationThread())
			_print(message);
		else
			Platform.runLater(() -> _print(message));
	}

	private void _print(final String message) {
		consoleTextArea.appendText(message);
	}

	public void println(final String message) {
		print(message + '\n');
	}
}
