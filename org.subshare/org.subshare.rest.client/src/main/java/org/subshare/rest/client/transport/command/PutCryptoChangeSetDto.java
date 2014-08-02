package org.subshare.rest.client.transport.command;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.CryptoChangeSetDto;

import co.codewizards.cloudstore.rest.client.command.VoidCommand;

public class PutCryptoChangeSetDto extends VoidCommand {

	private final String repositoryName;
	private final CryptoChangeSetDto cryptoChangeSetDto;

	public PutCryptoChangeSetDto(final String repositoryName, final CryptoChangeSetDto cryptoChangeSetDto) {
		this.repositoryName = assertNotNull("repositoryName", repositoryName);
		this.cryptoChangeSetDto = assertNotNull("cryptoChangeSetDto", cryptoChangeSetDto);
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createWebTarget(getPath(CryptoChangeSetDto.class), urlEncode(repositoryName));
		return assignCredentials(webTarget.request()).put(Entity.entity(cryptoChangeSetDto, MediaType.APPLICATION_XML_TYPE));
	}
}
