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

@PersistenceCapable
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Unique(name = "UK_PreliminaryDeletion_cryptoRepoFile", members = "cryptoRepoFile")
@Index(name = "PreliminaryDeletion_cryptoRepoFile", members = "cryptoRepoFile")
//@Unique(name = "PreliminaryDeletion_pathSha1", members = "pathSha1")
@Queries({
//	@Query(name = "getPreliminaryDeletion_pathSha1", value = "SELECT UNIQUE WHERE this.pathSha1 == :pathSha1")
	@Query(name = "getPreliminaryDeletion_cryptoRepoFile", value = "SELECT UNIQUE WHERE this.cryptoRepoFile == :cryptoRepoFile")
})
public class PreliminaryDeletion extends Entity {

//	@Persistent(nullValue=NullValue.EXCEPTION, defaultFetchGroup="true")
//	@Column(jdbcType="CLOB")
//	private String path;
//
//	@Persistent(nullValue=NullValue.EXCEPTION)
//	private String pathSha1;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

//	public String getPath() {
//		return path;
//	}
//
//	public void setPath(final String path) {
//		this.path = assertNotNull("path", path);
//		this.pathSha1 = sha1(path);
//	}
//
//	public String getPathSha1() {
//		return pathSha1;
//	}

	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
	}
	public void setCryptoRepoFile(CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		this.cryptoRepoFile = cryptoRepoFile;
	}
}
