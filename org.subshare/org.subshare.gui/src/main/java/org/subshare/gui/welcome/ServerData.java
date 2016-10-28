package org.subshare.gui.welcome;

import org.subshare.core.server.Server;
import org.subshare.gui.invitation.accept.AcceptInvitationData;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ServerData {

	private final BooleanProperty acceptInvitationProperty = new SimpleBooleanProperty(this, "acceptInvitation");
	private Server server;
	private AcceptInvitationData acceptInvitationData = new AcceptInvitationData();

	public Server getServer() {
		return server;
	}
	public void setServer(Server server) {
		this.server = server;
	}

	public BooleanProperty acceptInvitationProperty() {
		return acceptInvitationProperty;
	}

	public AcceptInvitationData getAcceptInvitationData() {
		return acceptInvitationData;
	}
}
