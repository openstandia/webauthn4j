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

package com.webauthn4j.test;

import com.webauthn4j.anchor.TrustAnchorsResolver;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.AttestationObjectConverter;
import com.webauthn4j.converter.AuthenticatorDataConverter;
import com.webauthn4j.converter.CollectedClientDataConverter;
import com.webauthn4j.converter.util.CborConverter;
import com.webauthn4j.converter.util.JsonConverter;
import com.webauthn4j.response.attestation.AttestationObject;
import com.webauthn4j.response.attestation.authenticator.*;
import com.webauthn4j.response.attestation.statement.*;
import com.webauthn4j.response.client.ClientDataType;
import com.webauthn4j.response.client.CollectedClientData;
import com.webauthn4j.response.client.Origin;
import com.webauthn4j.response.client.challenge.Challenge;
import com.webauthn4j.response.client.challenge.DefaultChallenge;
import com.webauthn4j.response.extension.authenticator.ExtensionAuthenticatorOutput;
import com.webauthn4j.response.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.test.authenticator.model.WebAuthnModelException;
import com.webauthn4j.util.*;
import com.webauthn4j.util.exception.NotImplementedException;
import com.webauthn4j.validator.RegistrationObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static com.webauthn4j.response.attestation.authenticator.AuthenticatorData.BIT_AT;
import static com.webauthn4j.response.attestation.authenticator.AuthenticatorData.BIT_UP;

/**
 * A utility class for core module test
 */
public class TestUtil {

    private static JsonConverter jsonConverter = new JsonConverter();
    private static CborConverter cborConverter = new CborConverter();
    private static CollectedClientDataConverter collectedClientDataConverter = new CollectedClientDataConverter(jsonConverter);
    private static AttestationObjectConverter attestationObjectConverter = new AttestationObjectConverter(cborConverter);
    private static AuthenticatorDataConverter authenticatorDataConverter = new AuthenticatorDataConverter(cborConverter);

    private TestUtil() {
    }

    public static RegistrationObject createRegistrationObject(Function<byte[], AttestationObject> attestationObjectProvider) {
        CollectedClientData collectedClientData = createClientData(ClientDataType.CREATE);
        byte[] collectedClientDataBytes = collectedClientDataConverter.convertToBytes(collectedClientData);
        AttestationObject attestationObject = attestationObjectProvider.apply(collectedClientDataBytes);
        byte[] attestationObjectBytes =attestationObjectConverter.convertToBytes(attestationObject);
        AuthenticatorData authenticatorData = TestUtil.createAuthenticatorData();
        byte[] authenticatorDataBytes = authenticatorDataConverter.convert(authenticatorData);
        return new RegistrationObject(
                collectedClientData,
                collectedClientDataBytes,
                attestationObject,
                attestationObjectBytes,
                authenticatorDataBytes,
                TestUtil.createServerProperty()
        );
    }

    public static RegistrationObject createRegistrationObjectWithPackedAttestation() {
        CollectedClientData collectedClientData = TestUtil.createClientData(ClientDataType.CREATE);
        byte[] collectedClientDataBytes = collectedClientDataConverter.convertToBytes(collectedClientData);
        byte[] clientDataHash = MessageDigestUtil.createSHA256().digest(collectedClientDataBytes);
        AttestationObject attestationObject = createAttestationObjectWithBasicPackedECAttestationStatement(clientDataHash);
        byte[] attestationObjectBytes = attestationObjectConverter.convertToBytes(attestationObject);
        byte[] authenticatorDataBytes = attestationObjectConverter.extractAuthenticatorData(attestationObjectBytes);
        return new RegistrationObject(collectedClientData, collectedClientDataBytes, attestationObject, attestationObjectBytes, authenticatorDataBytes, TestUtil.createServerProperty());
    }

    public static RegistrationObject createRegistrationObjectWithAndroidKeyAttestation(){
        byte[] collectedClientDataBytes = Base64UrlUtil.decode("eyJvcmlnaW4iOiJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJjaGFsbGVuZ2UiOiJ2MmgxYzJWeWJtRnRaWFEwYVY5T2JUUm9iakZEZUVrd1NHYzNPSGh6VFdsamFHRnNiR1Z1WjJWUXR1YkVEQzRPU3BHSGViSExMTmVyRmY4IiwidHlwZSI6IndlYmF1dGhuLmNyZWF0ZSJ9");
        CollectedClientData collectedClientData = collectedClientDataConverter.convert(collectedClientDataBytes);
        byte[] attestationObjectBytes = Base64UrlUtil.decode("o2NmbXRrYW5kcm9pZC1rZXlnYXR0U3RtdKNjYWxnJmNzaWdYSDBGAiEAl0EDZokwnDApmVkWnSc24ELfZCI-Fx3s7K6YLM-W-xACIQCHvO-RPrqBSVV8rHYlWvRUt-UXpwRc4NQPBnVZ6k9CGGN4NWOCWQMEMIIDADCCAqegAwIBAgIBATAKBggqhkjOPQQDAjCBzjFFMEMGA1UEAww8RkFLRSBBbmRyb2lkIEtleXN0b3JlIFNvZnR3YXJlIEF0dGVzdGF0aW9uIEludGVybWVkaWF0ZSBGQUtFMTEwLwYJKoZIhvcNAQkBFiJjb25mb3JtYW5jZS10b29sc0BmaWRvYWxsaWFuY2Uub3JnMRYwFAYDVQQKDA1GSURPIEFsbGlhbmNlMQwwCgYDVQQLDANDV0cxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJNWTESMBAGA1UEBwwJV2FrZWZpZWxkMCAXDTcwMDIwMTAwMDAwMFoYDzIwOTkwMTMxMjM1OTU5WjApMScwJQYDVQQDDB5GQUtFIEFuZHJvaWQgS2V5c3RvcmUgS2V5IEZBS0UwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQsOMT5hyL6rg0c9ilM8skJWRYWTG4vpnP2MpS9aOeYzxkCOREmADn0fEDOqrk0EqMoY2RE_NOgN8jqAlHtgFiRo4IBFjCCARIwCwYDVR0PBAQDAgeAMIHhBgorBgEEAdZ5AgERBIHSMIHPAgECCgEAAgEBCgEABCAhLBhI9_zUhPMmw_wgGYR4IbEhgriX50b2mPD1DoesJgQAMGm_hT0IAgYBXtPjz6C_hUVZBFcwVTEvMC0EKGNvbS5hbmRyb2lkLmtleXN0b3JlLmFuZHJvaWRrZXlzdG9yZWRlbW8CAQExIgQgdM_LUHSI9SkQhZHHpQWRnzJ3MvvB2ANSauqYAAbS2JgwMqEFMQMCAQKiAwIBA6MEAgIBAKUFMQMCAQSqAwIBAb-DeAMCAQK_hT4DAgEAv4U_AgUAMB8GA1UdIwQYMBaAFFKaGzLgVqrNUQ_vX4A3BovykSMdMAoGCCqGSM49BAMCA0cAMEQCIAgOX0m5-z0iFe-5iG049P5hmYwJ70PsC1gYvsQyL7SOAiA2cqK2McZgFvnoiGURFVEXR69LKX1gogUaO9IJZhR8TlkC7jCCAuowggKRoAMCAQICAQIwCgYIKoZIzj0EAwIwgcYxPTA7BgNVBAMMNEZBS0UgQW5kcm9pZCBLZXlzdG9yZSBTb2Z0d2FyZSBBdHRlc3RhdGlvbiBSb290IEZBS0UxMTAvBgkqhkiG9w0BCQEWImNvbmZvcm1hbmNlLXRvb2xzQGZpZG9hbGxpYW5jZS5vcmcxFjAUBgNVBAoMDUZJRE8gQWxsaWFuY2UxDDAKBgNVBAsMA0NXRzELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAk1ZMRIwEAYDVQQHDAlXYWtlZmllbGQwHhcNMTgwNTA5MTIzMTQ0WhcNNDUwOTI0MTIzMTQ0WjCBzjFFMEMGA1UEAww8RkFLRSBBbmRyb2lkIEtleXN0b3JlIFNvZnR3YXJlIEF0dGVzdGF0aW9uIEludGVybWVkaWF0ZSBGQUtFMTEwLwYJKoZIhvcNAQkBFiJjb25mb3JtYW5jZS10b29sc0BmaWRvYWxsaWFuY2Uub3JnMRYwFAYDVQQKDA1GSURPIEFsbGlhbmNlMQwwCgYDVQQLDANDV0cxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJNWTESMBAGA1UEBwwJV2FrZWZpZWxkMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEq1BhK2JNF7vDtRsESTsFSQuMH4udvPN5st7coHxSode2DdMhddwrft28JtsI1V-G9nG2lNwwTaSiioxOA6b1x6NmMGQwEgYDVR0TAQH_BAgwBgEB_wIBADAOBgNVHQ8BAf8EBAMCAoQwHQYDVR0OBBYEFKPSqizvDYzyJALVHLRgvL9qWyQUMB8GA1UdIwQYMBaAFFKaGzLgVqrNUQ_vX4A3BovykSMdMAoGCCqGSM49BAMCA0cAMEQCIGndnqPxgftCSjmtGgrfudLjM9eG_rlFYFX6PcyZeLnSAiA-0w-m9wa1VukUJCqwZvKHE92SOLyW1xhdBV8yF1SlFmhhdXRoRGF0YVikSZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NBAAAARlUOS1SqR0CfmpUat2wTATEAIHEiziyGohCFUc_hJJZGdtSu9ThnEb74K6NZC3U-KbwgpQECAyYgASFYICw4xPmHIvquDRz2KUzyyQlZFhZMbi-mc_YylL1o55jPIlggGQI5ESYAOfR8QM6quTQSoyhjZET806A3yOoCUe2AWJE");
        AttestationObject attestationObject = attestationObjectConverter.convert(attestationObjectBytes);
        byte[] authenticatorDataBytes = attestationObjectConverter.extractAuthenticatorData(attestationObjectBytes);
        return new RegistrationObject(collectedClientData, collectedClientDataBytes, attestationObject, attestationObjectBytes, authenticatorDataBytes, TestUtil.createServerProperty());
    }

    public static RegistrationObject createRegistrationObjectWithAndroidSafetyNetAttestation(){
        byte[] collectedClientDataBytes = Base64UrlUtil.decode("eyJvcmlnaW4iOiJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJjaGFsbGVuZ2UiOiJ2MmgxYzJWeWJtRnRaWFF5TFRKTWNGaEhNV2hXWm14V1RYbGxjSE40YzJsamFHRnNiR1Z1WjJWUXByUl9fSkRUUUotY2JZN3NYb1R4RFA4IiwidHlwZSI6IndlYmF1dGhuLmNyZWF0ZSJ9");
        CollectedClientData collectedClientData = collectedClientDataConverter.convert(collectedClientDataBytes);
        byte[] attestationObjectBytes = Base64UrlUtil.decode("o2NmbXRxYW5kcm9pZC1zYWZldHluZXRnYXR0U3RtdKJjdmVyaDEyNjg1MDIzaHJlc3BvbnNlWRWVZXlKaGJHY2lPaUpTVXpJMU5pSXNJbmcxWXlJNld5Sk5TVWxHVUVSRFEwSkRVMmRCZDBsQ1FXZEpVRUpPV1RWWldFZFJNRVY0VTNWbVMwWlViRXROVFVFd1IwTlRjVWRUU1dJelJGRkZRa04zVlVGTlJ6UjRRM3BCU2tKblRsWkNRVmxVUVd4V1ZFMVRjM2RMVVZsRVZsRlJTMFJEU2tkVFZWSlFTVVZHYzJKSGJHaGliVTVzWTNsQ1IxRlZkRVpKUmxKNVpGaE9NRWxHVG14amJscHdXVEpXZWsxVVNYZE5RVmxFVmxGUlJFUkRiRWRUVlZKUVNVVkdjMkpIYkdoaWJVNXNZM2xDUjFGVmRFWkpSV3gxWkVkV2VXSnRWakJKUlVZeFpFZG9kbU50YkRCbFUwSkhUVlJCWlVaM01IaFBSRUY1VFVSRmQwMUVRWGROUkVKaFJuY3dlVTFFUVhsTlJFbDNUbnBCZDA1VWJHRk5TRVY0UTNwQlNrSm5UbFpDUVZsVVFXeFdWRTFSYzNkRFVWbEVWbEZSU1VSQlNrNVhWRVZUVFVKQlIwRXhWVVZDZDNkS1ZqSkdjbHBYV25CYVYzaHJUVkpaZDBaQldVUldVVkZMUkVFeFIxTlZVbEJKUlVaellrZHNhR0p0VG14TlVYZDNRMmRaUkZaUlVVeEVRVTVFVmpCamVFZDZRVnBDWjA1V1FrRk5UVVZ0UmpCa1IxWjZaRU0xYUdKdFVubGlNbXhyVEcxT2RtSlVRME5CVTBsM1JGRlpTa3R2V2tsb2RtTk9RVkZGUWtKUlFVUm5aMFZRUVVSRFEwRlJiME5uWjBWQ1FVNXVVM2hvT1hrdldqRjNSV0ZVV2xjdmJTOVdORk52VjFwbVpHVTNTMnA2WW10SmRFRnNVbUZpYW1KdFUxRmFSamw2VDNwSlJ6Um1TU3RXZUVveFduQTFVVFpXWVd0clF6UXlWRWd2WW05TVRGbHNRamhxYjFGVFNsZHBTM1ZHTDNkQ2FWWkVSeXRuVEd3ck9WUldhekI1ZFc5V1RHSTVVMkpPYnpVNE4wVjNlbTUwVlVjeGRGTk9VMWhPTlVVck1raEhNeTlaYVhwMFdFNU5OSFJuUkhOc1ZuQkdTSEZXUVZaRmNHNUxWMlZYVTA0ellXa3ZkR2h1Wm1Sa2IzQTFSVk15ZFZsMlRtOU9jMDVaYWpOcVRHTkxORWRKZVhBd1kyRjNWVEpGWlM5eFdIWkdjalZHUVdOT01GZ3pSVXRNYmxreFQwRm5abFppTWxoNlRqUjZVbmwxT1ZoRGJXZHZVR0ZwZEZOMldHOVJSRWR0UVdNeU5WQmxkamhEY2xjMUx6aHNhREJQYlRsNGNtMVFkRmhNVlZwU2QydDFjVkJXVFRSMGNGSnJOVzFTU21oVFJFWmlVV3AzZVhneWR5dEhPVmgyYlZOaVkwTkJkMFZCUVdGUFEwRmtTWGRuWjBoUFRVTkZSMEV4VldSSlFWRmhUVUpuZDBSQldVdExkMWxDUWtGSVYyVlJTVVpCZWtGSlFtZGFibWRSZDBKQlowbDNSWGRaUkZaU01HeENRWGQzUTJkWlNVdDNXVUpDVVZWSVFYZEZkMFJCV1VSV1VqQlVRVkZJTDBKQlNYZEJSRUZtUW1kT1ZraFJORVZIUVZGWGQyaFNVbXhXU2xCRFQxaE9SMlpTTjJkQ1FVVnlUVGRDU0RGTWVEaDZRV1pDWjA1V1NGTk5SVWRCVVZkdlFsRnRVSGsxYVRGQ1NuUXlOU3RMTm5odWJXUlBNa1YwVUdoaGMycENWRUpuVGxaSVVqaEZWRVJDUzAxRmFXZFNjVUpGYUd0S2IyUklVbmRqZW05MlRESmFjRnBIT1doaVIzaHdXVmMxYWxwVE5XcGllVFYxWldrNWVsbFhXbXhrU0d4MVdsaFNkMkV5YTNaWk0wcHpUREpHTUdSSFZucGtRelZvWW0xU2VXSXliR3RNYlU1MllsTTFhbU50ZDNkblpUUkhRME56UjBGUlZVWkNkMFZDUWtsSWFFMUpTR1ZOUjNOSFEwTnpSMEZSVlVaQ2VrRkRhR3c1YjJSSVVuZGplbTkyVERKYWNGcEhPV2hpUjNod1dWYzFhbHBUTldwaWVUVjFaV2s1ZWxsWFdteGtTR3gxV2xoU2QyRXlhM1pTYTJ4RlZIbFZlVTFGV21oaE1sVnNUV3BDVTJJeU9UQktWRWwzVVRKV2VXUkhiRzFoVjA1b1pFZFZiRTFxUWtKa1dGSnZZak5LY0dSSWEyeE5ha0Y1VFVSRk5FeHRUbmxrUkVKMlFtZG5ja0puUlVaQ1VXTjNRVmxhYW1GSVVqQmpTRTAyVEhrNWJXRlhVblpaVjNoellWZEdkVmt5VlhWWk1qaDFZbTV2ZG1NeVJtMWFXRkkxWW0xV01HTkhkSEJNTWs1NVlrTTVSMU5WVWxCS1ZFbDNVbTFHY2xwVFZYbE5Sa3AyWWpOUmJFMXFRa1JhV0Vvd1lWZGFjRmt5UmpCYVUxVjVUVVZHTVdSSGFIWmpiV3d3WlZOVmVVMUVTWGROVkdkMVdUTktjMDFCTUVkRFUzRkhVMGxpTTBSUlJVSkRkMVZCUVRSSlFrRlJRV2hJUzNsNWVuTklNV2Q1YjB0UGRqQXZjMGhwTlZCQ1NHbFpiMkUzYkUxWVQxVlZZbVF3VnpnNU5UTXpjamxoZFdFd1FYRkVSVGg2UkhBMWJuVnlaVE5KZUhoeldXWm5ha2wzY1ZsSmREaGlibHBSU1RGVkx6UnROVWh3TUc5M1oxcFlhM3BUUjJaWVJGaE5ZVXczZEZBcldrTTFhMEV4WjFOSVYySmtjRzFTVVdwUVVERXJLemRHZGpsSGFVZHBkRlUyUmtaUFRUUXdia05YUzNwYVptVldTSGt5VDNVMWRqUjZVVEJKTm1SWVZrUmFSbW95ZGpoVldsUlRVbEpNVGpOSldVRmlVbTlHTlVkbFUwVktTSEZXVlM5blowWkJOVmRpWm5ObGRHVllXSFpXTUd4NGVub3diMFJtWTJsb2VtaFdVMUZOVldSTE1tcFFkbk56TW5aM2NtTjVkVE50YkVwdFlsbFBlVzVzZG5keFUxWk9jM1JoYjBSTmEzUnhSVlpXUlVzd1JHdDBURGRDVG1GeVRqa3daRzh3SzFoa1EwcHFLeTl1Y0cxSlMyaFhja2hHT0U4NVlrSXdaR3RtTVdoc1ZqWXhJaXdpVFVsSlJrUjZRME5CTDJWblFYZEpRa0ZuU1ZCQ1QzQTRjRVZRVG5kamR6aG1aVnBKUlUwclEwMUJNRWREVTNGSFUwbGlNMFJSUlVKRGQxVkJUVVpCZUVONlFVcENaMDVXUWtGWlZFRnNWbFJOVWxsM1JrRlpSRlpSVVV0RVFURkhVMVZTVUVsRlJuTmlSMnhvWW0xT2JFMVRhM2RLZDFsRVZsRlJSRVJEUWtkVFZWSlFTVVZHYzJKSGJHaGliVTVzWTNsQ1IxRlZkRVpKUmtwMllqTlJaMUV3UldkTVUwSlVUVlJCWlVaM01IaE9la0Y1VFVSRmQwMUVRWGROUkVKaFJuY3dlazVVUVhoTmVrVjVUWHBWTlU1VWJHRk5SelI0UTNwQlNrSm5UbFpDUVZsVVFXeFdWRTFUYzNkTFVWbEVWbEZSUzBSRFNrZFRWVkpRU1VWR2MySkhiR2hpYlU1c1kzbENSMUZWZEVaSlJsSjVaRmhPTUVsR1RteGpibHB3V1RKV2VrMVVTWGROUVZsRVZsRlJSRVJEYkVkVFZWSlFTVVZHYzJKSGJHaGliVTVzWTNsQ1IxRlZkRVpKUld4MVpFZFdlV0p0VmpCSlJVWXhaRWRvZG1OdGJEQmxVMEpIVFZSRFEwRlRTWGRFVVZsS1MyOWFTV2gyWTA1QlVVVkNRbEZCUkdkblJWQkJSRU5EUVZGdlEyZG5SVUpCU21aVmRtcGpWMU4wWVM4MlEyUmlSbmRZYkZSeVFXVnBSekJDUkdwdlNIcEVialZaZURkU05rUnFjV1ZNU0hsRGVFUTFWbGw0WldGMkwzSlFjVkJCWldKd1kyNWpSbkF3UTBwYWVXWnZWRE15ZFVsWmF6RlVUblI0Y0haWWJUUnFPVWhwZWtKaVUxVTJXVXhCY3pKRWVqZFhTRU0yUWxOeFJHZ3lTa1J4TkdOdmNqZFVSSEEyUVVNeFV5OUVRV2RPY0RSU05WaHVORXAxUjA4M2RIVm9kM2xDVkZkWGVtTkhVSEl6Vm5aTVNWQm1PWEI1UVdOdGFsSTFhR3AzTkhZeFJUaE5kVmxTVDFOWFoxZEZRVmxaSzNCcWJsSXhXRVYxWlVkaWRVdzRWMW8xVnpKT2RFWlJiRkZaZG1KT1ZFUnpZMWxLVkdOT1EzbDBNMDlYWmpsTmRFNUdlbUpxTUhSVGRHVlJjQ3R3WVN0aWFVa3ZLMkZ1U0ZOQ1NHcHVObGhKYm5kdFQxZHZhRkI0TVdwMVltTllWa0ZIYW5GVFZYUnFXa050VkVkUFIxSmtUbHBWYTFacFozZEpSbU5zWkhwd1ZGSXJVek5WUTBGM1JVRkJZVTlEUVdOWmQyZG5TRU5OUVhOSFFURlZaRVIzVVVWQmQwbENhR3BDVEVKblRsWklVMEZGVWtSQ1EwMUZRVWRDYldWQ1JFRkZRMEZxUVRKTlJGRkhRME56UjBGUlZVWkNkMGxDUm1sb2IyUklVbmRqZW05MlRESmFjRnBIT1doaVIzaHdXVmMxYWxwVE5XcGllVFYxWldrNWVsbFhXbXhrU0d4MVdsaFNkMkV5YTNaTlFqQkhRVEZWWkVwUlVWZE5RbEZIUTBOelIwRlJWVVpDZDAxQ1FtZG5ja0puUlVaQ1VXTkVRV3BCVTBKblRsWklVazFDUVdZNFJVTkVRVWRCVVVndlFXZEZRVTFDT0VkQk1WVmtSR2RSV1VKQ1lrTkdRMWt2VEcxTVZVVnRNMkp1TkhKeVIyVmFNRGRaVXpBclJuRjVUVUk0UjBFeFZXUkpkMUZaUWtKaFowWkxiekl2TUVad01IQXlTMDg1WjJOYVpFNWlNMjEwZDB4YVUxRk5TRkZIUVRGVlpFaDNVblJOUjNOM1lXRkNibTlIVjBkWk1tZ3daRWhDZWs5cE9IWmFiV3hyWWpKR2MySkhiR2hpYlU1c1RHMU9ka3h0TlRaTU0wNW9XbTFXTUdWWE5XeGtTRUp5WVZNNWFtTnRkM1pTYTJ4RlZIbFZlVTFGV21oaE1sVnNUV3BDVTJJeU9UQktWRWwzVVRKV2VXUkhiRzFoVjA1b1pFZFZiRTFxUWtKa1dGSnZZak5LY0dSSWEyeE5ha0Y1VFVSRk5FeHRUbmxpUkVJM1FtZG5ja0puUlVaQ1VXTkNRVkZTZGsxSE1IZGhkMWxKUzNkWlFrSlJWVWhOUVVkSFdESm9NR1JJUW5wUGFUaDJXbTFzYTJJeVJuTmlSMnhvWW0xT2JFeHRUblpNYlRVMlRETk9hRnB0VmpCbFZ6VnNaRWhDY21GVE9VZFRWVkpRU2xSSmQxSnRSbkphVTFWNVRVWktkbUl6VVd4TmFrSkVXbGhLTUdGWFduQlpNa1l3V2xOVmVVMUZSakZrUjJoMlkyMXNNR1ZUVlhsTlJFbDNUVlJuZFZrelNqQk5RVEJIUTFOeFIxTkpZak5FVVVWQ1EzZFZRVUUwU1VKQlVVRjJTRFpGYkVJdmRXUlFaMVp4YVdGaWNtMUNZMmxJUzJGQ09WSTJVR1UwYUVsWVN6ZG1UVXByZEhBd1dFNHdWVmxSU0V0a2VHWTRiVFJqTUVoeFJuSk5jMVJQWjA0cmRXbHFZMDFJUWtKRVZFTnBLMjlhZDNoRFpWWm1WWG80WVRsa1VVSjBiRTlFY2twclkwNVdjVWxZWjB4NFFtSm1hMDR2UWxobGJIaFNaazFxYVM5RVJreFZhRmw0ZEVwSVNGWjJXbkpzU2xkWmQyRkpNMlkzVTNOVGVtaElSWEZITDNKS2NXcDJWbnBRU0cxNGFsTm5VMUpzTVV4c05EQTFabGs0TlVvd05UTm1NemRVYW1ZMFZIcDRNMWx0VXpWQ1FscEZVVmxxZURsR1UwUXdNM0ZFUVd0UU1rVlJRVzlMT1VSNFpHSkdNV2xJYVM4eGExbzRURWhUUVZwcU0xTlllVGRzYTFGT2VWZHhha3RpWlc1SFF5dHlSamR6TWxjeFFWRTBhalEzUWxoM1VqRlNNVzlqUlVSV04xcEtNbloxUzI5NVNDOXJSQzh3VDJscU5sUktOMkp5T0hOWVJHRkNaalJQYUdzclNTSmRmUS5leUp1YjI1alpTSTZJazB6T1Zsb00yTm5VVll5YzJGV1NtWlhUbTVTUTIwM1JDdFhRWGR1V2pkMFEwRm9VWGcxWVdKdGFsRTlJaXdpZEdsdFpYTjBZVzF3VFhNaU9qRTFORGt3T1RBNE5Ua3lOREFzSW1Gd2ExQmhZMnRoWjJWT1lXMWxJam9pWTI5dExtRnVaSEp2YVdRdWEyVjVjM1J2Y21VdVlXNWtjbTlwWkd0bGVYTjBiM0psWkdWdGJ5SXNJbUZ3YTBScFoyVnpkRk5vWVRJMU5pSTZJbVJOTDB4VlNGTkpPVk5yVVdoYVNFaHdVVmRTYm5wS00wMTJka0l5UVU1VFlYVnhXVUZCWWxNeVNtYzlJaXdpWVhCclEyVnlkR2xtYVdOaGRHVkVhV2RsYzNSVGFHRXlOVFlpT2xzaVluTmlOQzlYVVdSaFlVOVhXVU5rTDJvNVQwcHBVWEJuTjJJd2FYZEdaMEZqTDNwNlFURjBRMlozUlQwaVhTd2lZM1J6VUhKdlptbHNaVTFoZEdOb0lqcDBjblZsTENKaVlYTnBZMGx1ZEdWbmNtbDBlU0k2ZEhKMVpYMC4xUE5HWnNUbVNXTlI5Umt3am5tT2hEdS1TZS1NX3BYQjhoRWg1elpGOUtqZXBRMllWbEJybi1nTjZVdGRMd2NkN3JjbXlORnkxUWw5OW1vQ2EySUZEcTdxcEdlcDZIRnZyeThXU2J2Qk1NUllHYVVsYUVQekl0QnZKaXJFNHhic182cWJhTDlzaWs3Y1dtRTM2RUF2V1JIZEg0Q1Exbk9zcThhZG8yTW4ta0VIZXpXLUJscFNVZ2hfWW5tX3BnR2pad3RGd0VER3BNVEhaYWJoVWFkTnAxTGprMkFOSEUwSXlvQkJSZGU2QWxpOWo1UUtldm9CZU5oRzZpQUpSQjVHbmx4ek5IaG1lRk5lZk9JM2RmRXgzOGZoUE5NUnlNd0JzRUZxSHJxQWZDQVJWek96VXJfV3hpOGN3ZEE0MVItZFlMMjdNSTdtcHRab1U5VzVhS3NQX0FoYXV0aERhdGFZAWdJlg3liA6MaHQ0Fw9kdmBbj-SuuaKGMseZXPO6gx2XY0EAAACUyHzdl1bBSbieJMs2NlTzUAAgoTaqg-uoaPbjf_WOSn57I5m3u5kQqb4FtWyANNpm1MakAQMDOQEAIFkBANIgdpYtfXjH-yGYu7kbu6giXuoCvGY0F3HuTC_yYVaSewHECwWCklEpl3kfHW2umFfvGSaifZWkYWIigZwDlV1aKGbuiXswDpqHX59QVHqjNGrSGtid_Nbu48xb8Cc4sUBkfFRTExKsCUEN-Xfbezx8tELtEd9AVKQVxaCLf30DN61C_hBNew_3oEt0hHeCPlYfzkkVYDRIjMf8Ud7phpBJM_vfEoz30xsXuoZFWhwhskicjtcHRI5Hyvz7z4korABDz6sYlELblzH2v7F-74Rt0u1Hw76cQ4X0BQxgtelwS5pbV_I8wdYReti5HLwgT0eH9zBbtWNC51sn00x8KDEhQwEAAQ");
        AttestationObject attestationObject = attestationObjectConverter.convert(attestationObjectBytes);
        byte[] authenticatorDataBytes = attestationObjectConverter.extractAuthenticatorData(attestationObjectBytes);
        LocalDateTime timestamp = LocalDateTime.parse("2019-02-02T07:01:00");
        return new RegistrationObject(collectedClientData, collectedClientDataBytes, attestationObject, attestationObjectBytes, authenticatorDataBytes, TestUtil.createServerProperty(), timestamp);
    }

    public static RegistrationObject createRegistrationObjectWithTPMAttestation(){
        byte[] collectedClientDataBytes = Base64UrlUtil.decode("ew0KCSJ0eXBlIiA6ICJ3ZWJhdXRobi5jcmVhdGUiLA0KCSJjaGFsbGVuZ2UiIDogIndrNkxxRVhBTUFacHFjVFlsWTJ5b3I1RGppeUlfYjFneTluRE90Q0IxeUdZbm1fNFdHNFVrMjRGQXI3QXhUT0ZmUU1laWdrUnhPVExaTnJMeEN2Vl9RIiwNCgkib3JpZ2luIiA6ICJodHRwczovL3dlYmF1dGhuLm9yZyIsDQoJInRva2VuQmluZGluZyIgOiANCgl7DQoJCSJzdGF0dXMiIDogInN1cHBvcnRlZCINCgl9DQp9");
        CollectedClientData collectedClientData = collectedClientDataConverter.convert(collectedClientDataBytes);
        byte[] attestationObjectBytes = Base64UrlUtil.decode("o2NmbXRjdHBtaGF1dGhEYXRhWQFnlWkIjx7O4yMpVANdvRDXyuORMFonUbVZu4_Xy7IpvdRFAAAAAAiYcFjK3EuBtuEw3lDcvpYAIIVs3RYj2zjEOSjQbDIbPmXofBdIkx6x-t2CpK8SRYI0pAEDAzkBACBZAQDF2m9Nk1e94gL1xVjNCjFW0lTy4K2atXkx-YJrdH3hrE8p1gcIdNzleRDhmERJnY5CRwM5sXDQIrUBq4jpwvTtMC5HGccN6-iEJAPtm9_CJzCmGhtw9hbF8bcAys94RhN9xLLUaajhWqtPrYZXCEAi0o9E2QdTIxJrcAfJgZOf33JMr0--R1BAQxpOoGRDC8ss-tfQW9ufZLWw4JUuz4Z5Jz1sbfqBYB8UUDMWoT0HgsMaPmvd7T17xGvB-pvvDf-Dt96vFGtYLEZEgho8Yu26pr5CK_BOQ-2vX9N4MIYVPXNhogMGGmKYqybhM3yhye0GdBpZBUd5iOcgME6uGJ1_IUMBAAFnYXR0U3RtdKZjdmVyYzIuMGNhbGc5__5jc2lnWQEAcV1izWGUWIs0DEOZNQGdriNNXo6nbrGDLzEAeswCK9njYGCLmOkHVgSyafhsjCEMZkQmuPUmEOMDKosqxup_tiXQwG4yCW9TyWoINWGayQ4vcr6Ys-l6KMPkg__d2VywhfonnTJDBfE_4BIRD60GR0qBzTarthDHQFMqRtoUtuOsTF5jedU3EQPojRA5iCNC2naCCZuMSURdlPmhlW5rAaRZVF41ZZECi5iFOM2rO0UpGuQSLUvr1MqQOsDytMf7qWZMvwT_5_8BF6GNdB2l2VzmIJBbV6g8z7dj0fRkjlCXBp8UG2LvTq5SsfugrRWXOJ8BkdMplPfl0mz6ssU_n2N4NWOCWQS2MIIEsjCCA5qgAwIBAgIQEyidpWZzRxOSMNfrAvV1fzANBgkqhkiG9w0BAQsFADBBMT8wPQYDVQQDEzZOQ1UtTlRDLUtFWUlELTE1OTFENEI2RUFGOThEMDEwNDg2NEI2OTAzQTQ4REQwMDI2MDc3RDMwHhcNMTgwNTIwMTYyMDQ0WhcNMjgwNTIwMTYyMDQ0WjAAMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvQ6XK2ujM11E7x4SL34p252ncyQTd3-4r5ALQhBbFKS95gUsuENTG-48GBQwu48i06cckm3eH20TUeJvn4-pj6i8LFOrIK14T3P3GFzbxgQLq1KVm63JWDdEXk789JgzQjHNO7DZFKWTEiktwmBUPUA88TjQcXOtrR5EXTrt1FzGzabOepFann3Ny_XtxI8lDZ3QLwPLJfmk7puGtkGNaXOsRC7GLAnoEB7UWvjiyKG6HAtvVTgxcW5OQnHFb9AHycU5QdukXrP0njdCpLCRR0Nq6VMKmVU3MaGh-DCwYEB32sPNPdDkPDWyk16ItwcmXqfSBV5ZOr8ifvcXbCWUWwIDAQABo4IB5TCCAeEwDgYDVR0PAQH_BAQDAgeAMAwGA1UdEwEB_wQCMAAwbQYDVR0gAQH_BGMwYTBfBgkrBgEEAYI3FR8wUjBQBggrBgEFBQcCAjBEHkIAVABDAFAAQQAgACAAVAByAHUAcwB0AGUAZAAgACAAUABsAGEAdABmAG8AcgBtACAAIABJAGQAZQBuAHQAaQB0AHkwEAYDVR0lBAkwBwYFZ4EFCAMwSgYDVR0RAQH_BEAwPqQ8MDoxODAOBgVngQUCAwwFaWQ6MTMwEAYFZ4EFAgIMB05QQ1Q2eHgwFAYFZ4EFAgEMC2lkOjRFNTQ0MzAwMB8GA1UdIwQYMBaAFMISqVvO-lb4wMFvsVvdAzRHs3qjMB0GA1UdDgQWBBSv4kXTSA8i3NUM0q57lrWpM8p_4TCBswYIKwYBBQUHAQEEgaYwgaMwgaAGCCsGAQUFBzAChoGTaHR0cHM6Ly9hemNzcHJvZG5jdWFpa3B1Ymxpc2guYmxvYi5jb3JlLndpbmRvd3MubmV0L25jdS1udGMta2V5aWQtMTU5MWQ0YjZlYWY5OGQwMTA0ODY0YjY5MDNhNDhkZDAwMjYwNzdkMy8zYjkxOGFlNC0wN2UxLTQwNTktOTQ5MS0wYWQyNDgxOTA4MTguY2VyMA0GCSqGSIb3DQEBCwUAA4IBAQAs-vqdkDX09fNNYqzbv3Lh0vl6RgGpPGl-MYgO8Lg1I9UKvEUaaUHm845ABS8m7r9p22RCWO6TSEPS0YUYzAsNuiKiGVna4nB9JWZaV9GDS6aMD0nJ8kNciorDsV60j0Yb592kv1VkOKlbTF7-Z10jaapx0CqhxEIUzEBb8y9Pa8oOaQf8ORhDHZp-mbn_W8rUzXSDS0rFbWKaW4tGpVoKGRH-f9vIeXxGlxVS0wqqRm_r-h1aZInta0OOiL_S4367gZyeLL3eUnzdd-eYySYn2XINPbVacK8ZifdsLMwiNtz5uM1jbqpEn2UoB3Hcdn0hc12jTLPWFfg7GiKQ0hk9WQXsMIIF6DCCA9CgAwIBAgITMwAAAQDiBsSROVGXhwAAAAABADANBgkqhkiG9w0BAQsFADCBjDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjE2MDQGA1UEAxMtTWljcm9zb2Z0IFRQTSBSb290IENlcnRpZmljYXRlIEF1dGhvcml0eSAyMDE0MB4XDTE3MDIwMTE3NDAyNFoXDTI5MTIzMTE3NDAyNFowQTE_MD0GA1UEAxM2TkNVLU5UQy1LRVlJRC0xNTkxRDRCNkVBRjk4RDAxMDQ4NjRCNjkwM0E0OEREMDAyNjA3N0QzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9IwUMSiQUbrQR0NLkKR-9RB8zfHYdlmDB0XN_m8qrNHKRJ__lBOR-mwU_h3MFRZF6X3ZZwka1DtwBdzLFV8lVu33bc15stjSd6B22HRRKQ3sIns5AYQxg0eX2PtWCJuIhxdM_jDjP2hq9Yvx-ibt1IO9UZwj83NGxXc7Gk2UvCs9lcFSp6U8zzl5fGFCKYcxIKH0qbPrzjlyVyZTKwGGSTeoMMEdsZiq-m_xIcrehYuHg-FAVaPLLTblS1h5cu80-ruFUm5Xzl61YjVU9tAV_Y4joAsJ5QP3VPocFhr5YVsBVYBiBcQtr5JFdJXZWWEgYcFLdAFUk8nJERS7-5xLuQIDAQABo4IBizCCAYcwCwYDVR0PBAQDAgGGMBsGA1UdJQQUMBIGCSsGAQQBgjcVJAYFZ4EFCAMwFgYDVR0gBA8wDTALBgkrBgEEAYI3FR8wEgYDVR0TAQH_BAgwBgEB_wIBADAdBgNVHQ4EFgQUwhKpW876VvjAwW-xW90DNEezeqMwHwYDVR0jBBgwFoAUeowKzi9IYhfilNGuVcFS7HF0pFYwcAYDVR0fBGkwZzBloGOgYYZfaHR0cDovL3d3dy5taWNyb3NvZnQuY29tL3BraW9wcy9jcmwvTWljcm9zb2Z0JTIwVFBNJTIwUm9vdCUyMENlcnRpZmljYXRlJTIwQXV0aG9yaXR5JTIwMjAxNC5jcmwwfQYIKwYBBQUHAQEEcTBvMG0GCCsGAQUFBzAChmFodHRwOi8vd3d3Lm1pY3Jvc29mdC5jb20vcGtpb3BzL2NlcnRzL01pY3Jvc29mdCUyMFRQTSUyMFJvb3QlMjBDZXJ0aWZpY2F0ZSUyMEF1dGhvcml0eSUyMDIwMTQuY3J0MA0GCSqGSIb3DQEBCwUAA4ICAQAKc9z1UUBAaybIVnK8yL1N1iGJFFFFw_PpkxW76hgQhUcCxNFQskfahfFzkBD05odVC1DKyk2PyOle0G86FCmZiJa14MtKNsiu66nVqk2hr8iIcu-cYEsgb446yIGd1NblQKA1C_28F2KHm8YRgcFtRSkWEMuDiVMa0HDU8aI6ZHO04Naj86nXeULJSZsA0pQwNJ04-QJP3MFQzxQ7md6D-pCx-LVA-WUdGxT1ofaO5NFxq0XjubnZwRjQazy_m93dKWp19tbBzTUKImgUKLYGcdmVWXAxUrkxHN2FbZGOYWfmE2TGQXS2Z-g4YAQo1PleyOav3HNB8ti7u5HpI3t9a73xuECy2gFcZQ24DJuBaQe4mU5I_hPiAa-822nPPL6w8m1eegxhHf7ziRW_hW8s1cvAZZ5Jpev96zL_zRv34MsRWhKwLbu2oOCSEYYh8D8DbQZjmsxlUYR_q1cP8JKiIo6NNJ85g7sjTZgXxeanA9wZwqwJB-P98VdVslC17PmVu0RHOqRtxrht7OFT7Z10ecz0tj9ODXrv5nmBktmbgHRirRMl84wp7-PJhTXdHbxZv-OoL4HP6FxyDbHxLB7QmR4-VoEZN0vsybb1A8KEj2pkNY_tmxHH6k87euM99bB8FHrW9FNrXCGL1p6-PYtiky52a5YQZGT8Hz-ZnxobTmhjZXJ0SW5mb1ih_1RDR4AXACIAC7xZ9N_ZpqQtw7hmr_LfDRmCa78BS2erCtbrsXYwa4AHABSsnz8FacZi-wkUkfHu4xjG8MPfmwAAAAGxWkjHaED549jznwUBqeDEpT-7xBMAIgALcSGuv6a5r9BwMvQvCSXg7GdAjdWZpXv6D4DH8VYBCE8AIgALAVI0eQ_AAZjNvrhUEMK2q4wxuwIFOnHIDF0Qljhf47RncHViQXJlYVkBNgABAAsABgRyACCd_8vzbDg65pn7mGjcbcuJ1xU4hL4oA5IsEkFYv60irgAQABAIAAAAAAABAMXab02TV73iAvXFWM0KMVbSVPLgrZq1eTH5gmt0feGsTynWBwh03OV5EOGYREmdjkJHAzmxcNAitQGriOnC9O0wLkcZxw3r6IQkA-2b38InMKYaG3D2FsXxtwDKz3hGE33EstRpqOFaq0-thlcIQCLSj0TZB1MjEmtwB8mBk5_fckyvT75HUEBDGk6gZEMLyyz619Bb259ktbDglS7PhnknPWxt-oFgHxRQMxahPQeCwxo-a93tPXvEa8H6m-8N_4O33q8Ua1gsRkSCGjxi7bqmvkIr8E5D7a9f03gwhhU9c2GiAwYaYpirJuEzfKHJ7QZ0GlkFR3mI5yAwTq4YnX8");
        AttestationObject attestationObject = attestationObjectConverter.convert(attestationObjectBytes);
        byte[] authenticatorDataBytes = attestationObjectConverter.extractAuthenticatorData(attestationObjectBytes);
        return new RegistrationObject(collectedClientData, collectedClientDataBytes, attestationObject, attestationObjectBytes, authenticatorDataBytes, TestUtil.createServerProperty());
    }

    public static AttestationObject createAttestationObjectWithFIDOU2FAttestationStatement() {
        return new AttestationObject(createAuthenticatorData(), createFIDOU2FAttestationStatement());
    }

    public static AttestationObject createAttestationObjectWithBasicPackedECAttestationStatement(byte[] clientDataHash) {
        PrivateKey privateKey = TestUtil.load3tierTestAuthenticatorAttestationPrivateKey();
        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData = createAuthenticatorData();
        byte[] authenticatorDataBytes = new AuthenticatorDataConverter(cborConverter).convert(authenticatorData);
        byte[] signedData = getSignedData(authenticatorDataBytes, clientDataHash);
        byte[] signature = calculateSignature(privateKey, signedData);
        return new AttestationObject(authenticatorData, createBasicPackedAttestationStatement(COSEAlgorithmIdentifier.ES256, signature));
    }

    public static AttestationObject createAttestationObjectWithSelfPackedECAttestationStatement(byte[] clientDataHash) {
        KeyPair keyPair = KeyUtil.createECKeyPair();
        EC2CredentialPublicKey ec2CredentialPublicKey = EC2CredentialPublicKey.create((ECPublicKey) keyPair.getPublic());
        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData = createAuthenticatorData(ec2CredentialPublicKey);
        byte[] authenticatorDataBytes = new AuthenticatorDataConverter(cborConverter).convert(authenticatorData);
        byte[] signedData = getSignedData(authenticatorDataBytes, clientDataHash);
        byte[] signature = calculateSignature(keyPair.getPrivate(), signedData);
        return new AttestationObject(authenticatorData, createSelfPackedAttestationStatement(COSEAlgorithmIdentifier.ES256, signature));
    }

    public static AttestationObject createAttestationObjectWithSelfPackedRSAAttestationStatement(byte[] clientDataHash) {
        KeyPair keyPair = KeyUtil.createRSAKeyPair();
        RSACredentialPublicKey rsaCredentialPublicKey = RSACredentialPublicKey.create((RSAPublicKey) keyPair.getPublic());
        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData = createAuthenticatorData(rsaCredentialPublicKey);
        byte[] authenticatorDataBytes = new AuthenticatorDataConverter(cborConverter).convert(authenticatorData);
        byte[] signedData = getSignedData(authenticatorDataBytes, clientDataHash);
        byte[] signature = calculateSignature(keyPair.getPrivate(), signedData);
        return new AttestationObject(authenticatorData, createSelfPackedAttestationStatement(COSEAlgorithmIdentifier.RS256, signature));
    }

    public static AttestationObject createAttestationObjectWithAndroidKeyAttestationStatement(byte[] clientDataHash) {
        PrivateKey privateKey = TestUtil.loadAndroidKeyAttestationPrivateKey();
        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData = createAuthenticatorData();
        byte[] authenticatorDataBytes = new AuthenticatorDataConverter(cborConverter).convert(authenticatorData);
        byte[] signedData = getSignedData(authenticatorDataBytes, clientDataHash);
        byte[] signature = calculateSignature(privateKey, signedData);
        return new AttestationObject(authenticatorData, createAndroidKeyAttestationStatement(COSEAlgorithmIdentifier.ES256, signature));
    }


    public static AttestationObject createAttestationObjectWithTPMAttestationStatement(byte[] clientDataHash) {
        PrivateKey privateKey = TestUtil.loadTPMAttestationPrivateKey();
        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData = createAuthenticatorData();
        byte[] authenticatorDataBytes = new AuthenticatorDataConverter(cborConverter).convert(authenticatorData);
        byte[] signedData = getSignedData(authenticatorDataBytes, clientDataHash);
        byte[] signature = calculateSignature(privateKey, signedData);
        return new AttestationObject(authenticatorData, createTPMAttestationStatement(COSEAlgorithmIdentifier.RS1, signature));
    }

    private static PrivateKey loadAndroidKeyAttestationPrivateKey() {
        throw new NotImplementedException();
    }


    private static PrivateKey loadTPMAttestationPrivateKey() {
        throw new NotImplementedException();
    }


    private static byte[] getSignedData(byte[] authenticatorData, byte[] clientDataHash) {
        return ByteBuffer.allocate(authenticatorData.length + clientDataHash.length).put(authenticatorData).put(clientDataHash).array();
    }

    public static <T extends ExtensionAuthenticatorOutput> AuthenticatorData<T> createAuthenticatorData() {
        byte flags = BIT_UP | BIT_AT;
        return new AuthenticatorData<>(new byte[32], flags, 1, createAttestedCredentialData());
    }

    public static <T extends RegistrationExtensionAuthenticatorOutput> AuthenticatorData<T> createAuthenticatorData(CredentialPublicKey credentialPublicKey) {
        byte flags = BIT_UP | BIT_AT;
        return new AuthenticatorData<>(new byte[32], flags, 1, createAttestedCredentialData(credentialPublicKey));
    }

    public static AttestedCredentialData createAttestedCredentialData() {
        return createAttestedCredentialData(createECCredentialPublicKey());
    }

    public static AttestedCredentialData createAttestedCredentialData(CredentialPublicKey credentialPublicKey) {
        return new AttestedCredentialData(AAGUID.ZERO, new byte[32], credentialPublicKey);
    }

    public static EC2CredentialPublicKey createECCredentialPublicKey() {
        return new EC2CredentialPublicKey(
                null,
                COSEAlgorithmIdentifier.ES256,
                null,
                null,
                Curve.SECP256R1,
                new byte[32],
                new byte[32]
        );
    }

    public static RSACredentialPublicKey createRSCredentialPublicKey() {
        RSACredentialPublicKey credentialPublicKey;
        credentialPublicKey = new RSACredentialPublicKey(
                null,
                COSEAlgorithmIdentifier.RS256,
                null,
                null,
                new byte[32],
                new byte[32]
        );
        return credentialPublicKey;
    }

    public static FIDOU2FAttestationStatement createFIDOU2FAttestationStatement() {
        return createFIDOU2FAttestationStatement(create2tierTestAuthenticatorCertPath());
    }

    public static FIDOU2FAttestationStatement createFIDOU2FAttestationStatement(AttestationCertificatePath certPath) {

        byte[] sig = new byte[32];

        return new FIDOU2FAttestationStatement(certPath, sig);
    }

    public static PackedAttestationStatement createBasicPackedAttestationStatement() {
        byte[] signature = new byte[32]; // dummy
        return createBasicPackedAttestationStatement(COSEAlgorithmIdentifier.ES256, signature);
    }

    public static PackedAttestationStatement createBasicPackedAttestationStatement(COSEAlgorithmIdentifier algorithm, byte[] signature) {
        AttestationCertificatePath certPath = load3tierTestCertPath();
        return new PackedAttestationStatement(algorithm, signature, certPath, null);
    }

    public static PackedAttestationStatement createSelfPackedAttestationStatement(COSEAlgorithmIdentifier algorithm, byte[] signature) {
        return new PackedAttestationStatement(algorithm, signature, null, null);
    }

    public static AndroidKeyAttestationStatement createAndroidKeyAttestationStatement(COSEAlgorithmIdentifier algorithm, byte[] signature) {
        AttestationCertificatePath certPath = loadAndroidKeyCertPath();
        return new AndroidKeyAttestationStatement(algorithm, signature, certPath);
    }

    public static AttestationStatement createTPMAttestationStatement(COSEAlgorithmIdentifier algorithm, byte[] signature) {
        AttestationCertificatePath certPath = loadAndroidKeyCertPath();
        TPMSAttest certInfo = null; //TODO
        TPMTPublic pubArea = null; //TODO
        return new TPMAttestationStatement(algorithm, certPath, signature, certInfo, pubArea);
    }

    public static AttestationCertificatePath create2tierTestAuthenticatorCertPath() {
        return new AttestationCertificatePath(Collections.singletonList(TestUtil.load2tierTestAuthenticatorAttestationCertificate()));
    }

    public static AttestationCertificatePath load3tierTestCertPath() {
        return new AttestationCertificatePath(Arrays.asList(load3tierTestAuthenticatorAttestationCertificate(), load3tierTestIntermediateCACertificate()));
    }

    public static X509Certificate load3tierTestRootCACertificate() {
        return loadCertificateFromClassPath("/attestation/3tier/certs/3tier-test-root-CA.crt");
    }

    public static X509Certificate load3tierTestIntermediateCACertificate() {
        return loadCertificateFromClassPath("/attestation/3tier/certs/3tier-test-intermediate-CA.crt");
    }

    public static X509Certificate load3tierTestAuthenticatorAttestationCertificate() {
        return loadCertificateFromClassPath("/attestation/3tier/certs/3tier-test-authenticator.crt");
    }

    public static X509Certificate load2tierTestRootCACertificate() {
        return loadCertificateFromClassPath("/attestation/2tier/certs/2tier-test-root-CA.crt");
    }

    public static X509Certificate load2tierTestAuthenticatorAttestationCertificate() {
        return loadCertificateFromClassPath("/attestation/2tier/certs/2tier-test-authenticator.crt");
    }

    private static AttestationCertificatePath loadAndroidKeyCertPath() {
        throw new NotImplementedException();
    }

    public static X509Certificate loadCertificateFromClassPath(String classPath) {
        ClassPathResource resource = new ClassPathResource(classPath);
        try {
            return CertificateUtil.generateX509Certificate(resource.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static X509Certificate loadFirefoxSWTokenAttestationCertificate() {
        String base64UrlCertificate = "MIIBMTCB2KADAgECAgRdWm5nMAoGCCqGSM49BAMCMCExHzAdBgNVBAMTFkZpcmVmb3ggVTJGIFNvZnQgVG9rZW4wHhcNMTcwODE5MTExMDI3WhcNMTcwODIxMTExMDI3WjAhMR8wHQYDVQQDExZGaXJlZm94IFUyRiBTb2Z0IFRva2VuMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEmNdtc7TW47xJcunwo_5ZuqSeHKJDZixC3AhTy2OEnYZfLmLZn9ssfWqLmPY4E642bKRDsm6qLNfjj_z9ufejNTAKBggqhkjOPQQDAgNIADBFAiEA6SdHwFyOq1trbQA6TLxLHS11EYUYDWyA24BnsJD8TrACIEw7k4aGBMOYlT5uMXLlj4bV5jo1Svi83VOpBo5ykMvd";
        return CertificateUtil.generateX509Certificate(Base64UrlUtil.decode(base64UrlCertificate));
    }

    public static X509Certificate loadFeitianU2FTokenAttestationCertificate() {
        String base64UrlCertificate = "MIIBTDCB86ADAgECAgrMFgqn4TlPa3dQMAoGCCqGSM49BAMCMBcxFTATBgNVBAMTDEZUIEZJRE8gMDEwMDAeFw0xNjA0MTUxNDUwMzJaFw0yNjA0MTUxNDUwMzJaMCcxJTAjBgNVBAMTHEZUIEZJRE8gVTJGIDExNjE2MTczMDMwNTAyMTAwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATG1tXh9HyGi4UJapsP3Fw8NEwRr5WEYfV5xOvP2XU4jpnJ17SEbyZTCn7rX38Ept32BPr6IaOmamqAdQvsDpNgoxcwFTATBgsrBgEEAYLlHAIBAQQEAwIEMDAKBggqhkjOPQQDAgNIADBFAiEA3wPvLOvjpbU3VCsKBjWtb5MzcX_I2p7NN_X03kyyFoUCIAxoJPinKGUxoNR_bhx3uZHtQQpwLWuaBND9y2Omhf47";
        return CertificateUtil.generateX509Certificate(Base64UrlUtil.decode(base64UrlCertificate));
    }

    public static X509Certificate loadYubikeyAttestationCertificate() {
        String base64UrlCertificate = "MIICRDCCAS6gAwIBAgIEeMDfDjALBgkqhkiG9w0BAQswLjEsMCoGA1UEAxMjWXViaWNvIFUyRiBSb290IENBIFNlcmlhbCA0NTcyMDA2MzEwIBcNMTQwODAxMDAwMDAwWhgPMjA1MDA5MDQwMDAwMDBaMCoxKDAmBgNVBAMMH1l1YmljbyBVMkYgRUUgU2VyaWFsIDIwMjU5MDU5MzQwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAS1uHFcg_3-DqFcRXeshY30jBdv3oedyvS4PUDTIPJvreYl_Pf1yK_YNRj4254h7Ag7GEWAxxfsSkcLlopvuj9vozswOTAiBgkrBgEEAYLECgIEFTEuMy42LjEuNC4xLjQxNDgyLjEuMTATBgsrBgEEAYLlHAIBAQQEAwIFIDALBgkqhkiG9w0BAQsDggEBAD72q_ZKkWsL-ZSTjdyVNOBUQAJoVninLEOnq-ZdkGX_YfRRzoo67thmidGQuVCvAHpU0THu8G_ia06nuz4yt5IFpd-nYAQ0U-NK-ETDfNSoX4xcLYcOCiiyt-1EAkH9s3krIHaw4Yr6m0Mu7vwmWLoJBcQbJKk8bsi7ptVvM-jWU9fPa9UBVFWiZZdA99zFHMAxYJzQPqbN6Tmeygh2MpB2P7TI0A9WkGmhJUkAauuwaiGiFOSZmDe0KegdflbTOlSS3ToWHIKTlUCBqn7vdJw6Vj2919ujlcxHPkRpbUGRhcJDesg6wGTBy-RyJ_96G3fH1eoMNn1F9jC9mY1Zsm4=";
        return CertificateUtil.generateX509Certificate(Base64UrlUtil.decode(base64UrlCertificate));
    }

    public static X509Certificate loadAndroidKeyAttestationCertificate() {
        String certificate =
                "-----BEGIN CERTIFICATE-----\n"
                        + "MIIByTCCAXCgAwIBAgIBATAKBggqhkjOPQQDAjAcMRowGAYDVQQDDBFBbmRyb2lkIE"
                        + "tleW1hc3Rl cjAgFw03MDAxMDEwMDAwMDBaGA8yMTA2MDIwNzA2MjgxNVowGjEYMBY"
                        + "GA1UEAwwPQSBLZXltYXN0 ZXIgS2V5MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE"
                        + "FpsFUWID9p2QPAvtfal4MRf9vJg0tNc3 vKJwoDhhSCMm7If0FljgvmroBYQyCIbnn"
                        + "Bxh2OU9SKxI/manPwIIUqOBojCBnzALBgNVHQ8EBAMC B4AwbwYKKwYBBAHWeQIBEQ"
                        + "RhMF8CAQEKAQACAQEKAQEEBWhlbGxvBAAwDL+FPQgCBgFWDy29GDA6 oQUxAwIBAqI"
                        + "DAgEDowQCAgEApQUxAwIBBKoDAgEBv4N4AwIBA7+DeQQCAgEsv4U+AwIBAL+FPwIF "
                        + "ADAfBgNVHSMEGDAWgBQ//KzWGrE6noEguNUlHMVlux6RqTAKBggqhkjOPQQDAgNHAD"
                        + "BEAiBKzJSk 9VNauKu4dr+ZJ5jMTNlAxSI99XkKEkXSolsGSAIgCnd5T99gv3B/IqM"
                        + "CHn0yZ7Wuu/jisU0epRRo xh8otA8=\n"
                        + "-----END CERTIFICATE-----";
        return createCertificate(certificate);
    }

    public static X509Certificate loadAndroidKeyIntermidiateCertificate() {
        String certificate =
                "-----BEGIN CERTIFICATE-----\n" +
                        "MIICeDCCAh6gAwIBAgICEAEwCgYIKoZIzj0EAwIwgZgxCzAJBgNVBAYTAlVTMRMwEQ"
                        + "YDVQQIDApD YWxpZm9ybmlhMRYwFAYDVQQHDA1Nb3VudGFpbiBWaWV3MRUwEwYDVQQ"
                        + "KDAxHb29nbGUsIEluYy4x EDAOBgNVBAsMB0FuZHJvaWQxMzAxBgNVBAMMKkFuZHJv"
                        + "aWQgS2V5c3RvcmUgU29mdHdhcmUgQXR0 ZXN0YXRpb24gUm9vdDAeFw0xNjAxMTEwM"
                        + "DQ2MDlaFw0yNjAxMDgwMDQ2MDlaMIGIMQswCQYDVQQG EwJVUzETMBEGA1UECAwKQ2"
                        + "FsaWZvcm5pYTEVMBMGA1UECgwMR29vZ2xlLCBJbmMuMRAwDgYDVQQL DAdBbmRyb2l"
                        + "kMTswOQYDVQQDDDJBbmRyb2lkIEtleXN0b3JlIFNvZnR3YXJlIEF0dGVzdGF0aW9u "
                        + "IEludGVybWVkaWF0ZTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABOueefhCY1msyy"
                        + "qRTImGzHCt kGaTgqlzJhP+rMv4ISdMIXSXSir+pblNf2bU4GUQZjW8U7ego6ZxWD7"
                        + "bPhGuEBSjZjBkMB0GA1Ud DgQWBBQ//KzWGrE6noEguNUlHMVlux6RqTAfBgNVHSME"
                        + "GDAWgBTIrel3TEXDo88NFhDkeUM6IVow zzASBgNVHRMBAf8ECDAGAQH/AgEAMA4GA"
                        + "1UdDwEB/wQEAwIChDAKBggqhkjOPQQDAgNIADBFAiBL ipt77oK8wDOHri/AiZi03c"
                        + "ONqycqRZ9pDMfDktQPjgIhAO7aAV229DLp1IQ7YkyUBO86fMy9Xvsi u+f+uXc/WT/"
                        + "7\n" +
                        "-----END CERTIFICATE-----";
        return createCertificate(certificate);
    }


    /**
     * Creates {@link X509Certificate} from PEM style certificate string.
     *
     * @param derEncodedCertificate DER-encoded certificate. Please note it is encoded in base64 string, not base64url.
     * @return created X509Certificate
     */
    public static X509Certificate createCertificate(String derEncodedCertificate) {
        return CertificateUtil.generateX509Certificate(derEncodedCertificate.getBytes());
    }

    public static CollectedClientData createClientData(ClientDataType type) {
        return createClientData(type, TestUtil.createChallenge());
    }

    public static CollectedClientData createClientData(ClientDataType type, Challenge challenge) {
        return new CollectedClientData(type, challenge, createOrigin(), null);
    }

    public static byte[] createClientDataJSON(ClientDataType type) {
        return new CollectedClientDataConverter(jsonConverter).convertToBytes(createClientData(type));
    }

    public static byte[] createClientDataJSON(ClientDataType type, Challenge challenge) {
        return new CollectedClientDataConverter(jsonConverter).convertToBytes(createClientData(type, challenge));
    }

    public static Challenge createChallenge() {
        UUID uuid = UUID.randomUUID();
        long hi = uuid.getMostSignificantBits();
        long lo = uuid.getLeastSignificantBits();
        byte[] challengeValue = ByteBuffer.allocate(16).putLong(hi).putLong(lo).array();
        return new DefaultChallenge(challengeValue);
    }

    public static Origin createOrigin() {
        return new Origin("https://localhost:8080");
    }

    public static ServerProperty createRelyingParty() {
        return new ServerProperty(createOrigin(), "localhost", createChallenge(), null);
    }

    public static PrivateKey load3tierTestAuthenticatorAttestationPrivateKey() {
        return loadPrivateKey("classpath:attestation/3tier/private/3tier-test-authenticator.der");
    }

    public static PrivateKey load2tierTestAuthenticatorAttestationPrivateKey() {
        return loadPrivateKey("classpath:attestation/2tier/private/2tier-test-authenticator.der");
    }

    public static PrivateKey loadPrivateKeyFromResource(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();
            byte[] data = StreamUtils.copyToByteArray(inputStream);
            return KeyUtil.loadECPrivateKey(data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static PrivateKey loadPrivateKey(String resourcePath) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(resourcePath);
        return loadPrivateKeyFromResource(resource);
    }

    public static Authenticator createAuthenticator(AttestationObject attestationObject) {
        AttestedCredentialData attestedCredentialData = attestationObject.getAuthenticatorData().getAttestedCredentialData();
        return new AuthenticatorImpl(attestedCredentialData, attestationObject.getAttestationStatement(), attestationObject.getAuthenticatorData().getSignCount());
    }

    public static TrustAnchorsResolver createTrustAnchorProviderWith2tierTestRootCACertificate() {
        return (aaguid) -> {
            Set<TrustAnchor> set = new HashSet<>();
            set.add(new TrustAnchor(load2tierTestRootCACertificate(), null));
            return set;
        };
    }

    public static TrustAnchorsResolver createTrustAnchorProviderWith3tierTestRootCACertificate() {
        return (aaguid) -> {
            Set<TrustAnchor> set = new HashSet<>();
            set.add(new TrustAnchor(load3tierTestRootCACertificate(), null));
            return set;
        };
    }

    public static ServerProperty createServerProperty() {
        return createServerProperty(TestUtil.createChallenge());
    }

    public static ServerProperty createServerProperty(Challenge challenge) {
        return new ServerProperty(TestUtil.createOrigin(), "example.com", challenge, new byte[32]);
    }

    public static Authenticator createAuthenticator(AttestedCredentialData attestedCredentialData, AttestationStatement attestationStatement) {
        return new AuthenticatorImpl(attestedCredentialData, attestationStatement, 1);
    }

    public static Authenticator createAuthenticator() {
        return createAuthenticator(TestUtil.createAttestedCredentialData(), TestUtil.createFIDOU2FAttestationStatement());
    }


    public static byte[] calculateSignature(PrivateKey privateKey, byte[] signedData) {
        try {
            Signature signature;
            if (privateKey.getAlgorithm().equals("EC")) {
                signature = SignatureUtil.getES256();
            } else {
                signature = SignatureUtil.getRS256();
            }
            signature.initSign(privateKey);
            signature.update(signedData);
            return signature.sign();
        } catch (InvalidKeyException | SignatureException e) {
            throw new WebAuthnModelException("Signature calculation error", e);
        }
    }

}
