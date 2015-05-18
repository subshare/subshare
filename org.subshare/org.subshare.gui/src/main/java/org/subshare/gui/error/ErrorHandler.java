package org.subshare.gui.error;

import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import org.subshare.gui.util.PlatformUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {

	private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

	private final String headerText;
	private final String contentText;
	private final Throwable error;
	private Alert alert;
	private final Map<Boolean, double[]> expanded2WidthHeight = new HashMap<>();

	private static final AtomicInteger serial = new AtomicInteger();

	private final Timer deferredWidthHeightHandlingTimer = new Timer("deferredWidthHeightHandlingTimer-" + serial.getAndIncrement(), true);
	private TimerTask deferredWidthHeightHandlingTimerTask;

	// ^(?:[^\d+\-\*/ \.][^+\-\*/ \.]*\.)*[^+\-\*/ \.]*$
	private static final Pattern fullyQualifiedClassNamePattern = Pattern.compile("^(?:[^\\d+\\-\\*/ \\.][^+\\-\\*/ \\.]*\\.)*[^+\\-\\*/ \\.]*$");

	private static final UncaughtExceptionHandler uncaughtExceptionHandler = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			handleError(e);
		}
	};

	public static UncaughtExceptionHandler getUncaughtExceptionHandler() {
		return uncaughtExceptionHandler;
	}

	private ErrorHandler(final String headerText, final String contentText, final Throwable error) {
		this.headerText = headerText;
		this.contentText = contentText;
		this.error = error;
	}

	public Throwable getError() {
		return error;
	}

	public static void handleError(final String headerText, final String contentText, final Throwable error) {
		new ErrorHandler(headerText, contentText, error).doHandleError();
	}

	public static void handleError(final Throwable error) {
		new ErrorHandler(null, null, error).doHandleError();
	}

	protected String getHeaderText() {
		if (headerText == null)
			return "Sorry! An error occurred!";

		return headerText;
	}

	protected String getContentText() {
		if (contentText == null) {
			if (error != null) {
				Throwable t = error;
				while (t != null) {
					final String localizedMessage = t.getLocalizedMessage();
					if (!isEmpty(localizedMessage) && !isFullyQualifiedClassName(localizedMessage))
						return localizedMessage;

					t = t.getCause();
				}

				t = error;
				while (t != null) {
					final String localizedMessage = t.getLocalizedMessage();
					if (!isEmpty(localizedMessage))
						return localizedMessage;

					t = t.getCause();
				}
			}
		}
		return contentText;
	}


	private boolean isFullyQualifiedClassName(final String string) {
		if (isEmpty(string))
			return false;

		return fullyQualifiedClassNamePattern.matcher(string).matches();
	}

	protected void doHandleError() {
		logger.error("doHandleError: " + error, error);
		PlatformUtil.runAndWait(new Runnable() {
			@Override
			public void run() {
				_doHandleError();
			}
		});
	}

	protected void _doHandleError() {
		buildAlert();
		alert.showAndWait();
	}

	protected void buildAlert() {
		alert = new Alert(AlertType.ERROR);
		alert.setHeaderText(getHeaderText());
		alert.setContentText(getContentText());

		alert.getDialogPane().setMinSize(500, 250);

		if (error != null) {
			Label label = new Label("The exception stacktrace was:");

			TextArea textArea = new TextArea(getStackTrace(error));
			textArea.setEditable(false);
			textArea.setWrapText(true);

			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setVgrow(textArea, Priority.ALWAYS);
			GridPane.setHgrow(textArea, Priority.ALWAYS);

			GridPane expContent = new GridPane();
			expContent.setMaxWidth(Double.MAX_VALUE);
			expContent.add(label, 0, 0);
			expContent.add(textArea, 0, 1);

			// Set expandable Exception into the dialog pane.
			alert.getDialogPane().setExpandableContent(expContent);

			setUpExpandCollapseWorkaround();
		}
	}

	private void setUpExpandCollapseWorkaround() {
		alert.widthProperty().addListener(widthHeightPropertyChangeListener);
		alert.heightProperty().addListener(widthHeightPropertyChangeListener);

		alert.getDialogPane().expandedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
				final boolean expanded = alert.getDialogPane().isExpanded();
				final double[] widthHeight = expanded2WidthHeight.get(expanded);
				final double width = widthHeight == null ? Double.NaN : widthHeight[0];
				final double height = widthHeight == null ? Double.NaN : widthHeight[1];

				logger.debug("alert.expandedProperty.changed: expanded={} width={} height={} >>>", expanded, width, height);

				alert.setWidth(Double.isNaN(width) ? 800 : width);
				alert.setHeight(Double.isNaN(height) ? 500 : height);

				logger.debug("alert.expandedProperty.changed: expanded={} width={} height={} <<<", expanded, width, height);
			}
		});
	}

	private static String getStackTrace(final Throwable error) {
		final StringWriter w = new StringWriter();
		final PrintWriter pw = new PrintWriter(w);
		error.printStackTrace(pw);
		return w.toString();
	}

	private final ChangeListener<Number> widthHeightPropertyChangeListener = (ChangeListener<Number>) (observable, oldValue, newValue) -> {
		if (deferredWidthHeightHandlingTimerTask != null) {
			deferredWidthHeightHandlingTimerTask.cancel();
			deferredWidthHeightHandlingTimerTask = null;
		}

		deferredWidthHeightHandlingTimerTask = new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						deferredWidthHeightHandlingTimerTask = null;
						final boolean expanded = alert.getDialogPane().isExpanded();
						final double[] widthHeight = new double[] { alert.getWidth(), alert.getHeight() };
						expanded2WidthHeight.put(expanded, widthHeight);
						logger.debug("widthHeightPropertyChangeListener: expanded={} width={} height={}",
								expanded, widthHeight[0], widthHeight[1]);
					}
				});
			}
		};

		deferredWidthHeightHandlingTimer.schedule(deferredWidthHeightHandlingTimerTask, 500L);
	};
}
