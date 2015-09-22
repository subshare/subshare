package org.subshare.gui.invitation.accept.source;

import javafx.stage.Window;
import co.codewizards.cloudstore.core.oio.File;

public interface ProblemSolver {

	Window getWindow();

	void setWindow(Window window);

	File getInvitationFile();

	void setInvitationFile(File invitationFile);

	CheckInvitationFileResult getCheckInvitationFileResult();

	void setCheckInvitationFileResult(CheckInvitationFileResult checkInvitationFileResult);

	int getPriority();

	boolean canSolveProblem();

	void solveProblem();
}
