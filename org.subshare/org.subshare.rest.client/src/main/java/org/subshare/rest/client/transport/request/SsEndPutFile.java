package org.subshare.rest.client.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.SsNormalFileDto;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.rest.client.request.EndPutFile;

public class SsEndPutFile extends EndPutFile {

	private final SsNormalFileDto normalFileDto;

	public SsEndPutFile(final String repositoryName, final String path, final SsNormalFileDto normalFileDto) {
		super(repositoryName, path, new DateTime(new Date(0)), 0, null);
		this.normalFileDto = assertNotNull("normalFileDto", normalFileDto);
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_endPutFile", urlEncode(repositoryName), encodePath(path))
				.request()).put(Entity.entity(normalFileDto, MediaType.APPLICATION_XML_TYPE));
	}
}
