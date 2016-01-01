package org.subshare.gui.control;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.text.ParseException;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.TimePeriod;

public class TimePeriodTextField extends TextField {

	private static final Logger logger = LoggerFactory.getLogger(TimePeriodTextField.class);

	private final ObjectProperty<TimePeriod> timePeriod = new SimpleObjectProperty<TimePeriod>(this, "timePeriod") {
		@Override
		public void set(final TimePeriod timePeriod) {
			if (equal(get(), timePeriod)) {
				logger.debug("timePeriod.set: newValue='{}' equals old value => ignore!", timePeriod);
				return;
			}

			logger.debug("timePeriod.set: newValue='{}'", timePeriod);
			super.set(timePeriod);

			TimePeriod shownTimePeriod;
			try {
				shownTimePeriod = new TimePeriod(trim(getText()));
			} catch (ParseException x) {
				shownTimePeriod = null;
			}

			if (!equal(timePeriod, shownTimePeriod))
				updateText();
		}
	};

	public TimePeriodTextField() {
		textProperty().addListener((InvalidationListener) observable -> tryParse(true));
		focusedProperty().addListener((InvalidationListener) observable -> {
			if (! focusedProperty().get())
				onFocusLost();
		});
	}

	private void tryParse(boolean silent) {
		final String text = trim(getText());
		if (logger.isDebugEnabled())
			logger.debug("tryParse: silent={} text='{}'", silent, text);

		if (isEmpty(text))
			setTimePeriod(null);
		else {
			final TimePeriod timePeriod;
			try {
				timePeriod = new TimePeriod(text);
			} catch (ParseException x) {
				if (! silent)
					showErrorMessage("Invalid input!", x.getLocalizedMessage());

				return;
			}
			setTimePeriod(timePeriod);
		}
	}

	private void showErrorMessage(final String headerText, final String detailText) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setHeaderText(headerText);

//		alert.setContentText(detailText);
		// The above does not adjust the dialog size :-( Using a Text node instead works better.

		final Text contentText = new Text(detailText);
		final HBox contentTextContainer = new HBox();
		contentTextContainer.getChildren().add(contentText);

		GridPane.setMargin(contentText, new Insets(8));
		alert.getDialogPane().setContent(contentTextContainer);

		alert.showAndWait();
	}

	private void onFocusLost() {
		tryParse(false);
		updateText();
	}

	private void updateText() {
		final TimePeriod timePeriod = getTimePeriod();

		if (logger.isDebugEnabled())
			logger.debug("updateText: oldText='{}' timePeriod='{}'", getText(), timePeriod);

		setText(timePeriod == null ? null : timePeriod.toString());
	}

	public ObjectProperty<TimePeriod> timePeriodProperty() {
		return timePeriod;
	}

	public TimePeriod getTimePeriod() {
		return timePeriod.get();
	}

	public void setTimePeriod(final TimePeriod timePeriod) {
		this.timePeriod.set(timePeriod);
	}

}
