package org.subshare.core.dto.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.subshare.core.pgp.PgpKeyFingerprint;

public class PgpKeyFingerprintXmlAdapter extends XmlAdapter<String, PgpKeyFingerprint> {

	@Override
	public PgpKeyFingerprint unmarshal(final String v) throws Exception {
		return new PgpKeyFingerprint(v);
	}

	@Override
	public String marshal(final PgpKeyFingerprint v) throws Exception {
		return v.toString();
	}

}
