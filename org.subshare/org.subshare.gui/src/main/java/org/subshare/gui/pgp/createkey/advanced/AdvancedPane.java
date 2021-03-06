package org.subshare.gui.pgp.createkey.advanced;

import static java.util.Objects.*;
import static javafx.application.Platform.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeListener;
import java.util.List;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.CreatePgpKeyParam.Algorithm;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import javafx.beans.InvalidationListener;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

public class AdvancedPane extends WizardPageContentGridPane {

	private final CreatePgpKeyParam createPgpKeyParam;

	@FXML
	private ComboBox<CreatePgpKeyParam.Algorithm> algorithmComboBox;
	private final JavaBeanObjectProperty<CreatePgpKeyParam.Algorithm> algorithm;

	@FXML
	private ComboBox<Integer> strengthComboBox;

	private StringConverter<CreatePgpKeyParam.Algorithm> algorithmStringConverter = new StringConverter<CreatePgpKeyParam.Algorithm>() {
		@Override
		public String toString(Algorithm algorithm) {
			return Messages.getString(String.format("AdvancedPane.Algorithm[%s]", algorithm)); //$NON-NLS-1$
		}
		@Override
		public Algorithm fromString(String string) { throw new UnsupportedOperationException(); }
	};

	private final PropertyChangeListener strengthPropertyChangeListener = event -> {
		runLater(() -> updateStrengthComboBoxItems() );
	};

	public AdvancedPane(final CreatePgpKeyParam createPgpKeyParam) {
		this.createPgpKeyParam = requireNonNull(createPgpKeyParam, "createPgpKeyParam");
		loadDynamicComponentFxml(AdvancedPane.class, this);

		algorithm = createAlgorithmProperty();
		algorithm.addListener((InvalidationListener) observable -> updateStrengthComboBoxItems());
		algorithmComboBox.setItems(FXCollections.observableArrayList(CreatePgpKeyParam.Algorithm.values()));
		algorithmComboBox.setConverter(algorithmStringConverter);
		algorithmComboBox.valueProperty().bindBidirectional(algorithm);

		updateStrengthComboBoxItems();
		strengthComboBox.valueProperty().addListener((InvalidationListener) observable -> createPgpKeyParam.setStrength(strengthComboBox.getValue()));

		createPgpKeyParam.addPropertyChangeListener(CreatePgpKeyParam.PropertyEnum.strength, strengthPropertyChangeListener);

		updateComplete();
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
	protected boolean isComplete() {
		boolean complete = createPgpKeyParam.getAlgorithm() != null;
		complete &= createPgpKeyParam.getStrength() > 0;
		return complete;
	}
}
