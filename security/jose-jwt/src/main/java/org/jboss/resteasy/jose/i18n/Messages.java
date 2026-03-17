package org.jboss.resteasy.jose.i18n;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.resteasy.jose.jwe.CompressionAlgorithm;
import org.jboss.resteasy.jose.jwe.EncryptionMethod;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Aug 28, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(Messages.class);

    @Message(id = 14000, value = "The algorithm of the shared symmetric key must be AES")
    String algorithmOfSharedSymmetricKey();

    @Message(id = 14005, value = "Algorithm was null")
    String algorithmWasNull();

    @Message(id = 14010, value = "The authentication tag must not be null")
    String authenticationTagMustNotBeNull();

    @Message(id = 14015, value = "CEK key length mismatch: {0} != {1}", format = Format.MESSAGE_FORMAT)
    String cekKeyLengthMismatch(int length1, int length2);

    @Message(id = 14020, value = "The cipher text must not be null")
    String cipherTextMustNotBeNull();

    @Message(id = 14025, value = "The Content Encryption Key (CEK) length must be {0} bits for {1} encryption", format = Format.MESSAGE_FORMAT)
    String contentEncryptionKeyLength(int length, EncryptionMethod method);

    @Message(id = 14030, value = "Could not find MessageBodyReader for JSON")
    String couldNotFindMessageBodyReaderForJSON();

    @Message(id = 14035, value = "Could not find MessageBodyWriter for JSON")
    String couldNotFindMessageBodyWriterForJSON();

    @Message(id = 14040, value = "Couldn't compress plain text: %s")
    String couldntCompressPlainText(String message);

    @Message(id = 14045, value = "Couldn't decompress plain text: %s")
    String couldntDecompressPlainText(String message);

    @Message(id = 14050, value = "Couldn't decrypt Content Encryption Key (CEK): %s")
    String couldntDecryptCEK(String message);

    @Message(id = 14055, value = "Couldn't encrypt Content Encryption Key (CEK): %s")
    String couldntEncryptCEK(String message);

    @Message(id = 14060, value = "Couldn't generate GCM authentication tag: %s")
    String couldntGenerateGCMAuthentication(String message);

    @Message(id = 14065, value = "Couldn't validate GCM authentication tag: %s")
    String couldntValidateGCMAuthentication(String message);

    @Message(id = 14070, value = "The encrypted key must not be null")
    String encryptedKeyMustNotBeNull();

    @Message(id = 14075, value = "EncryptionMethod was null")
    String encryptionMethodWasNull();

    @Message(id = 14080, value = "Illegal base64url string!")
    String illegalBase64UrlString();

    @Message(id = 14085, value = "The initialization vector (IV) must not be null")
    String initializationVectorMustNotBeNull();

    @Message(id = 14090, value = "Invalid HMAC key: %s")
    String invalidHMACkey(String message);

    @Message(id = 14095, value = "The length of the shared symmetric key must be 128 bits (16 bytes), 256 bits (32 bytes) or 512 bites (64 bytes)")
    String lengthOfSharedSymmetricKey();

    @Message(id = 14100, value = "MAC check failed")
    String macCheckFailed();

    @Message(id = 14105, value = "Not a MAC Algorithm")
    String notAMACalgorithm();

    @Message(id = 14110, value = "Not an RSA Algorithm")
    String notAnRSAalgorithm();

    @Message(id = 14115, value = "Not encrypted with dir algorithm")
    String notEncryptedWithDirAlgorithm();

    @Message(id = 14120, value = "Not encrypted with RSA algorithm")
    String notEncryptedWithRSAalgorithm();

    @Message(id = 14125, value = "Parsing error")
    String parsingError();

    @Message(id = 14130, value = "Unable to find MessageBodyWriter")
    String unableToFindMessageBodyWriter();

    @Message(id = 14135, value = "Unable to find reader for content type")
    String unableToFindReaderForContentType();

    @Message(id = 14140, value = "Unexpected encrypted key, must be omitted")
    String unexpectedEncryptedKey();

    @Message(id = 14145, value = "Unknown length")
    String unknownLength();

    @Message(id = 14150, value = "Unsupported algorithm, must be \"dir\"")
    String unsupportedAlgorithm();

    @Message(id = 14155, value = "Unsupported compression algorithm: %s")
    String unsupportedCompressionAlgorithm(CompressionAlgorithm algorithm);

    @Message(id = 14160, value = "Unsupported encryption method, must be A128CBC_HS256, A256CBC_HS512, A128GCM or A128GCM")
    String unsupportedEncryptionMethod();

    @Message(id = 14165, value = "Unsupported HMAC algorithm: %s")
    String unsupportedHMACalgorithm(String message);

    @Message(id = 14170, value = "Unsupported JWE algorithm, must be RSA1_5 or RSA_OAEP")
    String unsupportedJWEalgorithm();

    @Message(id = 14175, value = "Unsupported AES/CBC/PKCS5Padding/HMAC-SHA2 key length, must be 256 or 512 bits")
    String unsupportedKeyLength();
}
