package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.subshare.core.dto.CollisionDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.local.CollisionFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.gui.IconSize;
import org.subshare.gui.filetree.FileIconRegistry;
import org.subshare.gui.filetree.repoaware.RepoAwareFileTreePane;
import org.subshare.gui.localrepo.directory.LocalRepoDirectoryPane;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.oio.FileFilter;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class LocalRepoDirectoryMainTreeItem extends MainTreeItem<File> {

	private boolean childrenLoaded;
	private final ObjectProperty<Image> collisionIcon = new SimpleObjectProperty<>(this, "collisionIcon");

	private static final FileFilter directoryOnlyFileFilter = file
			-> file.isDirectory() && !LocalRepoManager.META_DIR_NAME.equals(file.getName());

	private static final Comparator<File> fileComparator = (o1, o2) -> o1.getName().compareTo(o2.getName());

	public LocalRepoDirectoryMainTreeItem(final File file) {
		super(assertNotNull("file", file),
				new ImageView(FileIconRegistry.getInstance().getIcon(file, IconSize._16x16)));

		collisionIcon.addListener((InvalidationListener) observable -> updateGraphic());
		Platform.runLater(() -> updateCollisionIcon());
	}

	public LocalRepo getLocalRepo() {
		final TreeItem<String> parent = getParent();
		if (parent == null)
			throw new IllegalStateException("parent == null");

		if (parent instanceof LocalRepoDirectoryMainTreeItem)
			return ((LocalRepoDirectoryMainTreeItem) parent).getLocalRepo();

		if (parent instanceof LocalRepoMainTreeItem)
			return ((LocalRepoMainTreeItem) parent).getLocalRepo();

		throw new IllegalStateException("parent is an instance of an unexpected type: " + parent.getClass().getName());
	}

	public File getFile() {
		return getValueObject();
	}

	@Override
	protected String getValueString() {
		final String fileName = getFile().getName();
		if (isEmpty(fileName)) // should never happen (who shares the root?!) but better handle anyway ;-)
			return getFile().getAbsolutePath();
		else
			return fileName;
	}

	@Override
	public ObservableList<TreeItem<String>> getChildren() {
		final ObservableList<TreeItem<String>> children = super.getChildren();
		if (! childrenLoaded) {
			childrenLoaded = true; // *must* be set before clear()/addAll(...), because of events being fired.
			final List<MainTreeItem<File>> c = loadChildren();
			if (c != null)
				children.addAll(c);
		}
		return children;
	}

	private List<MainTreeItem<File>> loadChildren() {
		final File file = getFile();
		final File[] childFiles = file.listFiles(directoryOnlyFileFilter);
		if (childFiles == null)
			return null;

		Arrays.sort(childFiles, fileComparator);
		final List<MainTreeItem<File>> result = new ArrayList<>(childFiles.length);
		for (final File childFile : childFiles)
			result.add(new LocalRepoDirectoryMainTreeItem(childFile));

		return result;
	}

	@Override
	public boolean isLeaf() { // TODO update this?! when? how?
		final File[] childFiles = getFile().listFiles(directoryOnlyFileFilter);
		return childFiles == null || childFiles.length == 0;
	}

	@Override
	protected Parent createMainDetailContent() {
		return new LocalRepoDirectoryPane(getLocalRepo(), getFile());
	}

	private void updateCollisionIcon() {
		new Service<Image>() {
			@Override
			protected Task<Image> createTask() {
				return new Task<Image>() {
					@Override
					protected Image call() throws Exception {
						return _getCollisionIcon();
					}

					@Override
					protected void succeeded() {
						final Image result;
						try { result = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						collisionIcon.set(result);
					}
				};
			}
		}.start();
	}

	private Image _getCollisionIcon() {
		synchronized (LocalRepoDirectoryMainTreeItem.class) {
			final LocalRepo localRepo = assertNotNull("localRepo", getLocalRepo());
			final File file = assertNotNull("file", getFile());
			final String localPath = localRepo.getLocalPath(file);

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
						return RepoAwareFileTreePane.getCollisionUnresolvedInChildIcon();
					else
						return RepoAwareFileTreePane.getCollisionUnresolvedIcon();
				}
			}
			return null;
		}
	}

	private void updateGraphic() {
		final ImageView fileIconImageView = new ImageView(FileIconRegistry.getInstance().getIcon(getFile(), IconSize._16x16));
		final Image collisionIcon = this.collisionIcon.get();

		if (collisionIcon == null)
			setGraphic(fileIconImageView);
		else {
			HBox box = new HBox();
			box.getChildren().add(fileIconImageView);
			box.getChildren().add(new ImageView(collisionIcon));
			setGraphic(box);
		}
	}

	private LocalRepoManager createLocalRepoManager() {
		final LocalRepo localRepo = assertNotNull("localRepo", getLocalRepo());
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}

}
