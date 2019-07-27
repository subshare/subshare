package org.subshare.local.persistence;

import static java.util.Objects.*;

import javax.jdo.Query;

import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class ScheduledReuploadDao extends Dao<ScheduledReupload, ScheduledReuploadDao> {

	public ScheduledReupload getScheduledReupload(final RepoFile repoFile) {
		requireNonNull(repoFile, "repoFile");
		final Query query = pm().newNamedQuery(getEntityClass(), "getScheduledReupload_repoFile");
		try {
			final ScheduledReupload result = (ScheduledReupload) query.execute(repoFile);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public ScheduledReupload scheduleReupload(final RepoFile repoFile) {
		requireNonNull(repoFile, "repoFile");
		ScheduledReupload scheduledReupload = getScheduledReupload(repoFile);
		if (scheduledReupload == null) {
			scheduledReupload = new ScheduledReupload();
			scheduledReupload.setRepoFile(repoFile);
			scheduledReupload = makePersistent(scheduledReupload);
		}
		return scheduledReupload;
	}
}
