package org.subshare.gui.maintree;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Parent;

import org.subshare.gui.server.ServerPane;

public class ServerMainTreeItem extends MainTreeItem {
	private String name;
	private final List<RepositoryMainTreeItem> repositoryMainTreeItems = new ArrayList<RepositoryMainTreeItem>();

	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	protected Parent createMainDetailContent() {
		return new ServerPane();
	}

	public List<RepositoryMainTreeItem> getRepositoryMainTreeItems() {
		return repositoryMainTreeItems;
	}
}
