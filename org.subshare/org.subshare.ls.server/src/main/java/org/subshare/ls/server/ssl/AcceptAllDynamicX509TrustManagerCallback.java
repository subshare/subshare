package org.subshare.ls.server.ssl;

import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;

/**
 * Callback accepting all server certificates.
 * <p>
 * TODO we have to decide how to proceed with SSL certificate handling. One one hand, we do not essentially need
 * SSL, because the client does not trust the server, anyway. On the other hand, some information that is hidden
 * by SSL can be read by passive attackers (e.g. the NSA), if we use plain HTTP: for example who is using which
 * repository.
 * <p>
 * Maybe we provide both, HTTP and HTTPS? And allow the user to select? And for HTTPS we maybe use our own trust
 * management?
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class AcceptAllDynamicX509TrustManagerCallback implements DynamicX509TrustManagerCallback {
	@Override
	public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(final CheckServerTrustedCertificateExceptionContext context) {
		final CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
		result.setTrusted(true);
		return result;
	}
}