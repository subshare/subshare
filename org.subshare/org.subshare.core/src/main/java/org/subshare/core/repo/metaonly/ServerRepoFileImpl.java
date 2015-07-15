package org.subshare.core.repo.metaonly;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.List;

import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.repo.ServerRepo;

import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.SymlinkDto;

public class ServerRepoFileImpl implements ServerRepoFile {

	private final CryptoRepoFileDto cryptoRepoFileDto;
	private final RepoFileDto repoFileDto;
	private final ServerRepo serverRepo;
	private final ServerRepoFileImpl parent;
	private List<ServerRepoFile> children;

	public ServerRepoFileImpl(final ServerRepo serverRepo, final CryptoRepoFileDto cryptoRepoFileDto, final RepoFileDto repoFileDto) {
		this.serverRepo = assertNotNull("serverRepo", serverRepo);
		parent = null;
		this.cryptoRepoFileDto = assertNotNull("cryptoRepoFileDto", cryptoRepoFileDto);
		this.repoFileDto = assertNotNull("repoFileDto", repoFileDto);
	}

	public ServerRepoFileImpl(final ServerRepoFileImpl parent, final CryptoRepoFileDto cryptoRepoFileDto, final RepoFileDto repoFileDto) {
		this.parent = assertNotNull("parent", parent);
		this.serverRepo = parent.getServerRepo();
		this.cryptoRepoFileDto = assertNotNull("cryptoRepoFileDto", cryptoRepoFileDto);
		this.repoFileDto = assertNotNull("repoFileDto", repoFileDto);
	}

	public ServerRepo getServerRepo() {
		return serverRepo;
	}

	@Override
	public ServerRepoFile getParent() {
		return parent;
	}

//	public String getServerName() {
//		return cryptoRepoFileDto.getCryptoRepoFileId().toString();
//	}

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

	public CryptoRepoFileDto getCryptoRepoFileDto() {
		return cryptoRepoFileDto;
	}

	public RepoFileDto getRepoFileDto() {
		return repoFileDto;
	}

	@Override
	public List<ServerRepoFile> getChildren() {
		if (children == null)
			children = getReadOnlyMetaRepoManager().getChildServerRepoFiles(cryptoRepoFileDto.getCryptoRepoFileId());

		return children;
	}

	protected MetaOnlyRepoManagerImpl getReadOnlyMetaRepoManager() {
		return (MetaOnlyRepoManagerImpl) MetaOnlyRepoManagerImpl.getInstance();
	}
}
