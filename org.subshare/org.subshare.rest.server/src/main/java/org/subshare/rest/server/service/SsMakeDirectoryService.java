package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.context.RepoFileContext;
import org.subshare.core.dto.SsDirectoryDto;

import co.codewizards.cloudstore.rest.server.service.MakeDirectoryService;

@Path("_makeDirectory/{repositoryName}") // need to redeclare - seems not to be inherited
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class SsMakeDirectoryService extends MakeDirectoryService {

	@POST
	public void makeDirectory(final SsDirectoryDto directoryDto)
	{
		makeDirectory("", directoryDto);
	}

	@POST
	@Path("{path:.*}")
	public void makeDirectory(@PathParam("path") final String path, final SsDirectoryDto directoryDto)
	{
		assertNotNull("path", path);
		assertNotNull("directoryDto", directoryDto);

		RepoFileContext.setContext(new RepoFileContext(path, directoryDto));
		try {
			super.makeDirectory(path);
		} finally {
			RepoFileContext.setContext(null);
		}
	}

	@Override
	@POST
	@Path("_SsMakeDirectoryService_NOT_SUPPORTED_1")
	public void makeDirectory()
	{
		throw new UnsupportedOperationException("Missing directoryDto!");
	}

	@Override
	@POST
	@Path("_SsMakeDirectoryService_NOT_SUPPORTED_2")
	public void makeDirectory(@PathParam("path") final String path)
	{
		throw new UnsupportedOperationException("Missing directoryDto!");
	}

}
