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

package com.webauthn4j.validator.attestation.statement.packed;

import com.webauthn4j.test.TestUtil;
import com.webauthn4j.validator.RegistrationObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NullPackedAttestationStatementValidatorTest {

    private NullPackedAttestationStatementValidator target = new NullPackedAttestationStatementValidator();

    @Test
    void validate_test() {
        RegistrationObject registrationObject = TestUtil.createRegistrationObjectWithPackedAttestation();
        target.validate(registrationObject);
    }

    @Test
    void validate_non_PackedAttestation_test() {
        RegistrationObject registrationObject = TestUtil.createRegistrationObjectWithTPMAttestation();
        assertThrows(IllegalArgumentException.class,
                () -> target.validate(registrationObject)
        );
    }
}