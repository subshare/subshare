package org.subshare.gui.localrepolist;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.text.DateFormat;
import java.util.Date;
import java.util.Set;

import org.subshare.core.repo.LocalRepo;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncActivity;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncState;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;

public class LocalRepoListItem {

	private final LocalRepo localRepo;
	private RepoSyncState repoSyncState;

	private StringProperty name;
	private final StringProperty nameShown = new SimpleStringProperty(this, "nameShown");
	private final ObjectProperty<Severity> severity = new SimpleObjectProperty<>(this, "severity");

	private ObjectProperty<File> localRoot;
	private final StringProperty localRootAsString = new SimpleStringProperty(this, "localRootAsString");

	private final ObjectProperty<Date> syncStarted = new SimpleObjectProperty<>(this, "syncStarted");
	private final StringProperty syncStartedAsString = new SimpleStringProperty(this, "syncStartedAsString");

	private ObjectProperty<Set<RepoSyncActivity>> repoSyncActivities = new SimpleObjectProperty<>(this, "repoSyncActivities");

	public LocalRepoListItem(final LocalRepo localRepo) {
		this.localRepo = assertNotNull(localRepo, "localRepo");
		bind();
		updateNameShown();
	}

	@SuppressWarnings("unchecked")
	private void bind() {
		try {
			name = JavaBeanStringPropertyBuilder.create()
					.bean(localRepo)
					.name(LocalRepo.PropertyEnum.name.name()).build();

			localRoot = JavaBeanObjectPropertyBuilder.create()
					.bean(localRepo)
					.name(LocalRepo.PropertyEnum.localRoot.name()).build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		name.addListener((InvalidationListener) observable -> updateNameShown());
		localRootAsString.bind(localRoot.asString());
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}

	public ReadOnlyStringProperty nameProperty() {
		return name;
	}

	public String getName() {
		return name.get();
	}

	public ReadOnlyStringProperty nameShownProperty() {
		return nameShown;
	}

	public String getNameShown() {
		return nameShown.get();
	}

	public File getLocalRoot() {
		return localRoot.get();
	}

	public ReadOnlyObjectProperty<File> localRootProperty() {
		return localRoot;
	}

	public String getLocalRootAsString() {
		return localRootAsString.get();
	}

	public ReadOnlyStringProperty localRootAsStringProperty() {
		return localRootAsString;
	}

	public ReadOnlyObjectProperty<Severity> severityProperty() {
		return severity;
	}

	public Date getSyncStarted() {
		return syncStarted.get();
	}
	public ReadOnlyObjectProperty<Date> syncStartedProperty() {
		return syncStarted;
	}

	public String getSyncStartedAsString() {
		return syncStartedAsString.get();
	}
	public ReadOnlyStringProperty syncStartedAsStringProperty() {
		return syncStartedAsString;
	}

	private void updateNameShown() {
		final String name = getName();
		if (isEmpty(name))
			nameShown.set(localRepo.getLocalRoot().getName());
		else
			nameShown.set(name);
	}

	private void updateSeverity() {
		final RepoSyncState repoSyncState = getRepoSyncState();
		severity.set(repoSyncState == null ? null : repoSyncState.getSeverity());
	}

	private void updateSyncStarted() {
		final RepoSyncState repoSyncState = getRepoSyncState();
		if (repoSyncState == null) {
			syncStarted.set(null);
			syncStartedAsString.set(null);
		}
		else {
			final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
			syncStarted.set(repoSyncState.getSyncStarted());
			syncStartedAsString.set(dateFormat.format(repoSyncState.getSyncStarted()));
		}
	}

	public RepoSyncState getRepoSyncState() {
		return repoSyncState;
	}
	public void setRepoSyncState(final RepoSyncState repoSyncState) {
		if (equal(this.repoSyncState, repoSyncState))
			return;

		this.repoSyncState = repoSyncState;
		updateSeverity();
		updateSyncStarted();
	}

	public String getTooltipText() {
		return getRepoSyncStateTooltipText();
	}

	public ObjectProperty<Set<RepoSyncActivity>> repoSyncActivitiesProperty() {
		return repoSyncActivities;
	}
	public Set<RepoSyncActivity> getRepoSyncActivities() {
		return repoSyncActivities.get();
	}
	public void setRepoSyncActivities(Set<RepoSyncActivity> repoSyncActivities) {
		this.repoSyncActivities.set(repoSyncActivities);
	}

	private String getRepoSyncStateTooltipText() {
		RepoSyncState syncState = this.repoSyncState;
		if (syncState != null) {
			if (!isEmpty(syncState.getMessage()))
				return syncState.getMessage();

			if (syncState.getError() != null) {
				if (!isEmpty(syncState.getError().getMessage()))
					return syncState.getError().getClassName() + ": " + syncState.getError().getMessage();
				else
					return syncState.getError().getClassName();
			}
		}
		return null;
	}
}
