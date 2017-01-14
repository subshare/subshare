package org.subshare.gui.pgp.createkey.validity;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.beans.PropertyChangeListener;
import java.util.function.UnaryOperator;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.pgp.createkey.TimeUnit;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.util.StringConverter;

public class ValidityPane extends WizardPageContentGridPane {

	private final CreatePgpKeyParam createPgpKeyParam;

	@FXML
	private Spinner<Integer> validityNumberSpinner;

	@FXML
	private ComboBox<TimeUnit> validityTimeUnitComboBox;

	private boolean ignoreUpdateValidity;

	private final PropertyChangeListener validitySecondsPropertyChangeListener = event -> {
		runAndWait(() -> updateValidityNumberSpinner() );
	};

	private StringConverter<TimeUnit> timeUnitStringConverter = new StringConverter<TimeUnit>() {
		@Override
		public String toString(TimeUnit timeUnit) {
			return Messages.getString(String.format("ValidityPane.TimeUnit[%s]", timeUnit)); //$NON-NLS-1$
		}
		@Override
		public TimeUnit fromString(String string) { throw new UnsupportedOperationException(); }
	};

	public ValidityPane(final CreatePgpKeyParam createPgpKeyParam) {
		this.createPgpKeyParam = assertNotNull(createPgpKeyParam, "createPgpKeyParam"); //$NON-NLS-1$
		loadDynamicComponentFxml(ValidityPane.class, this);

		validityNumberSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE));
		validityNumberSpinner.valueProperty().addListener((InvalidationListener) observable -> {
			updateValiditySeconds();
			updateComplete();
		});
		validityNumberSpinner.getEditor().setTextFormatter(new TextFormatter<String>(new UnaryOperator<Change>() {
			@Override
			public Change apply(Change change) {
				final String text = change.getControlNewText();
				// We cannot accept an empty String, because the IntegerValueFactory runs into an NPE, then :-(
				try {
					Integer.parseInt(text);
				} catch (NumberFormatException x) {
					return null;
				}
				return change;
			}
		}));

		validityTimeUnitComboBox.setItems(FXCollections.observableArrayList(TimeUnit.YEAR, TimeUnit.MONTH, TimeUnit.DAY));
		validityTimeUnitComboBox.setConverter(timeUnitStringConverter);
		validityTimeUnitComboBox.getSelectionModel().clearAndSelect(0);
		validityTimeUnitComboBox.valueProperty().addListener((InvalidationListener) observable -> updateValiditySeconds());

		createPgpKeyParam.addPropertyChangeListener(CreatePgpKeyParam.PropertyEnum.validitySeconds, validitySecondsPropertyChangeListener);
		updateValidityNumberSpinner();

		updateComplete();
	}

	private void updateValidityNumberSpinner() {
		if (ignoreUpdateValidity)
			return;

		ignoreUpdateValidity = true;
		try {
			final long validitySeconds = createPgpKeyParam.getValiditySeconds();
			TimeUnit timeUnit = validityTimeUnitComboBox.getValue();
			long remainder;
			do {
				remainder = validitySeconds % timeUnit.getSeconds();
				if (remainder != 0) {
					if (timeUnit.ordinal() + 1 < TimeUnit.values().length)
						timeUnit = TimeUnit.values()[timeUnit.ordinal() + 1];
					else
						break;
				}
			} while (remainder != 0);

			validityTimeUnitComboBox.setValue(timeUnit);

			long validityNumber = validitySeconds / timeUnit.getSeconds();
			if (remainder != 0)
				++validityNumber;

			validityNumberSpinner.getValueFactory().setValue((int) validityNumber);

			if (remainder != 0)
				createPgpKeyParam.setValiditySeconds(validityNumber * timeUnit.getSeconds());

		} finally {
			ignoreUpdateValidity = false;
		}
	}

	private void updateValiditySeconds() {
		if (ignoreUpdateValidity)
			return;

		ignoreUpdateValidity = true;
		try {

			final Integer validityNumber = validityNumberSpinner.getValue();
			if (validityNumber == null)
				return;

			long seconds = validityNumber.longValue() * validityTimeUnitComboBox.getValue().getSeconds();
			createPgpKeyParam.setValiditySeconds(seconds);
		} finally {
			ignoreUpdateValidity = false;
		}
	}

	@Override
	protected boolean isComplete() {
		boolean complete = validityNumberSpinner.getValue() != null;
		return complete;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		validityNumberSpinner.requestFocus();
	}
}
