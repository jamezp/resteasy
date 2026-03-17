package org.jboss.resteasy.security.doseta.i18n;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.resteasy.security.doseta.DKIMSignature;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Aug 29, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 13500, value = "Body hashes do not match.")
    String bodyHashesDoNotMatch();

    @Message(id = 13505, value = "Certificate nor public key properties set")
    String certificateNorPublicKeySet();

    @Message(id = 13510, value = "The certificate object was not set.")
    String certificateObjectNotSet();

    @Message(id = 13515, value = ">>>> Check DNS: %s")
    String checkDNS(String alias);

    @Message(id = 13520, value = "Could not find a message body reader for type: %s")
    String couldNotFindMessageBodyReader(String className);

    @Message(id = 13525, value = "Could not find PublicKey for DKIMSignature %s")
    String couldNotFindPublicKey(DKIMSignature signature);

    @Message(id = 13530, value = ">>>> DNS found record: %s")
    String dnsRecordFound(String record);

    @Message(id = 13535, value = "domain attribute is required in header to find a key.")
    String domainAttributeIsRequired();

    @Message(id = 13540, value = "Expected value ''{0}'' got ''{1}'' for attribute ''{2}''", format = Format.MESSAGE_FORMAT)
    String expectedValue(String expectedValue, String value, String attribute);

    @Message(id = 13545, value = "Failed to find public key in DNS %s")
    String failedToFindPublicKey(String alias);

    @Message(id = 13550, value = "Failed to find writer for type: %s")
    String failedToFindWriter(String className);

    @Message(id = 13555, value = "Failed to parse body hash (bh)")
    String failedToParseBodyHash();

    @Message(id = 13560, value = "Failed to sign")
    String failedToSign();

    @Message(id = 13565, value = "Failed to verify signature.")
    String failedToVerifySignature();

    @Message(id = 13570, value = "Failed to verify signatures:")
    String failedToVerifySignatures();

    @Message(id = 13575, value = "Malformed %s header")
    String malformedSignatureHeader(String signature);

    @Message(id = 13580, value = "No key to verify with.")
    String noKeyToVerifyWith();

    @Message(id = 13585, value = "No p entry in text record.")
    String noPEntry();

    @Message(id = 13590, value = "pem: %s")
    String pem(String pem);

    @Message(id = 13595, value = "private key is null, cannot sign")
    String privateKeyIsNull();

    @Message(id = 13600, value = "Public key is null.")
    String publicKeyIsNull();

    @Message(id = 13605, value = "Signature expired")
    String signatureExpired();

    @Message(id = 13610, value = "Signature is stale")
    String signatureIsStale();

    @Message(id = 13615, value = "There was no body hash (bh) in header")
    String thereWasNoBodyHash();

    @Message(id = 13620, value = "There was no %s header")
    String thereWasNoSignatureHeader(String signature);

    @Message(id = 13625, value = "Unable to find header {0} {1} to sign header with", format = Format.MESSAGE_FORMAT)
    String unableToFindHeader(String header, String index);

    @Message(id = 13630, value = "Unable to find key to sign message. Repository returned null. ")
    String unableToFindKey();

    @Message(id = 13635, value = "Unable to find key store in path: %s")
    String unableToFindKeyStore(String path);

    @Message(id = 13640, value = "Unable to locate a private key to sign message, repository is null.")
    String unableToLocatePrivateKey();

    @Message(id = 13645, value = "Unsupported algorithm %s")
    String unsupportedAlgorithm(String algorithm);

    @Message(id = 13650, value = "Unsupported key type: %s")
    String unsupportedKeyType(String type);
}
