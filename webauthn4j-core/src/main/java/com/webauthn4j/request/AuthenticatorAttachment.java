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

package com.webauthn4j.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * This enumeration’s values describe authenticators' attachment modalities. Relying Parties use this for two purposes:
 * <ul>
 *     <li>to express a preferred authenticator attachment modality when calling navigator.credentials.create()
 *     to create a credential, and</li>
 *     <li>to inform the client of the Relying Party's best belief about how to locate the managing authenticators of
 *     the credentials listed in allowCredentials when calling navigator.credentials.get().</li>
 * </ul>
 *
 * @see <a href="https://www.w3.org/TR/2019/PR-webauthn-20190117/#attachment">
 * §5.4.5. Authenticator Attachment Enumeration (enum AuthenticatorAttachment)</a>
 */
public enum AuthenticatorAttachment {
    PLATFORM("platform"),
    CROSS_PLATFORM("cross-platform");

    private String value;

    AuthenticatorAttachment(String value) {
        this.value = value;
    }

    public static AuthenticatorAttachment create(String value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case "platform":
                return PLATFORM;
            case "cross-platform":
                return CROSS_PLATFORM;
            default:
                throw new IllegalArgumentException("value '" + value + "' is out of range");
        }
    }

    @JsonCreator
    private static AuthenticatorAttachment fromJson(String value) throws InvalidFormatException {
        try{
            return create(value);
        }
        catch (IllegalArgumentException e){
            throw new InvalidFormatException(null, "value is out of range", value, AuthenticatorAttachment.class);
        }
    }

    @JsonValue
    public String getValue() {
        return value;
    }

}
