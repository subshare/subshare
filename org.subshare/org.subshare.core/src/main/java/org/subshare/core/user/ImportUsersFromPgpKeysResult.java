package org.subshare.core.user;

import static java.util.Objects.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.subshare.core.pgp.PgpKeyId;

public class ImportUsersFromPgpKeysResult implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map<PgpKeyId, List<ImportedUser>> pgpKeyId2ImportedUsers = new HashMap<>();

	public ImportUsersFromPgpKeysResult() {
	}

	public Map<PgpKeyId, List<ImportedUser>> getPgpKeyId2ImportedUsers() {
		return pgpKeyId2ImportedUsers;
	}

	public static class ImportedUser implements Serializable {
		private static final long serialVersionUID = 1L;

		private final User user;
		private final boolean _new;
		private final boolean modified;

		public ImportedUser(final User user, boolean _new, final boolean modified) {
			this.user = requireNonNull(user, "user");
			this._new = _new;
			this.modified = modified;
		}

		public User getUser() {
			return user;
		}

		public boolean isNew() {
			return _new;
		}

		public boolean isModified() {
			return modified;
		}
	}
}
