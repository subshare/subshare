package org.subshare.rest.client.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.SsDeleteModificationDto;

public class Delete extends co.codewizards.cloudstore.rest.client.request.Delete {

	private final SsDeleteModificationDto deleteModificationDto;

	public Delete(final String repositoryName, final SsDeleteModificationDto deleteModificationDto) {
		super(repositoryName, null);
		this.deleteModificationDto = assertNotNull("deleteModificationDto", deleteModificationDto);
	}

	@Override
	protected Response _execute() {
		return assignCredentials(createWebTarget("_delete", urlEncode(repositoryName)).request())
				.put(Entity.entity(deleteModificationDto, MediaType.APPLICATION_XML_TYPE));
	}
}
