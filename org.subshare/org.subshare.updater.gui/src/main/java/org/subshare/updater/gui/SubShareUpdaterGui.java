package org.subshare.updater.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.updater.SubShareUpdater;
import org.subshare.updater.gui.console.ConsolePane;
import org.subshare.updater.gui.console.ConsolePrintStream;

import co.codewizards.cloudstore.core.appid.AppIdRegistry;

public class SubShareUpdaterGui extends Application {

	private static final Logger logger = LoggerFactory.getLogger(SubShareUpdaterGui.class);

	private ConsolePane consolePane;

	@Override
	public void start(final Stage primaryStage) {
		consolePane = new ConsolePane() {
			@Override
			protected void okButtonClicked(ActionEvent event) {
				Platform.exit();
			}
		};
		consolePane.setHeaderText(false, String.format("Updating %s...", AppIdRegistry.getInstance().getAppIdOrFail().getName()));
		consolePane.println(String.format("Updating %s. This can take a while. Please be patient and wait...", AppIdRegistry.getInstance().getAppIdOrFail().getName()));

		ConsolePrintStream cps = new ConsolePrintStream(consolePane);
		System.setOut(cps);
		System.setErr(cps);

		primaryStage.setScene(new Scene(consolePane));
		primaryStage.show();
		primaryStage.setTitle("Subshare Updater");

		final String[] args = getParameters().getRaw().toArray(new String[0]);
		new UpdaterThread(args).start();
	}

	public static void main(final String[] args) {
		launch(args);
	}

	private class UpdaterThread extends Thread {
		private final String[] args;

		public UpdaterThread(final String[] args) {
			this.args = args;
			setName("UpdaterThread");
		}

		@Override
		public void run() {
			try {
				final int resultCode = new SubShareUpdater(args).execute();
				if (resultCode != 0) // should not happen, because an error should cause an exception to be thrown!
					throw new IllegalStateException("error-code: " + resultCode);

				Platform.runLater(() -> consolePane.setHeaderText(false, "Update successful!"));
			} catch (final Exception e) {
				handleError(e);
			} finally {
				Platform.runLater(() -> consolePane.setDone(true));
			}
		}
	}

	private void handleError(final Throwable error) {
		if (Platform.isFxApplicationThread())
			_handleError(error);
		else
			Platform.runLater(() -> _handleError(error));
	}

	private void _handleError(final Throwable error) {
		if (error == null) {
			logger.error("_handleError: error == null", new RuntimeException("StackTrace"));
			return;
		}

		logger.error("_handleError: " + error, error);

		consolePane.setHeaderText(true, "Update failed!");
		consolePane.appendError(error);
	}

}
