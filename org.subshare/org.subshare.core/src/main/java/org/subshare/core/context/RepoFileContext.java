package org.subshare.core.context;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.SsRepoFileDto;

import co.codewizards.cloudstore.core.dto.RepoFileDto;

public class RepoFileContext {

	private static final ThreadLocal<RepoFileContext> threadLocal = new ThreadLocal<RepoFileContext>();

	private final String path;
	private final RepoFileDto repoFileDto;

	public static void setContext(final RepoFileContext context) {
		if (context == null)
			threadLocal.remove();
		else
			threadLocal.set(context);
	}

	public static RepoFileContext getContext() {
		return threadLocal.get();
	}

//	public static RepoFileContext getContextOrFail() {
//		final RepoFileContext context = getContext();
//		if (context == null)
//			throw new IllegalStateException("There is no RepoFileContext currently associated with the current thread!");
//
//		return context;
//	}

	public RepoFileContext(final String path, final RepoFileDto repoFileDto) {
		this.path = assertNotNull("path", path);
		this.repoFileDto = assertNotNull("path", repoFileDto);

		if (!(repoFileDto instanceof SsRepoFileDto))
			throw new IllegalArgumentException("repoFileDto is not an instance of SsRepoFileDto!");
	}

	public String getPath() {
		return path;
	}

	public RepoFileDto getRepoFileDto() {
		return repoFileDto;
	}

	public SsRepoFileDto getSsRepoFileDto() {
		return (SsRepoFileDto) repoFileDto;
	}

}
