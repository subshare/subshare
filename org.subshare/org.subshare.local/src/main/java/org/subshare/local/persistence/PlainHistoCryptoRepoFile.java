package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import javax.jdo.annotations.Column;
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
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.dto.jaxb.PlainHistoCryptoRepoFileDtoIo;

import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name = "UK_PlainHistoCryptoRepoFile_histoCryptoRepoFile", members = "histoCryptoRepoFile")
})
@Indices({
	@Index(name = "PlainHistoCryptoRepoFile_histoCryptoRepoFile", members = "histoCryptoRepoFile")
})
@Queries({
	@Query(name = "getPlainHistoCryptoRepoFile_histoCryptoRepoFile", value = "SELECT UNIQUE WHERE this.histoCryptoRepoFile == :histoCryptoRepoFile")
})
public class PlainHistoCryptoRepoFile extends Entity implements StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private HistoCryptoRepoFile histoCryptoRepoFile;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType = "BLOB")
	private byte[] plainHistoCryptoRepoFileDtoData;

	public PlainHistoCryptoRepoFile() {
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFile() {
		return histoCryptoRepoFile;
	}
	public void setHistoCryptoRepoFile(HistoCryptoRepoFile histoCryptoRepoFile) {
		if (! equal(this.histoCryptoRepoFile, histoCryptoRepoFile))
			this.histoCryptoRepoFile = histoCryptoRepoFile;
	}

	public PlainHistoCryptoRepoFileDto getPlainHistoCryptoRepoFileDto() {
		if (plainHistoCryptoRepoFileDtoData == null)
			return null;
		else {
			final PlainHistoCryptoRepoFileDtoIo io = new PlainHistoCryptoRepoFileDtoIo();
			PlainHistoCryptoRepoFileDto dto = io.deserializeWithGz(plainHistoCryptoRepoFileDtoData);
			return dto;
		}
	}

	public void setPlainHistoCryptoRepoFileDto(final PlainHistoCryptoRepoFileDto dto) {
		assertNotNull(dto, "dto");
		plainHistoCryptoRepoFileDtoData = new PlainHistoCryptoRepoFileDtoIo().serializeWithGz(dto);
	}

	@Override
	public void jdoPreStore() {
	}
}
