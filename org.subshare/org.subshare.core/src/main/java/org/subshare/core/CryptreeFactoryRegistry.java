package org.subshare.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class CryptreeFactoryRegistry {

	private static class Holder {
		static CryptreeFactoryRegistry instance = new CryptreeFactoryRegistry();
	}

	private List<CryptreeFactory> cryptreeFactories;

	protected CryptreeFactoryRegistry() { }

	public static CryptreeFactoryRegistry getInstance() {
		return Holder.instance;
	}

	public synchronized List<CryptreeFactory> getCryptreeFactories() {
		List<CryptreeFactory> cryptreeFactories = this.cryptreeFactories;
		if (cryptreeFactories == null) {
			cryptreeFactories = loadCryptreeFactoriesViaServiceLoader();
			sortCryptreeFactories(cryptreeFactories);
			this.cryptreeFactories = cryptreeFactories = Collections.unmodifiableList(cryptreeFactories);
		}
		return cryptreeFactories;
	}

	private static List<CryptreeFactory> loadCryptreeFactoriesViaServiceLoader() {
		final ArrayList<CryptreeFactory> cryptreeFactories = new ArrayList<CryptreeFactory>();
		final ServiceLoader<CryptreeFactory> sl = ServiceLoader.load(CryptreeFactory.class);
		for (final Iterator<CryptreeFactory> it = sl.iterator(); it.hasNext(); ) {
			cryptreeFactories.add(it.next());
		}
		cryptreeFactories.trimToSize();
		return cryptreeFactories;
	}

	protected static int _compare(final int x, final int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

	private static void sortCryptreeFactories(final List<CryptreeFactory> cryptreeFactories) {
		Collections.sort(cryptreeFactories, new Comparator<CryptreeFactory>() {
			@Override
			public int compare(final CryptreeFactory o1, final CryptreeFactory o2) {
				final int result = -1 * _compare(o1.getPriority(), o2.getPriority());
				if (result != 0)
					return result;

				return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
		});
	}

	/**
	 * Gets the default {@link CryptreeFactory}.
	 * @return the default {@link CryptreeFactory}. Never <code>null</code>.
	 */
	public CryptreeFactory getCryptreeFactoryOrFail() {
		final List<CryptreeFactory> cryptreeFactories = getCryptreeFactories();
		if (cryptreeFactories.isEmpty())
			throw new IllegalStateException("There is no CryptreeFactory registered!");

		return cryptreeFactories.get(0);
	}
}
