package org.subshare.gui.filetree.repoaware;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.subshare.core.dto.CollisionDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.local.CollisionFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.gui.filetree.FileFileTreeItem;
import org.subshare.gui.filetree.FileTreeItem;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;

import co.codewizards.cloudstore.core.collection.WeakIdentityHashMap;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

public class RepoAwareFileTreePane extends FileTreePane {

	private LocalRepo localRepo;
	private static Image collisionUnresolvedIcon;
	private static Image collisionUnresolvedInChildIcon;

	private final WeakIdentityHashMap<FileTreeItem<?>, ImageView> treeItem2CollisionIconImageView = new WeakIdentityHashMap<>();

	// *Important*
	//
	// (1) This TreeTableCell only shows *additional* icons, i.e. not the one that the TreeItem itself
	// already declares as its primary icon.
	//
	// (2) The table is virtual, i.e. the association between treeTableCell.treeTableRow and FileTreeItem
	// might change! It is therefore important in asynchronous operations to check, if the association is
	// still the same!
	private final Callback<TreeTableColumn<FileTreeItem<?>, String>, TreeTableCell<FileTreeItem<?>, String>> nameColumnCellFactory = new Callback<TreeTableColumn<FileTreeItem<?>, String>, TreeTableCell<FileTreeItem<?>, String>>() {
		@Override
		public TreeTableCell<FileTreeItem<?>, String> call(TreeTableColumn<FileTreeItem<?>, String> param) {
			return new TreeTableCell<FileTreeItem<?>, String>() {
				@Override
				protected void updateItem(String value, boolean empty) {
					super.updateItem(value, empty);

					final FileTreeItem<?> treeItem = getTreeTableRow().getItem();

					if (value == null || treeItem == null || empty) {
						setText(null);
						setGraphic(null);
					} else {
						setText(value);
						setGraphic(treeItem2CollisionIconImageView.get(treeItem));
						updateGraphicAsync(this, treeItem);
					}
				}

			};
		}
	};

	private void updateGraphicAsync(final TreeTableCell<FileTreeItem<?>, String> treeTableCell, final FileTreeItem<?> treeItem) {
		new Service<Image>() {
			@Override
			protected Task<Image> createTask() {
				return new Task<Image>() {
					@Override
					protected Image call() throws Exception {
						return _getCollisionIcon(treeItem);
					}

					@Override
					protected void succeeded() {
						final Image icon;
						try { icon = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						final ImageView iconImageView = icon == null ? null : new ImageView(icon);

						if (iconImageView == null)
							treeItem2CollisionIconImageView.remove(treeItem);
						else
							treeItem2CollisionIconImageView.put(treeItem, iconImageView);

						final FileTreeItem<?> currentTreeItem = treeTableCell.getTreeTableRow().getItem();
						if (currentTreeItem == treeItem)
							treeTableCell.setGraphic(iconImageView);
					}
				};
			}
		}.start();
	}

	public RepoAwareFileTreePane() {
		getNameTreeTableColumn().setCellFactory(nameColumnCellFactory);
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}
	public void setLocalRepo(LocalRepo localRepo) {
		this.localRepo = localRepo;
	}

	private Image _getCollisionIcon(final FileTreeItem<?> treeItem) {
		assertNotNull("treeItem", treeItem);

		if (! (treeItem instanceof FileFileTreeItem))
			return null;

		synchronized (RepoAwareFileTreePane.class) {
			final FileFileTreeItem ffti = (FileFileTreeItem) treeItem;
			final String localPath = localRepo.getLocalPath(ffti.getFile());

			try (final LocalRepoManager localRepoManager = createLocalRepoManager()) {
				final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
				final CollisionFilter filter = new CollisionFilter();
				filter.setLocalPath(localPath);
				filter.setResolved(false);

				filter.setIncludeChildrenRecursively(true);
				Collection<CollisionDto> collisionDtos = localRepoMetaData.getCollisionDtos(filter);
				if (! collisionDtos.isEmpty()) {
					// There is a collision either directly associated or somewhere in the sub-tree.
					// => Query again to find out, if it is directly associated.
					filter.setIncludeChildrenRecursively(false);
					collisionDtos = localRepoMetaData.getCollisionDtos(filter);
					if (collisionDtos.isEmpty()) // not found => not directly associated => in child (sub-tree)
						return getCollisionUnresolvedInChildIcon();
					else
						return getCollisionUnresolvedIcon();
				}
			}
			return null;
		}
	}

	public static Image getCollisionUnresolvedIcon() {
		if (collisionUnresolvedIcon == null) {
			final String fileName = "collision-unresolved_16x16.png"; //$NON-NLS-1$;
			final URL url = RepoAwareFileTreePane.class.getResource(fileName);
			if (url == null)
				throw new IllegalArgumentException(String.format("Resource '%s' not found!", fileName));

			collisionUnresolvedIcon = new Image(url.toExternalForm());
		}
		return collisionUnresolvedIcon;
	}

	public static Image getCollisionUnresolvedInChildIcon() {
		if (collisionUnresolvedInChildIcon == null) {
			final String fileName = "collision-unresolved-child_16x16.png"; //$NON-NLS-1$;
			final URL url = RepoAwareFileTreePane.class.getResource(fileName);
			if (url == null)
				throw new IllegalArgumentException(String.format("Resource '%s' not found!", fileName));

			collisionUnresolvedInChildIcon = new Image(url.toExternalForm());
		}
		return collisionUnresolvedInChildIcon;
	}

	private LocalRepoManager createLocalRepoManager() {
		final LocalRepo localRepo = assertNotNull("localRepo", getLocalRepo());
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}
}
