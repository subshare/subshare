package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.Serializable;
import java.lang.ref.WeakReference;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.subshare.core.dto.jaxb.PgpKeyIdXmlAdapter;

@XmlJavaTypeAdapter(value=PgpKeyIdXmlAdapter.class)
public class PgpKeyId implements Comparable<PgpKeyId>, Serializable {
	private static final long serialVersionUID = 1L;

	private final long pgpKeyId;
	private transient WeakReference<String> toString;

	public PgpKeyId(final long pgpKeyId) {
		this.pgpKeyId = pgpKeyId;
	}

	public PgpKeyId(final String pgpKeyIdString) {
		this(bytesToLong(decodeHexStr(assertNotNull("pgpKeyIdString", pgpKeyIdString))));
	}

	@Override
	public String toString() {
		String s = toString == null ? null : toString.get();
		if (s == null) {
			s = encodeHexStr(longToBytes(pgpKeyId));
			toString = new WeakReference<String>(s);
		}
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (pgpKeyId ^ (pgpKeyId >>> 32));
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PgpKeyId other = (PgpKeyId) obj;
		return this.pgpKeyId == other.pgpKeyId;
	}

	@Override
	public int compareTo(PgpKeyId other) {
		assertNotNull("other", other);
		// Same semantics as for normal numbers.
		return (this.pgpKeyId < other.pgpKeyId ? -1 :
				(this.pgpKeyId > other.pgpKeyId ? 1 : 0));
	}

	public long longValue() {
		return pgpKeyId;
	}
}