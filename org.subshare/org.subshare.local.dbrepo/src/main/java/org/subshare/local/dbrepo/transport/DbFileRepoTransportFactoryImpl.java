package org.subshare.local.dbrepo.transport;

import org.subshare.local.transport.CryptreeFileRepoTransportImpl;

import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.local.transport.FileRepoTransportFactory;

public class DbFileRepoTransportFactoryImpl extends FileRepoTransportFactory {

	public DbFileRepoTransportFactoryImpl() {
	}

	@Override
	public int getPriority() {
		return super.getPriority() + 2;
	}

	@Override
	protected RepoTransport _createRepoTransport() {
		if (isOnServer())
			return new DbFileRepoTransportImpl();
		else
			return new CryptreeFileRepoTransportImpl();
	}

	protected boolean isOnServer() {
		return isServerThread();
	}

	protected boolean isOnClient() {
		return ! isOnServer();
	}

	private static boolean isServerThread() {
		final StackTraceElement[] stackTrace = new Exception().getStackTrace();
		for (final StackTraceElement stackTraceElement : stackTrace) {
			final String className = stackTraceElement.getClassName();
			if ("org.eclipse.jetty.server.Server".equals(className))
				return true;

			if (className.startsWith("co.codewizards.cloudstore.rest.server.service."))
				return true;

			if (className.startsWith("org.subshare.rest.server.service."))
				return true;
		}
		return false;
	}

}
