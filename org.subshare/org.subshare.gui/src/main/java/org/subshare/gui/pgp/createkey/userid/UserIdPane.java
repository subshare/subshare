package org.subshare.gui.pgp.createkey.userid;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;
import static javafx.application.Platform.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.PgpUserId;
import org.subshare.gui.pgp.createkey.FxPgpUserId;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

public class UserIdPane extends WizardPageContentGridPane {

	private final CreatePgpKeyParam createPgpKeyParam;

	@FXML
	private TableView<FxPgpUserId> userIdsTableView;
	@FXML
	private TableColumn<FxPgpUserId, String> nameTableColumn;
	@FXML
	private TableColumn<FxPgpUserId, String> emailTableColumn;

	private final PropertyChangeListener pgpUserIdsPropertyChangeListener = event -> {
		runLater(() -> updateEmailsTableViewItems() );
	};

	public UserIdPane(final CreatePgpKeyParam createPgpKeyParam) {
		this.createPgpKeyParam = requireNonNull(createPgpKeyParam, "createPgpKeyParam");
		loadDynamicComponentFxml(UserIdPane.class, this);

		for (PgpUserId pgpUserId : createPgpKeyParam.getUserIds())
			pgpUserId.addPropertyChangeListener(pgpUserIdsPropertyChangeListener);

		userIdsTableView.setItems(FXCollections.observableList(cast(createPgpKeyParam.getUserIds())));
		userIdsTableView.getItems().addListener((InvalidationListener) observable -> updateComplete());
		nameTableColumn.setCellFactory(cast(TextFieldTableCell.forTableColumn()));
		emailTableColumn.setCellFactory(cast(TextFieldTableCell.forTableColumn()));
		updateEmailsTableViewItems();
	}

	private void updateEmailsTableViewItems() {
		ObservableList<FxPgpUserId> items = userIdsTableView.getItems();
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
	protected boolean isComplete() {
		int nonEmptyPgpUserIdCount = 0;
		for (PgpUserId pgpUserId : createPgpKeyParam.getUserIds()) {
			if (! pgpUserId.isEmpty())
				++nonEmptyPgpUserIdCount;
		}
		boolean complete = nonEmptyPgpUserIdCount > 0;
		return complete;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		userIdsTableView.requestFocus();
	}
}
