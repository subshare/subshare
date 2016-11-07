package org.subshare.gui.pgp.imp.fromserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.PgpSignatureType;
import org.subshare.gui.pgp.certify.CertifyPgpKeyData;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;

public class CertifyPgpKeyPane extends org.subshare.gui.pgp.certify.CertifyPgpKeyPane {

	private RadioButton skipRadioButton;

	public CertifyPgpKeyPane(CertifyPgpKeyData certifyPgpKeyData) {
		super(certifyPgpKeyData);

		if (CertifyPgpKeyPane.class == this.getClass())
			init();
	}

	@Override
	protected void init() {
		super.init();
		initSkipRadioButton();
		updateToggleGroup();
	}

	@Override
	protected void updateToggleGroup() {
		if (skipRadioButton == null)
			return;

		super.updateToggleGroup();
	}

	private void initSkipRadioButton() {
		// Move all children down, first.
		for (Node child : getChildren()) {
			final Integer rowIndex = getRowIndex(child);
			assertNotNull("rowIndex[" + child + "]", rowIndex);
			setRowIndex(child, rowIndex + 1);
		}

		// Then add the skipRadioButton as top-most child.
		skipRadioButton = new RadioButton();
		skipRadioButton.setText("Do NOT certify this key, now. Skip signing it!");
		skipRadioButton.setToggleGroup(toggleGroup);
		setMargin(skipRadioButton, new Insets(0, 0, 8, 0));
		add(skipRadioButton, 0, 0, 2, 1);

		skipRadioButton.selectedProperty().bindBidirectional(certifyPgpKeyData.skipProperty());
	}

	@Override
	protected RadioButton getRadioButtonForCertificationLevel() {
		if (certifyPgpKeyData.isSkip())
			return skipRadioButton;

		return super.getRadioButtonForCertificationLevel();
	}

	@Override
	protected PgpSignatureType getCertificationLevelForRadioButton(RadioButton radioButton) {
		if (certifyPgpKeyData.isSkip())
			return certifyPgpKeyData.getCertificationLevel();

		return super.getCertificationLevelForRadioButton(radioButton);
	}

}
