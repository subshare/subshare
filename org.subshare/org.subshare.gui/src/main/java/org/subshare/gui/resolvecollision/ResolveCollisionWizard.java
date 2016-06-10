package org.subshare.gui.resolvecollision;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.local.PlainHistoCryptoRepoFileFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;
import org.subshare.gui.resolvecollision.collision.CollisionData;
import org.subshare.gui.resolvecollision.collision.CollisionWizardPage;
import org.subshare.gui.resolvecollision.loading.LoadingWizardPage;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class ResolveCollisionWizard extends Wizard {

	private final ResolveCollisionData resolveCollisionData;

	public ResolveCollisionWizard(final ResolveCollisionData resolveCollisionData) {
		super(new LoadingWizardPage());
		this.resolveCollisionData = assertNotNull("resolveCollisionData", resolveCollisionData);
	}

	@Override
	public void init() {
		super.init();
		loadDataAsync();
	}

	private void loadDataAsync() {
		final PlainHistoCryptoRepoFileFilter phcrfFilter = new PlainHistoCryptoRepoFileFilter();
		phcrfFilter.setCollisionIds(resolveCollisionData.getCollisionIds());

		new Service<Collection<CollisionDtoWithPlainHistoCryptoRepoFileDto>>() {
			@Override
			protected Task<Collection<CollisionDtoWithPlainHistoCryptoRepoFileDto>> createTask() {
				return new SsTask<Collection<CollisionDtoWithPlainHistoCryptoRepoFileDto>>() {
					@Override
					protected Collection<CollisionDtoWithPlainHistoCryptoRepoFileDto> call() throws Exception {
						try (final LocalRepoManager localRepoManager = createLocalRepoManager()) {
							final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
							final Collection<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = localRepoMetaData.getPlainHistoCryptoRepoFileDtos(phcrfFilter);

							final Map<Uid, CollisionDtoWithPlainHistoCryptoRepoFileDto> collisionId2Dto = new HashMap<>();
							for (PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto : plainHistoCryptoRepoFileDtos) {
								for (CollisionDto collisionDto : plainHistoCryptoRepoFileDto.getCollisionDtos()) {
									final Uid collisionId = collisionDto.getCollisionId();
									CollisionDtoWithPlainHistoCryptoRepoFileDto dto = collisionId2Dto.get(collisionId);
									if (dto == null) {
										dto = new CollisionDtoWithPlainHistoCryptoRepoFileDto();
										collisionId2Dto.put(collisionId, dto);
									}
									dto.setCollisionDto(collisionDto);

									if (collisionDto.getHistoCryptoRepoFileId1().equals(plainHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId()))
										dto.setPlainHistoCryptoRepoFileDto1(plainHistoCryptoRepoFileDto);
									else if (collisionDto.getHistoCryptoRepoFileId2() != null
											&& collisionDto.getHistoCryptoRepoFileId2().equals(plainHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto().getHistoCryptoRepoFileId()))
										dto.setPlainHistoCryptoRepoFileDto2(plainHistoCryptoRepoFileDto);
									else
										throw new IllegalStateException("WTF?!");
								}

								for (CollisionPrivateDto cpDto : plainHistoCryptoRepoFileDto.getCollisionPrivateDtos()) {
									final Uid collisionId = cpDto.getCollisionId();
									final CollisionDtoWithPlainHistoCryptoRepoFileDto dto = collisionId2Dto.get(collisionId);
									assertNotNull("collisionId2Dto[" + collisionId + "]", dto);
									dto.setCollisionPrivateDto(cpDto);
								}
							}

							final List<CollisionDtoWithPlainHistoCryptoRepoFileDto> collisionDtoWithPlainHistoCryptoRepoFileDtos = new ArrayList<>(
									collisionId2Dto.values());

							for (final CollisionDtoWithPlainHistoCryptoRepoFileDto dto : collisionDtoWithPlainHistoCryptoRepoFileDtos) {
								assertNotNull("dto.collisionDto", dto.getCollisionDto());
								assertNotNull("dto.collisionPrivateDto", dto.getCollisionPrivateDto());
								assertNotNull("dto.plainHistoCryptoRepoFileDto1", dto.getPlainHistoCryptoRepoFileDto1());
							}
							return collisionDtoWithPlainHistoCryptoRepoFileDtos;
						}
					}

					@Override
					protected void succeeded() {
						final Collection<CollisionDtoWithPlainHistoCryptoRepoFileDto> collisionDtoWithPlainHistoCryptoRepoFileDtos;
						try { collisionDtoWithPlainHistoCryptoRepoFileDtos = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }

						for (CollisionDtoWithPlainHistoCryptoRepoFileDto dto : collisionDtoWithPlainHistoCryptoRepoFileDtos) {
							CollisionData collisionData = new CollisionData();
							collisionData.setCollisionDtoWithPlainHistoCryptoRepoFileDto(dto);
							resolveCollisionData.getCollisionDatas().add(collisionData);
						}

						if (resolveCollisionData.getCollisionDatas().isEmpty())
							setFirstPage(null);
						else {
							CollisionWizardPage lastPage = null;
							for (CollisionData collisionData : resolveCollisionData.getCollisionDatas()) {
								final CollisionWizardPage currentPage = new CollisionWizardPage(collisionData);
								if (lastPage == null)
									setFirstPage(currentPage);
								else
									lastPage.setNextPage(currentPage);

								lastPage = currentPage;
							}
						}
					}
				};
			}
		}.start();
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		try (final LocalRepoManager localRepoManager = createLocalRepoManager()) {
			final SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();

			final List<CollisionData> collisionDatas = resolveCollisionData.getCollisionDatas();
			for (final CollisionData collisionData : collisionDatas) {
				final CollisionPrivateDto collisionPrivateDto = collisionData.getCollisionDtoWithPlainHistoCryptoRepoFileDto().getCollisionPrivateDto();
				localRepoMetaData.putCollisionPrivateDto(collisionPrivateDto);
			}
		}
	}

	@Override
	public String getTitle() {
		return "Resolve collision";
	}

	private LocalRepoManager createLocalRepoManager() {
		final LocalRepo localRepo = assertNotNull("resolveCollisionData.localRepo", resolveCollisionData.getLocalRepo());
		return LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRepo.getLocalRoot());
	}
}
