package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import static co.codewizards.cloudstore.core.util.UrlUtil.appendNonEncodedPath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.subshare.core.Cryptree;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.UserRepoInvitationDto;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpEncoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoInvitation;
import org.subshare.core.user.UserRepoInvitationDtoConverter;
import org.subshare.core.user.UserRepoInvitationManager;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.dto.jaxb.CloudStoreJaxbContext;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;

public class UserRepoInvitationManagerImpl implements UserRepoInvitationManager {
	private static final String USER_REPO_INVITATION_DTO_XML_FILE_NAME = "userRepoInvitationDto.xml";

	private static final Logger logger = LoggerFactory.getLogger(UserRepoInvitationManagerImpl.class);

	private static final String MANIFEST_PROPERTY_VERSION = "version";

	private static final String MANIFEST_PROPERTY_CONTENT_TYPE = "contentType";

	private static final String MANIFEST_PROPERTIES_FILE_NAME = "MANIFEST.properties";

	private final UserRepoInvitationDtoConverter userRepoInvitationDtoConverter = new UserRepoInvitationDtoConverter();

	private UserRegistry userRegistry;
	private Cryptree cryptree;

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public UserRegistry getUserRegistry() {
		return userRegistry;
	}
	@Override
	public void setUserRegistry(UserRegistry userRegistry) {
		this.userRegistry = userRegistry;
	}

	@Override
	public Cryptree getCryptree() {
		return cryptree;
	}
	@Override
	public void setCryptree(Cryptree cryptree) {
		this.cryptree = cryptree;
	}

	@Override
	public UserRepoInvitationToken createUserRepoInvitationToken(final String localPath, final User user, final long validityDurationMillis) {
		assertNotNull("localPath", localPath);
		assertNotNull("user", user);

		final UserRepoInvitation userRepoInvitation = createUserRepoInvitation(localPath, user, validityDurationMillis);
		final byte[] userRepoInvitationData = toUserRepoInvitationData(userRepoInvitation);

		try {
			FileOutputStream fout = new FileOutputStream(File.createTempFile("xxx-", ".zip"));
			fout.write(userRepoInvitationData);
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		final User grantingUser = determineGrantingUser(localPath);
		final PgpKey signPgpKey = grantingUser.getPgpKeyContainingPrivateKeyOrFail();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpEncoder encoder = getPgpOrFail().createEncoder(new ByteArrayInputStream(userRepoInvitationData), out);
		encoder.setSignPgpKey(signPgpKey);
		encoder.getEncryptPgpKeys().addAll(user.getPgpKeys());
		try {
			encoder.encode();
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}

		return new UserRepoInvitationToken(out.toByteArray());
	}

	@Override
	public void importUserRepoInvitationToken(final UserRepoInvitationToken userRepoInvitationToken) {
		assertNotNull("userRepoInvitationToken", userRepoInvitationToken);

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpDecoder pgpDecoder = getPgpOrFail().createDecoder(new ByteArrayInputStream(userRepoInvitationToken.getSignedEncryptedUserRepoInvitationData()), out);
		try {
			pgpDecoder.decode();
		} catch (final SignatureException | IOException e) {
			throw new RuntimeException(e);
		}

		final byte[] userRepoInvitationData = out.toByteArray();
		final UserRepoInvitation userRepoInvitation = fromUserRepoInvitationData(userRepoInvitationData);
		importUserRepoInvitation(userRepoInvitation);
	}

	private byte[] toUserRepoInvitationData(final UserRepoInvitation userRepoInvitation) {
		assertNotNull("userRepoInvitation", userRepoInvitation);
		try {
			final Marshaller marshaller = CloudStoreJaxbContext.getJaxbContext().createMarshaller();

			final UserRepoInvitationDto userRepoInvitationDto = userRepoInvitationDtoConverter.toUserRepoInvitationDto(userRepoInvitation);

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final ZipOutputStream zout = new ZipOutputStream(out);

			writeManifest(zout);

			zout.putNextEntry(new ZipEntry(USER_REPO_INVITATION_DTO_XML_FILE_NAME));
			marshaller.marshal(userRepoInvitationDto, zout);
			zout.closeEntry();

			zout.close();

			return out.toByteArray();
		} catch (final JAXBException | IOException x) {
			throw new RuntimeException(x);
		}
	}

	private void writeManifest(ZipOutputStream zout) throws IOException {
		final byte[] manifestData = createManifestData();
		zout.putNextEntry(createManifestZipEntry(manifestData));
		zout.write(manifestData);
		zout.closeEntry();
	}

	private UserRepoInvitation fromUserRepoInvitationData(byte[] userRepoInvitationData) {
		assertNotNull("userRepoInvitationData", userRepoInvitationData);
		try {

			final ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(userRepoInvitationData));

			// We expect the very first entry to be the MANIFEST.properties!
			final Properties manifestProperties = readManifest(zin);
			final int version = getVersionFromManifestProperties(manifestProperties);
			if (version != 1)
				throw new IllegalArgumentException("userRepoInvitationData invalid: Unsupported version: " + version);

			final Map<String, Object> name2Dto = readName2Dto(zin);
			final UserRepoInvitationDto userRepoInvitationDto = (UserRepoInvitationDto) name2Dto.get(USER_REPO_INVITATION_DTO_XML_FILE_NAME);
			if (userRepoInvitationDto == null)
				throw new IllegalArgumentException("userRepoInvitationData invalid: Missing zip-entry: " + USER_REPO_INVITATION_DTO_XML_FILE_NAME);

			final UserRepoInvitation userRepoInvitation = userRepoInvitationDtoConverter.fromUserRepoInvitationDto(userRepoInvitationDto);
			return userRepoInvitation;
		} catch (final JAXBException | IOException x) {
			throw new RuntimeException(x);
		}
	}

	private Map<String, Object> readName2Dto(ZipInputStream zin) throws IOException, JAXBException {
		final Unmarshaller unmarshaller = CloudStoreJaxbContext.getJaxbContext().createUnmarshaller();
		final Map<String, Object> name2Dto = new HashMap<>();
		ZipEntry ze;
		while (null != (ze = zin.getNextEntry())) {
			if (!ze.getName().endsWith(".xml")) {
				logger.warn("fromUserRepoInvitationData: Ignoring file (not ending on '.xml'): {}", ze.getName());
				continue;
			}

			final Object dto = unmarshaller.unmarshal(new NonClosableInputStream(zin));
			name2Dto.put(ze.getName(), dto);
		}
		return name2Dto;
	}

	private static class NonClosableInputStream extends FilterInputStream {
		public NonClosableInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() {
			// do nothing
		}
	}

	private Properties readManifest(final ZipInputStream zin) throws IOException {
		assertNotNull("zin", zin);

		final ZipEntry ze = zin.getNextEntry();
		if (ze == null)
			throw new IllegalArgumentException(String.format("userRepoInvitationData is not valid: It lacks the '%s' as very first zip-entry (there is no first ZipEntry)!", MANIFEST_PROPERTIES_FILE_NAME));

		if (!MANIFEST_PROPERTIES_FILE_NAME.equals(ze.getName()))
			throw new IllegalArgumentException(String.format("userRepoInvitationData is not valid: The very first zip-entry is not '%s' (it is '%s' instead)!", MANIFEST_PROPERTIES_FILE_NAME, ze.getName()));

		final Properties properties = new Properties();
		properties.load(zin);

		final String contentType = properties.getProperty(MANIFEST_PROPERTY_CONTENT_TYPE);
		if (!UserRepoInvitationToken.CONTENT_TYPE_USER_REPO_INVITATION.equals(contentType))
			throw new IllegalArgumentException(String.format("userRepoInvitationData is not valid: The manifest indicates the content-type '%s', but '%s' is expected!", contentType, UserRepoInvitationToken.CONTENT_TYPE_USER_REPO_INVITATION));

		return properties;
	}

	private int getVersionFromManifestProperties(final Properties manifestProperties) {
		final String versionStr = manifestProperties.getProperty(MANIFEST_PROPERTY_VERSION);
		final int version;
		try {
			version = Integer.parseInt(versionStr);
		} catch (NumberFormatException x) {
			throw new IllegalArgumentException(String.format("The manifest does not contain a valid version number ('%s' is not a valid integer)!", versionStr), x);
		}
		return version;
	}

	private ZipEntry createManifestZipEntry(final byte[] manifestData) {
		final ZipEntry ze = new ZipEntry(MANIFEST_PROPERTIES_FILE_NAME);
		ze.setMethod(ZipEntry.STORED);
		ze.setSize(manifestData.length);
		ze.setCompressedSize(manifestData.length);
		final CRC32 crc32 = new CRC32();
		crc32.update(manifestData);
		ze.setCrc(crc32.getValue());
		return ze;
	}

	private byte[] createManifestData() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);

		writeManifestEntry(w, MANIFEST_PROPERTY_CONTENT_TYPE, UserRepoInvitationToken.CONTENT_TYPE_USER_REPO_INVITATION);
		writeManifestEntry(w, MANIFEST_PROPERTY_VERSION, Integer.toString(1));

		w.close();
		return out.toByteArray();
	}

	private void writeManifestEntry(final Writer w, final String key, final String value) throws IOException {
		w.write(key);
		w.write('=');
		w.write(value);
		w.write('\n');
	}

	protected UserRepoInvitation createUserRepoInvitation(final String localPath, final User user, final long validityDurationMillis) {
		assertNotNull("localPath", localPath);
		assertNotNull("user", user);
		final PermissionType permissionType = PermissionType.read; // currently the only permission we allow to grant during invitation. maybe we'll change this later.

		final User grantingUser = determineGrantingUser(localPath);

		final UserRepoKey invitationUserRepoKey = grantingUser.createInvitationUserRepoKey(user, cryptree.getRemoteRepositoryId(), validityDurationMillis);
		cryptree.grantPermission(localPath, permissionType, invitationUserRepoKey.getPublicKey());

		final RemoteRepositoryDao remoteRepositoryDao = cryptree.getTransaction().getDao(RemoteRepositoryDao.class);
		final RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepositoryOrFail(cryptree.getRemoteRepositoryId());
		final URL remoteRoot = remoteRepository.getRemoteRoot();
		if (remoteRoot == null)
			throw new IllegalStateException("Could not determine the remoteRoot for the remoteRepositoryId " + cryptree.getRemoteRepositoryId());

		final String serverPath = cryptree.getServerPath(localPath);
		final URL completeUrl = appendNonEncodedPath(remoteRoot, serverPath);
		final UserRepoInvitation userRepoInvitation = new UserRepoInvitation(completeUrl, invitationUserRepoKey);
		return userRepoInvitation;
	}

	private User determineGrantingUser(final String localPath) {
		final UserRepoKey grantingUserRepoKey = cryptree.getUserRepoKeyOrFail(localPath, PermissionType.grant);
		final User grantingUser = findUserWithUserRepoKeyRingOrFail(grantingUserRepoKey);
		return grantingUser;
	}

	protected void importUserRepoInvitation(final UserRepoInvitation userRepoInvitation) {
		assertNotNull("userRepoInvitation", userRepoInvitation);
		final PgpKey decryptPgpKey = determineDecryptPgpKey(userRepoInvitation);
		final User user = findUserWithPgpKeyOrFail(decryptPgpKey);
		user.getUserRepoKeyRingOrCreate().addUserRepoKey(userRepoInvitation.getInvitationUserRepoKey()); // TODO convert into real UserRepoKey!
		userRegistry.write(); // TODO writeIfNeeded() and maybe make write() protected?!
	}

	private PgpKey determineDecryptPgpKey(final UserRepoInvitation userRepoInvitation) {
		final byte[] encryptedSignedPrivateKeyData = userRepoInvitation.getInvitationUserRepoKey().getEncryptedSignedPrivateKeyData();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpDecoder decoder = getPgpOrFail().createDecoder(new ByteArrayInputStream(encryptedSignedPrivateKeyData), out);
		try {
			decoder.decode();
			return decoder.getDecryptPgpKey();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Pgp getPgpOrFail() {
		return PgpRegistry.getInstance().getPgpOrFail();
	}

	private User findUserWithUserRepoKeyRingOrFail(UserRepoKey userRepoKey) {
		final Uid userRepoKeyId = userRepoKey.getUserRepoKeyId();
		for (final User user : userRegistry.getUsers()) {
			final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRing();
			if (userRepoKeyRing != null && userRepoKeyRing.getUserRepoKey(userRepoKeyId) != null)
				return user;
		}
		throw new IllegalArgumentException("No User found owning the UserRepoKey with id=" + userRepoKeyId);
	}

	private User findUserWithPgpKeyOrFail(PgpKey pgpKey) {
		final Long pgpKeyId = pgpKey.getPgpKeyId();
		for (final User user : userRegistry.getUsers()) {
			if (user.getPgpKeyIds().contains(pgpKeyId))
				return user;
		}
		throw new IllegalArgumentException("No User associated with the PgpKey with id=" + pgpKeyId);
	}
}
