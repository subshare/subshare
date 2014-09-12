package org.subshare.rest.client.transport.command;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.SsDirectoryDto;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.rest.client.request.MakeDirectory;

public class SsMakeDirectory extends MakeDirectory {

	protected final SsDirectoryDto directoryDto;

	public SsMakeDirectory(final String repositoryName, final String path, final Date lastModified, final SsDirectoryDto directoryDto) {
		super(repositoryName, path, lastModified);
		this.directoryDto = assertNotNull("directoryDto", directoryDto);
	}

	@Override
	protected Response _execute() {
		WebTarget webTarget = createWebTarget("_makeDirectory", urlEncode(repositoryName), encodePath(path));

		if (lastModified != null)
			webTarget = webTarget.queryParam("lastModified", new DateTime(lastModified));

		return assignCredentials(webTarget.request()).post(Entity.entity(directoryDto, MediaType.APPLICATION_XML_TYPE));
	}

}
