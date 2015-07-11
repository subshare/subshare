package org.subshare.gui.filetree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;

import org.subshare.gui.filetree.RefreshListener.RefreshEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFilter;
import co.codewizards.cloudstore.core.ref.IdentityWeakReference;
import co.codewizards.cloudstore.core.util.IOUtil;

public class FileTreePane extends BorderPane {

	private static final Logger logger = LoggerFactory.getLogger(FileTreePane.class);

	// TODO make use of the useCase => store and retrieve the last selection per use-case!
	private final StringProperty useCaseProperty = new SimpleStringProperty(this, "useCase", "default") {
		@Override
		public void set(final String useCase) {
			assertNotNull("useCase", useCase);
		}
	};

	@FXML
	private TreeTableView<FileTreeItem<?>> treeTableView;

	@FXML
	private CheckBox showHiddenFilesCheckBox;

	@FXML
	private Button refreshButton;

	private final BooleanProperty showHiddenFilesProperty = new SimpleBooleanProperty(this, "showHiddenFiles", false);

	private final RootFileTreeItem rootFileTreeItem;

	private final ObservableSet<File> selectedFiles = FXCollections.observableSet(new HashSet<File>());

	private final ObjectProperty<FileFilter> fileFilterProperty = new SimpleObjectProperty<FileFilter>(this, "fileFilter") {
		@Override
		public void set(FileFilter newValue) {
			// WORKAROUND: the selection is modified when setting the fileFilter from the outside (refreshing the children seems to cause it) :-(

			if (updatingSelectedFiles) {
				super.set(newValue);
				return;
			}

			updatingSelectedFiles = true;
			try {
				super.set(newValue);
				treeTableView.getSelectionModel().clearSelection();
			} finally {
				updatingSelectedFiles = false;
			}

			for (File f : selectedFiles)
				selectFileTreeItemForSelectedFile(f);
		}
	};

	private final SetChangeListener<File> selectedFilesChangeListener = new SetChangeListener<File>() {
		@Override
		public void onChanged(final SetChangeListener.Change<? extends File> c) {
			assertFxApplicationThread();

			if (c.getElementRemoved() != null)
				System.out.println("selectedFileRemoved: " + c.getElementRemoved());

			if (c.getElementAdded() != null)
				System.out.println("selectedFileAdded: " + c.getElementAdded());

			if (c.getElementRemoved() != null)
				unselectTreeItemForUnselectedFile(c.getElementRemoved());

			if (c.getElementAdded() != null)
				selectFileTreeItemForSelectedFile(c.getElementAdded());
		}
	};

	private boolean updatingSelectedFiles;

	private final CopyOnWriteArrayList<WeakReference<RefreshListener>> refreshListeners = new CopyOnWriteArrayList<>();
	private final ReferenceQueue<RefreshListener> refreshListenersReferenceQueue = new ReferenceQueue<>();

	public FileTreePane() {
		loadDynamicComponentFxml(FileTreePane.class, this);
		rootFileTreeItem = new RootFileTreeItem(this);

		// The root here is *not* the real root of the file system and should be hidden, because
		// (1) we might want to have 'virtual' visible roots like "Home", "Desktop", "Drives" and
		// (2) even if we displayed only the real file system without any shortcuts like "Desktop",
		// we'd still have multiple roots on a crappy pseudo-OS like Windows still having these shitty drive letters.
		treeTableView.setShowRoot(false);
		treeTableView.setRoot(rootFileTreeItem);
		treeTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		treeTableView.getSelectionModel().selectedItemProperty().addListener((ChangeListener<TreeItem<FileTreeItem<?>>>) (observable, o, n) -> updateSelectedFiles());
		treeTableView.getSelectionModel().getSelectedItems().addListener((InvalidationListener) observable -> updateSelectedFiles());

		selectedFiles.addListener(selectedFilesChangeListener);
		showHiddenFilesCheckBox.selectedProperty().bindBidirectional(showHiddenFilesProperty);
		refreshButton.setOnAction(event -> fireRefreshListeners());

		// TODO remove the following line - preferably replace by proper, use-case-dependent management!
//		selectedFiles.add(createFile("/home/mn/Desktop/Bilder_Juergen/Bilder Indien 2007"));
		selectedFiles.add(IOUtil.getUserHome());
	}

	private void updateSelectedFiles() {
		assertFxApplicationThread();

		if (updatingSelectedFiles)
			return;

		updatingSelectedFiles = true;
		try {
			final ObservableList<TreeItem<FileTreeItem<?>>> selectedItems = treeTableView.getSelectionModel().getSelectedItems();
			final Set<File> newSelectedFiles = new HashSet<File>(selectedItems.size());
			for (final TreeItem<FileTreeItem<?>> selectedItem : selectedItems) {
				final FileTreeItem<?> fileTreeItem = selectedItem == null ? null : selectedItem.getValue(); // strange but true: it can be null
				if (fileTreeItem instanceof FileFileTreeItem)
					newSelectedFiles.add(((FileFileTreeItem) fileTreeItem).getFile());
			}
			selectedFiles.retainAll(newSelectedFiles);
			selectedFiles.addAll(newSelectedFiles);
		} finally {
			updatingSelectedFiles = false;
		}
	}

	private void selectFileTreeItemForSelectedFile(final File file) {
		if (updatingSelectedFiles)
			return;

		updatingSelectedFiles = true;
		try {
			final FileTreeItem<?> fileTreeItem = rootFileTreeItem.findFirst(file);
			if (fileTreeItem == null) {
				IllegalStateException x = new IllegalStateException("File does not have corresponding FileTreeItem: " + file);
				logger.warn("selectFileTreeItemForSelectedFile: " + x, x);
			}
			else {
				TreeItem<?> ti = fileTreeItem.getParent();
				while (ti != null) {
					ti.setExpanded(true);
					ti = ti.getParent();
				}

				// TODO maybe, instead of scrolling here (after each single item is selected), immediately, we should wait (=> Timer) and scroll to the *first* selection, later?!
				treeTableView.getSelectionModel().select(fileTreeItem);
				int row = treeTableView.getRow(fileTreeItem);
				if (row >= 0)
					treeTableView.scrollTo(row);
			}
		} finally {
			updatingSelectedFiles = false;
		}
	}

	private void unselectTreeItemForUnselectedFile(final File file) {
		if (updatingSelectedFiles)
			return;

		updatingSelectedFiles = true;
		try {
			final FileTreeItem<?> fileTreeItem = rootFileTreeItem.findFirst(file);
			if (fileTreeItem != null)
				treeTableView.getSelectionModel().getSelectedItems().remove(fileTreeItem);
		} finally {
			updatingSelectedFiles = false;
		}
	}

	public ObservableSet<File> getSelectedFiles() {
		return selectedFiles;
	}

	private void fireRefreshListeners() {
		final RefreshEvent event = new RefreshEvent(this);
		expungeRefreshListeners();
		for (final Reference<RefreshListener> reference : refreshListeners) {
			final RefreshListener listener = reference.get();
			if (listener != null)
				listener.onRefresh(event);
		}
	}

	protected void addRefreshListener(RefreshListener listener) {
		expungeRefreshListeners();
		refreshListeners.add(new IdentityWeakReference<RefreshListener>(assertNotNull("listener", listener), refreshListenersReferenceQueue));
	}

	protected void removeRefreshListener(RefreshListener listener) {
		expungeRefreshListeners();
		refreshListeners.remove(new IdentityWeakReference<RefreshListener>(assertNotNull("listener", listener)));
	}

	private void expungeRefreshListeners() {
		Reference<? extends RefreshListener> ref;
		while ((ref = refreshListenersReferenceQueue.poll()) != null)
			refreshListeners.remove(ref);
	}

	public SelectionMode getSelectionMode() {
		return treeTableView.getSelectionModel().getSelectionMode();
	}

	public void setSelectionMode(SelectionMode selectionMode) {
		assertNotNull("selectionMode", selectionMode);
		treeTableView.getSelectionModel().setSelectionMode(selectionMode);
	}

	public TreeTableView<FileTreeItem<?>> getTreeTableView() {
		return treeTableView;
	}

	public RootFileTreeItem getRootFileTreeItem() {
		return rootFileTreeItem;
	}

	public BooleanProperty showHiddenFilesProperty() {
		return showHiddenFilesProperty;
	}

	public ObjectProperty<FileFilter> fileFilterProperty() {
		return fileFilterProperty;
	}

	/**
	 * Gets the use-case.
	 * @return the use-case. Never <code>null</code>.
	 * @see #useCaseProperty()
	 */
	public String getUseCase() {
		return useCaseProperty.get();
	}
	/**
	 * Sets the use-case.
	 * @param useCase the use-case. Must not be <code>null</code>.
	 * @see #useCaseProperty()
	 */
	public void setUseCase(String useCase) {
		useCaseProperty.set(useCase);
	}
	/**
	 * The use-case-property. This identifier is used to remember the last selection.
	 * @return the use-case-property. Never <code>null</code>.
	 */
	public StringProperty useCaseProperty() {
		return useCaseProperty;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		treeTableView.requestFocus();
	}
}
