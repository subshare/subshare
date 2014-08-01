package org.subshare.core.crypto;

import static org.assertj.core.api.Assertions.*;
import static org.subshare.core.crypto.KeyFactory.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.CryptoKeyType;
import org.junit.Test;


public class EncrypterDecrypterStreamTest {

	private static final Random random = new Random(); // non-secure is much faster.

	@Test
	public void symmetricWithoutIv() throws Exception {
		for (final CipherTransformation cipherTransformation : CipherTransformation.values()) {
			if (CryptoKeyType.symmetric != cipherTransformation.getType())
				continue;

			System.out.println();
			System.out.println("symmetricWithoutIv: cipherTransformation=" + cipherTransformation);
			long start = System.currentTimeMillis();

			final byte[] plain = new byte[random.nextInt(10 * 1024 * 1024)];
			random.nextBytes(plain);

			System.out.printf("symmetricWithoutIv: Generating %s random bytes plaintext took %s ms.\n", plain.length, System.currentTimeMillis() - start);
			start = System.currentTimeMillis();

			final KeyParameter key = KeyFactory.getInstance().createSymmetricKey();

			System.out.printf("symmetricWithoutIv: Creating symmetric key took %s ms.\n", System.currentTimeMillis() - start);
			start = System.currentTimeMillis();

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final OutputStream encrypterOut = new EncrypterOutputStream(out, cipherTransformation, key, null);
			writeWithRandomBuffer(new ByteArrayInputStream(plain), encrypterOut);
			encrypterOut.close();
			final byte[] encrypted = out.toByteArray();
			out.reset();

			System.out.printf("symmetricWithoutIv: Encrypting %s bytes plaintext to %s bytes ciphertext took %s ms.\n", plain.length, encrypted.length, System.currentTimeMillis() - start);
			start = System.currentTimeMillis();

			final InputStream decrypterIn = new DecrypterInputStream(new ByteArrayInputStream(encrypted), key);
			writeWithRandomBuffer(decrypterIn, out);
			decrypterIn.close();

			final byte[] decrypted = out.toByteArray();

			System.out.printf("symmetricWithoutIv: Decrypting %s bytes ciphertext to %s bytes plaintext took %s ms.\n", encrypted.length, decrypted.length, System.currentTimeMillis() - start);

			assertThat(decrypted).isEqualTo(plain);
		}
	}

	@Test
	public void symmetricWithRandomIv() throws Exception {
		for (final CipherTransformation cipherTransformation : CipherTransformation.values()) {
			if (CryptoKeyType.symmetric != cipherTransformation.getType())
				continue;

			System.out.println();
			System.out.println("symmetricWithRandomIv: cipherTransformation=" + cipherTransformation);
			long start = System.currentTimeMillis();

			final byte[] plain = new byte[random.nextInt(10 * 1024 * 1024)];
			random.nextBytes(plain);

			System.out.printf("symmetricWithRandomIv: Generating %s random bytes plaintext took %s ms.\n", plain.length, System.currentTimeMillis() - start);
			start = System.currentTimeMillis();

			final KeyParameter key = KeyFactory.getInstance().createSymmetricKey();

			System.out.printf("symmetricWithRandomIv: Creating symmetric key took %s ms.\n", System.currentTimeMillis() - start);
			start = System.currentTimeMillis();

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final byte[][] iv = new byte[1][];
			final OutputStream encrypterOut = new EncrypterOutputStream(out, cipherTransformation, key, new AbstractIvFactory() {
				@Override
				public byte[] createIv() {
					iv[0] = new byte[getCipher().getIVSize()];
					secureRandom.nextBytes(iv[0]);
					return iv[0];
				}
			});
			writeWithRandomBuffer(new ByteArrayInputStream(plain), encrypterOut);
			encrypterOut.close();
			final byte[] encrypted = out.toByteArray();
			out.reset();

			System.out.printf("symmetricWithRandomIv: Encrypting %s bytes plaintext to %s bytes ciphertext took %s ms.\n", plain.length, encrypted.length, System.currentTimeMillis() - start);
			start = System.currentTimeMillis();

			final DecrypterInputStream decrypterIn = new DecrypterInputStream(new ByteArrayInputStream(encrypted), key);
			writeWithRandomBuffer(decrypterIn, out);
			decrypterIn.close();

			assertThat(decrypterIn.getIv()).isEqualTo(iv[0]);

			final byte[] decrypted = out.toByteArray();

			System.out.printf("symmetricWithRandomIv: Decrypting %s bytes ciphertext to %s bytes plaintext took %s ms.\n", encrypted.length, decrypted.length, System.currentTimeMillis() - start);

			assertThat(decrypted).isEqualTo(plain);
		}
	}

	@Test
	public void asymmetricPlain() throws Exception {
		for (final CipherTransformation cipherTransformation : CipherTransformation.values()) {
			if (CryptoKeyType.asymmetric != cipherTransformation.getType())
				continue;

			long start = System.currentTimeMillis();

			final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();

			System.out.println();
			System.out.printf("asymmetricPlain: Creating asymmetric key took %s ms.\n", System.currentTimeMillis() - start);

			for (int i = 0; i < 100; ++i) {
				System.out.println();
				System.out.println("asymmetricPlain: cipherTransformation=" + cipherTransformation);
				start = System.currentTimeMillis();

				// The maximum that can be encrypted with plain RSA-4096 is 470 bytes.
				// At least one byte seems to be required, too. Thus, we generate a random length between 1 and 470 (including).
				final byte[] plain = new byte[1 + random.nextInt(470)];
				random.nextBytes(plain);

				System.out.printf("asymmetricPlain: Generating %s random bytes plaintext took %s ms.\n", plain.length, System.currentTimeMillis() - start);
				start = System.currentTimeMillis();

				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				final byte[][] iv = new byte[1][];
				final OutputStream encrypterOut = new EncrypterOutputStream(out, cipherTransformation, keyPair.getPublic(), null);
				writeWithRandomBuffer(new ByteArrayInputStream(plain), encrypterOut);
				encrypterOut.close();
				final byte[] encrypted = out.toByteArray();
				out.reset();

				System.out.printf("asymmetricPlain: Encrypting %s bytes plaintext to %s bytes ciphertext took %s ms.\n", plain.length, encrypted.length, System.currentTimeMillis() - start);
				start = System.currentTimeMillis();

				final DecrypterInputStream decrypterIn = new DecrypterInputStream(new ByteArrayInputStream(encrypted), keyPair.getPrivate());
				writeWithRandomBuffer(decrypterIn, out);
				decrypterIn.close();

				assertThat(decrypterIn.getIv()).isEqualTo(iv[0]);

				final byte[] decrypted = out.toByteArray();

				System.out.printf("asymmetricPlain: Decrypting %s bytes ciphertext to %s bytes plaintext took %s ms.\n", encrypted.length, decrypted.length, System.currentTimeMillis() - start);

				assertThat(decrypted).isEqualTo(plain);
			}
		}
	}

	@Test
	public void asymmetricCombiWithRandomIv() throws Exception {
		for (final CipherTransformation asymmetricCipherTransformation : CipherTransformation.values()) {
			if (CryptoKeyType.asymmetric != asymmetricCipherTransformation.getType())
				continue;

			long start = System.currentTimeMillis();

			final AsymmetricCipherKeyPair keyPair = KeyFactory.getInstance().createAsymmetricKeyPair();

			System.out.println();
			System.out.printf("asymmetricPlain: Creating asymmetric key took %s ms.\n", System.currentTimeMillis() - start);

			for (final CipherTransformation symmetricCipherTransformation : CipherTransformation.values()) {
				if (CryptoKeyType.symmetric != symmetricCipherTransformation.getType())
					continue;

				System.out.println();
				System.out.println("asymmetricCombiWithRandomIv: asymmetricCipherTransformation=" + asymmetricCipherTransformation);
				System.out.println("asymmetricCombiWithRandomIv: symmetricCipherTransformation=" + symmetricCipherTransformation);
				start = System.currentTimeMillis();

				final byte[] plain = new byte[random.nextInt(10 * 1024 * 1024)];
				random.nextBytes(plain);

				System.out.printf("asymmetricCombiWithRandomIv: Generating %s random bytes plaintext took %s ms.\n", plain.length, System.currentTimeMillis() - start);
				start = System.currentTimeMillis();

				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				final OutputStream encrypterOut = new AsymCombiEncrypterOutputStream(
						out, asymmetricCipherTransformation, keyPair.getPublic(), symmetricCipherTransformation,
						new DefaultKeyParameterFactory(),
						new RandomIvFactory());
				writeWithRandomBuffer(new ByteArrayInputStream(plain), encrypterOut);
				encrypterOut.close();
				final byte[] encrypted = out.toByteArray();
				out.reset();

				System.out.printf("asymmetricCombiWithRandomIv: Encrypting %s bytes plaintext to %s bytes ciphertext took %s ms.\n", plain.length, encrypted.length, System.currentTimeMillis() - start);
				start = System.currentTimeMillis();

				final InputStream decrypterIn = new AsymCombiDecrypterInputStream(new ByteArrayInputStream(encrypted), keyPair.getPrivate());
				writeWithRandomBuffer(decrypterIn, out);
				decrypterIn.close();

				final byte[] decrypted = out.toByteArray();

				System.out.printf("asymmetricCombiWithRandomIv: Decrypting %s bytes ciphertext to %s bytes plaintext took %s ms.\n", encrypted.length, decrypted.length, System.currentTimeMillis() - start);

				assertThat(decrypted).isEqualTo(plain);
			}
		}
	}

	private void writeWithRandomBuffer(final InputStream in, final OutputStream out) throws IOException {
		while (true) {
			if (random.nextInt(100) <= 50) { // because it is slower, we do this only in 50% of the cases
				final int singleByte = in.read();
				if (singleByte < 0)
					return;

				out.write(singleByte);
			}
			else {
				final byte[] buf = new byte[1 + random.nextInt(1024 * 1024)];
				final int bytesRead = in.read(buf, 0, buf.length);
				if (bytesRead < 0)
					return;

				out.write(buf, 0, bytesRead);
			}
		}
	}

}
