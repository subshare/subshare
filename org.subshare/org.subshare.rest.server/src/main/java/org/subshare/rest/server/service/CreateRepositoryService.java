package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.CreateRepositoryRequestDto;
import org.subshare.core.pgp.PgpSignature;
import org.subshare.core.sign.PgpSignableVerifier;

import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.CreateRepositoryContext;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.util.IOUtil;

@Path("_createRepository")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class CreateRepositoryService {

	public static final String CONFIG_KEY_REPO_BASE_DIR = "repoBaseDir";
	public static final String DEFAULT_REPO_BASE_DIR = "${user.home}/subshare-repo.d";

	@PUT
	public void createRepository(final CreateRepositoryRequestDto createRepositoryRequestDto) {
		requireNonNull(createRepositoryRequestDto, "createRepositoryRequestDto");
		final UUID serverRepositoryId = requireNonNull(createRepositoryRequestDto.getServerRepositoryId(), "createRepositoryRequestDto.serverRepositoryId");

		final PgpSignature pgpSignature = new PgpSignableVerifier().verify(createRepositoryRequestDto);
		// TODO introduce sth. like a server-owner or a list of server-admins who are allowed to do this.
		// TODO maybe additionally(?) introduce sth. like a create-repository-request (which is then granted by an admin?).
		// ...need to think more about how to handle all this... for the beginning, it's fine to allow this to everyone
		// it's not really a security issue, because the worst thing that can happen is that people put data nobody else
		// can read and the server owner can simply delete the repos...

		// TODO where do we put the repos??? need configuration!
		final String baseDir = ConfigImpl.getInstance().getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_REPO_BASE_DIR, DEFAULT_REPO_BASE_DIR);
		final String resolvedBaseDir = IOUtil.replaceTemplateVariables(baseDir, System.getProperties());
		final File localDirectory = createFile(resolvedBaseDir, serverRepositoryId.toString()).getAbsoluteFile();
		localDirectory.mkdirs();

		final File localRoot = LocalRepoHelper.getLocalRootContainingFile(localDirectory);
		if (localRoot == null) {
			CreateRepositoryContext.repositoryIdThreadLocal.set(serverRepositoryId);
			try {
				try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForNewRepository(localDirectory);) {
					// do nothing ;-)
				}
			} finally {
				CreateRepositoryContext.repositoryIdThreadLocal.remove();
			}
		}
		else {
			if (!localRoot.equals(localDirectory))
				throw new IllegalStateException(String.format("WTF?! The path '%s' which is configured for the key '%s' seems to be inside another repository!",
						baseDir, CONFIG_KEY_REPO_BASE_DIR));

			try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);) {
				// do nothing ;-)
			}
		}
	}

}
