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

package com.webauthn4j.converter;

import com.webauthn4j.converter.util.CborConverter;
import com.webauthn4j.response.attestation.AttestationObject;
import com.webauthn4j.util.Base64UrlUtil;
import com.webauthn4j.util.JacksonUtil;

/**
 * Converter for {@link AttestationObject}
 */
public class AttestationObjectConverter {

    //~ Instance fields
    // ================================================================================================
    private CborConverter cborConverter;

    //~ Constructors
    // ================================================================================================

    public AttestationObjectConverter(CborConverter cborConverter) {
        this.cborConverter = cborConverter;
    }

    //~ Methods
    // ================================================================================================

    public AttestationObject  convert(String source) {
        if (source == null) {
            return null;
        }
        byte[] value = Base64UrlUtil.decode(source);
        return convert(value);
    }

    public AttestationObject convert(byte[] source) {
        if (source == null) {
            return null;
        }
        return cborConverter.readValue(source, AttestationObject.class);
    }

    public byte[] convertToBytes(AttestationObject source) {
        return cborConverter.writeValueAsBytes(source);
    }

    public String convertToString(AttestationObject source) {
        byte[] bytes = convertToBytes(source);
        return Base64UrlUtil.encodeToString(bytes);
    }

    public byte[] extractAuthenticatorData(byte[] attestationObject) {
        return JacksonUtil.binaryValue(cborConverter.readTree(attestationObject).get("authData"));
    }


}
