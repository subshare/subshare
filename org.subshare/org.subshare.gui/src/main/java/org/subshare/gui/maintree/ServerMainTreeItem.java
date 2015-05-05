package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import javafx.scene.Parent;

import org.subshare.core.server.Server;
import org.subshare.gui.server.ServerPane;

public class ServerMainTreeItem extends MainTreeItem<Server> {

	public ServerMainTreeItem(final Server server) {
		super(server);
		assertNotNull("server", server);
	}

//	private final List<RepositoryMainTreeItem> repositoryMainTreeItems = new ArrayList<RepositoryMainTreeItem>();

	@Override
	protected String getValueString(Server valueObject) {
		return valueObject.getName();
	}

	@Override
	public String toString() {
		return getValueObject().getName();
	}

	@Override
	protected Parent createMainDetailContent() {
		return new ServerPane(getValueObject());
	}

//	public List<RepositoryMainTreeItem> getRepositoryMainTreeItems() {
//		return repositoryMainTreeItems;
//	}
}
