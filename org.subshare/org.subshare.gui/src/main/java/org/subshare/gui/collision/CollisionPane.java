package org.subshare.gui.collision;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import org.subshare.core.dto.CollisionDto;

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
		this.collisionData = assertNotNull("collisionData", collisionData);
		final CollisionDto collisionDto = assertNotNull("collisionData.collisionDto", collisionData.getCollisionDto());

		resolvedCheckBox.setSelected(collisionDto.getResolved() != null);
		resolvedCheckBox.selectedProperty().addListener((InvalidationListener) observable -> updateResolved());

		commentTextArea.setText(collisionDto.getComment());
		resolvedCheckBox.textProperty().addListener((InvalidationListener) observable -> updateComment());
	}

	private void updateResolved() {
		final boolean newResolved = resolvedCheckBox.selectedProperty().get();
		if (newResolved)
			collisionData.getCollisionDto().setResolved(new Date());
		else
			collisionData.getCollisionDto().setResolved(null);
	}

	private void updateComment() {
		final String newComment = commentTextArea.getText();
		collisionData.getCollisionDto().setComment(newComment);
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		resolvedCheckBox.requestFocus();
	}
}
