package org.subshare.rest.client.transport.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.CryptoChangeSetDto;

import co.codewizards.cloudstore.rest.client.command.AbstractCommand;

public class GetCryptoChangeSetDto extends AbstractCommand<CryptoChangeSetDto> {

	private final String repositoryName;

	public GetCryptoChangeSetDto(final String repositoryName) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
	}

	@Override
	public CryptoChangeSetDto execute() {
		final WebTarget webTarget = createWebTarget(getPath(CryptoChangeSetDto.class), urlEncode(repositoryName));
		final CryptoChangeSetDto dto = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(CryptoChangeSetDto.class);
		return dto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
