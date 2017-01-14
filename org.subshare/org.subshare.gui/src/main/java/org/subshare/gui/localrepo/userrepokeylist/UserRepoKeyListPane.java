package org.subshare.gui.localrepo.userrepokeylist;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static javafx.application.Platform.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.subshare.core.dto.DebugUserRepoKeyDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.listener.LocalRepoCommitEventListener;
import org.subshare.core.repo.listener.LocalRepoCommitEventManager;
import org.subshare.core.repo.listener.WeakLocalRepoCommitEventListener;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.LocalRepoCommitEventManagerLs;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

public class UserRepoKeyListPane extends GridPane {

	private LocalRepo localRepo;

	@FXML
	private TableView<UserRepoKeyListItem> tableView;

	private final LocalRepoCommitEventListener localRepoCommitEventListener; // Must not assign here! Causes error with xtext / E(fx)clipse

	private WeakLocalRepoCommitEventListener weakLocalRepoCommitEventListener;

	private static final Timer deferredUpdateUiTimer = new Timer(true);

	private TimerTask deferredUpdateUiTimerTask;

	public UserRepoKeyListPane() {
		loadDynamicComponentFxml(UserRepoKeyListPane.class, this);

		localRepoCommitEventListener = event -> {
			if (! event.getModifications().isEmpty())
				scheduleDeferredUpdateUiTimerTask();
		};
	}

	private void populateTableViewAsync() {
		if (getLocalRepo() == null)
			return;

		new Service<Collection<DebugUserRepoKeyDto>>() {
			@Override
			protected Task<Collection<DebugUserRepoKeyDto>> createTask() {
				return new SsTask<Collection<DebugUserRepoKeyDto>>() {
					@Override
					protected Collection<DebugUserRepoKeyDto> call() throws Exception {
						if (getLocalRepo() == null)
							return Collections.emptyList();

						try (final LocalRepoManager localRepoManager = createLocalRepoManager()) {
							final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
							return new ArrayList<>(localRepoMetaData.getDebugUserRepoKeyDtos());
						}
					}

					@Override
					protected void succeeded() {
						final Collection<DebugUserRepoKeyDto> dtos;
						try { dtos = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						addOrRemoveTableItemsViewCallback(dtos);
					}
				};
			}
		}.start();
	}

	private synchronized void scheduleDeferredUpdateUiTimerTask() {
		if (deferredUpdateUiTimerTask != null) {
			deferredUpdateUiTimerTask.cancel();
			deferredUpdateUiTimerTask = null;
		}

		deferredUpdateUiTimerTask = new TimerTask() {
			@Override
			public void run() {
				synchronized (UserRepoKeyListPane.this) {
					deferredUpdateUiTimerTask = null;
				}
				runLater(() -> populateTableViewAsync());
			}
		};

		deferredUpdateUiTimer.schedule(deferredUpdateUiTimerTask, 500);
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}

	public void setLocalRepo(LocalRepo localRepo) {
		assertFxApplicationThread();
		if (equal(this.localRepo, localRepo))
			return;

		if (weakLocalRepoCommitEventListener != null) {
			weakLocalRepoCommitEventListener.removeLocalRepoCommitEventListener();
			weakLocalRepoCommitEventListener = null;
		}

		this.localRepo = localRepo;

		if (localRepo != null) {
			final UUID localRepositoryId = localRepo.getRepositoryId();
			final LocalRepoCommitEventManager localRepoCommitEventManager = LocalRepoCommitEventManagerLs.getLocalRepoCommitEventManager();
			weakLocalRepoCommitEventListener = new WeakLocalRepoCommitEventListener(localRepoCommitEventManager, localRepositoryId, localRepoCommitEventListener);
			weakLocalRepoCommitEventListener.addLocalRepoCommitEventListener();
		}

		populateTableViewAsync();
	}

	private LocalRepoManager createLocalRepoManager() {
		final LocalRepo localRepo = assertNotNull(getLocalRepo(), "localRepo");
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}

	private void addOrRemoveTableItemsViewCallback(final Collection<DebugUserRepoKeyDto> dtos) {
		assertNotNull(dtos, "dtos");
		final Map<DebugUserRepoKeyDto, UserRepoKeyListItem> viewDto2ListItem = new HashMap<>();
		for (final UserRepoKeyListItem li : tableView.getItems())
			viewDto2ListItem.put(li.getDebugUserRepoKeyDto(), li);

		for (final DebugUserRepoKeyDto dto : dtos) {
			if (! viewDto2ListItem.containsKey(dto)) {
				final UserRepoKeyListItem li = new UserRepoKeyListItem(dto);
				viewDto2ListItem.put(dto, li);
				tableView.getItems().add(li);
			}
		}

		if (dtos.size() < viewDto2ListItem.size()) {
			for (final DebugUserRepoKeyDto dto : dtos)
				viewDto2ListItem.remove(dto);

			for (final UserRepoKeyListItem li : viewDto2ListItem.values())
				tableView.getItems().remove(li);
		}

//		tableView.requestLayout();
	}
}
