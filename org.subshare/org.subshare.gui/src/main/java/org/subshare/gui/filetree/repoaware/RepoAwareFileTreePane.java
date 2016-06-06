package org.subshare.gui.filetree.repoaware;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.CollectionUtil.*;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.subshare.core.dto.CollisionDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.local.CollisionFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.gui.filetree.FileFileTreeItem;
import org.subshare.gui.filetree.FileTreeItem;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class RepoAwareFileTreePane extends FileTreePane {

	private LocalRepo localRepo;
	private Image collisionUnresolvedIcon;
	private Image collisionUnresolvedInChildIcon;

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

						final Node treeItemGraphic = treeItem.getGraphic();

						final List<Image> additionalIcons = asListWithoutNullElements(
								getCollisionIcon(treeItem)
								);

						if (! additionalIcons.isEmpty()) {
							final HBox box = new HBox();
							box.getChildren().add(treeItemGraphic);

							for (final Image icon : additionalIcons)
								box.getChildren().add(new ImageView(icon));

							setGraphic(box);
						}
						else
							setGraphic(null);
					}
				}

			};
		}
	};

	public RepoAwareFileTreePane() {
		getNameTreeTableColumn().setCellFactory(nameColumnCellFactory);
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}
	public void setLocalRepo(LocalRepo localRepo) {
		this.localRepo = localRepo;
	}

	private Image getCollisionIcon(final FileTreeItem<?> treeItem) {
		assertNotNull("treeItem", treeItem);

		if (! (treeItem instanceof FileFileTreeItem))
			return null;

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

				filter.setIncludeChildrenRecursively(false);
				collisionDtos = localRepoMetaData.getCollisionDtos(filter);
				if (collisionDtos.isEmpty())
					return getCollisionUnresolvedInChildIcon();
				else
					return getCollisionUnresolvedIcon();
			}
		}
		return null;
	}

	private Image getCollisionUnresolvedIcon() {
		if (collisionUnresolvedIcon == null) {
			final String fileName = "collision-unresolved_16x16.png"; //$NON-NLS-1$;
			final URL url = RepoAwareFileTreePane.class.getResource(fileName);
			if (url == null)
				throw new IllegalArgumentException(String.format("Resource '%s' not found!", fileName));

			collisionUnresolvedIcon = new Image(url.toExternalForm());
		}
		return collisionUnresolvedIcon;
	}

	public Image getCollisionUnresolvedInChildIcon() {
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
