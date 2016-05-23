package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.RepoFile;

@PersistenceCapable
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Unique(name = "UK_ScheduledReupload_repoFile", members = "repoFile")
@Index(name = "ScheduledReupload_repoFile", members = "repoFile")
@Queries({
	@Query(name = "getScheduledReupload_repoFile", value = "SELECT UNIQUE WHERE this.repoFile == :repoFile")
})
public class ScheduledReupload extends Entity {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private RepoFile repoFile;

	public RepoFile getRepoFile() {
		return repoFile;
	}
	public void setRepoFile(RepoFile repoFile) {
		this.repoFile = assertNotNull("repoFile", repoFile);
	}
}
