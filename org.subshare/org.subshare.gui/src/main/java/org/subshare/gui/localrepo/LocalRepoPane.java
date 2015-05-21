package org.subshare.gui.localrepo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.ls.RepoSyncDaemonLs;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;

public class LocalRepoPane extends GridPane {
	private final LocalRepo localRepo;

	@FXML
	private TextField nameTextField;

	@FXML
	private TextField localRootTextField;

	private JavaBeanStringProperty nameProperty;

	public LocalRepoPane(final LocalRepo localRepo) {
		this.localRepo = assertNotNull("localRepo", localRepo);
		loadDynamicComponentFxml(LocalRepoPane.class, this);
		bind();
	}

	private void bind() {
		try {
			// nameProperty must be kept as field to prevent garbage-collection!
			nameProperty = JavaBeanStringPropertyBuilder.create()
				    .bean(localRepo)
				    .name(LocalRepo.PropertyEnum.name.name())
				    .build();
			nameTextField.textProperty().bindBidirectional(nameProperty);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	private void syncButtonClicked(final ActionEvent event) {
		final File localRoot = localRepo.getLocalRoot();
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(localRoot);
	}

}
