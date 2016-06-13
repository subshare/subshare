package org.subshare.gui.filetree.repoaware;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.local.CollisionPrivateFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.gui.concurrent.SsTask;
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

	private final WeakIdentityHashMap<FileTreeItem<?>, CollisionPrivateDtoSet> treeItem2CollisionDtoSet = new WeakIdentityHashMap<>();
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
		new Service<CollisionPrivateDtoSet>() {
			@Override
			protected Task<CollisionPrivateDtoSet> createTask() {
				return new SsTask<CollisionPrivateDtoSet>() {
					@Override
					protected CollisionPrivateDtoSet call() throws Exception {
						if (localRepo == null)
							return new CollisionPrivateDtoSet(Collections.emptyList(), Collections.emptyList());

						return _getCollisionDtoSet(treeItem);
					}

					@Override
					protected void succeeded() {
						final CollisionPrivateDtoSet collisionPrivateDtoSet;
						try { collisionPrivateDtoSet = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }

						if (collisionPrivateDtoSet == null)
							treeItem2CollisionDtoSet.remove(treeItem);
						else
							treeItem2CollisionDtoSet.put(treeItem, collisionPrivateDtoSet);

						final Image icon = _getCollisionIcon(collisionPrivateDtoSet);
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

	public CollisionPrivateDtoSet getCollisionDtoSet(final FileTreeItem<?> treeItem) {
		assertNotNull("treeItem", treeItem);
		assertFxApplicationThread();
		return treeItem2CollisionDtoSet.get(treeItem);
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

	private Image _getCollisionIcon(final CollisionPrivateDtoSet collisionPrivateDtoSet) {
		if (collisionPrivateDtoSet == null)
			return null;

		if (collisionPrivateDtoSet.getAllCollisionPrivateDtos().isEmpty()) // none - neither directly associated nor in child.
			return null;

		if (collisionPrivateDtoSet.getDirectCollisionPrivateDtos().isEmpty()) // not directly associated, but in child (sub-tree)
			return getCollisionUnresolvedInChildIcon();

		return getCollisionUnresolvedIcon(); // directly associated.
	}

	private CollisionPrivateDtoSet _getCollisionDtoSet(final FileTreeItem<?> treeItem) {
		assertNotNull("treeItem", treeItem);

		if (! (treeItem instanceof FileFileTreeItem))
			return null;

		synchronized (RepoAwareFileTreePane.class) {
			final FileFileTreeItem ffti = (FileFileTreeItem) treeItem;
			final String localPath = localRepo.getLocalPath(ffti.getFile());

			try (final LocalRepoManager localRepoManager = createLocalRepoManager()) {
				final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
				final CollisionPrivateFilter filter = new CollisionPrivateFilter();
				filter.setLocalPath(localPath);
				filter.setResolved(false);

				filter.setIncludeChildrenRecursively(true);
				Collection<CollisionPrivateDto> allCollisionDtos = localRepoMetaData.getCollisionPrivateDtos(filter);
				if (allCollisionDtos.isEmpty())
					return new CollisionPrivateDtoSet(Collections.emptyList(), Collections.emptyList());

				// There is a collision either directly associated or somewhere in the sub-tree.
				// => Query again to find out, if it is directly associated.
				filter.setIncludeChildrenRecursively(false);
				Collection<CollisionPrivateDto> directCollisionDtos = localRepoMetaData.getCollisionPrivateDtos(filter);
				return new CollisionPrivateDtoSet(allCollisionDtos, directCollisionDtos);
			}
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

	@Override
	public void refresh() {
		treeItem2CollisionDtoSet.clear();
		treeItem2CollisionIconImageView.clear();
		super.refresh();
	}
}
