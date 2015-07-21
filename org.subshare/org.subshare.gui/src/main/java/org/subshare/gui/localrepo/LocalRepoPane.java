package org.subshare.gui.localrepo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.function.UnaryOperator;

import javafx.beans.binding.Bindings;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.GridPane;

import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.invitation.issue.IssueInvitationWizard;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.util.FileStringConverter;
import org.subshare.gui.wizard.WizardDialog;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;

public class LocalRepoPane extends GridPane {
	private final LocalRepo localRepo;

	@FXML
	private Button syncButton;

	@FXML
	private Button inviteButton;

	@FXML
	private TextField nameTextField;

	@FXML
	private TextField localRootTextField;

	private JavaBeanStringProperty nameProperty;

	private JavaBeanObjectProperty<File> localRootProperty;

	public LocalRepoPane(final LocalRepo localRepo) {
		this.localRepo = assertNotNull("localRepo", localRepo);
		loadDynamicComponentFxml(LocalRepoPane.class, this);
		nameTextField.setTextFormatter(new TextFormatter<String>(new UnaryOperator<Change>() {
			@Override
			public Change apply(Change change) {
				String text = change.getText();
				if (text.startsWith("_") && change.getRangeStart() == 0)
					return null;

				if (text.indexOf('/') >= 0)
					return null;

				return change;
			}
		}));
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

			localRootProperty = JavaBeanObjectPropertyBuilder.create()
					.bean(localRepo)
					.name(LocalRepo.PropertyEnum.localRoot.name())
					.build();

			Bindings.bindBidirectional(localRootTextField.textProperty(), localRootProperty, new FileStringConverter());
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	private void syncButtonClicked(final ActionEvent event) {
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(localRepo.getLocalRoot());
	}

	@FXML
	private void inviteButtonClicked(final ActionEvent event) {
		final IssueInvitationWizard wizard = new IssueInvitationWizard(new IssueInvitationData(localRepo, localRepo.getLocalRoot()));
		final WizardDialog dialog = new WizardDialog(getScene().getWindow(), wizard);
		dialog.show(); // no need to wait ;-)
	}
}
