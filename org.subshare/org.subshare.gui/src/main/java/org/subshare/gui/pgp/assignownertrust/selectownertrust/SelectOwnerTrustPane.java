package org.subshare.gui.pgp.assignownertrust.selectownertrust;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import static co.codewizards.cloudstore.core.util.StringUtil.isEmpty;
import static org.subshare.gui.util.FxmlUtil.loadDynamicComponentFxml;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.subshare.core.pgp.PgpOwnerTrust;
import org.subshare.core.user.User;
import org.subshare.gui.pgp.assignownertrust.AssignOwnerTrustData;

public abstract class SelectOwnerTrustPane extends GridPane {

	private /*final*/ AssignOwnerTrustData assignOwnerTrustData;

	@FXML
	private ToggleGroup toggleGroup;

	@FXML
	private TextField userTextField;

	@FXML
	private VBox radioButtonBox;

	private Map<PgpOwnerTrust, RadioButton> ownerTrust2RadioButton = new HashMap<>();

	@FXML
	private Text selectedOwnerTrustDescriptionText;

	public SelectOwnerTrustPane(final AssignOwnerTrustData assignOwnerTrustData) {
		this.assignOwnerTrustData = assertNotNull("assignOwnerTrustData", assignOwnerTrustData);
		loadDynamicComponentFxml(SelectOwnerTrustPane.class, this);
		populateUserTextField();
		populateRadioButtonBox();
		assignOwnerTrustData.ownerTrustProperty().addListener((InvalidationListener) observable -> updateToggleGroup());
		updateToggleGroup();
	}

	protected boolean isComplete() {
		return assignOwnerTrustData.getOwnerTrust() != null
				&& assignOwnerTrustData.getOwnerTrust() != PgpOwnerTrust.UNSPECIFIED;
	}

	protected abstract void updateComplete();

	private void populateUserTextField() {
		final User user = assignOwnerTrustData.getUser();
		final StringBuilder sb = new StringBuilder();
		if (user != null) {
			if (! isEmpty(user.getFirstName()))
				sb.append(user.getFirstName());

			if (! isEmpty(user.getLastName())) {
				if (sb.length() > 0)
					sb.append(' ');

				sb.append(user.getLastName());
			}

			String email = null;
			for (String e : user.getEmails()) {
				if (! isEmpty(e)) {
					email = e;
					break;
				}
			}
			if (! isEmpty(email)) {
				if (sb.length() > 0)
					sb.append(' ');

				sb.append('<').append(email).append('>');
			}
		}
		userTextField.setText(sb.toString());
	}

	private void updateToggleGroup() {
		final PgpOwnerTrust ownerTrust = assignOwnerTrustData.ownerTrustProperty().get();
		RadioButton radioButton = ownerTrust2RadioButton.get(ownerTrust);
		toggleGroup.selectToggle(radioButton);
		selectedOwnerTrustDescriptionText.setText(ownerTrust == null ? null : ownerTrust.getDescription());
		updateComplete();
	}

	private void populateRadioButtonBox() {
		radioButtonBox.getChildren().clear();

		for (PgpOwnerTrust ownerTrust : PgpOwnerTrust.values()) {
			if (PgpOwnerTrust.UNSPECIFIED == ownerTrust)
				continue; // cannot be assigned!

			final RadioButton radioButton = new RadioButton(ownerTrust.getAnswer());
			radioButton.setUserData(ownerTrust);
			radioButton.setToggleGroup(toggleGroup);
			radioButtonBox.getChildren().add(radioButton);
			ownerTrust2RadioButton.put(ownerTrust, radioButton);
		}

		toggleGroup.selectedToggleProperty().addListener((ChangeListener<Toggle>) (observable, oldValue, newValue) -> {
			final PgpOwnerTrust ownerTrust = newValue == null ? null : getOwnerTrust((RadioButton) newValue);
			assignOwnerTrustData.setOwnerTrust(ownerTrust);
			selectedOwnerTrustDescriptionText.setText(ownerTrust == null ? null : ownerTrust.getDescription());
		});
	}

	private static PgpOwnerTrust getOwnerTrust(final RadioButton radioButton) {
		assertNotNull("radioButton", radioButton);
		return (PgpOwnerTrust) assertNotNull("radioButton.userData", radioButton.getUserData());
	}
}
