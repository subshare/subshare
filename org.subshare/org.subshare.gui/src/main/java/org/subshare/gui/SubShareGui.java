package org.subshare.gui;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.ResourceBundleUtil.*;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;
import org.subshare.gui.error.ErrorHandler;
import org.subshare.gui.ls.LocalServerInitLs;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.PgpPrivateKeyPassphraseManagerLs;
import org.subshare.gui.pgp.privatekeypassphrase.PgpPrivateKeyPassphrasePromptDialog;
import org.subshare.gui.splash.SplashPane;
import org.subshare.gui.util.PlatformUtil;
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
import co.codewizards.cloudstore.ls.server.LocalServer;

public class SubShareGui extends Application {

	private static final Logger logger = LoggerFactory.getLogger(SubShareGui.class);

	private SsLocalServer localServer;
	private Stage primaryStage;
	private SplashPane splashPane;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;

		// Show splash...
		showSplash();

		// ...and do initialisation in background.
		startInitThread();
	}

	private void showSplash() {
		splashPane = new SplashPane();
		final Scene scene = new Scene(splashPane, 400, 300);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.setTitle("SubShare");
		primaryStage.show();
	}

	private void startInitThread() {
		new Thread() {
			{
				setName("Initialisation");
			}

			@Override
			public void run() {
				try {
					Thread.setDefaultUncaughtExceptionHandler(ErrorHandler.getUncaughtExceptionHandler());
					initLogging();

					// We create the LocalServer before constructing the UI to make sure, the UI can access everything.
					// TODO we should start an external JVM and keep it running when closing - or maybe handle this differently?!
					localServer = new SsLocalServer();
					if (! localServer.start())
						localServer = null;

					LocalServerInitLs.initPrepare();

					tryPgpKeysNoPassphrase();

					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							try {
								promptPgpKeyPassphrases(primaryStage.getScene().getWindow());

								final Parent root = FXMLLoader.load(
										SubShareGui.class.getResource("MainPane.fxml"),
										getMessages(SubShareGui.class));

								final Scene scene = new Scene(root, 800, 600);
								scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
								primaryStage.hide();
								primaryStage.setScene(scene);
								primaryStage.show();
								splashPane = null;

								LocalServerInitLs.initFinish();
							} catch (Exception x) {
								ErrorHandler.handleError(x);
								System.exit(666);
							}
						}
					});
				} catch (Exception x) {
					ErrorHandler.handleError(x);
					System.exit(666);
				}
			}
		}.start();
	}

	@Override
	public void stop() throws Exception {
		PlatformUtil.notifyExiting();

		final LocalServer _localServer = localServer;
		localServer = null;

		super.stop();

		new Thread() {
			{
				setName(SubShareGui.class.getSimpleName() + ".StopThread");
				setDaemon(true);
			}

			@Override
			public void run() {
				LocalServerClient.getInstance().close();
				if (_localServer != null)
					_localServer.stop();

				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) { doNothing(); }

				System.exit(0);
			}

		}.start();
	}

	public static void main(final String[] args) {
		launch(args);
	}

	private void tryPgpKeysNoPassphrase() throws InterruptedException {
		final Pgp pgp = PgpLs.getPgpOrFail();
		final PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore();
		final Date now = new Date();

		// Trying to submit the empty passphrases takes a while, because the exceptions cause our
		// LocalServerClient to perform a retry. We therefore simply use multiple threads.
		final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
		try {
			for (final PgpKey pgpKey : pgp.getMasterKeysWithPrivateKey()) {
				if (pgpKey.isRevoked() || !pgpKey.isValid(now))
					continue;

				final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
				if (pgpPrivateKeyPassphraseStore.hasPassphrase(pgpKeyId))
					continue;

				executorService.submit(new Runnable() {
					@Override
					public void run() {
						// To prevent log pollution as well as speeding this up (LocalServer-RPC does retries in case of *all* exceptions),
						//  I first invoke testPassphrase(...) (even though this would not be necessary).
						if (! pgp.testPassphrase(pgpKey, new char[0]))
							return;

						// Try an empty password to prevent a dialog from popping up, if the PGP key is not passphrase-protected.
						try {
							pgpPrivateKeyPassphraseStore.putPassphrase(pgpKeyId, new char[0]);
							// successful => next PGP key
						} catch (Exception x) {
							doNothing();
						}
					}
				});
			}
		} finally {
			executorService.shutdown();
			executorService.awaitTermination(10, TimeUnit.MINUTES);
		}
	}

	private void promptPgpKeyPassphrases(Window owner) {
		final Pgp pgp = PgpLs.getPgpOrFail();
		final PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore();
		final Date now = new Date();

		for (final PgpKey pgpKey : pgp.getMasterKeysWithPrivateKey()) {
			if (pgpKey.isRevoked() || !pgpKey.isValid(now))
				continue;

			final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
			if (pgpPrivateKeyPassphraseStore.hasPassphrase(pgpKeyId))
				continue;

			boolean retry = false;
			String errorMessage = null;
			do {
				final PgpPrivateKeyPassphrasePromptDialog dialog = new PgpPrivateKeyPassphrasePromptDialog(owner, pgpKey, errorMessage);
				dialog.showAndWait();

				retry = false;
				errorMessage = null;

				final char[] passphrase = dialog.getPassphrase();
				if (passphrase != null) {
					try {
						pgpPrivateKeyPassphraseStore.putPassphrase(pgpKeyId, passphrase);
					} catch (SecurityException x) {
						logger.error("promptPgpKeyPassphrases: " + x, x);
						retry = true;
						errorMessage = "Sorry, the passphrase you entered is wrong! Please try again.";
					} catch (Exception x) {
						ErrorHandler.handleError(x);
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
