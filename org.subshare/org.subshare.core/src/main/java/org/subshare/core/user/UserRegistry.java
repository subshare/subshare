package org.subshare.core.user;

import java.util.Collection;
import java.util.Set;

import org.subshare.core.pgp.PgpKeyId;

import co.codewizards.cloudstore.core.dto.Uid;

public interface UserRegistry {
	public static final String USER_REGISTRY_FILE_NAME = "userList.xml.gz";
	public static final String USER_REGISTRY_FILE_LOCK = USER_REGISTRY_FILE_NAME + ".lock";

	User createUser();

	Collection<User> getUsers();
	Collection<User> getUsersByEmail(final String email);
	Collection<User> getUsersByPgpKeyIds(Set<PgpKeyId> pgpKeyIds);

	void addUser(final User user);
	User getUserByUserIdOrFail(final Uid userId);
	User getUserByUserId(final Uid userId);
	User getUserByUserRepoKeyIdOrFail(final Uid userRepoKeyId);
	User getUserByUserRepoKeyId(final Uid userRepoKeyId);
	void writeIfNeeded();
	void write();

}
