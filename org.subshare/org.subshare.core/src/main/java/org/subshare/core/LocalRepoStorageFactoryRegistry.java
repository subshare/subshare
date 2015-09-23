package org.subshare.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class LocalRepoStorageFactoryRegistry {

	private static class Holder {
		static LocalRepoStorageFactoryRegistry instance = new LocalRepoStorageFactoryRegistry();
	}

	private List<LocalRepoStorageFactory> localRepoStorageFactories;

	protected LocalRepoStorageFactoryRegistry() { }

	public static LocalRepoStorageFactoryRegistry getInstance() {
		return Holder.instance;
	}

	public synchronized List<LocalRepoStorageFactory> getLocalRepoStorageFactories() {
		List<LocalRepoStorageFactory> localRepoStorageFactories = this.localRepoStorageFactories;
		if (localRepoStorageFactories == null) {
			localRepoStorageFactories = loadLocalRepoStorageFactoriesViaServiceLoader();
			sortLocalRepoStorageFactories(localRepoStorageFactories);
			this.localRepoStorageFactories = localRepoStorageFactories = Collections.unmodifiableList(localRepoStorageFactories);
		}
		return localRepoStorageFactories;
	}

	private static List<LocalRepoStorageFactory> loadLocalRepoStorageFactoriesViaServiceLoader() {
		final ArrayList<LocalRepoStorageFactory> localRepoStorageFactories = new ArrayList<LocalRepoStorageFactory>();
		final ServiceLoader<LocalRepoStorageFactory> sl = ServiceLoader.load(LocalRepoStorageFactory.class);
		for (final Iterator<LocalRepoStorageFactory> it = sl.iterator(); it.hasNext(); ) {
			localRepoStorageFactories.add(it.next());
		}
		localRepoStorageFactories.trimToSize();
		return localRepoStorageFactories;
	}

	protected static int _compare(final int x, final int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

	private static void sortLocalRepoStorageFactories(final List<LocalRepoStorageFactory> localRepoStorageFactories) {
		Collections.sort(localRepoStorageFactories, new Comparator<LocalRepoStorageFactory>() {
			@Override
			public int compare(final LocalRepoStorageFactory o1, final LocalRepoStorageFactory o2) {
				final int result = -1 * _compare(o1.getPriority(), o2.getPriority());
				if (result != 0)
					return result;

				return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
		});
	}

	/**
	 * Gets the default {@link LocalRepoStorageFactory}.
	 * @return the default {@link LocalRepoStorageFactory}. Never <code>null</code>.
	 */
	public LocalRepoStorageFactory getLocalRepoStorageFactoryOrFail() {
		final List<LocalRepoStorageFactory> localRepoStorageFactories = getLocalRepoStorageFactories();
		if (localRepoStorageFactories.isEmpty())
			throw new IllegalStateException("There is no LocalRepoStorageFactory registered!");

		return localRepoStorageFactories.get(0);
	}
}
