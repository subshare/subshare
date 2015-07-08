package org.subshare.gui.pgp.createkey.passphrase;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import org.subshare.core.Severity;
import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.IconSize;
import org.subshare.gui.severity.SeverityImageRegistry;
import org.subshare.gui.util.CharArrayStringConverter;

public abstract class PassphrasePane extends GridPane {

	private final CreatePgpKeyParam createPgpKeyParam;

	@FXML
	private Label passphrase1Label;

	@FXML
	private PasswordField passphrase1PasswordField;
	private final JavaBeanObjectProperty<char[]> passphrase1Property;

	@FXML
	private CheckBox noPassphraseCheckBox;

	@FXML
	private Label passphrase2Label;

	@FXML
	private PasswordField passphrase2PasswordField;

	private Severity passphrase1StatusSeverity;

	@FXML
	private HBox passphrase1StatusMessageBox;
	@FXML
	private ImageView passphrase1StatusImageView;
	@FXML
	private Label passphrase1StatusMessageLabel;

	private Severity passphrase2StatusSeverity;

	@FXML
	private HBox passphrase2StatusMessageBox;
	private final BooleanProperty passphrase2StatusMessageBoxVisible = new SimpleBooleanProperty(this, "passphrase2StatusMessageBoxVisible", true); //$NON-NLS-1$
	@FXML
	private ImageView passphrase2StatusImageView;
	@FXML
	private Label passphrase2StatusMessageLabel;

	private char[] backupPassphrase;

	public PassphrasePane(final CreatePgpKeyParam createPgpKeyParam) {
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam); //$NON-NLS-1$
		loadDynamicComponentFxml(PassphrasePane.class, this);

		passphrase1Property = createPassphraseProperty();
		Bindings.bindBidirectional(
				passphrase1PasswordField.textProperty(), passphrase1Property, new CharArrayStringConverter());

		passphrase1Property.addListener((InvalidationListener) observable -> updatePassphraseStatus());
		passphrase2PasswordField.textProperty().addListener((InvalidationListener) observable -> updatePassphraseStatus());
		noPassphraseCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue.booleanValue()) {
					backupPassphrase = createPgpKeyParam.getPassphrase();
					createPgpKeyParam.setPassphrase(new char[0]);
				}
				else {
					createPgpKeyParam.setPassphrase(backupPassphrase);
					backupPassphrase = null;
				}
			}
		});
		noPassphraseCheckBox.selectedProperty().addListener((InvalidationListener) observable -> updateComplete());

		passphrase1Label.disableProperty().bind(noPassphraseCheckBox.selectedProperty());
		passphrase1PasswordField.disableProperty().bind(noPassphraseCheckBox.selectedProperty());
		passphrase1StatusMessageBox.visibleProperty().bind(noPassphraseCheckBox.selectedProperty().not());

		passphrase2PasswordField.disableProperty().bind(noPassphraseCheckBox.selectedProperty());
		passphrase2Label.disableProperty().bind(noPassphraseCheckBox.selectedProperty());
		passphrase2StatusMessageBox.visibleProperty().bind(noPassphraseCheckBox.selectedProperty().not().and(passphrase2StatusMessageBoxVisible));

		updatePassphraseStatus();
	}

	private void updatePassphraseStatus() {
		updatePassphrase1Status();
		updatePassphrase2Status();
		updateComplete();
	}

	private void updatePassphrase1Status() {
		Severity severity;
		String message;
		String tooltipText;
		final char[] p1 = createPgpKeyParam.getPassphrase();
		if (p1.length == 0) {
			severity = Severity.ERROR;
			message = Messages.getString("PassphrasePane.passphrase1Status[empty].message"); //$NON-NLS-1$
			tooltipText = Messages.getString("PassphrasePane.passphrase1Status[empty].tooltipText"); //$NON-NLS-1$
		}
		else if (p1.length < 10) {
			severity = Severity.ERROR;
			message = Messages.getString("PassphrasePane.passphrase1Status[tooShort].message"); //$NON-NLS-1$
			tooltipText = Messages.getString("PassphrasePane.passphrase1Status[tooShort].tooltipText"); //$NON-NLS-1$
		}
		else if (getCharacterTypes(p1).size() < 2) {
			severity = Severity.ERROR;
			message = Messages.getString("PassphrasePane.passphrase1Status[tooFewCharTypes].message"); //$NON-NLS-1$
			tooltipText = Messages.getString("PassphrasePane.passphrase1Status[tooFewCharTypes].tooltipText"); //$NON-NLS-1$
		}
		else if (getCharacterTypes(p1).size() < 3) {
			severity = Severity.WARNING;
			message = Messages.getString("PassphrasePane.passphrase1Status[fewCharTypes].message"); //$NON-NLS-1$
			tooltipText = Messages.getString("PassphrasePane.passphrase1Status[fewCharTypes].tooltipText"); //$NON-NLS-1$
		}
		else if (p1.length < 15) {
			severity = Severity.WARNING;
			message = Messages.getString("PassphrasePane.passphrase1Status[short].message"); //$NON-NLS-1$
			tooltipText = Messages.getString("PassphrasePane.passphrase1Status[short].tooltipText"); //$NON-NLS-1$
		}
		else if (p1.length >= 20 && getCharacterTypes(p1).size() > 3) {
			severity = Severity.INFO;
			message = Messages.getString("PassphrasePane.passphrase1Status[secure].message"); //$NON-NLS-1$
			tooltipText = Messages.getString("PassphrasePane.passphrase1Status[secure].toolTipText"); //$NON-NLS-1$
		}
		else {
			severity = Severity.INFO;
			message = Messages.getString("PassphrasePane.passphrase1Status[ok].message"); //$NON-NLS-1$
			tooltipText = Messages.getString("PassphrasePane.passphrase1Status[ok].toolTipText"); //$NON-NLS-1$
		}

		passphrase1StatusSeverity = severity;
		passphrase1StatusImageView.setImage(severity == null ? null : SeverityImageRegistry.getInstance().getImage(severity, IconSize._16x16));
		passphrase1StatusMessageLabel.setText(message);

		final Tooltip tooltip = isEmpty(tooltipText) ? null : new Tooltip(tooltipText);
		if (tooltip != null)
			tooltip.setWrapText(true);

		passphrase1StatusMessageLabel.setTooltip(tooltip);
		passphrase1PasswordField.setTooltip(tooltip);
		passphrase1Label.setTooltip(tooltip);
	}

	private Set<Integer> getCharacterTypes(char[] chars) {
		Set<Integer> result = new HashSet<>();
		for (char c : chars) {
			int type = Character.getType(c);
			result.add(type);
		}
		return result;
	}

	private void updatePassphrase2Status() {
		Severity severity;
		String message;
		String tooltipText;
		final char[] p1 = createPgpKeyParam.getPassphrase();
		final char[] p2 = passphrase2PasswordField.getText().toCharArray();

		if (passphrase1StatusSeverity != null && passphrase1StatusSeverity.compareTo(Severity.WARNING) > 0) {
			severity = null;
			message = null;
			tooltipText = null;
		}
		else {
			if (Arrays.equals(p1, p2)) {
				severity = null;
				message = null;
				tooltipText = null;
			}
			else {
				severity = Severity.ERROR;
				message = Messages.getString("PassphrasePane.passphrase2Status[mismatch].message"); //$NON-NLS-1$
				tooltipText = Messages.getString("PassphrasePane.passphrase2Status[mismatch].toolTipText"); //$NON-NLS-1$
			}
		}

		passphrase2StatusSeverity = severity;
		passphrase2StatusMessageBoxVisible.set(severity != null);
		passphrase2StatusImageView.setImage(severity == null ? null : SeverityImageRegistry.getInstance().getImage(severity, IconSize._16x16));
		passphrase2StatusMessageLabel.setText(message);

		final Tooltip tooltip = isEmpty(tooltipText) ? null : new Tooltip(tooltipText);
		if (tooltip != null)
			tooltip.setWrapText(true);

		passphrase2StatusMessageLabel.setTooltip(tooltip);
		passphrase2PasswordField.setTooltip(tooltip);
		passphrase2Label.setTooltip(tooltip);
	}

	@SuppressWarnings("unchecked")
	private JavaBeanObjectProperty<char[]> createPassphraseProperty() {
		try {
			return JavaBeanObjectPropertyBuilder.create()
					.bean(createPgpKeyParam)
					.name(CreatePgpKeyParam.PropertyEnum.passphrase.name())
					.build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	protected boolean isComplete() {
		boolean complete = true;

		if (! noPassphraseCheckBox.isSelected()) {
			if (passphrase1StatusSeverity != null && passphrase1StatusSeverity.compareTo(Severity.WARNING) > 0)
				complete = false;

			if (passphrase2StatusSeverity != null && passphrase2StatusSeverity.compareTo(Severity.WARNING) > 0)
				complete = false;
		}
		return complete;
	}

	protected abstract void updateComplete();

	@Override
	public void requestFocus() {
		super.requestFocus();

		passphrase1PasswordField.requestFocus();
	}
}
