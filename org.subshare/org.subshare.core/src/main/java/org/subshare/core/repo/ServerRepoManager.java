package org.subshare.core.repo;

import org.subshare.core.server.Server;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRepoInvitationToken;

import co.codewizards.cloudstore.core.oio.File;

public interface ServerRepoManager {

	/**
	 * Creates a new repository both locally and on the server.
	 * <p>
	 * Turns the given {@code localDirectory} into a local repository; then creates a new remote repository
	 * on the given {@code server}; and finally connects the two.
	 *
	 * @param localDirectory the local directory to be shared, i.e. uploaded to the server. Must not be <code>null</code>.
	 * @param server the server where to create a new repository. Must not be <code>null</code>.
	 * @param owner the owner of the newly created repository. Must not be <code>null</code>.
	 * @return the descriptor of the newly created server-repository (as registered in the {@link ServerRepoRegistry}).
	 */
	ServerRepo createRepository(File localDirectory, Server server, User owner);

	/**
	 * Checks the given {@code serverRepo} out from the given {@code server}.
	 * <p>
	 * Turns the given {@code localDirectory} into a local repository; then connects the remote repository with
	 * the local repository.
	 * <p>
	 * TODO We need to add a parameter for the sub-directory (on the server) in order to check-out only a part - not the entire repo!
	 *
	 * @param server the server from which to check-out. Must not be <code>null</code>. Must match the
	 * {@code serverRepo}'s {@link ServerRepo#getServerId() serverId}.
	 * @param serverRepo the remote repository on the server to be checked-out. Must not be <code>null</code>.
	 * @param localDirectory the local directory into which the server-repository will be checked out.
	 */
	void checkOutRepository(Server server, ServerRepo serverRepo, File localDirectory);

	ServerRepo checkOutRepository(File localDirectory, UserRepoInvitationToken userRepoInvitationToken);
}
