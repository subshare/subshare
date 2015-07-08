package org.subshare.gui.welcome.server;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeListener;
import java.net.URL;

import javafx.scene.Parent;

import org.subshare.core.server.Server;
import org.subshare.gui.welcome.ServerData;
import org.subshare.gui.wizard.WizardPage;

public class ServerWizardPage extends WizardPage {

	private final PropertyChangeListener updateCompletePropertyChangeListener = event -> updateComplete();

	private final ServerData serverData;
	private ServerPane serverPane;

	public ServerWizardPage(ServerData serverData) {
		super("Server");
		this.serverData = assertNotNull("serverData", serverData);
		addWeakPropertyChangeListener(serverData.getServer(), Server.PropertyEnum.url, updateCompletePropertyChangeListener);
		updateComplete();
	}

	@Override
	protected Parent createContent() {
		serverPane = new ServerPane(serverData.getServer());
		return serverPane;
	}

	private void updateComplete() {
		final URL url = serverData.getServer().getUrl();
		completeProperty().set(url != null);
	}
}
