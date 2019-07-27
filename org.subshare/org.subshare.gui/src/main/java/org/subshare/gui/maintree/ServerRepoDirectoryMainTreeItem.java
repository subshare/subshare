package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;
import static javafx.application.Platform.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.listener.LocalRepoCommitEventListener;
import org.subshare.core.repo.listener.LocalRepoCommitEventManager;
import org.subshare.core.repo.listener.WeakLocalRepoCommitEventListener;
import org.subshare.core.repo.metaonly.ServerRepoFile;
import org.subshare.core.repo.metaonly.ServerRepoFileType;
import org.subshare.core.server.Server;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.LocalRepoCommitEventManagerLs;
import org.subshare.gui.serverrepo.directory.ServerRepoDirectoryPane;

import co.codewizards.cloudstore.core.collection.ListMerger;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ServerRepoDirectoryMainTreeItem extends MainTreeItem<ServerRepoFile> {

	private static final Image icon = new Image(ServerRepoDirectoryMainTreeItem.class.getResource("server-repo-directory_16x16.png").toExternalForm());

	private boolean childrenLoaded;

	private final LocalRepoCommitEventListener localRepoCommitEventListener = event -> scheduleDeferredGetChildrenTimerTask();

	private final WeakLocalRepoCommitEventListener weakLocalRepoCommitEventListener;

	private static final Timer deferredGetChildrenTimer = new Timer(true);

	private TimerTask deferredGetChildrenTimerTask;

	public ServerRepoDirectoryMainTreeItem(final ServerRepoFile serverRepoFile) {
		super(requireNonNull(serverRepoFile, "serverRepoFile"));
		setGraphic(new ImageView(icon));
		final UUID localRepositoryId = serverRepoFile.getLocalRepositoryId();

		final LocalRepoCommitEventManager localRepoCommitEventManager = LocalRepoCommitEventManagerLs.getLocalRepoCommitEventManager();
		weakLocalRepoCommitEventListener = new WeakLocalRepoCommitEventListener(localRepoCommitEventManager, localRepositoryId, localRepoCommitEventListener);
		weakLocalRepoCommitEventListener.addLocalRepoCommitEventListener();
	}

	private synchronized void scheduleDeferredGetChildrenTimerTask() {
		if (deferredGetChildrenTimerTask != null) {
			deferredGetChildrenTimerTask.cancel();
			deferredGetChildrenTimerTask = null;
		}

		deferredGetChildrenTimerTask = new TimerTask() {
			@Override
			public void run() {
				synchronized (ServerRepoDirectoryMainTreeItem.this) {
					deferredGetChildrenTimerTask = null;
				}
				runLater(() -> {
					if (childrenLoaded) { // only *re*load, if it already was loaded before
						childrenLoaded = false;
						if (isExpanded()) // only reload, if currently expanded. otherwise defer until next expansion.
							getChildren();
					}
				});
			}
		};

		deferredGetChildrenTimer.schedule(deferredGetChildrenTimerTask, 500);
	}

	public Server getServer() {
		return getServerRepoFile().getServer();
	}

	@Override
	protected String getValueString() {
		final String localName = getServerRepoFile().getLocalName();
		return isEmpty(localName) ? "/" : localName;
	}

	public ServerRepoFile getServerRepoFile() {
		return getValueObject();
	}

	public ServerRepo getServerRepo() {
		final TreeItem<String> parent = getParent();
		if (parent == null)
			throw new IllegalStateException("parent == null");

		if (parent instanceof ServerRepoDirectoryMainTreeItem)
			return ((ServerRepoDirectoryMainTreeItem) parent).getServerRepo();

		if (parent instanceof ServerRepoMainTreeItem)
			return ((ServerRepoMainTreeItem) parent).getServerRepo();

		throw new IllegalStateException("parent is an instance of an unexpected type: " + parent.getClass().getName());
	}

	@Override
	public ObservableList<TreeItem<String>> getChildren() {
		final ObservableList<TreeItem<String>> children = super.getChildren();
		if (! childrenLoaded) {
			childrenLoaded = true; // *must* be set before clear()/addAll(...), because of events being fired.

			new Service<List<TreeItem<String>>>() {
				@Override
				protected Task<List<TreeItem<String>>> createTask() {
					return new SsTask<List<TreeItem<String>>>() {
						@Override
						protected List<TreeItem<String>> call() throws Exception {
							return loadChildren();
						}

						@Override
						protected void succeeded() {
							final List<TreeItem<String>> c;
							try { c = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
							new ChildrenListMerger().merge(c, children);
						}
					};
				}
			}.start();
		}
		return children;
	}

	private class ChildrenListMerger extends ListMerger<TreeItem<String>, Long> {
		@Override
		protected Long getKey(TreeItem<String> element) {
			final ServerRepoDirectoryMainTreeItem item = (ServerRepoDirectoryMainTreeItem) element;
			final ServerRepoFile serverRepoFile = item.getServerRepoFile();
			return serverRepoFile.getRepoFileId();
		}

		@Override
		protected void update(List<TreeItem<String>> dest, int index, TreeItem<String> sourceElement, TreeItem<String> destElement) {
			final ServerRepoDirectoryMainTreeItem sourceItem = (ServerRepoDirectoryMainTreeItem) sourceElement;
			final ServerRepoDirectoryMainTreeItem destItem = (ServerRepoDirectoryMainTreeItem) destElement;
			destItem.setValueObject(sourceItem.getValueObject());
		}
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	private List<TreeItem<String>> loadChildren() {
		final List<ServerRepoFile> childServerRepoFiles = getServerRepoFile().getChildren();
		if (childServerRepoFiles == null)
			return null;

		final List<ServerRepoFile> filtered = new ArrayList<ServerRepoFile>(childServerRepoFiles.size());
		for (ServerRepoFile childServerRepoFile : childServerRepoFiles) {
			if (childServerRepoFile.getType() == ServerRepoFileType.DIRECTORY)
				filtered.add(childServerRepoFile);
		}

		Collections.sort(filtered, (o1, o2) -> o1.getLocalName().compareTo(o2.getLocalName()));

		final ArrayList<TreeItem<String>> result = new ArrayList<>(filtered.size());
		for (final ServerRepoFile childServerRepoFile : filtered)
				result.add(new ServerRepoDirectoryMainTreeItem(childServerRepoFile));

		return result;
	}

	@Override
	protected Parent createMainDetailContent() {
		return new ServerRepoDirectoryPane(getServerRepoFile());
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
}
