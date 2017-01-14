package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static javafx.application.Platform.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.LocalRepoRegistry;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.localrepolist.LocalRepoListPane;
import org.subshare.gui.ls.LocalRepoRegistryLs;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class LocalRepoListMainTreeItem extends MainTreeItem<String> {

	private static final Image icon = new Image(ServerListMainTreeItem.class.getResource("local-repo-list_16x16.png").toExternalForm());
	private LocalRepoRegistry localRepoRegistry;

	private PropertyChangeListener localReposPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			@SuppressWarnings("unchecked")
			final Set<LocalRepo> localRepos = new LinkedHashSet<LocalRepo>((List<LocalRepo>) evt.getNewValue());
			runLater(new Runnable() {
				@Override
				public void run() {
					addOrRemoveTreeItemsViewCallback(localRepos);
				}
			});
		}
	};

	public LocalRepoListMainTreeItem() {
		super("Local repositories");
		setGraphic(new ImageView(icon));

		new Service<List<LocalRepo>>() {
			@Override
			protected Task<List<LocalRepo>> createTask() {
				return new SsTask<List<LocalRepo>>() {
					@Override
					protected List<LocalRepo> call() throws Exception {
						final LocalRepoRegistry localRepoRegistry = getLocalRepoRegistry();
						return localRepoRegistry.getLocalRepos();
					}

					@Override
					protected void succeeded() {
						final List<LocalRepo> localRepos;
						try { localRepos = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						addTableItemsViewCallback(localRepos);
						super.succeeded();
					}
				};
			}
		}.start();
	}

	protected synchronized LocalRepoRegistry getLocalRepoRegistry() {
		if (localRepoRegistry == null) {
			localRepoRegistry = LocalRepoRegistryLs.getLocalRepoRegistry();
//			localRepoRegistry.addPropertyChangeListener(LocalRepoRegistry.PropertyEnum.localRepos, localReposPropertyChangeListener);
			addWeakPropertyChangeListener(localRepoRegistry, LocalRepoRegistry.PropertyEnum.localRepos, localReposPropertyChangeListener);
		}
		return localRepoRegistry;
	}

	protected void addOrRemoveTreeItemsViewCallback(final Set<LocalRepo> localRepos) {
		assertNotNull(localRepos, "localRepos");
		final Map<LocalRepo, LocalRepoMainTreeItem> viewRepositoryId2LocalRepoMainTreeItem = new HashMap<>();
		for (final TreeItem<?> ti : getChildren()) {
			final LocalRepoMainTreeItem lrmti = (LocalRepoMainTreeItem) ti;
			viewRepositoryId2LocalRepoMainTreeItem.put(lrmti.getValueObject(), lrmti);
		}

		for (final LocalRepo localRepo : localRepos) {
			if (! viewRepositoryId2LocalRepoMainTreeItem.containsKey(localRepo)) {
				final LocalRepoMainTreeItem lrmti = new LocalRepoMainTreeItem(localRepo);
				viewRepositoryId2LocalRepoMainTreeItem.put(localRepo, lrmti);
				getChildren().add(lrmti);
			}
		}

		if (localRepos.size() < viewRepositoryId2LocalRepoMainTreeItem.size()) {
			for (final LocalRepo localRepo : localRepos)
				viewRepositoryId2LocalRepoMainTreeItem.remove(localRepo);

			for (final LocalRepoMainTreeItem lrmti : viewRepositoryId2LocalRepoMainTreeItem.values())
				getChildren().remove(lrmti);
		}
	}

	private void addTableItemsViewCallback(final Collection<LocalRepo> localRepos) {
		assertNotNull(localRepos, "localRepos");
		for (final LocalRepo localRepo : localRepos)
			getChildren().add(new LocalRepoMainTreeItem(localRepo));
	}

	@Override
	protected void finalize() throws Throwable {
//		final LocalRepoRegistry localRepoRegistry = this.localRepoRegistry;
//		if (localRepoRegistry != null)
//			localRepoRegistry.removePropertyChangeListener(LocalRepoRegistry.PropertyEnum.localRepos, localReposPropertyChangeListener);

		super.finalize();
	}

	@Override
	protected Parent createMainDetailContent() {
		return new LocalRepoListPane();
	}
}
