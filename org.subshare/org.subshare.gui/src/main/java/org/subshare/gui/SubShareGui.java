package org.subshare.gui;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.ResourceBundleUtil.*;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;
import org.subshare.gui.ls.LocalServerInitLs;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.PgpPrivateKeyPassphraseManagerLs;
import org.subshare.gui.pgp.privatekeypassphrase.PgpPrivateKeyPassphrasePromptDialog;
import org.subshare.ls.server.SsLocalServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.DerbyUtil;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class SubShareGui extends Application {

	private static final Logger logger = LoggerFactory.getLogger(SubShareGui.class);

	private SsLocalServer localServer;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		initLogging();

		// We create the LocalServer before constructing the UI to make sure, the UI can access everything.
		// TODO we should start an external JVM and keep it running when closing - or maybe handle this differently?!
		localServer = new SsLocalServer();
		if (! localServer.start())
			localServer = null;

		LocalServerInitLs.init();

		final Parent root = FXMLLoader.load(
				SubShareGui.class.getResource("MainPane.fxml"),
				getMessages(SubShareGui.class));

		final Scene scene = new Scene(root, 800, 600);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.setTitle("SubShare");
		primaryStage.show();

		promptPgpKeyPassphrases(primaryStage.getScene().getWindow());
	}

	@Override
	public void stop() throws Exception {
		LocalServerClient.getInstance().close();
		if (localServer != null) {
			localServer.stop();
			localServer = null;
		}
		super.stop();
	}

	public static void main(final String[] args) {
		launch(args);
	}

	private void promptPgpKeyPassphrases(Window owner) {
		final Pgp pgp = PgpLs.getPgpOrFail();
		final PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore();
		for (final PgpKey pgpKey : pgp.getMasterKeysWithPrivateKey()) {
			final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
			if (pgpPrivateKeyPassphraseStore.hasPassphrase(pgpKeyId))
				continue;

			boolean retry = false;
			do {
				final PgpPrivateKeyPassphrasePromptDialog dialog = new PgpPrivateKeyPassphrasePromptDialog(owner, pgpKey);
				dialog.showAndWait();

				retry = false;

				final char[] passphrase = dialog.getPassphrase();
				if (passphrase != null) {
					try {
						pgpPrivateKeyPassphraseStore.putPassphrase(pgpKeyId, passphrase);
					} catch (SecurityException x) {
						logger.error("promptPgpKeyPassphrases: " + x, x);
						final Alert alert = new Alert(AlertType.ERROR);
						alert.setHeaderText("Sorry, the passphrase you entered is wrong! Please try again.");
						alert.showAndWait();
						retry = true;
					} catch (Exception x) {
						logger.error("promptPgpKeyPassphrases: " + x, x);
					}
				}
			} while (retry);
		}
	}

	private static void initLogging() throws IOException, JoranException {
		final File logDir = ConfigDir.getInstance().getLogDir();
		DerbyUtil.setLogFile(createFile(logDir, "derby.log"));

		final String logbackXmlName = "logback.client.xml";
		final File logbackXmlFile = createFile(ConfigDir.getInstance().getFile(), logbackXmlName);
		if (!logbackXmlFile.exists()) {
			IOUtil.copyResource(SubShareGui.class, logbackXmlName, logbackXmlFile);
		}

		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
		  final JoranConfigurator configurator = new JoranConfigurator();
		  configurator.setContext(context);
		  // Call context.reset() to clear any previous configuration, e.g. default
		  // configuration. For multi-step configuration, omit calling context.reset().
		  context.reset();
		  configurator.doConfigure(logbackXmlFile.createInputStream());
		} catch (final JoranException je) {
			// StatusPrinter will handle this
			doNothing();
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}
}
