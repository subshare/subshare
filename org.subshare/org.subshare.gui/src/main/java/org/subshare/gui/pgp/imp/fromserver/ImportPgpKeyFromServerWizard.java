package org.subshare.gui.pgp.imp.fromserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.subshare.core.pgp.ImportKeysResult;
import org.subshare.core.pgp.ImportKeysResult.ImportedMasterKey;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.TempImportKeysResult;
import org.subshare.core.user.ImportUsersFromPgpKeysResult;
import org.subshare.core.user.UserRegistry;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.io.IByteArrayOutputStream;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.ls.client.util.ByteArrayInputStreamLs;
import co.codewizards.cloudstore.ls.client.util.ByteArrayOutputStreamLs;

public class ImportPgpKeyFromServerWizard extends Wizard {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData = new ImportPgpKeyFromServerData();
	private volatile Set<PgpKeyId> selectedPgpKeyIds;
	private volatile TempImportKeysResult tempImportKeysResult;
	private volatile ImportKeysResult importKeysResult;
	private volatile ImportUsersFromPgpKeysResult importUsersFromPgpKeysResult;

	public ImportPgpKeyFromServerWizard() {
		setFirstPage(new SearchCriteriaWizardPage(importPgpKeyFromServerData));
	}

	public ImportPgpKeyFromServerData getImportPgpKeyFromServerData() {
		return importPgpKeyFromServerData;
	}

	@Override
	protected void finishing() {
		super.finishing();

		// In order to work with the keys without actually importing them, a temporary key-ring
		// was created and and the keys were temporarily imported there.
		tempImportKeysResult = importPgpKeyFromServerData.getTempImportKeysResult();
		assertNotNull("tempImportKeysResult", tempImportKeysResult); //$NON-NLS-1$

		// selectedPgpKeyIds references those keys that were checked by the user.
		selectedPgpKeyIds = new HashSet<>(importPgpKeyFromServerData.getSelectedPgpKeyIds());

		importKeysResult = null; // to be assigned by finish(...)
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		// pgp is our real, productive key-ring.
		final Pgp pgp = PgpLs.getPgpOrFail();
		final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();

		// tempPgp is a temporary Pgp used by the wizard to work with the new keys *before* they are imported
		// to the real, productive key-ring.
		final Pgp tempPgp = tempImportKeysResult.getTempPgp();

		// tempPgpKeys are the keys from tempPgp referenced by selectedPgpKeyIds.
		final Set<PgpKey> tempPgpKeys = new HashSet<>(selectedPgpKeyIds.size());
		for (PgpKeyId pgpKeyId : selectedPgpKeyIds)
			tempPgpKeys.add(assertNotNull("tempPgp.getPgpKey(" + pgpKeyId + ")", tempPgp.getPgpKey(pgpKeyId))); //$NON-NLS-1$ //$NON-NLS-2$

		// now we export the selected keys into memory and import them into the productive key-ring.
		IByteArrayOutputStream bout = ByteArrayOutputStreamLs.create();
		tempPgp.exportPublicKeys(tempPgpKeys, bout);

		importKeysResult = pgp.importKeys(ByteArrayInputStreamLs.create(bout));
		assertNotNull("importKeysResult", importKeysResult); //$NON-NLS-1$

		// finally import users from the imported PGP keys.
		final Map<PgpKeyId, PgpKey> pgpKeyId2PgpKey = new HashMap<>();
		for (ImportedMasterKey importedMasterKey : importKeysResult.getPgpKeyId2ImportedMasterKey().values()) {
			final PgpKeyId pgpKeyId = importedMasterKey.getPgpKeyId();
			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
			assertNotNull("pgp.getPgpKey(" + pgpKeyId + ")", pgpKey); //$NON-NLS-1$ //$NON-NLS-2$
			pgpKeyId2PgpKey.put(pgpKeyId, pgpKey);
		}

		importUsersFromPgpKeysResult = userRegistry.importUsersFromPgpKeys(pgpKeyId2PgpKey.values());
	}

	@Override
	protected void preFinished() {
		super.preFinished();
		importPgpKeyFromServerData.setImportKeysResult(assertNotNull("importKeysResult", importKeysResult)); //$NON-NLS-1$
		importPgpKeyFromServerData.setImportUsersResult(assertNotNull("importUsersFromPgpKeysResult", importUsersFromPgpKeysResult)); //$NON-NLS-1$
	}

	@Override
	public String getTitle() {
		return Messages.getString("ImportPgpKeyFromServerWizard.title"); //$NON-NLS-1$
	}

}
