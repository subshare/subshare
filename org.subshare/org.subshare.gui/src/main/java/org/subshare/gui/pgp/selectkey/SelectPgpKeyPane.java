package org.subshare.gui.pgp.selectkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.subshare.core.pgp.PgpKey;
import org.subshare.gui.pgp.keytree.PgpKeyPgpKeyTreeItem;
import org.subshare.gui.pgp.keytree.PgpKeyTreeItem;
import org.subshare.gui.pgp.keytree.PgpKeyTreePane;
import org.subshare.gui.pgp.keytree.SimpleRootPgpKeyTreeItem;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public abstract class SelectPgpKeyPane extends GridPane {

	private final List<PgpKey> pgpKeys;

	private final ObservableSet<PgpKey> selectedPgpKeys;

	private final List<PgpKeyPgpKeyTreeItem> pgpKeyPgpKeyTreeItems;

	private final Timer applyFilterLaterTimer = new Timer(true);
	private TimerTask applyFilterLaterTimerTask;

	private final SimpleRootPgpKeyTreeItem simpleRootPgpKeyTreeItem;

	@FXML
	private Text headerText;

	@FXML
	private TextField filterTextField;

	@FXML
	private PgpKeyTreePane pgpKeyTreePane;

	@FXML
	private Button okButton;

	@FXML
	private Button cancelButton;

	public SelectPgpKeyPane(final List<PgpKey> pgpKeys, final Collection<PgpKey> selectedPgpKeys, final SelectionMode selectionMode, final String headerText) {
		assertNotNull("pgpKeys", pgpKeys);
		// selectedPgpKeys may be null
		assertNotNull("selectionMode", selectionMode);
		loadDynamicComponentFxml(SelectPgpKeyPane.class, this);

		this.headerText.setText(headerText);

		simpleRootPgpKeyTreeItem = new SimpleRootPgpKeyTreeItem(pgpKeyTreePane);

		this.pgpKeys = pgpKeys;
		pgpKeyPgpKeyTreeItems = new ArrayList<>(pgpKeys.size());

		final Map<PgpKey, PgpKeyPgpKeyTreeItem> pgpKey2TreeItem = new IdentityHashMap<>();
		for (final PgpKey pgpKey : pgpKeys) {
			final PgpKeyPgpKeyTreeItem treeItem = new PgpKeyPgpKeyTreeItem(pgpKey);

			if (pgpKey2TreeItem.put(pgpKey, treeItem) != null)
				throw new IllegalArgumentException("Duplicate PgpKey element: " + pgpKey);

			pgpKeyPgpKeyTreeItems.add(treeItem);
		}
		simpleRootPgpKeyTreeItem.getChildren().addAll(pgpKeyPgpKeyTreeItems);
		pgpKeyTreePane.getTreeTableView().setRoot(simpleRootPgpKeyTreeItem);

		this.selectedPgpKeys = FXCollections.observableSet(new HashSet<>(selectedPgpKeys != null ? selectedPgpKeys : Collections.<PgpKey>emptyList()));

		for (final PgpKey pgpKey : this.selectedPgpKeys) {
			final PgpKeyPgpKeyTreeItem treeItem = pgpKey2TreeItem.get(pgpKey);
			if (treeItem == null)
				throw new IllegalArgumentException("selectedPgpKeys contains PgpKey not being contained in pgpKeys: " + pgpKey);

			pgpKeyTreePane.getTreeTableView().getSelectionModel().getSelectedItems().add(treeItem);
		}

		filterTextField.textProperty().addListener((InvalidationListener) observable -> applyFilterLater());

		pgpKeyTreePane.getTreeTableView().getSelectionModel().getSelectedItems().addListener(selectedItemsChangeListener);
		pgpKeyTreePane.getTreeTableView().getSelectionModel().setSelectionMode(selectionMode);

		this.selectedPgpKeys.addListener((InvalidationListener) observable -> updateDisable());
		updateDisable();
	}

	private void applyFilterLater() {
		if (applyFilterLaterTimerTask != null) {
			applyFilterLaterTimerTask.cancel();
			applyFilterLaterTimerTask = null;
		}

		applyFilterLaterTimerTask = new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						applyFilterLaterTimerTask = null;
						applyFilter();
					}
				});

			}
		};

		applyFilterLaterTimer.schedule(applyFilterLaterTimerTask, 300);
	}

	private void applyFilter() {
		final String filterText = filterTextField.getText().toLowerCase();
		final Iterator<PgpKeyPgpKeyTreeItem> sourceIterator = pgpKeyPgpKeyTreeItems.iterator();
		final ListIterator<PgpKeyPgpKeyTreeItem> treeItemIterator = cast(simpleRootPgpKeyTreeItem.getChildren().listIterator());

		final Set<PgpKey> includedPgpKeys = new HashSet<>();

		while (sourceIterator.hasNext()) {
			final PgpKeyPgpKeyTreeItem sourceItem = sourceIterator.next();
			final boolean matchesFilter = matchesFilter(sourceItem, filterText);
			if (matchesFilter)
				includedPgpKeys.add(sourceItem.getPgpKey());

			final PgpKeyPgpKeyTreeItem treeItem = treeItemIterator.hasNext() ? treeItemIterator.next() : null;
			if (treeItem == null) {
				if (matchesFilter)
					treeItemIterator.add(sourceItem);
			}
			else if (treeItem == sourceItem) {
				if (! matchesFilter)
					treeItemIterator.remove();
			}
			else {
				if (matchesFilter) {
					treeItemIterator.previous();
					treeItemIterator.add(sourceItem);
				}
				else
					treeItemIterator.previous();
			}
		}

		while (treeItemIterator.hasNext()) {
			treeItemIterator.next();
			treeItemIterator.remove();
		}

		selectedPgpKeys.retainAll(includedPgpKeys);
	}

	private boolean matchesFilter(final PgpKeyPgpKeyTreeItem treeItem, final String filterText) {
		if (isEmpty(filterText))
			return true;

		final PgpKey pgpKey = treeItem.getPgpKey();

		for (final String userId : pgpKey.getUserIds()) {
			if (userId.toLowerCase().contains(filterText))
				return true;
		}

		return false;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		pgpKeyTreePane.requestFocus();
	}

	private final ListChangeListener<TreeItem<PgpKeyTreeItem<?>>> selectedItemsChangeListener = new ListChangeListener<TreeItem<PgpKeyTreeItem<?>>>() {
		@Override
		public void onChanged(ListChangeListener.Change<? extends TreeItem<PgpKeyTreeItem<?>>> c) {
			while (c.next()) {
				List<PgpKey> pgpKeys = new ArrayList<>(c.getRemoved().size());
				for (TreeItem<PgpKeyTreeItem<?>> treeItem : c.getRemoved()) {
					final PgpKeyPgpKeyTreeItem item = getPgpKeyPgpKeyTreeItem(treeItem);
					if (item != null)
						pgpKeys.add(item.getPgpKey());
				}

				selectedPgpKeys.removeAll(pgpKeys);

				pgpKeys = new ArrayList<>(c.getAddedSubList().size());
				for (TreeItem<PgpKeyTreeItem<?>> treeItem : c.getAddedSubList()) {
					final PgpKeyPgpKeyTreeItem item = getPgpKeyPgpKeyTreeItem(treeItem);
					if (item != null)
						pgpKeys.add(item.getPgpKey());
				}

				selectedPgpKeys.addAll(pgpKeys);
			}
		}
	};

	private PgpKeyPgpKeyTreeItem getPgpKeyPgpKeyTreeItem(TreeItem<PgpKeyTreeItem<?>> treeItem) {
		while (treeItem != null) {
			if (treeItem instanceof PgpKeyPgpKeyTreeItem)
				return (PgpKeyPgpKeyTreeItem) treeItem;

			treeItem = treeItem.getParent();
		}
		return null;
	}

	protected void updateDisable() {
		okButton.setDisable(selectedPgpKeys.isEmpty());
	}

	public ObservableSet<PgpKey> getSelectedPgpKeys() {
		return selectedPgpKeys;
	}

	@FXML
	protected abstract void okButtonClicked(final ActionEvent event);

	@FXML
	protected abstract void cancelButtonClicked(final ActionEvent event);
}
