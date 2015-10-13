package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.CurrentHistoCryptoRepoFile;
import org.subshare.local.persistence.CurrentHistoCryptoRepoFileDao;
import org.subshare.local.persistence.HistoCryptoRepoFile;
import org.subshare.local.persistence.HistoCryptoRepoFileDao;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CurrentHistoCryptoRepoFileDtoConverter {

	private final LocalRepoTransaction transaction;

	public static CurrentHistoCryptoRepoFileDtoConverter create(LocalRepoTransaction transaction) {
		return createObject(CurrentHistoCryptoRepoFileDtoConverter.class, transaction);
	}

	protected CurrentHistoCryptoRepoFileDtoConverter(final LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
	}

	public CurrentHistoCryptoRepoFileDto toCurrentHistoCryptoRepoFileDto(final CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile) {
		assertNotNull("currentHistoCryptoRepoFile", currentHistoCryptoRepoFile);
		final CurrentHistoCryptoRepoFileDto result = new CurrentHistoCryptoRepoFileDto();

		result.setHistoCryptoRepoFileId(currentHistoCryptoRepoFile.getHistoCryptoRepoFile().getHistoCryptoRepoFileId());
		result.setCryptoRepoFileId(currentHistoCryptoRepoFile.getCryptoRepoFile().getCryptoRepoFileId());

		return result;
	}

	public CurrentHistoCryptoRepoFile putCurrentCryptoRepoFileOnServer(final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto) {
		assertNotNull("cryptoRepoFileOnServerDto", currentHistoCryptoRepoFileDto);
		final CurrentHistoCryptoRepoFileDao currentHistoCryptoRepoFileDao = transaction.getDao(CurrentHistoCryptoRepoFileDao.class);
		final HistoCryptoRepoFileDao histoCryptoRepoFileDao = transaction.getDao(HistoCryptoRepoFileDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(currentHistoCryptoRepoFileDto.getCryptoRepoFileId());
		final HistoCryptoRepoFile histoCryptoRepoFile = histoCryptoRepoFileDao.getHistoCryptoRepoFileOrFail(currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileId());

		CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile = currentHistoCryptoRepoFileDao.getCurrentHistoCryptoRepoFile(cryptoRepoFile);
		if (currentHistoCryptoRepoFile == null) {
			currentHistoCryptoRepoFile = new CurrentHistoCryptoRepoFile();
			currentHistoCryptoRepoFile.setCryptoRepoFile(cryptoRepoFile);
		}

		currentHistoCryptoRepoFile.setHistoCryptoRepoFile(histoCryptoRepoFile);

		currentHistoCryptoRepoFile = currentHistoCryptoRepoFileDao.makePersistent(currentHistoCryptoRepoFile);

		transaction.flush();
		return currentHistoCryptoRepoFile;
	}
}
