package org.subshare.gui.filetree;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.gui.filetree.RefreshListener.RefreshEvent;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFilter;
import co.codewizards.cloudstore.core.ref.IdentityWeakReference;
import co.codewizards.cloudstore.core.util.IOUtil;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

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

	@FXML
	private Button createDirButton;

	@FXML
	private Button renameButton;

	@FXML
	private Button deleteButton;

	@FXML
	private TreeTableColumn<FileTreeItem<?>, String> nameTreeTableColumn;

	@FXML
	private TreeTableColumn<FileTreeItem<?>, String> sizeTreeTableColumn;

	@FXML
	private TreeTableColumn<FileTreeItem<?>, String> lastModifiedTreeTableColumn;

	private final BooleanProperty showHiddenFilesProperty = new SimpleBooleanProperty(this, "showHiddenFiles", false);

	private FileTreeItem<?> rootFileTreeItem;

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
				unselectTreeItemForUnselectedFile(c.getElementRemoved());

			if (c.getElementAdded() != null)
				selectFileTreeItemForSelectedFile(c.getElementAdded());

			updateDisable();
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

		// TODO remove the following line - preferably replace by proper, use-case-dependent management!
		selectedFiles.add(IOUtil.getUserHome());
		updateDisable();
	}

	private void updateDisable() {
		final int seletectItemsSize = treeTableView.getSelectionModel().getSelectedItems().size();
		createDirButton.setDisable(seletectItemsSize != 1); // the selection is the parent - and thus required!
		renameButton.setDisable(seletectItemsSize != 1);
		deleteButton.setDisable(seletectItemsSize < 1);
	}

	public boolean isRefreshButtonVisible() {
		return refreshButton.isVisible();
	}

	public void setRefreshButtonVisible(boolean visible) {
		refreshButton.setVisible(visible);
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
			if (fileTreeItem != null) {
				final Set<TreeItem<FileTreeItem<?>>> selectedItems = new LinkedHashSet<TreeItem<FileTreeItem<?>>>(treeTableView.getSelectionModel().getSelectedItems());
				if (selectedItems.remove(fileTreeItem)) {
					treeTableView.getSelectionModel().clearSelection();
					for (TreeItem<FileTreeItem<?>> treeItem : selectedItems)
						treeTableView.getSelectionModel().select(treeItem);
				}
			}
		} finally {
			updatingSelectedFiles = false;
		}
	}

	public ObservableSet<File> getSelectedFiles() {
		return selectedFiles;
	}

	public void refresh() {
		fireRefreshEvent();
	}

	private void fireRefreshEvent() {
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

	protected TreeTableView<FileTreeItem<?>> getTreeTableView() {
		return treeTableView;
	}

	public FileTreeItem<?> getRootFileTreeItem() {
		return rootFileTreeItem;
	}

	public void setRootFileTreeItem(FileTreeItem<?> rootFileTreeItem) {
		this.rootFileTreeItem = rootFileTreeItem;
		treeTableView.setRoot(rootFileTreeItem);
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

	@FXML
	private void refreshButtonClicked(final ActionEvent event) {
		fireRefreshEvent();
		treeTableView.refresh();
	}

	@FXML
	private void createDirButtonClicked(final ActionEvent event) {
		final File parent = assertNotNull("getSelectedDirectory()", getSelectedDirectory());

		final String dirName = showCreateOrRenameDialog(
				"Create folder",
				"What should be the new folder's name?",
				parent, null,
				(name) -> ! createFile(parent, name).exists());

		if (dirName != null) {
			final File directory = createFile(parent, dirName);
			directory.mkdirs();
			if (! directory.isDirectory())
				showErrorDialog("Failed to create directory!", "The directory could not be created! Maybe you're missing the required permissions?!");
			else {
				refresh();
				getSelectedFiles().clear();
				getSelectedFiles().add(directory);
			}
		}
	}

	@FXML
	private void renameButtonClicked(final ActionEvent event) {
		final File selectedFile = getSelectedFile();
		final File parent = selectedFile.getParentFile();

		final String newName = showCreateOrRenameDialog(
				"Rename",
				"What should be the new name?",
				parent, selectedFile.getName(),
				(name) -> ! selectedFile.getName().equals(name) && ! createFile(parent, name).exists() );

		if (newName != null) {
			final File newFile = createFile(parent, newName);
			if (!selectedFile.renameTo(newFile))
				showErrorDialog("Failed to rename file!", "The file could not be renamed! Maybe you're missing the required permissions?!");
			else {
				refresh();
				getSelectedFiles().clear();
				getSelectedFiles().add(newFile);
			}
		}
	}

	@FXML
	private void deleteButtonClicked(final ActionEvent event) {
		final Set<File> selectedFiles = getSelectedFiles();
		if (selectedFiles.isEmpty())
			return;

		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Delete");
		alert.setHeaderText("Delete these files?");

		final VBox contentContainer = new VBox();
		contentContainer.setSpacing(8);

		final Text contentText = new Text("The following files and folders are about to be deleted (folders recursively!):");
		contentText.setWrappingWidth(400);
		contentContainer.getChildren().add(contentText);

		final ListView<String> fileListView = new ListView<>();
		for (final File file : selectedFiles)
			fileListView.getItems().add(file.getAbsolutePath());

		fileListView.setPrefSize(400, 200);
		contentContainer.getChildren().add(fileListView);

		alert.getDialogPane().setContent(contentContainer);

		if (alert.showAndWait().get() == ButtonType.OK) {
			final List<File> notDeletedFiles = new ArrayList<>();
			for (final File file : selectedFiles) {
				file.deleteRecursively();
				if (file.exists())
					notDeletedFiles.add(file);
			}
			refresh();

			if (! notDeletedFiles.isEmpty())
				showErrorDialog("Deleting failed!", "The selected files (or directories) could be not deleted. They may have been deleted partially, though.");
		}
	}

	private String showCreateOrRenameDialog(final String title, final String headerText,
			final File parent, final String name, final NameVerifier nameVerifier) {
		final Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setHeaderText(headerText);

		final GridPane contentContainer = new GridPane();
		contentContainer.setPadding(new Insets(8));
		contentContainer.setHgap(8);
		contentContainer.setVgap(8);

		contentContainer.add(new Label("Parent:"), 0, 0);
		final TextField parentTextField = new TextField();
		parentTextField.setEditable(false);

		parentTextField.setText(parent.getAbsolutePath());
		contentContainer.add(parentTextField, 1, 0);

		contentContainer.add(new Label("Name:"), 0, 1);

		final TextField dirNameTextField = new TextField();
		dirNameTextField.setText(name);
		GridPane.setHgrow(dirNameTextField, Priority.ALWAYS);
		contentContainer.add(dirNameTextField, 1, 1);

		final InvalidationListener updateDisableInvalidationListener = (observable) -> {
			final String dirName = dirNameTextField.getText();
			final Node okButton = alert.getDialogPane().lookupButton(ButtonType.OK);
			if (isEmpty(dirName))
				okButton.setDisable(true);
			else {
				final boolean nameAcceptable = nameVerifier.isNameAcceptable(dirName);
				okButton.setDisable(! nameAcceptable);
			}
		};
		dirNameTextField.textProperty().addListener(updateDisableInvalidationListener);

		alert.getDialogPane().setContent(contentContainer);
		alert.setOnShowing((event) -> {
			dirNameTextField.requestFocus();
			dirNameTextField.selectAll();
			updateDisableInvalidationListener.invalidated(null);
		});

		if (alert.showAndWait().get() == ButtonType.OK)
			return dirNameTextField.getText();
		else
			return null;
	}

	private void showErrorDialog(final String headerText, final String contentText) {
		final Alert alert = new Alert(AlertType.ERROR);
//		alert.setTitle("Error");
		alert.setHeaderText(headerText);
		alert.setContentText(contentText);
		alert.showAndWait();
	}

	@FunctionalInterface
	private static interface NameVerifier {
		boolean isNameAcceptable(String name);
	}

	private File getSelectedFile() {
		final Iterator<File> iterator = getSelectedFiles().iterator();
		if (! iterator.hasNext())
			return null; // nothing selected

		final File file = iterator.next();

		if (iterator.hasNext())
			return null; // more than one selected.
		else
			return file;
	}

	private File getSelectedDirectory() {
		File directory = getSelectedFile();

		if (directory != null && ! directory.isDirectory())
			directory = directory.getParentFile();

		return directory;
	}

	protected TreeTableColumn<FileTreeItem<?>, String> getNameTreeTableColumn() {
		return nameTreeTableColumn;
	}

	protected TreeTableColumn<FileTreeItem<?>, String> getSizeTreeTableColumn() {
		return sizeTreeTableColumn;
	}

	protected TreeTableColumn<FileTreeItem<?>, String> getLastModifiedTreeTableColumn() {
		return lastModifiedTreeTableColumn;
	}
}
