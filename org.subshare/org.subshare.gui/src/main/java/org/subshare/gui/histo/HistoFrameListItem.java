package org.subshare.gui.histo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import org.subshare.core.dto.HistoFrameDto;

import co.codewizards.cloudstore.core.dto.Uid;

public class HistoFrameListItem {

	private final HistoFrameDto histoFrameDto;

	public HistoFrameListItem(final HistoFrameDto histoFrameDto) {
		// TODO pass resolved user for signingUserRepoKeyId! Or resolve+assign it afterwards!
		this.histoFrameDto = assertNotNull("histoFrameDto", histoFrameDto);
		assertNotNull("histoFrameDto.signature", histoFrameDto.getSignature());
	}

	public HistoFrameDto getHistoFrameDto() {
		return histoFrameDto;
	}

	public Date getDate() {
		final Date sealed = histoFrameDto.getSealed();
		return sealed != null ? sealed : histoFrameDto.getSignature().getSignatureCreated();
	}

	public Uid getSigningUserRepoKeyId() {
		return histoFrameDto.getSignature().getSigningUserRepoKeyId();
	}

	public String getSigningUserNameName() {
		// TODO resolve the user and return a nice name here!
		return String.valueOf(histoFrameDto.getSignature().getSigningUserRepoKeyId());
	}
}
