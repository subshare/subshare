package org.subshare.rest.server.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.TestException;

@Path("_testSubShare")
@Consumes(MediaType.WILDCARD)
public class TestSubShareService
{
	private static final Logger logger = LoggerFactory.getLogger(TestSubShareService.class);

	{
		logger.debug("<init>: Instance created.");
	}

	private @QueryParam("exception") boolean exception;

	@POST
	public String testPOST()
	{
		return test();
	}

	@GET
	public String test()
	{
		if (exception)
			throw new TestException("Test");

		return "SUCCESS";
	}
}
