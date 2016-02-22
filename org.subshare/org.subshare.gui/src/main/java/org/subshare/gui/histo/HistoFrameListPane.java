package org.subshare.gui.histo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static javafx.application.Platform.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.listener.LocalRepoCommitEventListener;
import org.subshare.core.repo.listener.LocalRepoCommitEventManager;
import org.subshare.core.repo.listener.WeakLocalRepoCommitEventListener;
import org.subshare.core.repo.local.HistoFrameFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.user.User;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.LocalRepoCommitEventManagerLs;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;
import org.subshare.gui.ls.UserRegistryLs;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class HistoFrameListPane extends VBox {

	private LocalRepo localRepo;

	@FXML
	private TableView<HistoFrameListItem> tableView;

	@FXML
	private TableColumn<HistoFrameListItem, Date> signatureCreatedColumn;

	private final Map<Uid, String> userRepoKey2UserName = Collections.synchronizedMap(new HashMap<>());

	private HistoFrameFilter filter = new HistoFrameFilter();
	{
		filter.setMaxResultSize(-1); // TODO add UI to edit filter!
	}

	private final LocalRepoCommitEventListener localRepoCommitEventListener = event -> scheduleDeferredUpdateUiTimerTask();

	private WeakLocalRepoCommitEventListener weakLocalRepoCommitEventListener;

	private static final Timer deferredUpdateUiTimer = new Timer(true);

	private TimerTask deferredUpdateUiTimerTask;

	private final Callback<TableColumn<HistoFrameListItem, Date>, TableCell<HistoFrameListItem, Date>> signatureCreateColumnCellFactory = new Callback<TableColumn<HistoFrameListItem, Date>, TableCell<HistoFrameListItem, Date>>() {
		private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

		@Override
		public TableCell<HistoFrameListItem, Date> call(TableColumn<HistoFrameListItem, Date> tableColumn) {
			return new TableCell<HistoFrameListItem, Date>() {
				@Override
				protected void updateItem(Date value, boolean empty) {
					super.updateItem(value, empty);

					if (value == null || empty) {
						setText(null);
					} else {
						setText(dateFormat.format(value));
					}
				}
			};
		}
	};

	public HistoFrameListPane() {
		loadDynamicComponentFxml(HistoFrameListPane.class, this);

		signatureCreatedColumn.setCellFactory(signatureCreateColumnCellFactory);
	}

	public ReadOnlyObjectProperty<HistoFrameListItem> selectedItemProperty() {
		return tableView.getSelectionModel().selectedItemProperty();
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}
	public void setLocalRepo(final LocalRepo localRepo) {
		assertFxApplicationThread();

		if (weakLocalRepoCommitEventListener != null) {
			weakLocalRepoCommitEventListener.removeLocalRepoCommitEventListener();
			weakLocalRepoCommitEventListener = null;
		}

		this.localRepo = localRepo;
		tableView.getItems().clear();

		if (localRepo != null) {
			populateTableViewAsync();

			final UUID localRepositoryId = localRepo.getRepositoryId();
			final LocalRepoCommitEventManager localRepoCommitEventManager = LocalRepoCommitEventManagerLs.getLocalRepoCommitEventManager();
			weakLocalRepoCommitEventListener = new WeakLocalRepoCommitEventListener(localRepoCommitEventManager, localRepositoryId, localRepoCommitEventListener);
			weakLocalRepoCommitEventListener.addLocalRepoCommitEventListener();
		}
	}

	private synchronized void scheduleDeferredUpdateUiTimerTask() {
		if (deferredUpdateUiTimerTask != null) {
			deferredUpdateUiTimerTask.cancel();
			deferredUpdateUiTimerTask = null;
		}

		deferredUpdateUiTimerTask = new TimerTask() {
			@Override
			public void run() {
				synchronized (HistoFrameListPane.this) {
					deferredUpdateUiTimerTask = null;
				}
				runLater(() -> updateUi());
			}
		};

		deferredUpdateUiTimer.schedule(deferredUpdateUiTimerTask, 500);
	}

	private void updateUi() {
		// TODO we should improve this: update/reload only what's needed - not all!
		new Service<List<HistoFrameDto>>() {
			@Override
			protected Task<List<HistoFrameDto>> createTask() {
				return new SsTask<List<HistoFrameDto>>() {
					@Override
					protected List<HistoFrameDto> call() throws Exception {
						try (final LocalRepoManager localRepoManager = createLocalRepoManager()) {
							final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
							final List<HistoFrameDto> histoFrameDtos = new ArrayList<>(localRepoMetaData.getHistoFrameDtos(filter));
							sortHistoFrameDtosBySignatureCreatedNewestFirst(histoFrameDtos);

							// fill UserName cache on this service's thread
							for (final HistoFrameDto histoFrameDto : histoFrameDtos)
								getUserNameByUserRepoKeyId(histoFrameDto.getSignature().getSigningUserRepoKeyId());

							return histoFrameDtos;
						}
					}

					@Override
					protected void succeeded() {
						final List<HistoFrameDto> histoFrameDtos;
						try { histoFrameDtos = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }

						final List<HistoFrameListItem> oldSelectedItems = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());

						tableView.getItems().clear();
						Map<Uid, HistoFrameListItem> histoFrameId2Item = addTableItemsViewCallback(histoFrameDtos);

						for (final HistoFrameListItem oldItem : oldSelectedItems) {
							final HistoFrameListItem newItem = histoFrameId2Item.get(oldItem.getHistoFrameDto().getHistoFrameId());
							if (newItem != null)
								tableView.getSelectionModel().select(newItem);
						}
					}
				};
			}
		}.start();
	}

	private String getUserNameByUserRepoKeyId(final Uid userRepoKeyId) {
		assertNotNull("userRepoKeyId", userRepoKeyId);

		String userName = userRepoKey2UserName.get(userRepoKeyId);
		if (userName == null) {
			final User user = UserRegistryLs.getUserRegistry().getUserByUserRepoKeyId(userRepoKeyId);

			if (user == null)
				userName = String.format("<%s>", userRepoKeyId);
			else
				userName = getUserName(user);

			userRepoKey2UserName.put(userRepoKeyId, userName);
		}
		return userName;
	}

	private String getUserName(final User user) { // TODO should I move this into User? Or instead replace it by 3 separate columns?!
		assertNotNull("user", user);
		StringBuilder sb = new StringBuilder();

		final String firstName = trim(user.getFirstName());
		if (! isEmpty(firstName))
			sb.append(firstName);

		final String lastName = trim(user.getLastName());
		if (! isEmpty(lastName)) {
			if (sb.length() > 0)
				sb.append(' ');

			sb.append(lastName);
		}

		final List<String> emails = user.getEmails();
		if (! emails.isEmpty()) {
			final String email = trim(emails.get(0));
			if (! isEmpty(email)) {
				if (sb.length() > 0)
					sb.append(' ');

				sb.append('<').append(email).append('>');
			}
		}
		return sb.toString();
	}

	private void populateTableViewAsync() {
		new Service<List<HistoFrameDto>>() {
			@Override
			protected Task<List<HistoFrameDto>> createTask() {
				return new SsTask<List<HistoFrameDto>>() {
					@Override
					protected List<HistoFrameDto> call() throws Exception {
						try (final LocalRepoManager localRepoManager = createLocalRepoManager()) {
							final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
							final List<HistoFrameDto> histoFrameDtos = new ArrayList<>(localRepoMetaData.getHistoFrameDtos(filter));
							sortHistoFrameDtosBySignatureCreatedNewestFirst(histoFrameDtos);

							// fill UserName cache on this service's thread
							for (final HistoFrameDto histoFrameDto : histoFrameDtos)
								getUserNameByUserRepoKeyId(histoFrameDto.getSignature().getSigningUserRepoKeyId());

							return histoFrameDtos;
						}
					}

					@Override
					protected void succeeded() {
						final List<HistoFrameDto> histoFrameDtos;
						try { histoFrameDtos = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						addTableItemsViewCallback(histoFrameDtos);
					}
				};
			}
		}.start();
	}

	private void sortHistoFrameDtosBySignatureCreatedNewestFirst(final List<HistoFrameDto> histoFrameDtos) {
		Collections.sort(histoFrameDtos, (o1, o2) -> {
			final Date signatureCreated1 = assertNotNull("o1.signature", o1.getSignature()).getSignatureCreated();
			assertNotNull("o1.signature.signatureCreated", signatureCreated1);

			final Date signatureCreated2 = assertNotNull("o2.signature", o2.getSignature()).getSignatureCreated();
			assertNotNull("o2.signature.signatureCreated", signatureCreated2);

			return -1 * signatureCreated1.compareTo(signatureCreated2);
		});
	}

	private Map<Uid, HistoFrameListItem> addTableItemsViewCallback(final List<HistoFrameDto> histoFrameDtos) {
		final Map<Uid, HistoFrameListItem> histoFrameId2Item = new HashMap<>();
		for (final HistoFrameDto histoFrameDto : histoFrameDtos) {
			final String userName = getUserNameByUserRepoKeyId(histoFrameDto.getSignature().getSigningUserRepoKeyId());
			final HistoFrameListItem userListItem = new HistoFrameListItem(histoFrameDto, userName);
			histoFrameId2Item.put(userListItem.getHistoFrameDto().getHistoFrameId(), userListItem);
			tableView.getItems().add(userListItem);
		}
		tableView.requestLayout();
		return histoFrameId2Item;
	}

	private LocalRepoManager createLocalRepoManager() {
		final LocalRepo localRepo = assertNotNull("localRepo", getLocalRepo());
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}
}
