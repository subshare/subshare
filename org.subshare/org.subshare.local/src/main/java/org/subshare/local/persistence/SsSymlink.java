package org.subshare.local.persistence;

import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.local.persistence.Symlink;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="SsSymlink")
public class SsSymlink extends Symlink implements SsRepoFile {

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//			@Persistent(nullValue=NullValue.EXCEPTION)
//			@Embedded
//			private SignatureImpl signature;

//	@Persistent(nullValue=NullValue.EXCEPTION)
	private Date signatureCreated;

//	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String signingUserRepoKeyId;

//	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] signatureData;
	// END WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//			@Override
//			public Signature getSignature() {
//				return signature;
//			}
//			@Override
//			public void setSignature(final Signature signature) {
//				if (!equal(this.signature, signature))
//					this.signature = SignatureImpl.copy(signature);
//			}
	@Override
	public Signature getSignature() {
		String.valueOf(signatureCreated);
		String.valueOf(signingUserRepoKeyId);
		String.valueOf(signatureData);
		return SignableEmbeddedWorkaround.getSignature(this);
	}
	@Override
	public void setSignature(final Signature signature) {
		SignableEmbeddedWorkaround.setSignature(this, signature);
	}
// END WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
}
