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
	 *
	 * @param server the server from which to check-out. Must not be <code>null</code>. Must match the
	 * {@code serverRepo}'s {@link ServerRepo#getServerId() serverId}.
	 * @param serverPath an (optional) sub-directory to be checked out. <code>null</code> is equivalent to an empty string
	 * meaning the repository's root should be checked out (rather than a sub-directory).
	 * @param serverRepo the remote repository on the server to be checked-out. Must not be <code>null</code>.
	 * @param localDirectory the local directory into which the server-repository will be checked out.
	 * Must not be <code>null</code>.
	 */
	void checkOutRepository(Server server, ServerRepo serverRepo, String serverPath, File localDirectory);

	/**
	 * Check-out a repository (or one of its sub-directories) by consuming the given {@code userRepoInvitationToken}.
	 * <p>
	 * The coordinates of the repository (server + path, i.e. exact URL) is taken from the invitation-token.
	 * @param localDirectory the local directory into which the server-repository will be checked out.
	 * Must not be <code>null</code>.
	 * @param userRepoInvitationToken the invitation-token. Must not be <code>null</code>.
	 * @return the descriptor of the checked-out server-repository (as registered in the {@link ServerRepoRegistry}).
	 */
	ServerRepo checkOutRepository(File localDirectory, UserRepoInvitationToken userRepoInvitationToken);

	/**
	 * Determines, if the given {@code localDirectory} can be used to
	 * {@linkplain #createRepository(File, Server, User) create a repository} or
	 * for {@linkplain #checkOutRepository(File, UserRepoInvitationToken) check-out}.
	 * @param localDirectory the local directory to be checked. Must not be <code>null</code>.
	 * @return <code>true</code> if the directory can be used; <code>false</code> otherwise.
	 */
	boolean canUseLocalDirectory(File localDirectory);
}
