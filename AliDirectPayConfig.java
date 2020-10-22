package com.lly835.bestpay.config;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

public class AliDirectPayConfig extends PayConfig {
    private String partnerId;
    private String partnerMD5Key;
    private String partnerRSAPrivateKey;
    private String alipayRSAPublicKey;
    private PrivateKey partnerRSAPrivateKeyObject;
    private PublicKey alipayRSAPublicKeyObject;
    private SignType signType;

    public AliDirectPayConfig() {
    }

    public void check() {
        super.check();
        Objects.requireNonNull(this.partnerId, "config param 'partnerId' is null.");
        if (!this.partnerId.matches("^2088[0-9]{12}$")) {
            throw new IllegalArgumentException("config param 'partnerId' [" + this.partnerId + "] is incorrect.");
        } else {
            this.partnerId = this.partnerId;
            Objects.requireNonNull(this.signType, "config param 'signType' is null.");
            this.signType = this.signType;
            switch(this.signType) {
            case MD5:
                if (StringUtils.isEmpty(this.partnerMD5Key)) {
                    throw new IllegalArgumentException("config param 'partnerMD5Key' is empty.");
                }

                this.partnerMD5Key = this.partnerMD5Key;
                break;
            case RSA:
                Objects.requireNonNull(this.partnerRSAPrivateKey, "config param 'partnerRSAPrivateKey' is null.");

                KeyFactory keyFactory;
                try {
                    keyFactory = KeyFactory.getInstance("RSA");
                    this.partnerRSAPrivateKeyObject = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.decodeBase64(this.partnerRSAPrivateKey)));
                } catch (InvalidKeySpecException | NoSuchAlgorithmException var3) {
                    throw new IllegalArgumentException("config param 'partnerRSAPrivateKey' is incorrect.", var3);
                }

                Objects.requireNonNull(this.alipayRSAPublicKey, "config param 'alipayRSAPublicKey' is null.");

                try {
                    keyFactory = KeyFactory.getInstance("RSA");
                    this.alipayRSAPublicKeyObject = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.decodeBase64(this.alipayRSAPublicKey)));
                    break;
                } catch (InvalidKeySpecException | NoSuchAlgorithmException var2) {
                    throw new IllegalArgumentException("config param 'alipayRSAPublicKey' is incorrect.", var2);
                }
            case RSA2:
                throw new IllegalArgumentException("config param 'signType' [" + this.signType + "] is not match.");
            }

        }
    }

    public String getInputCharset() {
        return "utf-8";
    }

    public String getPartnerId() {
        return this.partnerId;
    }

    public String getPartnerMD5Key() {
        return this.partnerMD5Key;
    }

    public PrivateKey getPartnerRSAPrivateKeyObject() {
        return this.partnerRSAPrivateKeyObject;
    }

    public PublicKey getAlipayRSAPublicKeyObject() {
        return this.alipayRSAPublicKeyObject;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public void setPartnerRSAPrivateKey(String partnerRSAPrivateKey) {
        this.partnerRSAPrivateKey = partnerRSAPrivateKey;
    }

    public void setAlipayRSAPublicKey(String alipayRSAPublicKey) {
        this.alipayRSAPublicKey = alipayRSAPublicKey;
    }

    public SignType getSignType() {
        return this.signType;
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }
}
