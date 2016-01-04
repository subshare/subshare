package org.subshare.gui.histo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.local.HistoFrameFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class HistoFrameListPane extends VBox {

	private LocalRepo localRepo;

	@FXML
	private TableView<HistoFrameListItem> tableView;

	private HistoFrameFilter filter = new HistoFrameFilter();
	{
		filter.setMaxResultSize(-1);
	}

	public HistoFrameListPane() {
		loadDynamicComponentFxml(HistoFrameListPane.class, this);
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}
	public void setLocalRepo(final LocalRepo localRepo) {
		this.localRepo = localRepo;
		tableView.getItems().clear();

		if (localRepo != null)
			populateTableViewAsync();
	}

	private void populateTableViewAsync() {
		new Service<List<HistoFrameDto>>() {
			@Override
			protected Task<List<HistoFrameDto>> createTask() {
				return new SsTask<List<HistoFrameDto>>() {
					@Override
					protected List<HistoFrameDto> call() throws Exception {
						try (final LocalRepoManager localRepoManager = createLocalRepoManager(localRepo);) {
							final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
							final List<HistoFrameDto> histoFrameDtos = new ArrayList<>(localRepoMetaData.getHistoFrameDtos(filter));
							sortHistoFrameDtosBySignatureCreatedNewestFirst(histoFrameDtos);
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

	private void addTableItemsViewCallback(List<HistoFrameDto> histoFrameDtos) {
		for (final HistoFrameDto histoFrameDto : histoFrameDtos) {
			final HistoFrameListItem userListItem = new HistoFrameListItem(histoFrameDto);
			tableView.getItems().add(userListItem);
		}
		tableView.requestLayout();
	}

	private LocalRepoManager createLocalRepoManager(final LocalRepo localRepo) {
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}

}
