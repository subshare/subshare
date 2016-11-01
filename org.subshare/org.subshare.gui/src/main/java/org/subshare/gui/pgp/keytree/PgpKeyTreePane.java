package org.subshare.gui.pgp.keytree;

import static org.subshare.gui.util.FxmlUtil.*;

import java.util.HashSet;

import org.subshare.core.pgp.Pgp;
import org.subshare.gui.ls.PgpLs;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class PgpKeyTreePane extends BorderPane {

	private final ObjectProperty<Pgp> pgp = new SimpleObjectProperty<Pgp>(this, "pgp") {
		@Override
		public Pgp get() {
			Pgp result = super.get();
			if (result == null) {
				result = PgpLs.getPgpOrFail();
				set(result);
			}
			return result;
		}
	};

	@FXML
	private TreeTableView<PgpKeyTreeItem<?>> treeTableView;

	@FXML
	private TreeTableColumn<PgpKeyTreeItem<?>, String> nameTreeTableColumn;

	private final ObservableSet<Class<? extends PgpKeyTreeItem<?>>> checkBoxVisibleForPgpKeyTreeItemClasses = FXCollections.observableSet(new HashSet<Class<? extends PgpKeyTreeItem<?>>>());

	private final ObservableSet<PgpKeyTreeItem<?>> checkedTreeItems = FXCollections.observableSet(new HashSet<PgpKeyTreeItem<?>>());

	public PgpKeyTreePane() {
		loadDynamicComponentFxml(PgpKeyTreePane.class, this);

		treeTableView.setShowRoot(false);
		treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		nameTreeTableColumn.setCellFactory(new Callback<TreeTableColumn<PgpKeyTreeItem<?>,String>, TreeTableCell<PgpKeyTreeItem<?>,String>>() {
			@Override
			public TreeTableCell<PgpKeyTreeItem<?>, String> call(TreeTableColumn<PgpKeyTreeItem<?>, String> param) {
				return new NameCell();
			}
		});

		checkedTreeItems.addListener(new SetChangeListener<PgpKeyTreeItem<?>>() {
			@Override
			public void onChanged(SetChangeListener.Change<? extends PgpKeyTreeItem<?>> change) {
				final PgpKeyTreeItem<?> elementAdded = change.getElementAdded();
				if (elementAdded != null)
					elementAdded.setChecked(true);

				final PgpKeyTreeItem<?> elementRemoved = change.getElementRemoved();
				if (elementRemoved != null)
					elementRemoved.setChecked(false);
			}
		});
	}

	private class NameCell extends TreeTableCell<PgpKeyTreeItem<?>, String> {
		@Override
		protected void updateItem(String cellValue, boolean empty) {
			super.updateItem(cellValue, empty);
			final PgpKeyTreeItem<?> treeItem = getTreeTableRow().getItem();
			setGraphic(null);
			setText(cellValue);
			if (treeItem != null && checkBoxVisibleForPgpKeyTreeItemClasses.contains(treeItem.getClass())) {
				final CheckBox checkBox = new CheckBox();
				checkBox.selectedProperty().bindBidirectional(treeItem.checkedProperty());
				setGraphic(checkBox);
			}
		}
	}

	public ObservableSet<Class<? extends PgpKeyTreeItem<?>>> getCheckBoxVisibleForPgpKeyTreeItemClasses() {
		return checkBoxVisibleForPgpKeyTreeItemClasses;
	}

	public TreeTableView<PgpKeyTreeItem<?>> getTreeTableView() {
		return treeTableView;
	}

	public ObservableSet<PgpKeyTreeItem<?>> getCheckedTreeItems() {
		return checkedTreeItems;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		treeTableView.requestFocus();
	}

	public Pgp getPgp() {
		return pgp.get();
	}
	public void setPgp(Pgp pgp) {
		this.pgp.set(pgp);
	}
	public ObjectProperty<Pgp> pgpProperty() {
		return pgp;
	}
}
