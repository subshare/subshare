package org.subshare.rest.client.transport.request;

import static java.util.Objects.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.SsNormalFileDto;

import co.codewizards.cloudstore.rest.client.request.BeginPutFile;

public class SsBeginPutFile extends BeginPutFile {

	private final SsNormalFileDto normalFileDto;

	public SsBeginPutFile(final String repositoryName, final String path, final SsNormalFileDto normalFileDto) {
		super(repositoryName, path);
		this.normalFileDto = requireNonNull(normalFileDto, "normalFileDto");
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_beginPutFile", urlEncode(repositoryName), encodePath(path))
				.request()).put(Entity.entity(normalFileDto, MediaType.APPLICATION_XML_TYPE));
	}
}
