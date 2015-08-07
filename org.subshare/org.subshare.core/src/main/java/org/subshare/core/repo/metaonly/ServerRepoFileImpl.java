package org.subshare.core.repo.metaonly;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.repo.ServerRepo;
import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.SymlinkDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.util.UrlUtil;

public class ServerRepoFileImpl implements ServerRepoFile {

	private final ServerRepoFileImpl parent;
	private final CryptoRepoFileDto cryptoRepoFileDto;
	private final RepoFileDto repoFileDto;
	private final Server server;
	private final ServerRepo serverRepo;
	private final UUID localRepositoryId;
	private List<ServerRepoFile> children;

	public ServerRepoFileImpl(final Server server, final ServerRepo serverRepo, final UUID localRepositoryId, final CryptoRepoFileDto cryptoRepoFileDto, final RepoFileDto repoFileDto) {
		parent = null;
		this.server = assertNotNull("server", server);
		this.serverRepo = assertNotNull("serverRepo", serverRepo);
		this.localRepositoryId = assertNotNull("localRepositoryId", localRepositoryId);
		this.cryptoRepoFileDto = assertNotNull("cryptoRepoFileDto", cryptoRepoFileDto);
		this.repoFileDto = assertNotNull("repoFileDto", repoFileDto);
	}

	public ServerRepoFileImpl(final ServerRepoFileImpl parent, final CryptoRepoFileDto cryptoRepoFileDto, final RepoFileDto repoFileDto) {
		this.parent = assertNotNull("parent", parent);
		this.server = parent.getServer();
		this.serverRepo = parent.getServerRepo();
		this.localRepositoryId = parent.getLocalRepositoryId();
		this.cryptoRepoFileDto = assertNotNull("cryptoRepoFileDto", cryptoRepoFileDto);
		this.repoFileDto = assertNotNull("repoFileDto", repoFileDto);
	}

	@Override
	public Server getServer() {
		return server;
	}

	@Override
	public ServerRepo getServerRepo() {
		return serverRepo;
	}

	@Override
	public UUID getLocalRepositoryId() {
		return localRepositoryId;
	}

	@Override
	public ServerRepoFile getParent() {
		return parent;
	}

	@Override
	public ServerRepoFileType getType() {
		if (repoFileDto instanceof NormalFileDto)
			return ServerRepoFileType.FILE;

		if (repoFileDto instanceof DirectoryDto)
			return ServerRepoFileType.DIRECTORY;

		if (repoFileDto instanceof SymlinkDto)
			return ServerRepoFileType.SYMLINK;

		assertNotNull("repoFileDto", repoFileDto); // should really never happen, because we checked already in the constructor - but maybe we'll allow null later.
		throw new IllegalStateException("Unknown RepoFileDto sub-class: " + repoFileDto.getClass().getName());
	}

	@Override
	public String getLocalName() {
		return repoFileDto.getName();
	}

	@Override
	public String getLocalPath() {
		final String parentPath = parent == null ? "" : parent.getLocalPath(); //$NON-NLS-1$
		return appendPath(parentPath, getLocalName());
	}

	@Override
	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileDto.getCryptoRepoFileId();
	}

	public String getServerName() {
		if ("/".equals(getLocalPath()))
			return "";

		return getCryptoRepoFileId().toString();
	}

	@Override
	public String getServerPath() {
		final String parentPath = parent == null ? "" : parent.getServerPath(); //$NON-NLS-1$
		return appendPath(parentPath, getServerName());
	}

	private static String appendPath(String path1, String path2) {
		final StringBuilder sb = new StringBuilder(path1.length() + path2.length() + 1);
		sb.append(path1);
		if (! path1.endsWith("/"))
			sb.append('/');

		sb.append(path2);
		return sb.toString();
	}

	@Override
	public URL getServerUrl() {
		URL url = server.getUrl();
		url = UrlUtil.appendNonEncodedPath(url, serverRepo.getRepositoryId().toString());
		url = UrlUtil.appendEncodedPath(url, getServerPath());
		return url;
	}

	public CryptoRepoFileDto getCryptoRepoFileDto() {
		return cryptoRepoFileDto;
	}

	public RepoFileDto getRepoFileDto() {
		return repoFileDto;
	}

	public long getRepoFileId() {
		return repoFileDto.getId();
	}

	@Override
	public List<ServerRepoFile> getChildren() {
		if (children == null)
			children = getReadOnlyMetaRepoManager().getChildServerRepoFiles(this);

		return children;
	}

	protected MetaOnlyRepoManagerImpl getReadOnlyMetaRepoManager() {
		return (MetaOnlyRepoManagerImpl) MetaOnlyRepoManagerImpl.getInstance();
	}
}
