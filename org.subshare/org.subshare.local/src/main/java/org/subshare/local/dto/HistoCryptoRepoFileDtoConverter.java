package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.HistoCryptoRepoFile;
import org.subshare.local.persistence.HistoCryptoRepoFileDao;
import org.subshare.local.persistence.HistoFrame;
import org.subshare.local.persistence.HistoFrameDao;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class HistoCryptoRepoFileDtoConverter {

	private final LocalRepoTransaction transaction;

	public static HistoCryptoRepoFileDtoConverter create(LocalRepoTransaction transaction) {
		return createObject(HistoCryptoRepoFileDtoConverter.class, transaction);
	}

	protected HistoCryptoRepoFileDtoConverter(final LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
	}

	public HistoCryptoRepoFileDto toHistoCryptoRepoFileDto(final HistoCryptoRepoFile histoCryptoRepoFile) {
		assertNotNull("cryptoRepoFileOnServer", histoCryptoRepoFile);
		final HistoCryptoRepoFileDto result = new HistoCryptoRepoFileDto();

		result.setHistoCryptoRepoFileId(histoCryptoRepoFile.getHistoCryptoRepoFileId());
		result.setHistoFrameId(histoCryptoRepoFile.getHistoFrame().getHistoFrameId());
		result.setCryptoRepoFileId(histoCryptoRepoFile.getCryptoRepoFile().getCryptoRepoFileId());

		final HistoCryptoRepoFile previousHistoCryptoRepoFile = histoCryptoRepoFile.getPreviousHistoCryptoRepoFile();
		result.setPreviousHistoCryptoRepoFileId(previousHistoCryptoRepoFile == null ? null : previousHistoCryptoRepoFile.getHistoCryptoRepoFileId());

		final CryptoKey cryptoKey = assertNotNull("cryptoRepoFileOnServer.cryptoKey", histoCryptoRepoFile.getCryptoKey());
		result.setCryptoKeyId(cryptoKey.getCryptoKeyId());

		final byte[] repoFileDtoData = assertNotNull("cryptoRepoFileOnServer.repoFileDtoData", histoCryptoRepoFile.getRepoFileDtoData());
		result.setRepoFileDtoData(repoFileDtoData);
		result.setDeleted(histoCryptoRepoFile.getDeleted());
		result.setDeletedByIgnoreRule(histoCryptoRepoFile.isDeletedByIgnoreRule());
		result.setSignature(assertNotNull("cryptoRepoFileOnServer.signature", histoCryptoRepoFile.getSignature()));

		return result;
	}

	public HistoCryptoRepoFile putHistoCryptoRepoFile(final HistoCryptoRepoFileDto histoCryptoRepoFileDto) {
		assertNotNull("cryptoRepoFileOnServerDto", histoCryptoRepoFileDto);
		final HistoFrameDao histoFrameDao = transaction.getDao(HistoFrameDao.class);
		final HistoCryptoRepoFileDao histoCryptoRepoFileDao = transaction.getDao(HistoCryptoRepoFileDao.class);
		final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);
		final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);

		HistoCryptoRepoFile histoCryptoRepoFile = histoCryptoRepoFileDao.getHistoCryptoRepoFile(histoCryptoRepoFileDto.getHistoCryptoRepoFileId());
		if (histoCryptoRepoFile == null)
			histoCryptoRepoFile = new HistoCryptoRepoFile(histoCryptoRepoFileDto.getHistoCryptoRepoFileId());

		final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFileOrFail(histoCryptoRepoFileDto.getCryptoRepoFileId());
		histoCryptoRepoFile.setCryptoRepoFile(cryptoRepoFile);

		if (histoCryptoRepoFileDto.getPreviousHistoCryptoRepoFileId() == null)
			histoCryptoRepoFile.setPreviousHistoCryptoRepoFile(null);
		else {
			final HistoCryptoRepoFile previous = histoCryptoRepoFileDao.getHistoCryptoRepoFileOrFail(histoCryptoRepoFileDto.getPreviousHistoCryptoRepoFileId());
			histoCryptoRepoFile.setPreviousHistoCryptoRepoFile(previous);
		}

		final Uid histoFrameId = assertNotNull("histoCryptoRepoFileDto.histoFrameId", histoCryptoRepoFileDto.getHistoFrameId());
		final HistoFrame histoFrame = histoFrameDao.getHistoFrameOrFail(histoFrameId);
		histoCryptoRepoFile.setHistoFrame(histoFrame);

		final CryptoKey cryptoKey = cryptoKeyDao.getCryptoKeyOrFail(histoCryptoRepoFileDto.getCryptoKeyId());
		histoCryptoRepoFile.setCryptoKey(cryptoKey);
		histoCryptoRepoFile.setRepoFileDtoData(histoCryptoRepoFileDto.getRepoFileDtoData());
		histoCryptoRepoFile.setDeleted(histoCryptoRepoFileDto.getDeleted());
		histoCryptoRepoFile.setDeletedByIgnoreRule(histoCryptoRepoFileDto.isDeletedByIgnoreRule());
		histoCryptoRepoFile.setSignature(histoCryptoRepoFileDto.getSignature());

		histoCryptoRepoFile = histoCryptoRepoFileDao.makePersistent(histoCryptoRepoFile);

		transaction.flush();
		return histoCryptoRepoFile;
	}
}
