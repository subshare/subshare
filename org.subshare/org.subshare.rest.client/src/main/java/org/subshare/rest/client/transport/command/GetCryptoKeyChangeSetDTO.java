package org.subshare.rest.client.transport.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.CryptoKeyChangeSetDTO;

import co.codewizards.cloudstore.rest.client.command.AbstractCommand;

public class GetCryptoKeyChangeSetDTO extends AbstractCommand<CryptoKeyChangeSetDTO> {

	private final String repositoryName;

	public GetCryptoKeyChangeSetDTO(final String repositoryName) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
	}

	@Override
	public CryptoKeyChangeSetDTO execute() {
		final WebTarget webTarget = createWebTarget(getPath(CryptoKeyChangeSetDTO.class), urlEncode(repositoryName));
		final CryptoKeyChangeSetDTO dto = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(CryptoKeyChangeSetDTO.class);
		return dto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
