package org.subshare.core.pgp.transport;

import java.net.URL;
import java.util.List;

public interface PgpTransportFactoryRegistry {

	PgpTransportFactory getPgpTransportFactoryOrFail(URL url);

	PgpTransportFactory getPgpTransportFactory(URL remoteRoot);

	List<PgpTransportFactory> getPgpTransportFactories(URL remoteRoot);

	List<PgpTransportFactory> getPgpTransportFactories();

	<F extends PgpTransportFactory> F getPgpTransportFactoryOrFail(Class<F> factoryClass);

	<F extends PgpTransportFactory> F getPgpTransportFactory(Class<F> factoryClass);

}
