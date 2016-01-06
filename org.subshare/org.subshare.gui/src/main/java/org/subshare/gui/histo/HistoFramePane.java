package org.subshare.gui.histo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

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

public class HistoFramePane extends BorderPane {

	private LocalRepo localRepo;

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
		this.localRepo = localRepo;
		populateTreeTableViewAsync();
	}

	@FXML
	private TreeTableView<HistoCryptoRepoFileTreeItem> treeTableView;

	@FXML
	private TreeTableColumn<HistoCryptoRepoFileTreeItem, String> nameTreeTableColumn;

	private PlainHistoCryptoRepoFileFilter filter;

	private final Map<HistoCryptoRepoFileTreeItem.Action, Image> action2ActionIcon = new HashMap<>();
	{
		for (final HistoCryptoRepoFileTreeItem.Action action : HistoCryptoRepoFileTreeItem.Action.values()) {
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

						final String iconId;
						if (treeItem.getRepoFileDto() instanceof DirectoryDto)
							iconId = FileIconRegistry.ICON_ID_DIRECTORY;
						else if (treeItem.getRepoFileDto() instanceof NormalFileDto)
							iconId = FileIconRegistry.ICON_ID_FILE;
						else if (treeItem.getRepoFileDto() instanceof SymlinkDto)
							iconId = null; // TODO treat symlinks differently!
						else
							iconId = null;

						final Image fileIcon = iconId == null ? null : FileIconRegistry.getInstance().getIcon(iconId, IconSize._16x16);
						final Image actionIcon = action2ActionIcon.get(treeItem.getAction());
						if (fileIcon != null && actionIcon != null)
							setGraphic(new HBox(new ImageView(actionIcon), new ImageView(fileIcon)));
						else if (fileIcon != null)
							setGraphic(new ImageView(fileIcon));
						else if (actionIcon != null)
							setGraphic(new ImageView(actionIcon));
						else
							setGraphic(null);
					}
				}
			};
		}
	};

	public HistoFramePane() {
		loadDynamicComponentFxml(HistoFramePane.class, this);
		nameTreeTableColumn.setCellFactory(nameColumnCellFactory);
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
		return root;
	}

	private LocalRepoManager createLocalRepoManager() {
		final LocalRepo localRepo = assertNotNull("localRepo", getLocalRepo());
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}
}
