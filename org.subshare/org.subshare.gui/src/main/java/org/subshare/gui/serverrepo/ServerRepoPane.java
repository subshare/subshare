package org.subshare.gui.serverrepo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.server.Server;
import org.subshare.gui.checkout.CheckOutWizard;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;
import org.subshare.gui.ls.MetaOnlyRepoManagerLs;
import org.subshare.gui.ls.MetaOnlyRepoSyncDaemonLs;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class ServerRepoPane extends GridPane {
	private final Server server;
	private final ServerRepo serverRepo;

	@FXML
	private TextField nameTextField;

	private final StringProperty nameProperty;

	public ServerRepoPane(final Server server, final ServerRepo serverRepo) {
		this.server = assertNotNull(server, "server");
		this.serverRepo = assertNotNull(serverRepo, "serverRepo");
		loadDynamicComponentFxml(ServerRepoPane.class, this);

		try {
			nameProperty = new JavaBeanStringPropertyBuilder().bean(serverRepo).name(ServerRepo.PropertyEnum.name.name()).build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		nameTextField.textProperty().bindBidirectional(nameProperty);
	}

	@FXML
	private void checkOutButtonClicked(final ActionEvent event) {
		new CheckOutWizard(server, serverRepo).checkOut(getScene().getWindow());
	}

	@FXML
	private void redownMetaButtonClicked(final ActionEvent event) {
		try (final LocalRepoManager localRepoManager = createLocalRepoManager()) {
			final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
			localRepoMetaData.resetLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced();
		}
		MetaOnlyRepoSyncDaemonLs.getMetaOnlyRepoSyncDaemon().sync();
	}

	private LocalRepoManager createLocalRepoManager() {
		File localRoot = MetaOnlyRepoManagerLs.getMetaOnlyRepoManager().getLocalRoot(serverRepo);
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRoot);
	}
}
