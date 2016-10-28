package org.subshare.gui.createrepo;

import java.util.HashSet;

import org.subshare.core.server.Server;
import org.subshare.core.user.User;

import co.codewizards.cloudstore.core.oio.File;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class CreateRepoData {

	private Server server;
	private File localDirectory;
	private ObservableSet<User> ownerList = FXCollections.observableSet(new HashSet<User>());

	public CreateRepoData() {
	}

	public CreateRepoData(final Server server) {
		this.server = server;
	}

	public Server getServer() {
		return server;
	}
	public void setServer(Server server) {
		this.server = server;
	}

	public File getLocalDirectory() {
		return localDirectory;
	}
	public void setLocalDirectory(File localDirectory) {
		this.localDirectory = localDirectory;
	}

	/**
	 * Gets the selected owner (0 or 1).
	 * <p>
	 * Despite the name, this should be either empty or contain exactly one single element! It must never
	 * contain more than one!
	 * @return the set holding the selected owner (or being empty). Never <code>null</code>.
	 */
	public ObservableSet<User> getOwners() {
		return ownerList;
	}
}
