/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.security.ec;

import sun.security.ec.point.AffinePoint;
import sun.security.ec.point.Point;
import sun.security.util.ArrayUtil;
import sun.security.util.CurveDB;
import sun.security.util.NamedCurve;
import sun.security.util.math.ImmutableIntegerModuloP;
import sun.security.util.math.IntegerFieldModuloP;
import sun.security.util.math.MutableIntegerModuloP;
import sun.security.util.math.SmallValue;

import javax.crypto.KeyAgreementSpi;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.util.Optional;

/**
 * KeyAgreement implementation for ECDH.
 *
 * @since   1.7
 */
public final class ECDHKeyAgreement extends KeyAgreementSpi {

    // private key, if initialized
    private ECPrivateKey privateKey;
    ECOperations privateKeyOps;

    // public key, non-null between doPhase() & generateSecret() only
    private ECPublicKey publicKey;

    // length of the secret to be derived
    private int secretLen;

    /**
     * Constructs a new ECDHKeyAgreement.
     */
    public ECDHKeyAgreement() {
    }

    // Generic init
    private void init(Key key) throws
        InvalidKeyException, InvalidAlgorithmParameterException {
        if (!(key instanceof PrivateKey)) {
            throw new InvalidKeyException("Key must be instance of PrivateKey");
        }
        privateKey = (ECPrivateKey)ECKeyFactory.toECKey(key);
        publicKey = null;
        Optional<ECOperations> opsOpt =
            ECOperations.forParameters(privateKey.getParams());
        if (opsOpt.isEmpty()) {
            NamedCurve nc = CurveDB.lookup(privateKey.getParams());
            throw new InvalidAlgorithmParameterException(
                "Curve not supported: " + (nc != null ? nc.toString() :
                    "unknown"));
        }
        privateKeyOps = opsOpt.get();
    }

    // see JCE spec
    @Override
    protected void engineInit(Key key, SecureRandom random)
            throws InvalidKeyException {
        try {
            init(key);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e);
        }
    }

    // see JCE spec
    @Override
    protected void engineInit(Key key, AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidKeyException,
            InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException
                        ("Parameters not supported");
        }
        init(key);
    }

    // see JCE spec
    @Override
    protected Key engineDoPhase(Key key, boolean lastPhase)
            throws InvalidKeyException, IllegalStateException {
        if (privateKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        if (publicKey != null) {
            throw new IllegalStateException("Phase already executed");
        }
        if (!lastPhase) {
            throw new IllegalStateException
                ("Only two party agreement supported, lastPhase must be true");
        }
        if (!(key instanceof ECPublicKey)) {
            throw new InvalidKeyException
                ("Key must be a PublicKey with algorithm EC");
        }

        this.publicKey = (ECPublicKey) key;

        int keyLenBits =
            publicKey.getParams().getCurve().getField().getFieldSize();
        secretLen = (keyLenBits + 7) >> 3;

        // Validate public key
        validate(privateKeyOps, publicKey);

        return null;
    }

    private static void validateCoordinate(BigInteger c, BigInteger mod)
        throws InvalidKeyException{
        if (c.compareTo(BigInteger.ZERO) < 0) {
            throw new InvalidKeyException("Invalid coordinate");
        }

        if (c.compareTo(mod) >= 0) {
            throw new InvalidKeyException("Invalid coordinate");
        }
    }

    /*
     * Check whether a public key is valid.
     */
    private static void validate(ECOperations ops, ECPublicKey key)
        throws InvalidKeyException {

        ECParameterSpec spec = key.getParams();

        // ensure that integers are in proper range
        BigInteger x = key.getW().getAffineX();
        BigInteger y = key.getW().getAffineY();

        BigInteger p = ops.getField().getSize();
        validateCoordinate(x, p);
        validateCoordinate(y, p);

        // ensure the point is on the curve
        EllipticCurve curve = spec.getCurve();
        BigInteger rhs = x.modPow(BigInteger.valueOf(3), p).add(curve.getA()
            .multiply(x)).add(curve.getB()).mod(p);
        BigInteger lhs = y.modPow(BigInteger.valueOf(2), p).mod(p);
        if (!rhs.equals(lhs)) {
            throw new InvalidKeyException("Point is not on curve");
        }

        // check the order of the point
        ImmutableIntegerModuloP xElem = ops.getField().getElement(x);
        ImmutableIntegerModuloP yElem = ops.getField().getElement(y);
        AffinePoint affP = new AffinePoint(xElem, yElem);
        byte[] order = spec.getOrder().toByteArray();
        ArrayUtil.reverse(order);
        Point product = ops.multiply(affP, order);
        if (!ops.isNeutral(product)) {
            throw new InvalidKeyException("Point has incorrect order");
        }

    }

    // see JCE spec
    @Override
    protected byte[] engineGenerateSecret() throws IllegalStateException {
        if ((privateKey == null) || (publicKey == null)) {
            throw new IllegalStateException("Not initialized correctly");
        }

        byte[] result;
        try {
            result = deriveKeyImpl(privateKey, privateKeyOps, publicKey);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        publicKey = null;
        return result;
    }

    // see JCE spec
    @Override
    protected int engineGenerateSecret(byte[] sharedSecret, int
            offset) throws IllegalStateException, ShortBufferException {
        if (secretLen > sharedSecret.length - offset) {
            throw new ShortBufferException("Need " + secretLen
                + " bytes, only " + (sharedSecret.length - offset)
                + " available");
        }
        byte[] secret = engineGenerateSecret();
        System.arraycopy(secret, 0, sharedSecret, offset, secret.length);
        return secret.length;
    }

    // see JCE spec
    @Override
    protected SecretKey engineGenerateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException,
            InvalidKeyException {
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("Algorithm must not be null");
        }
        if (!(algorithm.equals("TlsPremasterSecret"))) {
            throw new NoSuchAlgorithmException
                ("Only supported for algorithm TlsPremasterSecret");
        }
        return new SecretKeySpec(engineGenerateSecret(), "TlsPremasterSecret");
    }

    private static
    byte[] deriveKeyImpl(ECPrivateKey priv, ECOperations ops,
        ECPublicKey pubKey) throws InvalidKeyException {

        IntegerFieldModuloP field = ops.getField();
        // convert s array into field element and multiply by the cofactor
        MutableIntegerModuloP scalar = field.getElement(priv.getS()).mutable();
        SmallValue cofactor =
            field.getSmallValue(priv.getParams().getCofactor());
        scalar.setProduct(cofactor);
        int keySize =
            (priv.getParams().getCurve().getField().getFieldSize() + 7) / 8;
        ImmutableIntegerModuloP x =
            field.getElement(pubKey.getW().getAffineX());
        ImmutableIntegerModuloP y =
            field.getElement(pubKey.getW().getAffineY());
        Point product = ops.multiply(new AffinePoint(x, y),
            scalar.asByteArray(keySize));
        if (ops.isNeutral(product)) {
            throw new InvalidKeyException("Product is zero");
        }

        byte[] result = product.asAffine().getX().asByteArray(keySize);
        ArrayUtil.reverse(result);

        return result;
    }
}
