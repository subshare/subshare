package org.subshare.core.repo;

import java.util.UUID;

public class CreateRepositoryContext {

	public static final ThreadLocal<UUID> repositoryIdThreadLocal = new ThreadLocal<UUID>();

}
