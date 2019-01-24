package de.retest.ui.actions;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlTransient;

import de.retest.ui.Environment;
import de.retest.ui.components.Component;
import de.retest.ui.descriptors.Element;
import de.retest.ui.image.Screenshot;
import de.retest.ui.review.ActionChangeSet;

public interface Action extends Serializable, Comparable<Action> {

	Action randomize();

	/**
	 * Executes this action in the given environment. Returns a {@link Throwable} if this causes an exception within the
	 * SUT.
	 *
	 * @param <T>
	 *            the enviroment type
	 *
	 * @param environment
	 *            enviroment in which the action will be executed
	 *
	 * @return the result of the execution
	 */
	<T> ActionExecutionResult execute( Environment<T> environment );

	void execute( Component<?> component ) throws TargetNotFoundException;

	Element getTargetElement();

	ActionIdentifyingAttributes getActionIdentifyingAttributes();

	@Override
	int hashCode();

	Screenshot[] getWindowsScreenshots();

	@XmlTransient
	void setWindowsScreenshots( Screenshot[] windowsScreenshots );

	String getUuid();

	Action applyChanges( ActionChangeSet reviewResult );

}
