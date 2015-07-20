package org.subshare.gui.invitation.issue;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashSet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.user.User;

import co.codewizards.cloudstore.core.oio.File;

public class IssueInvitationData {

	private LocalRepo localRepo;
	private File invitationTargetFile;
	private final ObservableSet<User> invitees = FXCollections.observableSet(new HashSet<User>());
	private File invitationTokenDirectory;
	private PermissionType permissionType = PermissionType.write; // TODO UI!
	private long validityDurationMillis = 5L * 24L * 3600L; // TODO UI!

	public IssueInvitationData() {
	}

	public IssueInvitationData(LocalRepo localRepo, File invitationTargetFile) {
		setLocalRepo(localRepo);
		setInvitationTargetFile(invitationTargetFile);
	}

	public ObservableSet<User> getInvitees() {
		return invitees;
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}
	public void setLocalRepo(LocalRepo localRepo) {
		this.localRepo = localRepo;
	}

	/**
	 * Gets the file - or more likely directory - to which an invitation should be issued.
	 * @return the file - or more likely directory - to which an invitation should be issued.
	 */
	public File getInvitationTargetFile() {
		return invitationTargetFile;
	}
	public void setInvitationTargetFile(File invitationTargetFile) {
		this.invitationTargetFile = invitationTargetFile;
	}

	/**
	 * Gets the directory, into which the invitation token files are written.
	 * @return the directory, into which the invitation token files are written.
	 */
	public File getInvitationTokenDirectory() {
		return invitationTokenDirectory;
	}

	public void setInvitationTokenDirectory(File invitationDirectory) {
		this.invitationTokenDirectory = invitationDirectory;
	}

	public PermissionType getPermissionType() {
		return permissionType;
	}
	public void setPermissionType(final PermissionType permissionType) {
		this.permissionType = assertNotNull("permissionType", permissionType);
	}
	public long getValidityDurationMillis() {
		return validityDurationMillis;
	}
	public void setValidityDurationMillis(final long validityDurationMillis) {
		if (validityDurationMillis < 0)
			throw new IllegalArgumentException("validityDurationMillis < 0");

		this.validityDurationMillis = validityDurationMillis;
	}
}
