package org.subshare.gui.wizard;

public enum WizardState {

	/**
	 * The wizard just was created.
	 * <p>
	 * This is the initial state of a wizard - before any UI (e.g. a {@link WizardDialog}) picked it up.
	 */
	NEW,

	/**
	 * The wizard currently interacts with the user.
	 * <p>
	 * The user currently navigates through the wizard pages and fills in data.
	 */
	IN_USER_INTERACTION,

	/**
	 * The user pressed the "Finish" button and the wizard is currently performing the appropriate work.
	 */
	FINISHING,

	/**
	 * The wizard finished his actual work. This is the normal end state. The wizard has fulfilled his purpose of life ;-)
	 * @see #FAILED
	 * @see #CANCELLED
	 */
	FINISHED,

	/**
	 * The wizard failed finishing his actual work.
	 */
	FAILED,

	/**
	 * The wizard was cancelled. This is an alternative end state to {@link #FINISHED}.
	 */
	CANCELLED
}
