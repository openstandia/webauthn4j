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

package com.webauthn4j.response.attestation.statement;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class TPMAObjectTest {

    @Test
    void getter_test() {
        TPMAObject tpmaObject = new TPMAObject(394354);
        assertAll(
                () -> assertThat(tpmaObject.isFixedTPM()).isTrue(),
                () -> assertThat(tpmaObject.isStClear()).isFalse(),
                () -> assertThat(tpmaObject.isFixedParent()).isTrue(),
                () -> assertThat(tpmaObject.isSensitiveDataOrigin()).isTrue(),
                () -> assertThat(tpmaObject.isUserWithAuth()).isTrue(),
                () -> assertThat(tpmaObject.isAdminWithPolicy()).isFalse(),
                () -> assertThat(tpmaObject.isNoDA()).isTrue(),
                () -> assertThat(tpmaObject.isEncryptedDuplication()).isFalse(),
                () -> assertThat(tpmaObject.isRestricted()).isFalse(),
                () -> assertThat(tpmaObject.isDecrypt()).isTrue(),
                () -> assertThat(tpmaObject.isSignEncrypt()).isTrue()
        );
    }
}