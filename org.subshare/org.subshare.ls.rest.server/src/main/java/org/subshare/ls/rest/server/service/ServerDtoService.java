package org.subshare.ls.rest.server.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.ServerDto;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerDtoConverter;
import org.subshare.core.server.ServerRegistry;

import co.codewizards.cloudstore.core.dto.ListDto;

/**
 * @deprecated Not needed - or is it?
 */
@Deprecated
@Path("ServerDto")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class ServerDtoService {

	@GET
	public ListDto<ServerDto> getServerDtos() {
		final ListDto<ServerDto> result = new ListDto<ServerDto>();
		final ServerDtoConverter converter = new ServerDtoConverter();

		for (Server server : ServerRegistry.getInstance().getServers())
			result.getElements().add(converter.toServerDto(server));

		return result;
	}

}
