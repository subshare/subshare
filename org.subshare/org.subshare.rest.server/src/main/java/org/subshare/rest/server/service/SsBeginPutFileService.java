package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.context.RepoFileContext;
import org.subshare.core.dto.SsNormalFileDto;

import co.codewizards.cloudstore.rest.server.service.BeginPutFileService;

@Path("_beginPutFile/{repositoryName}") // need to redeclare - seems not to be inherited
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class SsBeginPutFileService extends BeginPutFileService {

	@POST
	@Path("{path:.*}")
	public void beginPutFile(@PathParam("path") final String path, final SsNormalFileDto normalFileDto) {
		assertNotNull("path", path);
		assertNotNull("normalFileDto", normalFileDto);

		// TODO verify signature!

		RepoFileContext.setContext(new RepoFileContext(path, normalFileDto));
		try {
			super.beginPutFile(path);
		} finally {
			RepoFileContext.setContext(null);
		}
	}

	@POST
	@Path("_SsBeginPutFileService_NOT_SUPPORTED_1")
	@Override
	public void beginPutFile(final String path) {
		throw new UnsupportedOperationException("Missing normalFileDto!");
	}

}
