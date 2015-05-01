package org.subshare.ls.rest.client.request;

import java.util.List;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.ServerDto;

import co.codewizards.cloudstore.core.dto.ListDto;
import co.codewizards.cloudstore.ls.rest.client.request.AbstractRequest;

/**
 * @deprecated Not needed - or is it?
 */
@Deprecated
public class GetServerDtos extends AbstractRequest<List<ServerDto>> {

	@Override
	public List<ServerDto> execute() {
		final WebTarget webTarget = createWebTarget(getPath(ServerDto.class));

		@SuppressWarnings("unchecked")
		final ListDto<ServerDto> listDto = assignCredentials(webTarget.request(MediaType.APPLICATION_XML_TYPE)).get(ListDto.class);

		return listDto.getElements();
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
