package org.subshare.gui;

import static org.subshare.gui.util.ResourceBundleUtil.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.subshare.core.pgp.transport.PgpTransportFactoryRegistry;
import org.subshare.gui.ssl.AcceptAllDynamicX509TrustManagerCallback;
import org.subshare.ls.server.SsLocalServer;
import org.subshare.rest.client.pgp.transport.RestPgpTransportFactory;
import org.subshare.rest.client.transport.CryptreeRepoTransportFactory;

import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class SubShareGui extends Application {

	private SsLocalServer localServer;

	@Override
	public void start(final Stage primaryStage) throws Exception {
		RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(CryptreeRepoTransportFactory.class).setDynamicX509TrustManagerCallbackClass(AcceptAllDynamicX509TrustManagerCallback.class);
		PgpTransportFactoryRegistry.getInstance().getPgpTransportFactoryOrFail(RestPgpTransportFactory.class).setDynamicX509TrustManagerCallbackClass(AcceptAllDynamicX509TrustManagerCallback.class);

		// We create the LocalServer before constructing the UI to make sure, the UI can access everything.
		// TODO we should start an external JVM and keep it running when closing - or maybe handle this differently?!
		localServer = new SsLocalServer();
		if (! localServer.start())
			localServer = null;

//		LocalServerClient.getInstance().invokeStatic(clazz, methodName, arguments)

		final Parent root = FXMLLoader.load(
				SubShareGui.class.getResource("MainPane.fxml"),
				getMessages(SubShareGui.class));

		final Scene scene = new Scene(root, 800, 600);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.setTitle("SubShare");
		primaryStage.show();
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
}
