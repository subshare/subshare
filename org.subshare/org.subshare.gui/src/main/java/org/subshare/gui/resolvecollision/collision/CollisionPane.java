package org.subshare.gui.resolvecollision.collision;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Date;

import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.gui.resolvecollision.CollisionDtoWithPlainHistoCryptoRepoFileDto;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

public class CollisionPane extends GridPane {

	private final CollisionData collisionData;

	@FXML
	private CheckBox resolvedCheckBox;

	@FXML
	private TextArea commentTextArea;

	public CollisionPane(final CollisionData collisionData) {
		loadDynamicComponentFxml(CollisionPane.class, this);
		this.collisionData = assertNotNull("collisionData", collisionData);

		final CollisionDtoWithPlainHistoCryptoRepoFileDto collisionDtoWithPlainHistoCryptoRepoFileDto = collisionData.getCollisionDtoWithPlainHistoCryptoRepoFileDto();
		final CollisionPrivateDto collisionPrivateDto = collisionDtoWithPlainHistoCryptoRepoFileDto.getCollisionPrivateDto();

		resolvedCheckBox.setSelected(collisionPrivateDto.getResolved() != null);
		resolvedCheckBox.selectedProperty().addListener((InvalidationListener) observable -> updateResolved());

		commentTextArea.setText(collisionPrivateDto.getComment());
		commentTextArea.textProperty().addListener((InvalidationListener) observable -> updateComment());
	}

	private void updateResolved() {
		final CollisionDtoWithPlainHistoCryptoRepoFileDto collisionDtoWithPlainHistoCryptoRepoFileDto = collisionData.getCollisionDtoWithPlainHistoCryptoRepoFileDto();
		final CollisionPrivateDto collisionPrivateDto = collisionDtoWithPlainHistoCryptoRepoFileDto.getCollisionPrivateDto();

		final boolean newResolved = resolvedCheckBox.selectedProperty().get();
		if (newResolved)
			collisionPrivateDto.setResolved(new Date());
		else
			collisionPrivateDto.setResolved(null);
	}

	private void updateComment() {
		final CollisionDtoWithPlainHistoCryptoRepoFileDto collisionDtoWithPlainHistoCryptoRepoFileDto = collisionData.getCollisionDtoWithPlainHistoCryptoRepoFileDto();
		final CollisionPrivateDto collisionPrivateDto = collisionDtoWithPlainHistoCryptoRepoFileDto.getCollisionPrivateDto();

		final String newComment = commentTextArea.getText();
		collisionPrivateDto.setComment(newComment);
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		resolvedCheckBox.requestFocus();
	}
}
