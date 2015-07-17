package org.subshare.gui.invitation.accept;

import co.codewizards.cloudstore.core.oio.File;

public class AcceptInvitationData {

	private File invitationFile;
	private File checkOutDirectory;

	public File getInvitationFile() {
		return invitationFile;
	}
	public void setInvitationFile(File invitationFile) {
		this.invitationFile = invitationFile;
	}

	public File getCheckOutDirectory() {
		return checkOutDirectory;
	}
	public void setCheckOutDirectory(File checkOutDirectory) {
		this.checkOutDirectory = checkOutDirectory;
	}
}
