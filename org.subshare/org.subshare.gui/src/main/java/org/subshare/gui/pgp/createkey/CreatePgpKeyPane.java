package org.subshare.gui.pgp.createkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.CreatePgpKeyParam.Algorithm;
import org.subshare.core.pgp.PgpUserId;
import org.subshare.gui.util.CharArrayStringConverter;
import org.subshare.gui.util.PlatformUtil;

public abstract class CreatePgpKeyPane extends GridPane {

	private final CreatePgpKeyParam createPgpKeyParam;

	@FXML
	private TableView<FxPgpUserId> emailsTableView;
	@FXML
	private TableColumn<FxPgpUserId, String> nameTableColumn;
	@FXML
	private TableColumn<FxPgpUserId, String> emailTableColumn;

	@FXML
	private PasswordField passwordField;
	private final JavaBeanObjectProperty<char[]> passwordProperty;

	@FXML
	private CheckBox noPasswordCheckBox;

	@FXML
	private PasswordField passwordField2;

	@FXML
	private Spinner<Integer> validityNumberSpinner;

	@FXML
	private ComboBox<TimeUnit> validityTimeUnitComboBox;

	@FXML
	private ComboBox<CreatePgpKeyParam.Algorithm> algorithmComboBox;
	private final JavaBeanObjectProperty<CreatePgpKeyParam.Algorithm> algorithmProperty;

	@FXML
	private ComboBox<Integer> strengthComboBox;

	@FXML
	private Button okButton;

	private boolean ignoreUpdateValidity;

	private final InvalidationListener updateDisabledInvalidationListener = (InvalidationListener) observable -> updateDisabled();

	private final PropertyChangeListener pgpUserIdsPropertyChangeListener = event -> {
		Platform.runLater(() -> updateEmailsTableViewItems() );
	};

	private final PropertyChangeListener validitySecondsPropertyChangeListener = event -> {
		PlatformUtil.runAndWait(() -> updateValidityNumberSpinner() );
	};

	private final PropertyChangeListener strengthPropertyChangeListener = event -> {
		Platform.runLater(() -> updateStrengthComboBoxItems() );
	};

	public CreatePgpKeyPane(final CreatePgpKeyParam createPgpKeyParam) {
		loadDynamicComponentFxml(CreatePgpKeyPane.class, this);
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam); //$NON-NLS-1$

		for (PgpUserId pgpUserId : createPgpKeyParam.getUserIds())
			pgpUserId.addPropertyChangeListener(pgpUserIdsPropertyChangeListener);

		emailsTableView.setItems(FXCollections.observableList(cast(createPgpKeyParam.getUserIds())));
		emailsTableView.getItems().addListener(updateDisabledInvalidationListener);
		nameTableColumn.setCellFactory(cast(TextFieldTableCell.forTableColumn()));
		emailTableColumn.setCellFactory(cast(TextFieldTableCell.forTableColumn()));
		updateEmailsTableViewItems();

		passwordProperty = createPasswordProperty();
		Bindings.bindBidirectional(
				passwordField.textProperty(), passwordProperty, new CharArrayStringConverter());
		passwordProperty.addListener(updateDisabledInvalidationListener);
		passwordField2.textProperty().addListener(updateDisabledInvalidationListener);

		noPasswordCheckBox.selectedProperty().addListener(updateDisabledInvalidationListener);
		passwordField.disableProperty().bind(noPasswordCheckBox.selectedProperty());
		passwordField2.disableProperty().bind(noPasswordCheckBox.selectedProperty());

		validityNumberSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE));
		validityNumberSpinner.valueProperty().addListener((InvalidationListener) observable -> {
			updateValiditySeconds();
			updateDisabled();
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

		algorithmProperty = createAlgorithmProperty();
		algorithmProperty.addListener((InvalidationListener) observable -> updateStrengthComboBoxItems());
		algorithmComboBox.setItems(FXCollections.observableArrayList(CreatePgpKeyParam.Algorithm.values()));
		algorithmComboBox.setConverter(algorithmStringConverter);
		algorithmComboBox.valueProperty().bindBidirectional(algorithmProperty);

		updateStrengthComboBoxItems();
		strengthComboBox.valueProperty().addListener((InvalidationListener) observable -> createPgpKeyParam.setStrength(strengthComboBox.getValue()));

		createPgpKeyParam.addPropertyChangeListener(CreatePgpKeyParam.PropertyEnum.validitySeconds, validitySecondsPropertyChangeListener);
		createPgpKeyParam.addPropertyChangeListener(CreatePgpKeyParam.PropertyEnum.strength, strengthPropertyChangeListener);
		updateValidityNumberSpinner();

		updateDisabled();
	}

	private void updateDisabled() {
		boolean disable = validityNumberSpinner.getValue() == null;

		if (! disable) {
			int nonEmptyPgpUserIdCount = 0;
			for (PgpUserId pgpUserId : createPgpKeyParam.getUserIds()) {
				if (! pgpUserId.isEmpty())
					++nonEmptyPgpUserIdCount;
			}

			disable |= nonEmptyPgpUserIdCount == 0;
		}

		if (! disable) {
			if (! noPasswordCheckBox.isSelected()) {
				final char[] p1 = createPgpKeyParam.getPassphrase();
				final char[] p2 = passwordField2.getText().toCharArray();
				disable |= ! Arrays.equals(p1, p2);

				if (! disable && p1.length == 0)
					disable = true;
			}
		}
		okButton.setDisable(disable);
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

	private void updateEmailsTableViewItems() {
		ObservableList<FxPgpUserId> items = emailsTableView.getItems();
		List<FxPgpUserId> itemsToRemove = new ArrayList<FxPgpUserId>();

		for (Iterator<FxPgpUserId> it = items.iterator(); it.hasNext(); ) {
			FxPgpUserId fxPgpUserId = it.next();
			if (it.hasNext() && fxPgpUserId.isEmpty()) {
				fxPgpUserId.removePropertyChangeListener(pgpUserIdsPropertyChangeListener);
				itemsToRemove.add(fxPgpUserId);
			}
		}
		items.removeAll(itemsToRemove);

		if (items.isEmpty() || ! items.get(items.size() - 1).isEmpty()) {
			FxPgpUserId fxPgpUserId = new FxPgpUserId();
			fxPgpUserId.addPropertyChangeListener(pgpUserIdsPropertyChangeListener);
			items.add(fxPgpUserId);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		// is this necessary? isn't the createPgpKeyParam gc-ed, anyway? if this is necessary, we must use a WeakPropertyChangeListener!
//		for (PgpUserId pgpUserId : createPgpKeyParam.getUserIds())
//			pgpUserId.removePropertyChangeListener(pgpUserIdsPropertyChangeListener);
//
//		createPgpKeyParam.removePropertyChangeListener(CreatePgpKeyParam.PropertyEnum.validitySeconds, validitySecondsPropertyChangeListener);
//		createPgpKeyParam.removePropertyChangeListener(CreatePgpKeyParam.PropertyEnum.strength, strengthPropertyChangeListener);
		super.finalize();
	}

	private StringConverter<CreatePgpKeyParam.Algorithm> algorithmStringConverter = new StringConverter<CreatePgpKeyParam.Algorithm>() {
		@Override
		public String toString(Algorithm algorithm) {
			return Messages.getString(String.format("CreatePgpKeyPane.Algorithm[%s]", algorithm)); //$NON-NLS-1$
		}
		@Override
		public Algorithm fromString(String string) { throw new UnsupportedOperationException(); }
	};

	private StringConverter<TimeUnit> timeUnitStringConverter = new StringConverter<TimeUnit>() {
		@Override
		public String toString(TimeUnit timeUnit) {
			return Messages.getString(String.format("CreatePgpKeyPane.TimeUnit[%s]", timeUnit)); //$NON-NLS-1$
		}
		@Override
		public TimeUnit fromString(String string) { throw new UnsupportedOperationException(); }
	};

	@SuppressWarnings("unchecked")
	private JavaBeanObjectProperty<char[]> createPasswordProperty() {
		try {
			return JavaBeanObjectPropertyBuilder.create()
					.bean(createPgpKeyParam)
					.name(CreatePgpKeyParam.PropertyEnum.passphrase.name())
					.build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private void updateStrengthComboBoxItems() {
		final List<Integer> supportedStrengths = createPgpKeyParam.getAlgorithm().getSupportedStrengths();
		final ObservableList<Integer> items = strengthComboBox.getItems();
		items.retainAll(supportedStrengths);
		int index = -1;
		for (final Integer supportedStrength : supportedStrengths) {
			if (++index >= items.size() || !supportedStrength.equals(items.get(index)))
				items.add(index, supportedStrength);
		}
		strengthComboBox.setValue(createPgpKeyParam.getStrength());
	}

	@SuppressWarnings("unchecked")
	private JavaBeanObjectProperty<CreatePgpKeyParam.Algorithm> createAlgorithmProperty() {
		try {
			return JavaBeanObjectPropertyBuilder.create()
					.bean(createPgpKeyParam)
					.name(CreatePgpKeyParam.PropertyEnum.algorithm.name())
					.build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		passwordField.requestFocus();
	}

	@FXML
	protected abstract void okButtonClicked(final ActionEvent event);

	@FXML
	protected abstract void cancelButtonClicked(final ActionEvent event);

	public CreatePgpKeyParam getCreatePgpKeyParam() {
		return createPgpKeyParam;
	}
}
