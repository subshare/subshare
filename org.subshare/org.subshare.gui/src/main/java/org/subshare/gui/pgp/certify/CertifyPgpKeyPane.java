package org.subshare.gui.pgp.certify;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.HashMap;
import java.util.Map;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpSignatureType;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.pgp.createkey.FxPgpUserId;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class CertifyPgpKeyPane extends WizardPageContentGridPane {

	private final CertifyPgpKeyData certifyPgpKeyData;
	private final PgpKey pgpKey;

	@FXML
	private ToggleGroup toggleGroup;

	@FXML
	private TextField keyIdTextField;

	@FXML
	private TableView<FxPgpUserId> userIdsTableView;

	@FXML
	private TextField fingerprintTextField;

	@FXML
	private VBox radioButtonBox;

	@FXML
	private ComboBox<PgpKey> signKeyComboBox;

	private Map<PgpSignatureType, RadioButton> certificationLevel2RadioButton = new HashMap<>();

	public CertifyPgpKeyPane(final CertifyPgpKeyData certifyPgpKeyData) {
		this.certifyPgpKeyData = assertNotNull("certifyPgpKeyData", certifyPgpKeyData);
		this.pgpKey = assertNotNull("certifyPgpKeyData.pgpKey", certifyPgpKeyData.getPgpKey());
		loadDynamicComponentFxml(CertifyPgpKeyPane.class, this);

		certifyPgpKeyData.signPgpKeyProperty().bind(signKeyComboBox.getSelectionModel().selectedItemProperty());
		certifyPgpKeyData.signPgpKeyProperty().addListener((InvalidationListener) observable -> updateComplete());

		keyIdTextField.setText(pgpKey.getPgpKeyId().toHumanString());
		populateUserIdsTableView();
		fingerprintTextField.setText(pgpKey.getFingerprint().toHumanString());
		populateSignKeyComboBox();

		populateRadioButtonBox();
		certifyPgpKeyData.certificationLevelProperty().addListener((InvalidationListener) observable -> updateToggleGroup());
		updateToggleGroup();
	}

	private void updateToggleGroup() {
		final PgpSignatureType certificationLevel = certifyPgpKeyData.certificationLevelProperty().get();
		RadioButton radioButton = certificationLevel2RadioButton.get(certificationLevel);
		toggleGroup.selectToggle(radioButton);
//		selectedCertificationLevelDescriptionText.setText(certificationLevel == null ? null : certificationLevel.getDescription());
		updateComplete();
	}

	private void populateRadioButtonBox() {
		radioButtonBox.getChildren().clear();

		for (PgpSignatureType certificationLevel : PgpSignatureType.CERTIFICATIONS) {
			final RadioButton radioButton = new RadioButton(certificationLevel.getAnswer());
			radioButton.setUserData(certificationLevel);
			radioButton.setToggleGroup(toggleGroup);
			radioButtonBox.getChildren().add(radioButton);
			certificationLevel2RadioButton.put(certificationLevel, radioButton);
		}

		toggleGroup.selectedToggleProperty().addListener((ChangeListener<Toggle>) (observable, oldValue, newValue) -> {
			final PgpSignatureType certificationLevel = newValue == null ? null : getCertificationLevel((RadioButton) newValue);
			certifyPgpKeyData.setCertificationLevel(certificationLevel);
//			selectedCertificationLevelDescriptionText.setText(certificationLevel == null ? null : certificationLevel.getDescription());
		});
	}


	private void populateUserIdsTableView() {
		for (final String userIdString : pgpKey.getUserIds()) {
			FxPgpUserId pgpUserId = new FxPgpUserId(userIdString);
			userIdsTableView.getItems().add(pgpUserId);
		}
	}

	private void populateSignKeyComboBox() {
		signKeyComboBox.setConverter(new StringConverter<PgpKey>() {
			@Override
			public String toString(PgpKey pgpKey) {
				final StringBuilder sb = new StringBuilder();
				if (! pgpKey.getUserIds().isEmpty()) // should really *never* happen -- but better check
					sb.append(pgpKey.getUserIds().get(0));

				sb.append(" - ");
				sb.append(pgpKey.getPgpKeyId().toHumanString());
				return sb.toString();
			}
			@Override
			public PgpKey fromString(String string) {
				throw new UnsupportedOperationException();
			}
		});
		for (PgpKey pgpKey : getPgp().getMasterKeysWithSecretKey()) {
			signKeyComboBox.getItems().add(pgpKey);
		}
		if (! signKeyComboBox.getItems().isEmpty())
			signKeyComboBox.getSelectionModel().clearAndSelect(0);
	}

	@Override
	protected boolean isComplete() {
		return certifyPgpKeyData.getSignPgpKey() != null;
	}

	protected Pgp getPgp() {
		return PgpLs.getPgpOrFail();
	}

	private static PgpSignatureType getCertificationLevel(final RadioButton radioButton) {
		assertNotNull("radioButton", radioButton);
		return (PgpSignatureType) assertNotNull("radioButton.userData", radioButton.getUserData());
	}
}
