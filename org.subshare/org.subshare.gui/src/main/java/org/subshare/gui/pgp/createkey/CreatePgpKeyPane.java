package org.subshare.gui.pgp.createkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeListener;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.CreatePgpKeyParam.Algorithm;
import org.subshare.gui.util.CharArrayStringConverter;

public abstract class CreatePgpKeyPane extends GridPane {

	private final CreatePgpKeyParam createPgpKeyParam;

	@FXML
	private TableView<UserId> emailsTableView;

	@FXML
	private PasswordField passwordField;
	private final JavaBeanObjectProperty<char[]> passwordProperty;

	@FXML
	private PasswordField passwordField2;

	@FXML
	private Spinner<Number> validityNumberSpinner;
//	private final JavaBeanIntegerProperty validitySecondsProperty;

	@FXML
	private ComboBox<TimeUnit> validityTimeUnitComboBox;

	@FXML
	private ComboBox<CreatePgpKeyParam.Algorithm> algorithmComboBox;
	private final JavaBeanObjectProperty<CreatePgpKeyParam.Algorithm> algorithmProperty;

	@FXML
	private ComboBox<Integer> strengthComboBox;

	private final PropertyChangeListener validitySecondsPropertyChangeListener = event -> {
		Platform.runLater(() -> updateStrengthComboBoxItems() );
	};

	private final PropertyChangeListener strengthPropertyChangeListener = event -> {
		Platform.runLater(() -> updateStrengthComboBoxItems() );
	};

	public CreatePgpKeyPane(final CreatePgpKeyParam createPgpKeyParam) {
		loadDynamicComponentFxml(CreatePgpKeyPane.class, this);
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam);

		passwordProperty = createPasswordProperty();
		Bindings.bindBidirectional(
				passwordField.textProperty(), passwordProperty, new CharArrayStringConverter());

		algorithmProperty = createAlgorithmProperty();
		algorithmProperty.addListener((InvalidationListener) observable -> updateStrengthComboBoxItems());
		algorithmComboBox.setItems(FXCollections.observableArrayList(CreatePgpKeyParam.Algorithm.values()));
		algorithmComboBox.setConverter(algorithmStringConverter);
		algorithmComboBox.valueProperty().bindBidirectional(algorithmProperty);

		updateStrengthComboBoxItems();
		strengthComboBox.valueProperty().addListener((InvalidationListener) observable -> createPgpKeyParam.setStrength(strengthComboBox.getValue()));

		createPgpKeyParam.addPropertyChangeListener(CreatePgpKeyParam.PropertyEnum.validitySeconds, validitySecondsPropertyChangeListener);
		createPgpKeyParam.addPropertyChangeListener(CreatePgpKeyParam.PropertyEnum.strength, strengthPropertyChangeListener);
	}

	@Override
	protected void finalize() throws Throwable {
		createPgpKeyParam.removePropertyChangeListener(CreatePgpKeyParam.PropertyEnum.validitySeconds, validitySecondsPropertyChangeListener);
		createPgpKeyParam.removePropertyChangeListener(CreatePgpKeyParam.PropertyEnum.strength, strengthPropertyChangeListener);
		super.finalize();
	}

	private StringConverter<CreatePgpKeyParam.Algorithm> algorithmStringConverter = new StringConverter<CreatePgpKeyParam.Algorithm>() {
		@Override
		public String toString(Algorithm algorithm) {
			return algorithm.toString().toLowerCase();
		}

		@Override
		public Algorithm fromString(String string) {
			throw new UnsupportedOperationException("NYI");
		}
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

//	private JavaBeanObjectProperty<Integer> createValiditySecondsProperty() {
//		try {
////			return JavaBeanIntegerPropertyBuilder.create()
////					.bean(createPgpKeyParam)
////					.name(CreatePgpKeyParam.PropertyEnum.validitySeconds.name())
////					.build();
//			return JavaBeanObjectPropertyBuilder.create()
//					.bean(createPgpKeyParam)
//					.name(CreatePgpKeyParam.PropertyEnum.validitySeconds.name())
//					.build();
//		} catch (NoSuchMethodException e) {
//			throw new RuntimeException(e);
//		}
//	}

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

	@FXML
	protected abstract void okButtonClicked(final ActionEvent event);

	@FXML
	protected abstract void cancelButtonClicked(final ActionEvent event);

	public CreatePgpKeyParam getCreatePgpKeyParam() {
		return createPgpKeyParam;
	}
}
