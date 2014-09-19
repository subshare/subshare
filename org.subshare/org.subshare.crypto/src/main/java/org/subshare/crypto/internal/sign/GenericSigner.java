package org.subshare.crypto.internal.sign;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.Arrays;

/**
 * @deprecated This is a temporary work-around for the bug in org.bouncycastle.crypto.signers.GenericSigner and should be removed once the new BouncyCastle is released.
 * <p>
 * TODO see: https://github.com/bcgit/bc-java/commit/5e87a65aa2b0d325530a52aa8ae781cf811b9c55
 */
@Deprecated
public class GenericSigner
implements Signer
{
	private final AsymmetricBlockCipher engine;
	private final Digest digest;
	private boolean forSigning;

	public GenericSigner(
			final AsymmetricBlockCipher engine,
			final Digest                digest)
	{
		this.engine = engine;
		this.digest = digest;
	}

	/**
	 * initialise the signer for signing or verification.
	 *
	 * @param forSigning
	 *            true if for signing, false otherwise
	 * @param parameters
	 *            necessary parameters.
	 */
	@Override
	public void init(
			final boolean          forSigning,
			final CipherParameters parameters)
	{
		this.forSigning = forSigning;
		AsymmetricKeyParameter k;

		if (parameters instanceof ParametersWithRandom)
		{
			k = (AsymmetricKeyParameter)((ParametersWithRandom)parameters).getParameters();
		}
		else
		{
			k = (AsymmetricKeyParameter)parameters;
		}

		if (forSigning && !k.isPrivate())
		{
			throw new IllegalArgumentException("signing requires private key");
		}

		if (!forSigning && k.isPrivate())
		{
			throw new IllegalArgumentException("verification requires public key");
		}

		reset();

		engine.init(forSigning, parameters);
	}

	/**
	 * update the internal digest with the byte b
	 */
	@Override
	public void update(
			final byte input)
	{
		digest.update(input);
	}

	/**
	 * update the internal digest with the byte array in
	 */
	@Override
	public void update(
			final byte[]  input,
			final int     inOff,
			final int     length)
	{
		digest.update(input, inOff, length);
	}

	/**
	 * Generate a signature for the message we've been loaded with using the key
	 * we were initialised with.
	 */
	@Override
	public byte[] generateSignature()
			throws CryptoException, DataLengthException
	{
		if (!forSigning)
		{
			throw new IllegalStateException("GenericSigner not initialised for signature generation.");
		}

		final byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);

		return engine.processBlock(hash, 0, hash.length);
	}

	/**
	 * return true if the internal state represents the signature described in
	 * the passed in array.
	 */
	@Override
	public boolean verifySignature(
			final byte[] signature)
	{
		if (forSigning)
		{
			throw new IllegalStateException("GenericSigner not initialised for verification");
		}

		final byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);

		try
		{
			byte[] sig = engine.processBlock(signature, 0, signature.length);

			// Extend with leading zeroes to match the digest size, if necessary.
			if (sig.length < hash.length)
			{
				final byte[] tmp = new byte[hash.length];
				System.arraycopy(sig, 0, tmp, tmp.length - sig.length, sig.length);
				sig = tmp;
			}

			return Arrays.constantTimeAreEqual(sig, hash);
		}
		catch (final Exception e)
		{
			return false;
		}
	}

	@Override
	public void reset()
	{
		digest.reset();
	}
}
