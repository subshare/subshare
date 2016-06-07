//package org.subshare.local;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
//
//public class PlainHistoCryptoRepoFileDtoCache { // TODO improve or remove this class! this is just a simple test!
//
//	private final Map<Long, PlainHistoCryptoRepoFileDto> histoCryptoRepoFileOid2PlainHistoCryptoRepoFileDto = new HashMap<>();
//
//	private static class Holder {
//		public static final PlainHistoCryptoRepoFileDtoCache instance = new PlainHistoCryptoRepoFileDtoCache();
//	}
//
//	public static PlainHistoCryptoRepoFileDtoCache getInstance() {
//		return Holder.instance;
//	}
//
//	protected PlainHistoCryptoRepoFileDtoCache() {
//	}
//
//	public synchronized PlainHistoCryptoRepoFileDto getPlainHistoCryptoRepoFileDto(long oid) {
//		return histoCryptoRepoFileOid2PlainHistoCryptoRepoFileDto.get(oid);
//	}
//
//	public synchronized void putPlainHistoCryptoRepoFileDto(long oid, PlainHistoCryptoRepoFileDto dto) {
//		histoCryptoRepoFileOid2PlainHistoCryptoRepoFileDto.put(oid, dto);
//	}
//}
