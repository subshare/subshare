package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Uniques;

import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="UK_CurrentHistoCryptoRepoFile_cryptoRepoFile", members="cryptoRepoFile")
})
@Indices({
	@Index(name="CurrentHistoCryptoRepoFile_localRevision", members="localRevision"),
	@Index(name="CurrentHistoCryptoRepoFile_cryptoRepoFile", members="cryptoRepoFile")
})
@Queries({
	@Query(name="getCurrentHistoCryptoRepoFile_cryptoRepoFile", value="SELECT UNIQUE WHERE this.cryptoRepoFile == :cryptoRepoFile"),
	@Query(
			name="getCurrentHistoCryptoRepoFilesChangedAfter_localRevision",
			value="SELECT WHERE this.localRevision > :localRevision")
})
public class CurrentHistoCryptoRepoFile extends Entity implements AutoTrackLocalRevision { // TODO implement WriteProtected, too!

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private HistoCryptoRepoFile histoCryptoRepoFile;

	private long localRevision;

	public CurrentHistoCryptoRepoFile() {
	}

	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
	}
	public void setCryptoRepoFile(CryptoRepoFile cryptoRepoFile) {
		if (equal(this.cryptoRepoFile, cryptoRepoFile))
			return;

		if (this.cryptoRepoFile != null)
			throw new IllegalStateException("this.cryptoRepoFile already assigned! Cannot re-assign!");

		this.cryptoRepoFile = cryptoRepoFile;
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFile() {
		return histoCryptoRepoFile;
	}
	public void setHistoCryptoRepoFile(HistoCryptoRepoFile histoCryptoRepoFile) {
		this.histoCryptoRepoFile = histoCryptoRepoFile;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		if (! equal(this.localRevision, localRevision))
			this.localRevision = localRevision;
	}
}
