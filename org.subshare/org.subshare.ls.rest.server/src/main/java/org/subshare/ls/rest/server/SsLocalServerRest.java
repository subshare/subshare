package org.subshare.ls.rest.server;

import org.subshare.ls.rest.server.service.ServerDtoService;

import co.codewizards.cloudstore.ls.rest.server.LocalServerRest;

public class SsLocalServerRest extends LocalServerRest {

	{
		registerClasses(
				// BEGIN services
				ServerDtoService.class
				// END services

				// ... no additional infrastructure classes needed - services are sufficient ...
				);
	}

}
