package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.CryptoConfigPropSetDto;
import org.subshare.local.persistence.CryptoConfigPropSet;
import org.subshare.local.persistence.CryptoConfigPropSetDao;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CryptoConfigPropSetDtoConverter {

	private LocalRepoTransaction transaction;

	public static CryptoConfigPropSetDtoConverter create(LocalRepoTransaction transaction) {
		return createObject(CryptoConfigPropSetDtoConverter.class, transaction);
	}

	protected CryptoConfigPropSetDtoConverter(LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
	}

	public CryptoConfigPropSetDto toCryptoConfigPropSetDto(final CryptoConfigPropSet cryptoConfigPropSet) {
		assertNotNull("cryptoConfigPropSet", cryptoConfigPropSet);
		CryptoConfigPropSetDto result = new CryptoConfigPropSetDto();
		result.setCryptoRepoFileId(cryptoConfigPropSet.getCryptoRepoFileId());
		result.setCryptoKeyId(assertNotNull("cryptoConfigPropSet.cryptoKey", cryptoConfigPropSet.getCryptoKey()).getCryptoKeyId());
		result.setConfigPropSetDtoData(cryptoConfigPropSet.getConfigPropSetDtoData());
		result.setSignature(cryptoConfigPropSet.getSignature());
		return result;
	}

	public CryptoConfigPropSet putCryptoConfigPropSetDto(final CryptoConfigPropSetDto cryptoConfigPropSetDto) {
		assertNotNull("cryptoConfigPropSetDto", cryptoConfigPropSetDto);

		final CryptoRepoFileDao crfDao = transaction.getDao(CryptoRepoFileDao.class);
		final CryptoKeyDao ckDao = transaction.getDao(CryptoKeyDao.class);
		final CryptoConfigPropSetDao ccpsDao = transaction.getDao(CryptoConfigPropSetDao.class);

		final CryptoRepoFile cryptoRepoFile = crfDao.getCryptoRepoFileOrFail(cryptoConfigPropSetDto.getCryptoRepoFileId());
		final CryptoKey cryptoKey = ckDao.getCryptoKeyOrFail(cryptoConfigPropSetDto.getCryptoKeyId());

		CryptoConfigPropSet result = ccpsDao.getCryptoConfigPropSet(cryptoRepoFile);
		if (result == null)
			result = new CryptoConfigPropSet(cryptoRepoFile);

		result.setCryptoKey(cryptoKey);
		result.setConfigPropSetDtoData(cryptoConfigPropSetDto.getConfigPropSetDtoData());
		result.setSignature(cryptoConfigPropSetDto.getSignature());

		result = ccpsDao.makePersistent(result);
		return result;
	}


}
