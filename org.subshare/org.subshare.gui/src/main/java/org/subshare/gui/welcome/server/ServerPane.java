package org.subshare.gui.welcome.server;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.net.URL;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.subshare.core.server.Server;
import org.subshare.gui.util.UrlStringConverter;

public class ServerPane extends GridPane {

	private final Server server;

	@FXML
	private TextField urlTextField;
	private final ObjectProperty<URL> urlProperty;

	@FXML
	private TextField nameTextField;
	private final StringProperty nameProperty;

	@SuppressWarnings("unchecked")
	public ServerPane(final Server server) {
		this.server = assertNotNull("server", server);
		loadDynamicComponentFxml(ServerPane.class, this);

		try {
			urlProperty = JavaBeanObjectPropertyBuilder.create().bean(server).name(Server.PropertyEnum.url.name()).build();
			nameProperty = JavaBeanStringPropertyBuilder.create().bean(server).name(Server.PropertyEnum.name.name()).build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		urlTextField.textProperty().bindBidirectional(urlProperty, new UrlStringConverter());
		nameTextField.textProperty().bindBidirectional(nameProperty);
	}
}
