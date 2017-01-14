package org.subshare.rest.client.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.CreateRepositoryRequestDto;

import co.codewizards.cloudstore.rest.client.request.VoidRequest;

public class CreateRepository extends VoidRequest {

	private final CreateRepositoryRequestDto createRepositoryRequestDto;

	public CreateRepository(final CreateRepositoryRequestDto createRepositoryRequestDto) {
		this.createRepositoryRequestDto = assertNotNull(createRepositoryRequestDto, "createRepositoryRequestDto");
	}

	@Override
	protected Response _execute() {
		return createWebTarget("_createRepository").request().put(Entity.entity(createRepositoryRequestDto, MediaType.APPLICATION_XML_TYPE));
	}
}
