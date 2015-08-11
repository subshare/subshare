package org.subshare.core.repo.metaonly;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.listener.LocalRepoCommitEvent;
import org.subshare.core.repo.listener.LocalRepoCommitEventListener;
import org.subshare.core.repo.listener.LocalRepoCommitEventManagerImpl;
import org.subshare.core.repo.listener.WeakLocalRepoCommitEventListener;
import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.collection.ListMerger;
import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.SymlinkDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.util.UrlUtil;

public class ServerRepoFileImpl implements ServerRepoFile {

	private final ServerRepoFileImpl parent;
	private final long repoFileId;
	private CryptoRepoFileDto cryptoRepoFileDto;
	private RepoFileDto repoFileDto;
	private final Server server;
	private final ServerRepo serverRepo;
	private final UUID localRepositoryId;
	private List<ServerRepoFile> children;
	private boolean childrenInvalid;

	private final LocalRepoCommitEventListener localRepoCommitEventListener = new LocalRepoCommitEventListener() {
		@Override
		public void postCommit(LocalRepoCommitEvent event) {
			invalidate();
		}
	};

	private final WeakLocalRepoCommitEventListener weakLocalRepoCommitEventListener;

	public ServerRepoFileImpl(final Server server, final ServerRepo serverRepo, final UUID localRepositoryId, final CryptoRepoFileDto cryptoRepoFileDto, final RepoFileDto repoFileDto) {
		parent = null;
		this.server = assertNotNull("server", server);
		this.serverRepo = assertNotNull("serverRepo", serverRepo);
		this.localRepositoryId = assertNotNull("localRepositoryId", localRepositoryId);
		this.cryptoRepoFileDto = assertNotNull("cryptoRepoFileDto", cryptoRepoFileDto);
		this.repoFileDto = assertNotNull("repoFileDto", repoFileDto);
		this.repoFileId = repoFileDto.getId();

		weakLocalRepoCommitEventListener = new WeakLocalRepoCommitEventListener(
				LocalRepoCommitEventManagerImpl.getInstance(), localRepositoryId, localRepoCommitEventListener);
		weakLocalRepoCommitEventListener.addLocalRepoCommitEventListener();
	}

	public ServerRepoFileImpl(final ServerRepoFileImpl parent, final CryptoRepoFileDto cryptoRepoFileDto, final RepoFileDto repoFileDto) {
		this.parent = assertNotNull("parent", parent);
		this.server = parent.getServer();
		this.serverRepo = parent.getServerRepo();
		this.localRepositoryId = parent.getLocalRepositoryId();
		this.cryptoRepoFileDto = assertNotNull("cryptoRepoFileDto", cryptoRepoFileDto);
		this.repoFileDto = assertNotNull("repoFileDto", repoFileDto);
		this.repoFileId = repoFileDto.getId();

		weakLocalRepoCommitEventListener = new WeakLocalRepoCommitEventListener(
				LocalRepoCommitEventManagerImpl.getInstance(), localRepositoryId, localRepoCommitEventListener);
		weakLocalRepoCommitEventListener.addLocalRepoCommitEventListener();
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
		final RepoFileDto repoFileDto = getRepoFileDto();

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
		return getRepoFileDto().getName();
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

	public synchronized CryptoRepoFileDto getCryptoRepoFileDto() {
		if (cryptoRepoFileDto == null) {
			getRepoFileDto();
			assertNotNull("cryptoRepoFileDto", cryptoRepoFileDto);
		}
		return cryptoRepoFileDto;
	}

	public synchronized RepoFileDto getRepoFileDto() {
		if (repoFileDto == null) {
			final ServerRepoFileImpl serverRepoFile = (ServerRepoFileImpl) getReadOnlyMetaRepoManager().getServerRepoFile(serverRepo, repoFileId);
			copyFrom(assertNotNull("serverRepoFile", serverRepoFile));
		}
		return repoFileDto;
	}

	protected void copyFrom(final ServerRepoFileImpl serverRepoFile) {
		assertNotNull("serverRepoFile", serverRepoFile);
		repoFileDto = serverRepoFile.getRepoFileDto();
		cryptoRepoFileDto = serverRepoFile.getCryptoRepoFileDto();
	}

	@Override
	public long getRepoFileId() {
		return repoFileId;
	}

	@Override
	public synchronized List<ServerRepoFile> getChildren() {
		if (children == null || childrenInvalid) {
			final List<ServerRepoFile> c = getReadOnlyMetaRepoManager().getChildServerRepoFiles(this);
			if (children == null || c == null)
				children = c;
			else
				new ChildrenListMerger().merge(c, children);

			childrenInvalid = false;
		}

		return children;
	}

	protected static class ChildrenListMerger extends ListMerger<ServerRepoFile, Long> {
		@Override
		protected Long getKey(ServerRepoFile element) {
			return element.getRepoFileId();
		}

		@Override
		protected void update(List<ServerRepoFile> dest, int index,
				ServerRepoFile sourceElement, ServerRepoFile destElement) {
			final ServerRepoFileImpl se = (ServerRepoFileImpl) sourceElement;
			final ServerRepoFileImpl de = (ServerRepoFileImpl) destElement;
			de.copyFrom(se);
		}
	}

	protected synchronized void invalidate() {
		childrenInvalid = true;
		cryptoRepoFileDto = null;
		repoFileDto = null;
	}

	protected MetaOnlyRepoManagerImpl getReadOnlyMetaRepoManager() {
		return (MetaOnlyRepoManagerImpl) MetaOnlyRepoManagerImpl.getInstance();
	}
}
