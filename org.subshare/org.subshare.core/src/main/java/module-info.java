open module org.subshare.core {

	requires transitive co.codewizards.cloudstore.core;

	requires transitive org.subshare.crypto;

	exports org.subshare.core;
	exports org.subshare.core.appid;
	exports org.subshare.core.crypto;
	exports org.subshare.core.dto;
	exports org.subshare.core.dto.jaxb;
	exports org.subshare.core.dto.split;
	exports org.subshare.core.fbor;
	exports org.subshare.core.file;
	exports org.subshare.core.io;
	exports org.subshare.core.locker;
	exports org.subshare.core.locker.sync;
	exports org.subshare.core.locker.transport;
	exports org.subshare.core.locker.transport.local;
	exports org.subshare.core.observable;
	exports org.subshare.core.observable.standard;
	exports org.subshare.core.pgp;
	exports org.subshare.core.pgp.gnupg;
	exports org.subshare.core.pgp.man;
	exports org.subshare.core.pgp.sync;
	exports org.subshare.core.pgp.transport;
	exports org.subshare.core.pgp.transport.local;
	exports org.subshare.core.repair;
	exports org.subshare.core.repo;
	exports org.subshare.core.repo.histo;
	exports org.subshare.core.repo.listener;
	exports org.subshare.core.repo.local;
	exports org.subshare.core.repo.metaonly;
	exports org.subshare.core.repo.sync;
	exports org.subshare.core.repo.transport;
	exports org.subshare.core.server;
	exports org.subshare.core.sign;
	exports org.subshare.core.sync;
	exports org.subshare.core.user;
	exports org.subshare.core.version;

}