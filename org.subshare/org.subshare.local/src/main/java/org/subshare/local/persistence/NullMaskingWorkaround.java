package org.subshare.local.persistence;

/**
 * TODO temporary workaround for https://github.com/datanucleus/datanucleus-rdbms/issues/234
 * @author mangu
 */
public class NullMaskingWorkaround {

	/**
	 * @deprecated TODO temporary workaround for https://github.com/datanucleus/datanucleus-rdbms/issues/234
	 */
	@Deprecated
	public static byte[] nullMaskingWorkaround(byte[] bytes) {
		if (bytes == null)
			return new byte[0];
		else
			return bytes;
	}

}
