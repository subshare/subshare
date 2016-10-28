package org.subshare.core.pgp.transport;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class PgpTransportFactoryRegistryImpl implements PgpTransportFactoryRegistry {

	private static class PgpTransportFactoryRegistryHolder {
		public static final PgpTransportFactoryRegistryImpl instance = new PgpTransportFactoryRegistryImpl();
	}

	public static PgpTransportFactoryRegistry getInstance() {
		return PgpTransportFactoryRegistryHolder.instance;
	}

	protected PgpTransportFactoryRegistryImpl() { }

	private List<PgpTransportFactory> pgpTransportFactories;

	@Override
	public PgpTransportFactory getPgpTransportFactoryOrFail(URL url) {
		PgpTransportFactory pgpTransportFactory = getPgpTransportFactory(url);
		if (pgpTransportFactory == null)
			throw new IllegalStateException("There is no PgpTransportFactory supporting this URL: " + url);

		return pgpTransportFactory;
	}

	@Override
	public PgpTransportFactory getPgpTransportFactory(URL remoteRoot) {
		for (PgpTransportFactory factory : getPgpTransportFactories()) {
			if (factory.isSupported(remoteRoot))
				return factory;
		}
		return null;
	}

	@Override
	public List<PgpTransportFactory> getPgpTransportFactories(URL remoteRoot) {
		List<PgpTransportFactory> result = new ArrayList<PgpTransportFactory>();
		for (PgpTransportFactory factory : getPgpTransportFactories()) {
			if (factory.isSupported(remoteRoot))
				result.add(factory);
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public synchronized List<PgpTransportFactory> getPgpTransportFactories() {
		List<PgpTransportFactory> pgpTransportFactories = this.pgpTransportFactories;
		if (pgpTransportFactories == null) {
			pgpTransportFactories = loadPgpTransportFactoriesViaServiceLoader();
			sortPgpTransportFactories(pgpTransportFactories);
			this.pgpTransportFactories = pgpTransportFactories = Collections.unmodifiableList(pgpTransportFactories);
		}
		return pgpTransportFactories;
	}

	private static List<PgpTransportFactory> loadPgpTransportFactoriesViaServiceLoader() {
		ArrayList<PgpTransportFactory> pgpTransportFactories = new ArrayList<PgpTransportFactory>();
		ServiceLoader<PgpTransportFactory> sl = ServiceLoader.load(PgpTransportFactory.class);
		for (Iterator<PgpTransportFactory> it = sl.iterator(); it.hasNext(); ) {
			pgpTransportFactories.add(it.next());
		}
		pgpTransportFactories.trimToSize();
		return pgpTransportFactories;
	}

	protected static int _compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

	private static void sortPgpTransportFactories(List<PgpTransportFactory> pgpTransportFactories) {
		Collections.sort(pgpTransportFactories, new Comparator<PgpTransportFactory>() {
			@Override
			public int compare(PgpTransportFactory o1, PgpTransportFactory o2) {
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

	@Override
	public <F extends PgpTransportFactory> F getPgpTransportFactoryOrFail(Class<F> factoryClass) {
		F pgpTransportFactory = getPgpTransportFactory(factoryClass);
		if (pgpTransportFactory == null)
			throw new IllegalArgumentException("There is no factory registered implementing this interface or extending this class: " + factoryClass.getName());

		return pgpTransportFactory;
	}

	@Override
	public <F extends PgpTransportFactory> F getPgpTransportFactory(Class<F> factoryClass) {
		AssertUtil.assertNotNull("factoryClass", factoryClass);
		List<PgpTransportFactory> pgpTransportFactories = getPgpTransportFactories();
		for (PgpTransportFactory pgpTransportFactory : pgpTransportFactories) {
			if (factoryClass.isInstance(pgpTransportFactory)) {
				return factoryClass.cast(pgpTransportFactory);
			}
		}
		return null;
	}
}
