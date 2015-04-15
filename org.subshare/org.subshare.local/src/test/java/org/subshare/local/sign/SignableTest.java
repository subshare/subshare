package org.subshare.local.sign;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.UUID;

import org.subshare.core.dto.CryptoKeyDto;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.sign.SignableSigner;
import org.subshare.core.sign.SignableVerifier;
import org.subshare.core.sign.Signature;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.AbstractTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.dto.jaxb.DtoIo;

public class SignableTest extends AbstractTest {

	private static UUID repositoryId;
	private static UserRepoKeyRing userRepoKeyRing;
	private static UserRepoKey userRepoKey;
	private static UserRepoKeyPublicKeyLookup userRepoKeyPublicKeyLookup;

	@BeforeClass
	public static void beforeClass() {
		repositoryId = UUID.randomUUID();
		userRepoKeyRing = createUserRepoKeyRing(repositoryId);
		userRepoKey = userRepoKeyRing.getPermanentUserRepoKeys(repositoryId).get(1);
		userRepoKeyPublicKeyLookup = new UserRepoKeyPublicKeyLookup() {
			@Override
			public PublicKey getUserRepoKeyPublicKey(final Uid userRepoKeyId) {
				if (userRepoKeyId.equals(userRepoKey.getUserRepoKeyId()))
					return userRepoKey.getPublicKey();

				return null;
			}
		};
	}

	@AfterClass
	public static void afterClass() {
		repositoryId = null;
		userRepoKeyRing = null;
		userRepoKey = null;
		userRepoKeyPublicKeyLookup = null;
	}

	@Test
	public void signAndVerify() {
		final CryptoKeyDto cryptoKeyDto = createCryptoRepoKeyDto();

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(cryptoKeyDto);

		final SignableVerifier signableVerifier = new SignableVerifier(userRepoKeyPublicKeyLookup);
		signableVerifier.verify(cryptoKeyDto);
	}

	@Test
	public void signAndSerializeAndVerify() {
		CryptoKeyDto cryptoKeyDto = createCryptoRepoKeyDto();

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(cryptoKeyDto);

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final CryptoKeyDtoIo cryptoKeyDtoIo = new CryptoKeyDtoIo();
		cryptoKeyDtoIo.serialize(cryptoKeyDto, out);
		cryptoKeyDto = null;

		final CryptoKeyDto deserialised = cryptoKeyDtoIo.deserialize(new ByteArrayInputStream(out.toByteArray()));

		final SignableVerifier signableVerifier = new SignableVerifier(userRepoKeyPublicKeyLookup);
		signableVerifier.verify(deserialised);
	}

	@Test
	public void signAndModifySignatureCreatedAndVerify() {
		final CryptoKeyDto cryptoKeyDto = createCryptoRepoKeyDto();

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(cryptoKeyDto);

		final Signature cleanSignature = cryptoKeyDto.getSignature();

		final SignatureDto brokenSignature = new SignatureDto(
				new Date(cleanSignature.getSignatureCreated().getTime() + 1),
				cleanSignature.getSigningUserRepoKeyId(),
				cleanSignature.getSignatureData()
				);

		final SignableVerifier signableVerifier = new SignableVerifier(userRepoKeyPublicKeyLookup);
		signableVerifier.verify(cryptoKeyDto); // this is expected to be fine!

		cryptoKeyDto.setSignature(brokenSignature);
		try {
			signableVerifier.verify(cryptoKeyDto);
			fail("Broken signature was not detected! signableVerifier.verify(...) did not fail.");
		} catch (final SignatureException x) {
			doNothing(); // This is expected!
		}

		cryptoKeyDto.setSignature(cleanSignature);
		signableVerifier.verify(cryptoKeyDto); // again this should be fine!
	}

	@Test(expected=SignatureException.class)
	public void signAndModifyCryptoKeyIdAndVerify() {
		final CryptoKeyDto cryptoKeyDto = createCryptoRepoKeyDto();

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(cryptoKeyDto);

		cryptoKeyDto.setCryptoKeyId(new Uid());

		final SignableVerifier signableVerifier = new SignableVerifier(userRepoKeyPublicKeyLookup);
		signableVerifier.verify(cryptoKeyDto);
	}

	@Test(expected=SignatureException.class)
	public void signAndModifyCryptoRepoFileIdAndVerify() {
		final CryptoKeyDto cryptoKeyDto = createCryptoRepoKeyDto();

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(cryptoKeyDto);

		cryptoKeyDto.setCryptoRepoFileId(new Uid());

		final SignableVerifier signableVerifier = new SignableVerifier(userRepoKeyPublicKeyLookup);
		signableVerifier.verify(cryptoKeyDto);
	}

	public void signAndModifyLocalRevisionAndVerify() {
		final CryptoKeyDto cryptoKeyDto = createCryptoRepoKeyDto();

		final SignableSigner signableSigner = new SignableSigner(userRepoKey);
		signableSigner.sign(cryptoKeyDto);

		cryptoKeyDto.setLocalRevision(7); // *not* signed => no effect

		final SignableVerifier signableVerifier = new SignableVerifier(userRepoKeyPublicKeyLookup);
		signableVerifier.verify(cryptoKeyDto);
	}

	private static final class CryptoKeyDtoIo extends DtoIo<CryptoKeyDto> { }

	private CryptoKeyDto createCryptoRepoKeyDto() {
		final CryptoKeyDto cryptoKeyDto = new CryptoKeyDto();
//		cryptoKeyDto.setActive(true);
		cryptoKeyDto.setCryptoKeyId(new Uid());
		cryptoKeyDto.setCryptoKeyRole(CryptoKeyRole.dataKey);
		cryptoKeyDto.setCryptoKeyType(CryptoKeyType.symmetric);
		cryptoKeyDto.setCryptoRepoFileId(new Uid());
		cryptoKeyDto.setLocalRevision(4);
		return cryptoKeyDto;
	}

}
