package org.subshare.core.locker.transport;

import static java.util.Objects.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class LockerTransportFactoryRegistry {

	private static class LockerTransportFactoryRegistryHolder {
		public static final LockerTransportFactoryRegistry instance = new LockerTransportFactoryRegistry();
	}

	public static LockerTransportFactoryRegistry getInstance() {
		return LockerTransportFactoryRegistryHolder.instance;
	}

	protected LockerTransportFactoryRegistry() { }

	private List<LockerTransportFactory> lockerTransportFactories;

	public LockerTransportFactory getLockerTransportFactoryOrFail(URL url) {
		LockerTransportFactory lockerTransportFactory = getLockerTransportFactory(url);
		if (lockerTransportFactory == null)
			throw new IllegalStateException("There is no LockerTransportFactory supporting this URL: " + url);

		return lockerTransportFactory;
	}

	public LockerTransportFactory getLockerTransportFactory(URL remoteRoot) {
		for (LockerTransportFactory factory : getLockerTransportFactories()) {
			if (factory.isSupported(remoteRoot))
				return factory;
		}
		return null;
	}

	public List<LockerTransportFactory> getLockerTransportFactories(URL remoteRoot) {
		List<LockerTransportFactory> result = new ArrayList<LockerTransportFactory>();
		for (LockerTransportFactory factory : getLockerTransportFactories()) {
			if (factory.isSupported(remoteRoot))
				result.add(factory);
		}
		return Collections.unmodifiableList(result);
	}

	public synchronized List<LockerTransportFactory> getLockerTransportFactories() {
		List<LockerTransportFactory> lockerTransportFactories = this.lockerTransportFactories;
		if (lockerTransportFactories == null) {
			lockerTransportFactories = loadLockerTransportFactoriesViaServiceLoader();
			sortLockerTransportFactories(lockerTransportFactories);
			this.lockerTransportFactories = lockerTransportFactories = Collections.unmodifiableList(lockerTransportFactories);
		}
		return lockerTransportFactories;
	}

	private static List<LockerTransportFactory> loadLockerTransportFactoriesViaServiceLoader() {
		ArrayList<LockerTransportFactory> lockerTransportFactories = new ArrayList<LockerTransportFactory>();
		ServiceLoader<LockerTransportFactory> sl = ServiceLoader.load(LockerTransportFactory.class);
		for (Iterator<LockerTransportFactory> it = sl.iterator(); it.hasNext(); ) {
			lockerTransportFactories.add(it.next());
		}
		lockerTransportFactories.trimToSize();
		return lockerTransportFactories;
	}

	protected static int _compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

	private static void sortLockerTransportFactories(List<LockerTransportFactory> lockerTransportFactories) {
		Collections.sort(lockerTransportFactories, new Comparator<LockerTransportFactory>() {
			@Override
			public int compare(LockerTransportFactory o1, LockerTransportFactory o2) {
				int result = -1 * _compare(o1.getPriority(), o2.getPriority());
				if (result != 0)
					return result;

				String name1 = o1.getName() == null ? "" : o1.getName();
				String name2 = o2.getName() == null ? "" : o2.getName();
				result = name1.compareTo(name2);
				if (result != 0)
					return result;

				return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
		});
	}

	public <F extends LockerTransportFactory> F getLockerTransportFactoryOrFail(Class<F> factoryClass) {
		F lockerTransportFactory = getLockerTransportFactory(factoryClass);
		if (lockerTransportFactory == null)
			throw new IllegalArgumentException("There is no factory registered implementing this interface or extending this class: " + factoryClass.getName());

		return lockerTransportFactory;
	}

	public <F extends LockerTransportFactory> F getLockerTransportFactory(Class<F> factoryClass) {
		requireNonNull(factoryClass, "factoryClass");
		List<LockerTransportFactory> lockerTransportFactories = getLockerTransportFactories();
		for (LockerTransportFactory lockerTransportFactory : lockerTransportFactories) {
			if (factoryClass.isInstance(lockerTransportFactory)) {
				return factoryClass.cast(lockerTransportFactory);
			}
		}
		return null;
	}
}
