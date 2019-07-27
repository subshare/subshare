package org.subshare.rest.client.transport.request;

import static java.util.Objects.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.HistoFrameDto;

import co.codewizards.cloudstore.rest.client.request.VoidRequest;

public class PutHistoFrameDto extends VoidRequest {

	private final String repositoryName;
	private final HistoFrameDto histoFrameDto;

	public PutHistoFrameDto(final String repositoryName, final HistoFrameDto histoFrameDto) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.histoFrameDto = requireNonNull(histoFrameDto, "histoFrameDto");
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createWebTarget(getPath(HistoFrameDto.class), urlEncode(repositoryName));
		return assignCredentials(webTarget.request()).put(Entity.entity(histoFrameDto, MediaType.APPLICATION_XML_TYPE));
	}
}
