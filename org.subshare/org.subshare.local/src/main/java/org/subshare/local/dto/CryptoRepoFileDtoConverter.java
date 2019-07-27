package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

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
		requireNonNull(cryptoRepoFile, "cryptoRepoFile");
		final CryptoRepoFileDto cryptoRepoFileDto = new CryptoRepoFileDto();

		cryptoRepoFileDto.setCryptoRepoFileId(cryptoRepoFile.getCryptoRepoFileId());

		final CryptoRepoFile parent = cryptoRepoFile.getParent();
		cryptoRepoFileDto.setParentCryptoRepoFileId(parent == null ? null : parent.getCryptoRepoFileId());

		final CryptoKey cryptoKey = requireNonNull(cryptoRepoFile.getCryptoKey(), "cryptoRepoFile.cryptoKey");
		cryptoRepoFileDto.setCryptoKeyId(cryptoKey.getCryptoKeyId());

		cryptoRepoFileDto.setDirectory(cryptoRepoFile.isDirectory());

		final byte[] repoFileDtoData = requireNonNull(cryptoRepoFile.getRepoFileDtoData(), "cryptoRepoFile.repoFileDtoData");
		cryptoRepoFileDto.setRepoFileDtoData(repoFileDtoData);

		cryptoRepoFileDto.setCryptoRepoFileCreated(cryptoRepoFile.getCryptoRepoFileCreated());
		cryptoRepoFileDto.setDeleted(cryptoRepoFile.getDeleted());
		cryptoRepoFileDto.setDeletedByIgnoreRule(cryptoRepoFile.isDeletedByIgnoreRule());

		cryptoRepoFileDto.setSignature(requireNonNull(cryptoRepoFile.getSignature(), "cryptoRepoFile.signature"));

		return cryptoRepoFileDto;
	}

}
