/**
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.mqtt.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.mqtt.MqttClientConfig;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Optional;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertPemClientCredentials implements MqttClientCredentials {

    private static final String TLS_VERSION = "TLSv1.2";

    private String caCert;
    private String cert;
    private String privateKey;
    private String password;

    @Override
    public Optional<SslContext> initSslContext() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            return Optional.of(SslContextBuilder.forClient()
                    .keyManager(createAndInitKeyManagerFactory())
                    .trustManager(createAndInitTrustManagerFactory())
                    .clientAuth(ClientAuth.REQUIRE)
                    .build());
        } catch (Exception e) {
            log.error("[{}:{}] Creating TLS factory failed!", caCert, cert, e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

    @Override
    public void configure(MqttClientConfig config) {

    }

    private KeyManagerFactory createAndInitKeyManagerFactory() throws Exception {
        X509Certificate certHolder = readCertFile(cert);
        Object keyObject = readPrivateKeyFile(privateKey);
        char[] passwordCharArray = "".toCharArray();
        if (!StringUtils.isEmpty(password)) {
            passwordCharArray = password.toCharArray();
        }

        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");

        PrivateKey privateKey;
        if (keyObject instanceof PEMEncryptedKeyPair) {
            PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build(passwordCharArray);
            KeyPair key = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
            privateKey = key.getPrivate();
        } else if (keyObject instanceof PEMKeyPair) {
            KeyPair key = keyConverter.getKeyPair((PEMKeyPair) keyObject);
            privateKey = key.getPrivate();
        } else if (keyObject instanceof PrivateKey) {
            privateKey = (PrivateKey)keyObject;
        } else {
            throw new RuntimeException("Unable to get private key from object: " + keyObject.getClass());
        }

        KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeyStore.load(null, null);
        clientKeyStore.setCertificateEntry("cert", certHolder);
        clientKeyStore.setKeyEntry("private-key",
                privateKey,
                passwordCharArray,
                new Certificate[]{certHolder});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, passwordCharArray);
        return keyManagerFactory;
    }

    private TrustManagerFactory createAndInitTrustManagerFactory() throws Exception {
        X509Certificate caCertHolder;
        caCertHolder = readCertFile(caCert);

        KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        caKeyStore.load(null, null);
        caKeyStore.setCertificateEntry("caCert-cert", caCertHolder);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(caKeyStore);
        return trustManagerFactory;
    }

    private X509Certificate readCertFile(String fileContent) throws Exception {
        X509Certificate certificate = null;
        if (fileContent != null && !fileContent.trim().isEmpty()) {
            fileContent = fileContent.replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.decodeBase64(fileContent);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decoded));
        }
        return certificate;
    }

    private PrivateKey readPrivateKeyFile(String fileContent) throws Exception {
        RSAPrivateKey privateKey = null;
        if (fileContent != null && !fileContent.isEmpty()) {
            fileContent = fileContent.replaceAll(".*BEGIN.*PRIVATE KEY.*", "")
                    .replaceAll(".*END.*PRIVATE KEY.*", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.decodeBase64(fileContent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        }
        return privateKey;
    }

}
