open module org.subshare.crypto {
	
//	requires transitive java.base;
	requires transitive java.se;

	requires transitive org.slf4j;

	requires transitive org.bouncycastle.provider;

//	/**
//	 * c.c.cloudstore.core is needed for the junit-tests.
//	 */
//	requires static co.codewizards.cloudstore.core;

	exports org.subshare.crypto;

}