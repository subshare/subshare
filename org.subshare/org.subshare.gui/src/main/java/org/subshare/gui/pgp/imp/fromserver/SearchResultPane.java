package org.subshare.gui.pgp.imp.fromserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.net.URL;
import java.util.List;

import org.subshare.core.pgp.transport.PgpTransport;
import org.subshare.core.pgp.transport.PgpTransportFactory;
import org.subshare.core.server.Server;
import org.subshare.gui.ls.PgpTransportFactoryRegistryLs;
import org.subshare.gui.ls.ServerRegistryLs;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class SearchResultPane extends WizardPageContentGridPane {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData;

	public SearchResultPane(final ImportPgpKeyFromServerData importPgpKeyFromServerData) {
		this.importPgpKeyFromServerData = assertNotNull("importPgpKeyFromServerData", importPgpKeyFromServerData);
		loadDynamicComponentFxml(SearchResultPane.class, this);
	}

	public void searchAsync() {
		assertFxApplicationThread();
		clearPreviousSearchResult();
		final String queryString = importPgpKeyFromServerData.getQueryString();

		new Service<Void>() {
			@Override
			protected Task<Void> createTask() {
				return new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						final List<Server> servers = ServerRegistryLs.getServerRegistry().getServers();
						for (final Server server : servers) {
							final URL serverUrl = server.getUrl();
							final PgpTransportFactory pgpTransportFactory = PgpTransportFactoryRegistryLs.getPgpTransportFactoryRegistry().getPgpTransportFactoryOrFail(serverUrl);
							final PgpTransport pgpTransport = pgpTransportFactory.createPgpTransport(serverUrl);
//							pgpTransport.exportPublicKeysMatchingQuery(queryString, out);
						}
						return null;
					}
				};
			}
		};
	}


	private void clearPreviousSearchResult() {
		// TODO clear!


		updateComplete();
	}

	@Override
	protected boolean isComplete() {
		return false;
	}

}
