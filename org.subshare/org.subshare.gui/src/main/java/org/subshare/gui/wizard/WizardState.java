package org.subshare.gui.wizard;

/**
 * State of a {@link Wizard}.
 * <p>
 * A new {@link Wizard} is initially in state {@link #NEW}. As soon as it is picked by some UI and
 * shown to the user, (usually by a {@link WizardDialog}), its state changes to {@link #IN_USER_INTERACTION}.
 * <p>
 * When the user clicks the "Finish" button, the wizard's state transitions to {@link #FINISHING} and the
 * wizard performs its actualy work in the background on a worker thread.
 * <p>
 * If the work succeeded, the state switches to {@link #FINISHED}; in case of a failure, it instead switches
 * to {@link #FAILED}. Usually, the user may click "Finish" again after a failure. The wizard's UI is normally
 * closed only when the wizard is either cancelled or successfully finished.
 * <p>
 * Please note: We currently keep the state {@code #FAILED}, even though the user can modify the data entered
 * before and thus {@link #IN_USER_INTERACTION} would seem more appropriate. We might change this later.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
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
