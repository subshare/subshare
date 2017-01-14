package org.subshare.gui.pgp.assignownertrust.selectkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import org.subshare.core.pgp.PgpOwnerTrust;
import org.subshare.gui.pgp.assignownertrust.AssignOwnerTrustData;
import org.subshare.gui.pgp.keytree.PgpKeyPgpKeyTreeItem;
import org.subshare.gui.pgp.keytree.PgpKeyTreeItem;
import org.subshare.gui.pgp.keytree.PgpKeyTreePane;
import org.subshare.gui.pgp.keytree.UserRootPgpKeyTreeItem;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;

public class SelectKeyPane extends WizardPageContentGridPane {

	private /*final*/ AssignOwnerTrustData assignOwnerTrustData;

	@FXML
	private ToggleGroup toggleGroup;

	@FXML
	private RadioButton assignToAllPgpKeysRadioButton;

	@FXML
	private RadioButton assignToSelectedPgpKeysRadioButton;

	@FXML
	private TextField newOwnerTrustTextField;

	@FXML
	private PgpKeyTreePane pgpKeyTreePane;

	private final ChangeListener<Boolean> assignToAllPgpKeysChangeListener = (observable, oldValue, newValue) -> {
		if (newValue == null)
			toggleGroup.selectToggle(null);
		else if (newValue)
			toggleGroup.selectToggle(assignToAllPgpKeysRadioButton);
		else
			toggleGroup.selectToggle(assignToSelectedPgpKeysRadioButton);
	};

	private final InvalidationListener toggleGroupSelectedToggleListener = observable -> {
		final Toggle toggle = toggleGroup.selectedToggleProperty().get();
		if (toggle == assignToAllPgpKeysRadioButton)
			assignOwnerTrustData.setAssignToAllPgpKeys(true);
		else if (toggle == assignToSelectedPgpKeysRadioButton)
			assignOwnerTrustData.setAssignToAllPgpKeys(false);
		else
			assignOwnerTrustData.setAssignToAllPgpKeys(null);

		updateComplete();
	};

	private final InvalidationListener ownerTrustListener = observable -> {
		final PgpOwnerTrust ownerTrust = assignOwnerTrustData.ownerTrustProperty().get();
		newOwnerTrustTextField.setText(ownerTrust == null ? null : ownerTrust.toString());
	};

	public SelectKeyPane(final AssignOwnerTrustData assignOwnerTrustData) {
		this.assignOwnerTrustData = assertNotNull(assignOwnerTrustData, "assignOwnerTrustData");
		loadDynamicComponentFxml(SelectKeyPane.class, this);
		assignOwnerTrustData.assignToAllPgpKeysProperty().addListener(assignToAllPgpKeysChangeListener);
		toggleGroup.selectedToggleProperty().addListener(toggleGroupSelectedToggleListener);
		assignOwnerTrustData.ownerTrustProperty().addListener(ownerTrustListener);

		pgpKeyTreePane.getCheckBoxVisibleForPgpKeyTreeItemClasses().add(PgpKeyPgpKeyTreeItem.class);
		final UserRootPgpKeyTreeItem root = new UserRootPgpKeyTreeItem(pgpKeyTreePane, assignOwnerTrustData.getUser());
		pgpKeyTreePane.getTreeTableView().setRoot(root);

		for (TreeItem<PgpKeyTreeItem<?>> treeItem : root.getChildren()) {
			if (treeItem instanceof PgpKeyPgpKeyTreeItem) {
				PgpKeyPgpKeyTreeItem ti = (PgpKeyPgpKeyTreeItem) treeItem;
				ti.setChecked(assignOwnerTrustData.getPgpKeys().contains(ti.getPgpKey()));
			}
		}

		pgpKeyTreePane.getCheckedTreeItems().addListener(new SetChangeListener<PgpKeyTreeItem<?>>() {
			@Override
			public void onChanged(SetChangeListener.Change<? extends PgpKeyTreeItem<?>> change) {
				final PgpKeyTreeItem<?> elementAdded = change.getElementAdded();
				if (elementAdded instanceof PgpKeyPgpKeyTreeItem)
					assignOwnerTrustData.getPgpKeys().add(((PgpKeyPgpKeyTreeItem)elementAdded).getPgpKey());

				final PgpKeyTreeItem<?> elementRemoved = change.getElementRemoved();
				if (elementRemoved instanceof PgpKeyPgpKeyTreeItem)
					assignOwnerTrustData.getPgpKeys().remove(((PgpKeyPgpKeyTreeItem)elementRemoved).getPgpKey());

				assignToSelectedPgpKeysRadioButton.setSelected(true);
				updateComplete();
			}
		});
	}

	@Override
	protected boolean isComplete() {
		return assignOwnerTrustData.getAssignToAllPgpKeys() == Boolean.TRUE
				|| ! assignOwnerTrustData.getPgpKeys().isEmpty();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		pgpKeyTreePane.requestFocus();
	}
}
