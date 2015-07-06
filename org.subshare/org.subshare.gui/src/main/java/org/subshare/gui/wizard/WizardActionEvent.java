package org.subshare.gui.wizard;

import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.event.EventType;

public class WizardActionEvent extends ActionEvent {

	private static final long serialVersionUID = 1L;

    public static final EventType<WizardActionEvent> FINISH = new EventType<WizardActionEvent>(ActionEvent.ACTION, "FINISH");
    public static final EventType<WizardActionEvent> CANCEL = new EventType<WizardActionEvent>(ActionEvent.ACTION, "CANCEL");

	public WizardActionEvent() {
	}

	public WizardActionEvent(Object source, EventTarget target, EventType<WizardActionEvent> eventType) {
		super(source, target);
		this.eventType = eventType;
	}
}
