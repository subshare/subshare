package org.subshare.rest.server;

import javax.ws.rs.ApplicationPath;

import org.subshare.rest.server.service.CryptoKeyChangeSetDTOService;
import org.subshare.rest.server.service.TestSubShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.rest.server.CloudStoreREST;

@ApplicationPath("SubShareREST")
public class SubShareREST extends CloudStoreREST {
	private static final Logger logger = LoggerFactory.getLogger(SubShareREST.class);

	static {
		logger.debug("<static_init>: Class loaded.");
	}

	{
		logger.debug("<init>: Instance created.");

		registerClasses(
				CryptoKeyChangeSetDTOService.class,
				TestSubShareService.class
				);
	}

}
