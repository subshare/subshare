package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.jdo.Query;

import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class ScheduledReuploadDao extends Dao<ScheduledReupload, ScheduledReuploadDao> {

	public ScheduledReupload getScheduledReupload(final RepoFile repoFile) {
		assertNotNull("repoFile", repoFile);
		final Query query = pm().newNamedQuery(getEntityClass(), "getScheduledReupload_repoFile");
		try {
			final ScheduledReupload result = (ScheduledReupload) query.execute(repoFile);
			return result;
		} finally {
			query.closeAll();
		}
	}
}
