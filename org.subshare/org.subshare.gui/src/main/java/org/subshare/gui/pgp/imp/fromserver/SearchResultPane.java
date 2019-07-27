package org.subshare.gui.pgp.imp.fromserver;

import static java.util.Objects.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.subshare.core.pgp.ImportKeysResult;
import org.subshare.core.pgp.ImportKeysResult.ImportedMasterKey;
import org.subshare.core.pgp.ImportKeysResult.ImportedSubKey;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.TempImportKeysResult;
import org.subshare.core.pgp.transport.PgpTransport;
import org.subshare.core.pgp.transport.PgpTransportFactory;
import org.subshare.core.server.Server;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.PgpTransportFactoryRegistryLs;
import org.subshare.gui.ls.ServerRegistryLs;
import org.subshare.gui.pgp.keytree.PgpKeyPgpKeyTreeItem;
import org.subshare.gui.pgp.keytree.PgpKeyTreeItem;
import org.subshare.gui.pgp.keytree.PgpKeyTreePane;
import org.subshare.gui.pgp.keytree.SimpleRootPgpKeyTreeItem;
import org.subshare.gui.wizard.WizardPageContentGridPane;

import co.codewizards.cloudstore.core.io.IByteArrayOutputStream;
import co.codewizards.cloudstore.ls.client.util.ByteArrayInputStreamLs;
import co.codewizards.cloudstore.ls.client.util.ByteArrayOutputStreamLs;
import javafx.beans.InvalidationListener;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class SearchResultPane extends WizardPageContentGridPane {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData;

	@FXML
	private Text headerText;

	@FXML
	private PgpKeyTreePane pgpKeyTreePane;

	public SearchResultPane(final ImportPgpKeyFromServerData importPgpKeyFromServerData) {
		this.importPgpKeyFromServerData = requireNonNull(importPgpKeyFromServerData, "importPgpKeyFromServerData"); //$NON-NLS-1$
		loadDynamicComponentFxml(SearchResultPane.class, this);
		pgpKeyTreePane.getCheckBoxVisibleForPgpKeyTreeItemClasses().add(PgpKeyPgpKeyTreeItem.class);

		pgpKeyTreePane.getCheckedTreeItems().addListener(new SetChangeListener<PgpKeyTreeItem<?>>() {
			@Override
			public void onChanged(SetChangeListener.Change<? extends PgpKeyTreeItem<?>> change) {
				final PgpKeyTreeItem<?> elementAdded = change.getElementAdded();
				if (elementAdded instanceof PgpKeyPgpKeyTreeItem)
					importPgpKeyFromServerData.getSelectedPgpKeyIds().add(((PgpKeyPgpKeyTreeItem)elementAdded).getPgpKey().getPgpKeyId());

				final PgpKeyTreeItem<?> elementRemoved = change.getElementRemoved();
				if (elementRemoved instanceof PgpKeyPgpKeyTreeItem)
					importPgpKeyFromServerData.getSelectedPgpKeyIds().remove(((PgpKeyPgpKeyTreeItem)elementRemoved).getPgpKey().getPgpKeyId());
			}
		});

		importPgpKeyFromServerData.getSelectedPgpKeyIds().addListener((InvalidationListener) observable -> updateComplete());
	}

	public void searchAsync() {
		assertFxApplicationThread();
		onSearchStart();
		final String queryString = importPgpKeyFromServerData.getQueryString();

		new Service<TempImportKeysResult>() {
			@Override
			protected Task<TempImportKeysResult> createTask() {
				return new SsTask<TempImportKeysResult>() {
					@Override
					protected TempImportKeysResult call() throws Exception {
						final Pgp pgp = PgpLs.getPgpOrFail();
						TempImportKeysResult tempImportKeysResult = null;
						final IByteArrayOutputStream bout = ByteArrayOutputStreamLs.create();
						final List<Server> servers = ServerRegistryLs.getServerRegistry().getServers();
						for (final Server server : servers) {
							final URL serverUrl = server.getUrl();
							final PgpTransportFactory pgpTransportFactory = PgpTransportFactoryRegistryLs.getPgpTransportFactoryRegistry().getPgpTransportFactoryOrFail(serverUrl);
							try (final PgpTransport pgpTransport = pgpTransportFactory.createPgpTransport(serverUrl)) {
								bout.reset();
								pgpTransport.exportPublicKeysMatchingQuery(queryString, bout);
								if (tempImportKeysResult == null)
									tempImportKeysResult = pgp.importKeysTemporarily(ByteArrayInputStreamLs.create(bout));
								else {
									ImportKeysResult importKeysResult = tempImportKeysResult.getTempPgp().importKeys(ByteArrayInputStreamLs.create(bout));
									mergeImportKeysResult(tempImportKeysResult, importKeysResult);
								}
							}
						}
						return tempImportKeysResult;
					}

					@Override
					protected void succeeded() {
						final TempImportKeysResult tempImportKeysResult;
						try { tempImportKeysResult = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						onSearchDone(tempImportKeysResult);
					}
				};
			}
		}.start();
	}

	private void mergeImportKeysResult(final TempImportKeysResult destTempImportKeysResult, final ImportKeysResult srcImportKeysResult) {
		final Map<PgpKeyId, ImportedMasterKey> destPgpKeyId2ImportedMasterKey = destTempImportKeysResult.getImportKeysResult().getPgpKeyId2ImportedMasterKey();
		final Map<PgpKeyId, ImportedMasterKey> srcPgpKeyId2ImportedMasterKey = srcImportKeysResult.getPgpKeyId2ImportedMasterKey();

		for (final ImportedMasterKey srcImportedMasterKey : srcPgpKeyId2ImportedMasterKey.values()) {
			final PgpKeyId masterKeyId = srcImportedMasterKey.getPgpKeyId();
			final ImportedMasterKey destImportedMasterKey = destPgpKeyId2ImportedMasterKey.get(masterKeyId);
			if (destImportedMasterKey == null) {
				destPgpKeyId2ImportedMasterKey.put(masterKeyId, srcImportedMasterKey);
			}
			else {
				for (final ImportedSubKey srcImportedSubKey : srcImportedMasterKey.getPgpKeyId2ImportedSubKey().values()) {
					final PgpKeyId subKeyId = srcImportedSubKey.getPgpKeyId();
					destImportedMasterKey.getPgpKeyId2ImportedSubKey().put(subKeyId, srcImportedSubKey);
				}
			}
		}
	}

	protected TempImportKeysResult getTempImportKeysResult() {
		assertFxApplicationThread();
		return importPgpKeyFromServerData.getTempImportKeysResult();
	}
	protected void setTempImportKeysResult(final TempImportKeysResult tempImportKeysResult) {
		assertFxApplicationThread();
		// tempImportKeysResult MAY BE NULL!!!
		importPgpKeyFromServerData.setTempImportKeysResult(tempImportKeysResult);

		pgpKeyTreePane.setVisible(tempImportKeysResult != null
				&& ! tempImportKeysResult.getImportKeysResult().getPgpKeyId2ImportedMasterKey().isEmpty());

		pgpKeyTreePane.getTreeTableView().setRoot(null);
		pgpKeyTreePane.setPgp(tempImportKeysResult == null ? null : tempImportKeysResult.getTempPgp());

		if (tempImportKeysResult != null) {
			final SimpleRootPgpKeyTreeItem root = new SimpleRootPgpKeyTreeItem(pgpKeyTreePane);
			final Pgp tempPgp = tempImportKeysResult.getTempPgp();
			for (final ImportedMasterKey importedMasterKey : tempImportKeysResult.getImportKeysResult().getPgpKeyId2ImportedMasterKey().values()) {
				final PgpKeyId pgpKeyId = importedMasterKey.getPgpKeyId();
				final PgpKey pgpKey = tempPgp.getPgpKey(pgpKeyId);
				requireNonNull(pgpKey, String.format("tempPgp.getPgpKey(%s)", pgpKeyId));

				PgpKeyPgpKeyTreeItem ti = new PgpKeyPgpKeyTreeItem(pgpKey);
				root.getChildren().add(ti);
				if (importPgpKeyFromServerData.getSelectedPgpKeyIds().contains(pgpKeyId))
					ti.setChecked(true);
			}
			pgpKeyTreePane.getTreeTableView().setRoot(root);
		}

		updateComplete();
	}

	protected void onSearchStart() {
		assertFxApplicationThread();
		headerText.setText(String.format(Messages.getString("SearchResultPane.headerText[searching].text"), importPgpKeyFromServerData.getQueryString())); //$NON-NLS-1$
		setTempImportKeysResult(null);
	}

	protected void onSearchDone(TempImportKeysResult tempImportKeysResult) {
		assertFxApplicationThread();

		if (tempImportKeysResult.getImportKeysResult().getPgpKeyId2ImportedMasterKey().isEmpty())
			headerText.setText(String.format(Messages.getString("SearchResultPane.headerText[notFound].text"), importPgpKeyFromServerData.getQueryString())); //$NON-NLS-1$
		else
			headerText.setText(String.format(Messages.getString("SearchResultPane.headerText[found].text"), importPgpKeyFromServerData.getQueryString())); //$NON-NLS-1$

		setTempImportKeysResult(tempImportKeysResult);
	}

	@Override
	protected boolean isComplete() {
		return ! importPgpKeyFromServerData.getSelectedPgpKeyIds().isEmpty();
	}
}
