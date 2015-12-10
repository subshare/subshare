package org.subshare.gui.welcome.server;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeListener;
import java.net.URL;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.subshare.core.server.Server;
import org.subshare.gui.util.UrlStringConverter;
import org.subshare.gui.welcome.ServerData;

public abstract class ServerPane extends GridPane {

	private final PropertyChangeListener updateCompletePropertyChangeListener = event -> updateComplete();
	private final InvalidationListener updateCompleteInvalidationListener = observable -> updateComplete();

	private final ServerData serverData;
	private final Server server;

	@FXML
	private Label urlLabel;

	@FXML
	private TextField urlTextField;
	private final ObjectProperty<URL> url;

	@FXML
	private Label nameLabel;

	@FXML
	private TextField nameTextField;
	private final StringProperty name;

	@FXML
	private CheckBox acceptInvitationCheckBox;

	@SuppressWarnings("unchecked")
	public ServerPane(final ServerData serverData) {
		this.serverData = assertNotNull("serverData", serverData);
		this.server = assertNotNull("serverData.server", serverData.getServer());
		loadDynamicComponentFxml(ServerPane.class, this);

		try {
			url = JavaBeanObjectPropertyBuilder.create().bean(server).name(Server.PropertyEnum.url.name()).build();
			name = JavaBeanStringPropertyBuilder.create().bean(server).name(Server.PropertyEnum.name.name()).build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		urlTextField.textProperty().bindBidirectional(url, new UrlStringConverter());
		nameTextField.textProperty().bindBidirectional(name);

		acceptInvitationCheckBox.selectedProperty().bindBidirectional(serverData.acceptInvitationProperty());
		nameLabel.disableProperty().bind(acceptInvitationCheckBox.selectedProperty());
		nameTextField.disableProperty().bind(acceptInvitationCheckBox.selectedProperty());
		urlLabel.disableProperty().bind(acceptInvitationCheckBox.selectedProperty());
		urlTextField.disableProperty().bind(acceptInvitationCheckBox.selectedProperty());

		addWeakPropertyChangeListener(server, Server.PropertyEnum.url, updateCompletePropertyChangeListener);
		serverData.acceptInvitationProperty().addListener(new WeakInvalidationListener(updateCompleteInvalidationListener));
		updateComplete();
	}

	protected boolean isComplete() {
		if (serverData.acceptInvitationProperty().get())
			return true;

		final URL url = server.getUrl();
		return url != null;
	}

	protected abstract void updateComplete();
}
