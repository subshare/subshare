package org.subshare.gui.invitation.accept.source;

import co.codewizards.cloudstore.core.oio.File;
import javafx.stage.Window;

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
