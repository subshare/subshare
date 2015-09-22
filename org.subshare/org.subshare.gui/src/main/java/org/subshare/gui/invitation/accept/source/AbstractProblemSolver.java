package org.subshare.gui.invitation.accept.source;

import javafx.stage.Window;
import co.codewizards.cloudstore.core.oio.File;

public abstract class AbstractProblemSolver implements ProblemSolver {

	private Window window;
	private File invitationFile;
	private CheckInvitationFileResult checkInvitationFileResult;

	@Override
	public Window getWindow() {
		return window;
	}
	@Override
	public void setWindow(Window window) {
		this.window = window;
	}

	@Override
	public File getInvitationFile() {
		return invitationFile;
	}
	@Override
	public void setInvitationFile(File invitationFile) {
		this.invitationFile = invitationFile;
	}

	@Override
	public CheckInvitationFileResult getCheckInvitationFileResult() {
		return checkInvitationFileResult;
	}
	@Override
	public void setCheckInvitationFileResult(CheckInvitationFileResult checkInvitationFileResult) {
		this.checkInvitationFileResult = checkInvitationFileResult;
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
