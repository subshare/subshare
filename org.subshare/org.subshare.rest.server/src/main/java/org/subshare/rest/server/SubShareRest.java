package org.subshare.rest.server;

import javax.ws.rs.ApplicationPath;

import org.subshare.rest.server.service.CryptoChangeSetDtoService;
import org.subshare.rest.server.service.TestSubShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.rest.server.CloudStoreRest;

@ApplicationPath("SubShareRest")
public class SubShareRest extends CloudStoreRest {
	private static final Logger logger = LoggerFactory.getLogger(SubShareRest.class);

	static {
		logger.debug("<static_init>: Class loaded.");
	}

	{
		logger.debug("<init>: Instance created.");

		registerClasses(
				CryptoChangeSetDtoService.class,
				TestSubShareService.class
				);
	}

}
