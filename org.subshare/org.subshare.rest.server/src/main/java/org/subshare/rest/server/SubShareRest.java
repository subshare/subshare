package org.subshare.rest.server;

import javax.ws.rs.ApplicationPath;

import org.subshare.rest.server.service.SsBeginPutFileService;
import org.subshare.rest.server.service.SsMakeDirectoryService;
import org.subshare.rest.server.service.CryptoChangeSetDtoService;
import org.subshare.rest.server.service.TestSubShareService;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.rest.server.CloudStoreRest;
import co.codewizards.cloudstore.rest.server.service.BeginPutFileService;
import co.codewizards.cloudstore.rest.server.service.MakeDirectoryService;

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

	@Override
	public ResourceConfig register(Class<?> componentClass) {
		if (componentClass == BeginPutFileService.class)
			componentClass = SsBeginPutFileService.class;

		if (componentClass == MakeDirectoryService.class)
			componentClass = SsMakeDirectoryService.class;

		return super.register(componentClass);
	}

}
