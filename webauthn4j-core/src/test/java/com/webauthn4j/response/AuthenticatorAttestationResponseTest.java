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

package com.webauthn4j.response;

import com.webauthn4j.request.AuthenticatorTransport;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class AuthenticatorAttestationResponseTest {

    @Test
    void equals_hashCode_test(){
        AuthenticatorAttestationResponse instanceA = new AuthenticatorAttestationResponse(new byte[0], new byte[1]);
        AuthenticatorAttestationResponse instanceB = new AuthenticatorAttestationResponse(new byte[0], new byte[1]);

        assertAll(
                () -> assertThat(instanceA).isEqualTo(instanceB),
                () -> assertThat(instanceA).hasSameHashCodeAs(instanceB)
        );
    }

    @Test
    void getTransports_test(){
        AuthenticatorAttestationResponse target;
        target = new AuthenticatorAttestationResponse(new byte[0], new byte[0], Collections.singletonList(AuthenticatorTransport.USB));
        assertThat(target.getAuthenticatorTransports()).containsExactly(AuthenticatorTransport.USB);

        target = new AuthenticatorAttestationResponse(new byte[0], new byte[0]);
        assertThat(target.getAuthenticatorTransports()).isEmpty();
    }
}
