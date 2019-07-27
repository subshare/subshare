package org.subshare.gui.histo;

import static java.util.Objects.*;

import java.util.Date;

import org.subshare.core.dto.HistoFrameDto;

import co.codewizards.cloudstore.core.Uid;

public class HistoFrameListItem {

	private final HistoFrameDto histoFrameDto;
	private final String signingUserName;

	public HistoFrameListItem(final HistoFrameDto histoFrameDto, final String signingUserName) {
		// TODO pass resolved user for signingUserRepoKeyId! Or resolve+assign it afterwards!
		this.histoFrameDto = requireNonNull(histoFrameDto, "histoFrameDto");
		requireNonNull(histoFrameDto.getSignature(), "histoFrameDto.signature");
		this.signingUserName = requireNonNull(signingUserName, "signingUserName");
	}

	public HistoFrameDto getHistoFrameDto() {
		return histoFrameDto;
	}

	public Date getSignatureCreated() {
		return histoFrameDto.getSignature().getSignatureCreated();
	}

	public Uid getSigningUserRepoKeyId() {
		return histoFrameDto.getSignature().getSigningUserRepoKeyId();
	}

	public String getSigningUserName() {
		return signingUserName;
	}
}
