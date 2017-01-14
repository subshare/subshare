package org.subshare.gui.invitation.accept.source;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.file.EncryptedDataFile;
import org.subshare.core.pgp.ImportKeysResult;
import org.subshare.core.pgp.ImportKeysResult.ImportedMasterKey;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.user.UserRegistry;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.UserRegistryLs;

import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.client.util.ByteArrayInputStreamLs;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class ImportSigningKeyProblemSolver extends AbstractProblemSolver {

	private static final Logger logger = LoggerFactory.getLogger(ImportSigningKeyProblemSolver.class);

	@Override
	public boolean canSolveProblem() {
		if (getCheckInvitationFileResult().getType() != CheckInvitationFileResult.Type.SIGNING_KEY_MISSING)
			return false;

		final byte[] signingKeyData;
		try {
			signingKeyData = readSigningKeyData();
		} catch (Exception x) {
			logger.warn("readSigningKeyData failed: " + x, x);
			return false;
		}

		if (signingKeyData == null) {
			logger.warn("signingKeyData == null");
			return false;
		}

		if (signingKeyData.length < 1) {
			logger.warn("signingKeyData is empty");
			return false;
		}
		return true;
	}

	@Override
	public void solveProblem() {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setHeaderText("Import the signing key?");

		final String text = "The signing key can be imported from the invitation file, now.\n\nBut note: If you import it, it stays in your PGP key ring (and your user management),\neven if you abort accepting the invitation, afterwards.\n\nShall we import the signing key now?";

//		alert.setContentText(text);
		// The above does not adjust the dialog size :-( Using a Text node instead works better.

		final Text contentText = new Text(text);
		final HBox contentTextContainer = new HBox();
		contentTextContainer.getChildren().add(contentText);

		GridPane.setMargin(contentText, new Insets(8));
		alert.getDialogPane().setContent(contentTextContainer);
		alert.getButtonTypes().clear();
		alert.getButtonTypes().add(ButtonType.YES);
		alert.getButtonTypes().add(ButtonType.NO);

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.YES) {
			try {
				importSigningKey();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void importSigningKey() throws IOException {
		final byte[] signingKeyData = readSigningKeyData();
		final Pgp pgp = PgpLs.getPgpOrFail();
		final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();

		final ImportKeysResult importKeysResult = pgp.importKeys(ByteArrayInputStreamLs.create(signingKeyData));
		final Map<PgpKeyId, PgpKey> pgpKeyId2PgpKey = new HashMap<>();

		for (ImportedMasterKey importedMasterKey : importKeysResult.getPgpKeyId2ImportedMasterKey().values()) {
			final PgpKeyId pgpKeyId = importedMasterKey.getPgpKeyId();
			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
			assertNotNull(pgpKey, "pgp.getPgpKey(" + pgpKeyId + ")");
			pgpKeyId2PgpKey.put(pgpKeyId, pgpKey);
		}
		userRegistry.importUsersFromPgpKeys(pgpKeyId2PgpKey.values());
	}

	private byte[] readSigningKeyData() throws IOException {
		final LocalServerClient lsc = LocalServerClient.getInstance();
		final Pgp pgp = PgpLs.getPgpOrFail();
		try (InputStream in = castStream(getInvitationFile().createInputStream())) {
			final EncryptedDataFile encryptedDataFile = new EncryptedDataFile(in);
			final Object bout = lsc.invokeConstructor(ByteArrayOutputStream.class);
			final Object bin = lsc.invokeConstructor(ByteArrayInputStream.class, encryptedDataFile.getSigningKeyData());
			final PgpDecoder pgpDecoder = lsc.invoke(pgp, "createDecoder", bin, bout);
			try {
				pgpDecoder.decode();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
			final byte[] signingKeyData = lsc.invoke(bout,"toByteArray"); // only encrypted - not signed! thus not checking signature!
			assertNotNull(signingKeyData, "signingKeyData");
			return signingKeyData;
		}
	}
}
