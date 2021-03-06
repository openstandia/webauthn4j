package com.webauthn4j.validator.attestation.statement.androidkey;

import com.webauthn4j.test.TestUtil;
import com.webauthn4j.util.Base64UrlUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class KeyDescriptionValidatorTest {

    private KeyDescriptionValidator keyDescriptionValidator = new KeyDescriptionValidator();

    @Test
    void validate_test() {
        X509Certificate certificate = TestUtil.loadAndroidKeyAttestationCertificate();
        byte[] clientDataHash = Base64UrlUtil.decode("aGVsbG8");
        keyDescriptionValidator.validate(certificate, clientDataHash, false);
    }

    @Test
    void validate_with_teeEnforcedOnly_true_test() {
        X509Certificate certificate = TestUtil.loadAndroidKeyAttestationCertificate();
        byte[] clientDataHash = Base64UrlUtil.decode("aGVsbG8");
        keyDescriptionValidator.validate(certificate, clientDataHash, true);
    }

    @Test
    void validate_with_IOException_test() throws IOException {
        KeyDescriptionValidator target = spy(KeyDescriptionValidator.class);
        doThrow(new IOException()).when(target).extractKeyDescription(any());
        assertThrows(UncheckedIOException.class,
                () -> target.validate(TestUtil.loadAndroidKeyAttestationCertificate(), null, false)
        );
    }
}
