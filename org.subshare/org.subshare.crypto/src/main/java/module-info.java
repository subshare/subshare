open module org.subshare.crypto {
	
	requires transitive java.base;
	requires transitive java.se;

	requires transitive org.slf4j;

	requires transitive org.bouncycastle.provider;

	exports org.subshare.crypto;

}