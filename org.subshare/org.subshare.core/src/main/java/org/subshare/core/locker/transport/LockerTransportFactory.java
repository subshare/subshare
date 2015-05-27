package org.subshare.core.locker.transport;

import java.net.URL;

import org.subshare.core.pgp.transport.AbstractPgpTransportFactory;
import org.subshare.core.pgp.transport.PgpTransport;

/**
 * Factory creating instances of classes implementing {@link LockerTransport}.
 * <p>
 * <b>Important:</b> Implementors should <i>not</i> implement this interface directly. Instead,
 * {@link AbstractPgpTransportFactory} should be sub-classed.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface LockerTransportFactory {

	/**
	 * Gets the priority of this factory.
	 * <p>
	 * Factories are sorted primarily by this priority (descending). Thus, the greater the priority as a number
	 * the more likely it will be used.
	 * <p>
	 * Or in other words: The factory with the highest priority is chosen.
	 * <p>
	 * The default implementation in {@link AbstractPgpTransportFactory} returns 0. Thus, if you implement your
	 * own factory and register it for a URL type that is already handled by another factory,
	 * you must return a number greater than the other factory's priority (i.e. usually &gt 0).
	 * @return the priority of this factory.
	 */
	int getPriority();

	/**
	 * Gets the human-readable short name of this factory.
	 * <p>
	 * This should be a very short name like "File", "REST", "SOAP", etc. to be listed in a
	 * combo box or similar UI element.
	 * @return the human-readable short name of this factory. May be <code>null</code>, but
	 * implementors are highly discouraged to return <code>null</code> (or an empty string)!
	 * @see #getDescription()
	 */
	String getName();

	/**
	 * Gets the human-readable long description of this factory.
	 * <p>
	 * In contrast to {@link #getName()}, this method should provide an elaborate decription. It may be
	 * composed of multiple complete sentences and it may contain line breaks.
	 * @return the human-readable long description of this factory.  May be <code>null</code>. But
	 * implementors are encouraged to provide a meaningful description.
	 * @see #getName()
	 */
	String getDescription();

	/**
	 * Determine, whether the factory (or more precisely its {@link PgpTransport}s) is able to handle the given URL.
	 * @param url the URL of the server or "local:" for the local machine.
	 * @return <code>true</code>, if the URL is supported (i.e. a {@link PgpTransport} created by this factory will
	 * operate with it); <code>false</code>, if the URL is not supported.
	 */
	boolean isSupported(URL url);

	/**
	 * Create a {@link LockerTransport} instance.
	 * @param url the remote-root. Must not be <code>null</code>.
	 * @return a new {@link LockerTransport} instance. Never <code>null</code>.
	 */
	LockerTransport createLockerTransport(URL url);

}