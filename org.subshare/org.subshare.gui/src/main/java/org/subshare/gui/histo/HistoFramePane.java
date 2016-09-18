package org.subshare.gui.histo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.CollectionUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.local.PlainHistoCryptoRepoFileFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.gui.IconSize;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.filetree.FileIconRegistry;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;

import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.SymlinkDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class HistoFramePane extends BorderPane {

	private LocalRepo localRepo;
	private String localPath;
	private boolean populatePending;
	private Image collisionUnresolvedIcon;
	private Image collisionResolvedIcon;

	private final ObjectProperty<Uid> histoFrameId = new SimpleObjectProperty<Uid>(this, "histoFrameId") {
		@Override
		public void set(final Uid newValue) {
			super.set(newValue);
			populateTreeTableViewAsync();
		}
	};

	public LocalRepo getLocalRepo() {
		return localRepo;
	}
	public void setLocalRepo(final LocalRepo localRepo) {
		assertFxApplicationThread();
		if (equal(this.localRepo, localRepo))
			return;

		this.localRepo = localRepo;
		populatePending = true;
		Platform.runLater(() -> postSetLocalRepoOrLocalPath());
	}

	public String getLocalPath() {
		return localPath;
	}
	public void setLocalPath(String localPath) {
		assertFxApplicationThread();
		if (equal(this.localPath, localPath))
			return;

		this.localPath = localPath;
		populatePending = true;
		Platform.runLater(() -> postSetLocalRepoOrLocalPath());
	}

	private void postSetLocalRepoOrLocalPath() {
		assertFxApplicationThread();
		if (populatePending) {
			populatePending = false;

			populateTreeTableViewAsync();
		}
	}

	@FXML
	private TreeTableView<HistoCryptoRepoFileTreeItem> treeTableView;

	@FXML
	private TreeTableColumn<HistoCryptoRepoFileTreeItem, String> nameTreeTableColumn;

	private PlainHistoCryptoRepoFileFilter filter;

	private final Map<PlainHistoCryptoRepoFileDto.Action, Image> action2ActionIcon = new HashMap<>();
	{
		for (final PlainHistoCryptoRepoFileDto.Action action : PlainHistoCryptoRepoFileDto.Action.values()) {
			final String imageName = "Action_" + action.name() + IconSize._16x16.name() + ".png";
			final URL url = HistoFramePane.class.getResource(imageName);
			if (url == null)
				throw new IllegalStateException("Resource not found: " + imageName);

			final Image image = new Image(url.toString());
			action2ActionIcon.put(action, image);
		}
	}

	private final Callback<TreeTableColumn<HistoCryptoRepoFileTreeItem, String>, TreeTableCell<HistoCryptoRepoFileTreeItem, String>> nameColumnCellFactory = new Callback<TreeTableColumn<HistoCryptoRepoFileTreeItem, String>, TreeTableCell<HistoCryptoRepoFileTreeItem, String>>() {
		@Override
		public TreeTableCell<HistoCryptoRepoFileTreeItem, String> call(TreeTableColumn<HistoCryptoRepoFileTreeItem, String> param) {
			return new TreeTableCell<HistoCryptoRepoFileTreeItem, String>() {
				@Override
				protected void updateItem(String value, boolean empty) {
					super.updateItem(value, empty);

					final HistoCryptoRepoFileTreeItem treeItem = getTreeTableRow().getItem();

					if (value == null || treeItem == null || empty) {
						setText(null);
						setGraphic(null);
					} else {
						setText(value);

						final List<Image> icons = asListWithoutNullElements(
								getFileIcon(treeItem),
								action2ActionIcon.get(treeItem.getPlainHistoCryptoRepoFileDto().getAction()),
								getCollisionIcon(treeItem)
								);

						if (icons.isEmpty())
							setGraphic(null);
						else if (icons.size() == 1)
							setGraphic(new ImageView(icons.get(0)));
						else {
							final HBox box = new HBox();
							for (final Image icon : icons)
								box.getChildren().add(new ImageView(icon));

							setGraphic(box);
						}
					}
				}
			};
		}
	};

	public HistoFramePane() {
		loadDynamicComponentFxml(HistoFramePane.class, this);
		nameTreeTableColumn.setCellFactory(nameColumnCellFactory);
		treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	public ObservableList<TreeItem<HistoCryptoRepoFileTreeItem>> getSelectedHistoCryptoRepoFileTreeItems() {
		return treeTableView.getSelectionModel().getSelectedItems();
	}

	public ObjectProperty<Uid> histoFrameIdProperty() {
		return histoFrameId;
	}
	public Uid getHistoFrameId() {
		return histoFrameId.get();
	}
	public void setHistoFrameId(Uid histoFrameId) {
		this.histoFrameId.set(histoFrameId);
	}

	private void populateTreeTableViewAsync() {
		final LocalRepo localRepo = getLocalRepo();
		final Uid histoFrameId = getHistoFrameId();

		treeTableView.setRoot(null);
		if (localRepo == null || histoFrameId == null)
			return;

		// TODO refactor to lazy-load tree-items when expanding?!

		filter = new PlainHistoCryptoRepoFileFilter();
		filter.setFillParents(true);
		filter.setHistoFrameId(histoFrameId);
		filter.setLocalPath(localPath);

		new Service<HistoCryptoRepoFileTreeItem>() {
			private final PlainHistoCryptoRepoFileFilter filter = HistoFramePane.this.filter;

			@Override
			protected Task<HistoCryptoRepoFileTreeItem> createTask() {
				return new SsTask<HistoCryptoRepoFileTreeItem>() {
					@Override
					protected HistoCryptoRepoFileTreeItem call() throws Exception {
						try (final LocalRepoManager localRepoManager = createLocalRepoManager()) {
							final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
							final Collection<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = localRepoMetaData.getPlainHistoCryptoRepoFileDtos(filter);
							return buildTree(plainHistoCryptoRepoFileDtos);
						}
					}

					@Override
					protected void succeeded() {
						if (filter != HistoFramePane.this.filter)
							return; // out-dated result (new invocation triggered, already)

						final HistoCryptoRepoFileTreeItem rootTreeItem;
						try { rootTreeItem = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						treeTableView.setRoot(rootTreeItem);
					}
				};
			}
		}.start();
	}

	private HistoCryptoRepoFileTreeItem.Root buildTree(final Collection<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos) {
		assertNotNull("plainHistoCryptoRepoFileDtos", plainHistoCryptoRepoFileDtos);

		final HistoCryptoRepoFileTreeItem.Root root = new HistoCryptoRepoFileTreeItem.Root();

		final Map<Uid, HistoCryptoRepoFileTreeItem> cryptoRepoFileId2HistoCryptoRepoFileTreeItem =
				new HashMap<>(plainHistoCryptoRepoFileDtos.size());

		for (final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto : plainHistoCryptoRepoFileDtos) {
			final HistoCryptoRepoFileTreeItem item = new HistoCryptoRepoFileTreeItem(plainHistoCryptoRepoFileDto);
			cryptoRepoFileId2HistoCryptoRepoFileTreeItem.put(item.getCryptoRepoFileId(), item);
			item.setExpanded(true);
		}

		for (final HistoCryptoRepoFileTreeItem item : cryptoRepoFileId2HistoCryptoRepoFileTreeItem.values()) {
			final Uid parentCryptoRepoFileId = item.getPlainHistoCryptoRepoFileDto().getParentCryptoRepoFileId();
			if (parentCryptoRepoFileId == null)
				root.getChildren().add(item);
			else {
				final HistoCryptoRepoFileTreeItem parentItem = cryptoRepoFileId2HistoCryptoRepoFileTreeItem.get(parentCryptoRepoFileId);
				if (parentItem == null)
					throw new IllegalStateException("No parent with cryptoRepoFileId=" + parentCryptoRepoFileId);

				parentItem.getChildren().add(item);
			}
		}

		if (! isEmpty(localPath) && ! root.getChildren().isEmpty()) {
			HistoCryptoRepoFileTreeItem newRoot = root;
			for (String pathSegment : localPath.split("/")) {
				newRoot = newRoot.getChildOrFail(pathSegment);
			}
			root.getChildren().clear();
			root.getChildren().add(newRoot);
		}
		return root;
	}

	private LocalRepoManager createLocalRepoManager() {
		final LocalRepo localRepo = assertNotNull("localRepo", getLocalRepo());
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}

	private Image getCollisionIcon(final HistoCryptoRepoFileTreeItem treeItem) {
		assertNotNull("treeItem", treeItem);
		if (! treeItem.hasCollision())
			return null;

		if (treeItem.hasUnresolvedCollision())
			return getCollisionUnresolvedIcon();

		return getCollisionResolvedIcon();
	}

	private Image getFileIcon(final HistoCryptoRepoFileTreeItem treeItem) {
		assertNotNull("treeItem", treeItem);
		final String iconId;
		if (treeItem.getRepoFileDto() instanceof DirectoryDto)
			iconId = FileIconRegistry.ICON_ID_DIRECTORY;
		else if (treeItem.getRepoFileDto() instanceof NormalFileDto)
			iconId = FileIconRegistry.ICON_ID_FILE;
		else if (treeItem.getRepoFileDto() instanceof SymlinkDto)
			iconId = FileIconRegistry.ICON_ID_SYMLINK;
		else
			iconId = null;

		final Image fileIcon = iconId == null ? null : FileIconRegistry.getInstance().getIcon(iconId, IconSize._16x16);
		return fileIcon;
	}

	private Image getCollisionUnresolvedIcon() {
		if (collisionUnresolvedIcon == null) {
			final String fileName = "collision-unresolved_16x16.png"; //$NON-NLS-1$;
			final URL url = HistoFramePane.class.getResource(fileName);
			if (url == null)
				throw new IllegalArgumentException(String.format("Resource '%s' not found!", fileName));

			collisionUnresolvedIcon = new Image(url.toExternalForm());
		}
		return collisionUnresolvedIcon;
	}

	private Image getCollisionResolvedIcon() {
		if (collisionResolvedIcon == null) {
			final String fileName = "collision-resolved_16x16.png"; //$NON-NLS-1$;
			final URL url = HistoFramePane.class.getResource(fileName);
			if (url == null)
				throw new IllegalArgumentException(String.format("Resource '%s' not found!", fileName));

			collisionResolvedIcon = new Image(url.toExternalForm());
		}
		return collisionResolvedIcon;
	}
}
