package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.HistoCryptoRepoFileDto;
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
		this.transaction = assertNotNull(transaction, "transaction");
	}

	public CurrentHistoCryptoRepoFileDto toCurrentHistoCryptoRepoFileDto(final CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile, boolean withHistoCryptoRepoFileDto) {
		assertNotNull(currentHistoCryptoRepoFile, "currentHistoCryptoRepoFile");
		final CurrentHistoCryptoRepoFileDto result = new CurrentHistoCryptoRepoFileDto();

		final HistoCryptoRepoFile histoCryptoRepoFile = assertNotNull(currentHistoCryptoRepoFile.getHistoCryptoRepoFile(), "currentHistoCryptoRepoFile.histoCryptoRepoFile");
		final CryptoRepoFile cryptoRepoFile = assertNotNull(currentHistoCryptoRepoFile.getCryptoRepoFile(), "currentHistoCryptoRepoFile.cryptoRepoFile");

		if (withHistoCryptoRepoFileDto) {
			final HistoCryptoRepoFileDto histoCryptoRepoFileDto = HistoCryptoRepoFileDtoConverter.create(transaction).toHistoCryptoRepoFileDto(histoCryptoRepoFile);
			result.setHistoCryptoRepoFileDto(histoCryptoRepoFileDto);
		}
		else {
			result.setHistoCryptoRepoFileId(histoCryptoRepoFile.getHistoCryptoRepoFileId());
			result.setCryptoRepoFileId(cryptoRepoFile.getCryptoRepoFileId());
		}

		result.setSignature(currentHistoCryptoRepoFile.getSignature());

		return result;
	}

	public CurrentHistoCryptoRepoFile putCurrentHistoCryptoRepoFile(final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto) {
		assertNotNull(currentHistoCryptoRepoFileDto, "cryptoRepoFileOnServerDto");
		final CurrentHistoCryptoRepoFileDao currentHistoCryptoRepoFileDao = transaction.getDao(CurrentHistoCryptoRepoFileDao.class);
		final HistoCryptoRepoFileDao histoCryptoRepoFileDao = transaction.getDao(HistoCryptoRepoFileDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

		final HistoCryptoRepoFileDto histoCryptoRepoFileDto = currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto();

		final CryptoRepoFile cryptoRepoFile;
		final HistoCryptoRepoFile histoCryptoRepoFile;
		if (histoCryptoRepoFileDto != null) {
			histoCryptoRepoFile = HistoCryptoRepoFileDtoConverter.create(transaction).putHistoCryptoRepoFile(histoCryptoRepoFileDto);
			cryptoRepoFile = assertNotNull(histoCryptoRepoFile.getCryptoRepoFile(),
					"histoCryptoRepoFile[" + histoCryptoRepoFile.getHistoCryptoRepoFileId() + "].cryptoRepoFile");
		}
		else {
			cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(currentHistoCryptoRepoFileDto.getCryptoRepoFileId());
			histoCryptoRepoFile = histoCryptoRepoFileDao.getHistoCryptoRepoFileOrFail(currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileId());
		}

		CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile = currentHistoCryptoRepoFileDao.getCurrentHistoCryptoRepoFile(cryptoRepoFile);
		if (currentHistoCryptoRepoFile == null) {
			currentHistoCryptoRepoFile = new CurrentHistoCryptoRepoFile();
			currentHistoCryptoRepoFile.setCryptoRepoFile(cryptoRepoFile);
		}
		else {
			if (! cryptoRepoFile.equals(currentHistoCryptoRepoFile.getCryptoRepoFile()))
				throw new IllegalArgumentException(String.format("cryptoRepoFile != currentHistoCryptoRepoFile.cryptoRepoFile :: %s != %s :: %s",
						cryptoRepoFile, currentHistoCryptoRepoFile.getCryptoRepoFile(), currentHistoCryptoRepoFile));
		}

		currentHistoCryptoRepoFile.setHistoCryptoRepoFile(histoCryptoRepoFile);
		currentHistoCryptoRepoFile.setSignature(currentHistoCryptoRepoFileDto.getSignature());

		currentHistoCryptoRepoFile = currentHistoCryptoRepoFileDao.makePersistent(currentHistoCryptoRepoFile);

		transaction.flush();
		return currentHistoCryptoRepoFile;
	}
}
