package org.subshare.ls.core.invoke.filter;

import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;

import java.lang.reflect.Proxy;

import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;

import co.codewizards.cloudstore.ls.core.invoke.filter.AbstractInvocationFilter;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;

public class AllowSubShareInvocationFilter extends AbstractInvocationFilter {

	@Override
	public Boolean canInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		final Class<?> targetClass = extMethodInvocationRequest.getTargetClass();

		if (PgpPrivateKeyPassphraseStore.class.isAssignableFrom(targetClass))
			return canInvoke_PgpPrivateKeyPassphraseStore(extMethodInvocationRequest);

		if (PgpAuthenticationCallback.class.isAssignableFrom(targetClass))
			return false;

		if (isWhiteListed(targetClass))
			return true;

		if (Proxy.isProxyClass(targetClass)) {
			for (final Class<?> iface : getAllInterfaces(targetClass)) {
				if (isWhiteListed(iface))
					return true;
			}
		}

		return null;
	}

	private boolean isWhiteListed(Class<?> classOrInterface) {
		return classOrInterface.getName().startsWith("org.subshare.");
	}

	private boolean canInvoke_PgpPrivateKeyPassphraseStore(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		final String methodName = extMethodInvocationRequest.getMethodInvocationRequest().getMethodName();
		return "getInstance".equals(methodName)
				|| "hasPassphrase".equals(methodName)
				|| "putPassphrase".equals(methodName)
				|| "getPgpKeyIdsHavingPassphrase".equals(methodName);
	}
}
