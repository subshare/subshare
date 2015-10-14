package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoRepoFile;

public class CryptoRepoFileDtoConverter {

	public static CryptoRepoFileDtoConverter create() {
		return createObject(CryptoRepoFileDtoConverter.class);
	}

	protected CryptoRepoFileDtoConverter() {
	}

	public CryptoRepoFileDto toCryptoRepoFileDto(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		final CryptoRepoFileDto cryptoRepoFileDto = new CryptoRepoFileDto();

		cryptoRepoFileDto.setCryptoRepoFileId(cryptoRepoFile.getCryptoRepoFileId());

		final CryptoRepoFile parent = cryptoRepoFile.getParent();
		cryptoRepoFileDto.setParentCryptoRepoFileId(parent == null ? null : parent.getCryptoRepoFileId());

		final CryptoKey cryptoKey = assertNotNull("cryptoRepoFile.cryptoKey", cryptoRepoFile.getCryptoKey());
		cryptoRepoFileDto.setCryptoKeyId(cryptoKey.getCryptoKeyId());

		cryptoRepoFileDto.setDirectory(cryptoRepoFile.isDirectory());

		final byte[] repoFileDtoData = assertNotNull("cryptoRepoFile.repoFileDtoData", cryptoRepoFile.getRepoFileDtoData());
		cryptoRepoFileDto.setRepoFileDtoData(repoFileDtoData);

		cryptoRepoFileDto.setDeleted(cryptoRepoFile.getDeleted());

		cryptoRepoFileDto.setSignature(assertNotNull("cryptoRepoFile.signature", cryptoRepoFile.getSignature()));

		return cryptoRepoFileDto;
	}

}
