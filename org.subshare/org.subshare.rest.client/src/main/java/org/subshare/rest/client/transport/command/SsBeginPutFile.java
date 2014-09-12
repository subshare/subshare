package org.subshare.rest.client.transport.command;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.SsNormalFileDto;

import co.codewizards.cloudstore.rest.client.request.BeginPutFile;

public class SsBeginPutFile extends BeginPutFile {

	private final SsNormalFileDto normalFileDto;

	public SsBeginPutFile(final String repositoryName, final String path, final SsNormalFileDto normalFileDto) {
		super(repositoryName, path);
		this.normalFileDto = assertNotNull("normalFileDto", normalFileDto);
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_beginPutFile", urlEncode(repositoryName), encodePath(path))
				.request()).post(Entity.entity(normalFileDto, MediaType.APPLICATION_XML_TYPE));
	}

}
