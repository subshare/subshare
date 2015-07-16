package org.subshare.gui.invitation.accept.source;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.scene.layout.GridPane;

import org.subshare.gui.invitation.accept.AcceptInvitationData;

public class AcceptInvitationSourcePane extends GridPane {
	private final AcceptInvitationData acceptInvitationData;

	public AcceptInvitationSourcePane(final AcceptInvitationData acceptInvitationData) {
		this.acceptInvitationData = assertNotNull("acceptInvitationData", acceptInvitationData);
		loadDynamicComponentFxml(AcceptInvitationSourcePane.class, this);
	}

}
