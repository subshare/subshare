package org.subshare.gui.localrepo;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.sync.RepoSyncTimer;
import org.subshare.gui.IconSize;
import org.subshare.gui.error.ErrorHandler;
import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.invitation.issue.IssueInvitationWizard;
import org.subshare.gui.ls.ConfigLs;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.severity.SeverityImageRegistry;
import org.subshare.gui.util.FileStringConverter;
import org.subshare.gui.wizard.WizardDialog;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.config.Config;
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
	private Label syncStateSeverityLabel;

	@FXML
	private ImageView syncStateSeverityImageView;

	@FXML
	private CheckBox syncPeriodCheckBox;

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
		syncPeriodSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
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
		updateSyncPeriodUi();

		final EventHandler<? super MouseEvent> syncStateMouseEventFilter = event -> showSyncStateDialog();
		syncStateStartedFinishedTextField.addEventFilter(MouseEvent.MOUSE_CLICKED, syncStateMouseEventFilter);
		syncStateSeverityLabel.addEventFilter(MouseEvent.MOUSE_CLICKED, syncStateMouseEventFilter);
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

		syncPeriodCheckBox.selectedProperty().addListener((InvalidationListener) observable -> updateSyncPeriodInConfig());
		syncPeriodSpinner.valueProperty().addListener((InvalidationListener) observable -> updateSyncPeriodInConfig());

		addWeakPropertyChangeListener(repoSyncDaemon, RepoSyncDaemon.PropertyEnum.activities, activityPropertyChangeListener);
		addWeakPropertyChangeListener(repoSyncDaemon, RepoSyncDaemon.PropertyEnum.states, statePropertyChangeListener);
	}

	private void updateSyncPeriodUi() {
		Long syncPeriod = getSyncPeriodInConfig();

		if (syncPeriod == null) {
			final Config config = ConfigLs.getInstanceForDirectory(localRepo.getLocalRoot());
			syncPeriodCheckBox.setSelected(false);
			syncPeriod = config.getPropertyAsPositiveOrZeroLong(RepoSyncTimer.CONFIG_KEY_SYNC_PERIOD, RepoSyncTimer.DEFAULT_SYNC_PERIOD);
		}
		else
			syncPeriodCheckBox.setSelected(true);

		syncPeriodSpinner.getValueFactory().setValue(syncPeriod.intValue()); // int/long => we need to replace this Spinner by a better component, anyway!
	}

	private void updateSyncPeriodInConfig() {
		Integer syncPeriodFromUi = syncPeriodSpinner.getValue();

		if (syncPeriodCheckBox.isSelected() && syncPeriodFromUi == null) {
			syncPeriodFromUi = (int) RepoSyncTimer.DEFAULT_SYNC_PERIOD;
			syncPeriodSpinner.getValueFactory().setValue(syncPeriodFromUi);
		}

		if (syncPeriodCheckBox.isSelected())
			setSyncPeriodInConfig(syncPeriodFromUi.longValue());
		else
			setSyncPeriodInConfig(null);
	}

	private Long getSyncPeriodInConfig() {
		final Config config = ConfigLs.getInstanceForDirectory(localRepo.getLocalRoot());
		String syncPeriodStr = config.getDirectProperty(RepoSyncTimer.CONFIG_KEY_SYNC_PERIOD);
		try {
			return isEmpty(syncPeriodStr) ? null : Long.parseLong(syncPeriodStr);
		} catch (NumberFormatException x) {
			return null;
		}
	}

	private void setSyncPeriodInConfig(final Long syncPeriod) {
		if (equal(getSyncPeriodInConfig(), syncPeriod))
			return;

		final Config config = ConfigLs.getInstanceForDirectory(localRepo.getLocalRoot());
		config.setDirectProperty(RepoSyncTimer.CONFIG_KEY_SYNC_PERIOD, syncPeriod == null ? null : Long.toString(syncPeriod));
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

	private void showSyncStateDialog() {
		final RepoSyncState state = states.isEmpty() ? null : states.get(states.size() - 1); // last one!
		final Error error = state == null ? null : state.getError();
		if (error == null) {
			if (state == null)
				showSyncStateInfoDialog("There was no synchonisation, yet (since the application was started).");
			else
				showSyncStateInfoDialog("The last synchonisation was successful.");
		}
		else
			showSyncStateErrorDialog(error);
	}

	private void showSyncStateInfoDialog(String message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setHeaderText("All fine, dude!");

//		alert.setContentText(text);
		// The above does not adjust the dialog size :-( Using a Text node instead works better.

		final Text contentText = new Text(message);
		final HBox contentTextContainer = new HBox();
		contentTextContainer.getChildren().add(contentText);

		GridPane.setMargin(contentText, new Insets(8));
		alert.getDialogPane().setContent(contentTextContainer);

		alert.show();
	}

	private void showSyncStateErrorDialog(final Error error) {
		assertNotNull("error", error);
		ErrorHandler.handleError("Last synchronisation failed!", null, error); // take 'contentText' from throwable
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

				final Severity severity = state == null ? Severity.INFO : state.getSeverity();
				syncStateSeverityImageView.setDisable(state == null);
				syncStateSeverityImageView.setImage(SeverityImageRegistry.getInstance().getImage(severity, IconSize._24x24));

				Tooltip tooltip = null;

				if (error != null)
					tooltip = new Tooltip(error.getClassName() + "\n\n" + error.getMessage());
				else if (state == null)
					tooltip = new Tooltip("There was no synchonisation, yet (since the application was started).");
				else
					tooltip = new Tooltip("The last synchonisation was successful.");

				syncStateStartedFinishedTextField.setTooltip(tooltip);
				syncStateSeverityLabel.setTooltip(tooltip);
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
