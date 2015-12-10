package org.subshare.gui.localrepo;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
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

import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncActivity;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncState;

public class LocalRepoPane extends GridPane {
	private final LocalRepo localRepo;
	private final RepoSyncDaemon repoSyncDaemon;

	@FXML
	private Button syncButton;

	@FXML
	private Button inviteButton;

	@FXML
	private TextField nameTextField;

	@FXML
	private TextField localRootTextField;

	@FXML
	private TextField activityTextField;

	@FXML
	private TextField syncStateStartedFinishedTextField;

	@FXML
	private TextArea syncStateErrorTextArea;

	@FXML
	private Spinner<Integer> syncPeriodSpinner;

	private Set<RepoSyncActivity> activities;
	private List<RepoSyncState> states;

	private JavaBeanStringProperty nameProperty;

	private JavaBeanObjectProperty<File> localRootProperty;

	private final PropertyChangeListener activityPropertyChangeListener = event -> updateActivities();

	private final PropertyChangeListener statePropertyChangeListener = event -> updateState();

	public LocalRepoPane(final LocalRepo localRepo) {
		this.localRepo = assertNotNull("localRepo", localRepo);
		this.repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
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
		updateActivities();
		updateState();
	}

	@SuppressWarnings("unchecked")
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

		addWeakPropertyChangeListener(repoSyncDaemon, RepoSyncDaemon.PropertyEnum.activities, activityPropertyChangeListener);
		addWeakPropertyChangeListener(repoSyncDaemon, RepoSyncDaemon.PropertyEnum.states, statePropertyChangeListener);
	}

	private void updateActivities() {
		final Set<RepoSyncActivity> newActivities = repoSyncDaemon.getActivities(localRepo.getRepositoryId());
		Platform.runLater(() -> {
			if (activities == null || ! activities.equals(newActivities)) {
				activities = newActivities;
				activityTextField.setText(getActivityTypesDisplayString(newActivities));
			}
		});
	}

	private void updateState() {
		final List<RepoSyncState> newStates = repoSyncDaemon.getStates(localRepo.getRepositoryId());
		Platform.runLater(() -> {
			if (states == null || ! states.equals(newStates)) {
				states = newStates;
				final RepoSyncState state = states.isEmpty() ? null : states.get(states.size() - 1); // last one!
				final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
				syncStateStartedFinishedTextField.setText(state == null ? null :
					dateFormat.format(state.getSyncStarted()) + " ... " + dateFormat.format(state.getSyncFinished()));

				final Error error = state == null ? null : state.getError();
				syncStateErrorTextArea.setText(error == null ? null : error.getClassName() + "\n\n" + error.getMessage());
			}
		});
	}

	private String getActivityTypesDisplayString(Set<RepoSyncActivity> activities) {
		if (activities.isEmpty())
			return "{none}";

		final StringBuilder sb = new StringBuilder();
		for (RepoSyncActivity repoSyncActivity : activities) {
			if (sb.length() > 0)
				sb.append(" + ");

			sb.append(repoSyncActivity.getActivityType()); // TODO nicer String!!!
		}

		return sb.toString();
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
