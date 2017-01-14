package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.repo.transport.CryptreeServerFileRepoTransport;

import co.codewizards.cloudstore.rest.server.service.BeginPutFileService;

@Path("_beginPutFile/{repositoryName}") // need to redeclare - seems not to be inherited
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class SsBeginPutFileService extends BeginPutFileService {

	@PUT
	@Path("{path:.*}")
	public void beginPutFile(@PathParam("path") String path, final SsNormalFileDto normalFileDto) {
		assertNotNull(path, "path");
		assertNotNull(normalFileDto, "normalFileDto");

//		RepoFileContext.setContext(new RepoFileContext(path, normalFileDto, null));
//		try {
//			super.beginPutFile(path);
//		} finally {
//			RepoFileContext.setContext(null);
//		}

		final CryptreeServerFileRepoTransport repoTransport = (CryptreeServerFileRepoTransport) authenticateAndCreateLocalRepoTransport();
		try {
			path = repoTransport.unprefixPath(path);
			repoTransport.beginPutFile(path, normalFileDto);
		} finally {
			repoTransport.close();
		}
	}

	@POST // just annotating to get it out of the way (otherwise the super-method-stuff seems to be inherited)
	@Override
	public void beginPutFile(@PathParam("path") final String path) {
		throw new UnsupportedOperationException("Missing normalFileDto!");
	}
}
