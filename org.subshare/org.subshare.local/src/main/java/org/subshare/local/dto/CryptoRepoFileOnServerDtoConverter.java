package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.CryptoRepoFileOnServerDto;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.CryptoRepoFileOnServer;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CryptoRepoFileOnServerDtoConverter {

	private final LocalRepoTransaction transaction;

	public static CryptoRepoFileOnServerDtoConverter create(LocalRepoTransaction transaction) {
		return createObject(CryptoRepoFileOnServerDtoConverter.class, transaction);
	}

	protected CryptoRepoFileOnServerDtoConverter(final LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
	}

	public CryptoRepoFileOnServerDto toCryptoRepoFileOnServerDto(final CryptoRepoFileOnServer cryptoRepoFileOnServer) {
		assertNotNull("cryptoRepoFileOnServer", cryptoRepoFileOnServer);
		final CryptoRepoFileOnServerDto result = new CryptoRepoFileOnServerDto();

		result.setCryptoRepoFileId(cryptoRepoFileOnServer.getCryptoRepoFile().getCryptoRepoFileId());

		final CryptoKey cryptoKey = assertNotNull("cryptoRepoFileOnServer.cryptoKey", cryptoRepoFileOnServer.getCryptoKey());
		result.setCryptoKeyId(cryptoKey.getCryptoKeyId());

		final byte[] repoFileDtoData = assertNotNull("cryptoRepoFileOnServer.repoFileDtoData", cryptoRepoFileOnServer.getRepoFileDtoData());
		result.setRepoFileDtoData(repoFileDtoData);

		result.setSignature(assertNotNull("cryptoRepoFileOnServer.signature", cryptoRepoFileOnServer.getSignature()));

		return result;
	}

	public CryptoRepoFileOnServer putCryptoRepoFileOnServer(final CryptoRepoFileOnServerDto cryptoRepoFileOnServerDto) {
		assertNotNull("cryptoRepoFileOnServerDto", cryptoRepoFileOnServerDto);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);

		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(cryptoRepoFileOnServerDto.getCryptoRepoFileId());
		CryptoRepoFileOnServer cryptoRepoFileOnServer = cryptoRepoFile.getCryptoRepoFileOnServer();
		if (cryptoRepoFileOnServer == null) {
			cryptoRepoFileOnServer = new CryptoRepoFileOnServer();
			cryptoRepoFileOnServer.setCryptoRepoFile(cryptoRepoFile);
		}

		final CryptoKey cryptoKey = cryptoKeyDao.getCryptoKeyOrFail(cryptoRepoFileOnServerDto.getCryptoKeyId());
		cryptoRepoFileOnServer.setCryptoKey(cryptoKey);
		cryptoRepoFileOnServer.setRepoFileDtoData(cryptoRepoFileOnServerDto.getRepoFileDtoData());
		cryptoRepoFileOnServer.setSignature(cryptoRepoFileOnServerDto.getSignature());

		if (cryptoRepoFile.getCryptoRepoFileOnServer() != cryptoRepoFileOnServer)
			cryptoRepoFile.setCryptoRepoFileOnServer(cryptoRepoFileOnServer);

		transaction.flush();
		return cryptoRepoFileOnServer;
	}
}
