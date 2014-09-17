package org.subshare.local.sign;

import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.subshare.core.sign.SignerTransformation;
import org.subshare.crypto.CryptoRegistry;
import org.junit.Test;

public class BcSignerTest {

	@Test
	public void signAndVerifyProblematicData() throws Exception {
		final byte[] plain = readResource("sign_verify_1_plain");
		final AsymmetricKeyParameter privateKey = PrivateKeyFactory.createKey(readResource("sign_verify_1_key.private"));
		final AsymmetricKeyParameter publicKey = PublicKeyFactory.createKey(readResource("sign_verify_1_key.public"));

		final Signer signer = CryptoRegistry.getInstance().createSigner(SignerTransformation.RSA_SHA1.getTransformation());
		signer.init(true, privateKey);
		final byte[] signatureCreatedBytes = longToBytes(new Date(0).getTime());
		signer.update(signatureCreatedBytes, 0, signatureCreatedBytes.length);
		signer.update(plain, 0, plain.length);
		final byte[] signature = signer.generateSignature();

		final Signer verifier = CryptoRegistry.getInstance().createSigner(SignerTransformation.RSA_SHA1.getTransformation());
		verifier.init(false, publicKey);
		verifier.update(signatureCreatedBytes, 0, signatureCreatedBytes.length);
		verifier.update(plain, 0, plain.length);
		assertThat(verifier.verifySignature(signature)).isTrue();
	}

	private byte[] readResource(final String name) throws IOException {
		final InputStream in = BcSignerTest.class.getResourceAsStream(name);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		transferStreamData(in, out);
		in.close();
		return out.toByteArray();
	}
}
