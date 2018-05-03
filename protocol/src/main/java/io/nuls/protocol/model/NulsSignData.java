/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.protocol.model;

import io.nuls.core.crypto.ECKey;
import io.nuls.core.utils.crypto.Hex;

import java.math.BigInteger;

/**
 * @author facjas
 * @date 2017/11/20
 */
public class NulsSignData extends BaseNulsData {

    public static transient short SIGN_ALG_ECC = (short) 0;
    public static transient short SIGN_ALG_DEFAULT = NulsSignData.SIGN_ALG_ECC;

    protected short signAlgType;
    protected byte[] signBytes;

    public int getSignAlgType() {
        return signAlgType;
    }

    public void setSignAlgType(short signAlgType) {
        this.signAlgType = signAlgType;
    }

    public NulsSignData() {
    }

    public NulsSignData(byte[] bytes) {
        this();
        this.parse(bytes);
    }

    public int getSignLength() {
        //todo
        if (null == this.signBytes) {
            return 0;
        }
        return this.signBytes.length;
    }

    public byte[] getSignBytes() {
        return signBytes;
    }

    public void setSignBytes(byte[] signBytes) {
        this.signBytes = signBytes;
    }

    public String getSignHex() {
        return Hex.encode(serialize());
    }

    public NulsSignData sign(NulsDigestData nulsDigestData, short signAlgType, BigInteger privkey) {
        if (signAlgType == NulsSignData.SIGN_ALG_ECC) {
            ECKey ecKey = ECKey.fromPrivate(privkey);
            byte[] signBytes = ecKey.sign(nulsDigestData.getDigestBytes(), privkey);
            return new NulsSignData(signBytes);
        }
        return null;
    }

    public NulsSignData sign(NulsDigestData nulsDigestData, BigInteger privkey) {
        short signAlgType = NulsSignData.SIGN_ALG_DEFAULT;
        if (signAlgType == NulsSignData.SIGN_ALG_ECC) {
            ECKey ecKey = ECKey.fromPrivate(privkey);
            return sign(nulsDigestData, signAlgType, privkey);
        }
        return null;
    }

    @Override
    public String toString() {
        return getSignHex();
    }
}
