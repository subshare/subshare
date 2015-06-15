package org.subshare.core.user;

import static org.subshare.core.file.FileConst.*;

import java.util.Collection;
import java.util.Set;

import org.subshare.core.pgp.PgpKeyId;

import co.codewizards.cloudstore.core.dto.Uid;

public interface UserRegistry {
	public static final String USER_REGISTRY_FILE_NAME = "userRegistry" + SUBSHARE_FILE_EXTENSION;

	User createUser();

	Collection<User> getUsers();
	Collection<User> getUsersByEmail(String email);
	Collection<User> getUsersByPgpKeyIds(Set<PgpKeyId> pgpKeyIds);

	void addUser(User user);
	void removeUser(User user);

	User getUserByUserIdOrFail(Uid userId);
	User getUserByUserId(Uid userId);
	User getUserByUserRepoKeyIdOrFail(Uid userRepoKeyId);
	User getUserByUserRepoKeyId(Uid userRepoKeyId);
	void writeIfNeeded();
}
