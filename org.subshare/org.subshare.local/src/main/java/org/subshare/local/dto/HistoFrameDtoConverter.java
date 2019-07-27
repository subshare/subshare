package org.subshare.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

import org.subshare.core.dto.HistoFrameDto;
import org.subshare.local.persistence.HistoFrame;
import org.subshare.local.persistence.HistoFrameDao;

import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class HistoFrameDtoConverter {

	private final LocalRepoTransaction transaction;

	public static HistoFrameDtoConverter create(final LocalRepoTransaction transaction) {
		return createObject(HistoFrameDtoConverter.class, transaction);
	}

	protected HistoFrameDtoConverter(final LocalRepoTransaction transaction) {
		this.transaction = requireNonNull(transaction, "transaction");
	}

	public HistoFrameDto toHistoFrameDto(final HistoFrame histoFrame) {
		requireNonNull(histoFrame, "histoFrame");
		HistoFrameDto result = new HistoFrameDto();
		result.setHistoFrameId(histoFrame.getHistoFrameId());
		result.setFromRepositoryId(histoFrame.getFromRepositoryId());
//		result.setPreviousHistoFrameId(
//				histoFrame.getPreviousHistoFrame() == null ? null : histoFrame.getPreviousHistoFrame().getHistoFrameId());
		result.setSealed(histoFrame.getSealed());
		result.setSignature(histoFrame.getSignature());
		return result;
	}

	public HistoFrame putHistoFrameDto(final HistoFrameDto histoFrameDto) {
		requireNonNull(histoFrameDto, "histoFrameDto");

		final HistoFrameDao dao = transaction.getDao(HistoFrameDao.class);
		HistoFrame result = dao.getHistoFrame(histoFrameDto.getHistoFrameId());
		if (result == null)
			result = new HistoFrame(histoFrameDto.getHistoFrameId());

		result.setFromRepositoryId(histoFrameDto.getFromRepositoryId());
		result.setSealed(histoFrameDto.getSealed());
		result.setSignature(histoFrameDto.getSignature());
		result = dao.makePersistent(result);
		return result;
	}
}
