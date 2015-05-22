package org.subshare.core.repo.transport;

import org.subshare.core.dto.SsDeleteModificationDto;

import co.codewizards.cloudstore.core.repo.transport.LocalRepoTransport;

public interface CryptreeFileRepoTransport extends LocalRepoTransport {

	void delete(SsDeleteModificationDto deleteModificationDto);

}
