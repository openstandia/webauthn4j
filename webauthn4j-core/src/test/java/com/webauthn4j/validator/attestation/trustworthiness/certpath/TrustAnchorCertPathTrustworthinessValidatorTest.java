/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webauthn4j.validator.attestation.trustworthiness.certpath;

import com.webauthn4j.anchor.TrustAnchorsResolver;
import com.webauthn4j.response.attestation.authenticator.AAGUID;
import com.webauthn4j.response.attestation.statement.AttestationCertificatePath;
import com.webauthn4j.response.attestation.statement.CertificateBaseAttestationStatement;
import com.webauthn4j.test.TestUtil;
import com.webauthn4j.util.CertificateUtil;
import com.webauthn4j.validator.exception.CertificateException;
import com.webauthn4j.validator.exception.TrustAnchorNotFoundException;
import org.junit.jupiter.api.Test;

import java.security.cert.TrustAnchor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrustAnchorCertPathTrustworthinessValidatorTest {

    private TrustAnchorsResolver trustAnchorsResolver = mock(TrustAnchorsResolver.class);
    private TrustAnchorCertPathTrustworthinessValidator target = new TrustAnchorCertPathTrustworthinessValidator(trustAnchorsResolver);
    private AAGUID aaguid = AAGUID.ZERO;

    @Test
    void validate_test() {

        Set<TrustAnchor> trustAnchors = CertificateUtil.generateTrustAnchors(
                Collections.singletonList(TestUtil.load2tierTestRootCACertificate()));
        when(trustAnchorsResolver.resolve(aaguid)).thenReturn(trustAnchors);

        CertificateBaseAttestationStatement attestationStatement = TestUtil.createFIDOU2FAttestationStatement(TestUtil.create2tierTestAuthenticatorCertPath());
        target.validate(aaguid, attestationStatement);
    }

    @Test
    void validate_with_empty_trustAnchors_test() {

        Set<TrustAnchor> trustAnchors = Collections.emptySet();
        when(trustAnchorsResolver.resolve(aaguid)).thenReturn(trustAnchors);

        CertificateBaseAttestationStatement attestationStatement = TestUtil.createFIDOU2FAttestationStatement(TestUtil.create2tierTestAuthenticatorCertPath());
        assertThrows(TrustAnchorNotFoundException.class,
                () -> target.validate(aaguid, attestationStatement)
        );
    }

    @Test
    void validate_full_chain_test() {

        Set<TrustAnchor> trustAnchors = CertificateUtil.generateTrustAnchors(
                Collections.singletonList(TestUtil.load3tierTestRootCACertificate()));
        when(trustAnchorsResolver.resolve(aaguid)).thenReturn(trustAnchors);

        AttestationCertificatePath attestationCertificatePath
                = new AttestationCertificatePath(Arrays.asList(
                TestUtil.load3tierTestAuthenticatorAttestationCertificate(),
                TestUtil.load3tierTestIntermediateCACertificate(),
                TestUtil.load3tierTestRootCACertificate()));

        CertificateBaseAttestationStatement attestationStatement = TestUtil.createFIDOU2FAttestationStatement(attestationCertificatePath);
        target.setFullChainProhibited(true);
        assertThrows(CertificateException.class,
                () -> target.validate(aaguid, attestationStatement)
        );
    }

    @Test
    void test(){
        assertThat(target.isFullChainProhibited()).isFalse();
    }
}
