/**
 * MIT License
 **
 * Copyright (c) 2017-2018 nuls.io
 **
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 **
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 **
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.ledger.validator;

import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.validate.NulsDataValidator;
import io.nuls.core.validate.ValidateResult;
import io.nuls.protocol.constant.TransactionConstant;
import io.nuls.protocol.model.NulsDigestData;
import io.nuls.protocol.model.Transaction;
import io.nuls.protocol.script.P2PKHScriptSig;
import io.nuls.protocol.utils.io.NulsByteBuffer;

import java.util.Arrays;

/**
 * @author Niels
 * @date 2017/11/20
 */
public class TxSignValidator implements NulsDataValidator<Transaction> {
    private static final TxSignValidator INSTANCE = new TxSignValidator();

    private TxSignValidator() {
    }

    public static TxSignValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public ValidateResult validate(Transaction tx) {

        if (tx.getType() == TransactionConstant.TX_TYPE_COIN_BASE || tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
            return ValidateResult.getSuccessResult();
        }

        NulsDigestData nulsDigestData;
        try {
            nulsDigestData = NulsDigestData.calcDigestData(tx.serializeForHash());
        } catch (Exception e) {
            return ValidateResult.getFailedResult(this.getClass().getName(),ErrorCode.DATA_ERROR);
        }
        if (!Arrays.equals(nulsDigestData.getDigestBytes(), tx.getHash().getDigestBytes())) {
            return ValidateResult.getFailedResult(this.getClass().getName(),ErrorCode.DATA_ERROR);
        }

        P2PKHScriptSig p2PKHScriptSig = new P2PKHScriptSig();
        try {
            p2PKHScriptSig.parse(tx.getScriptSig());
        } catch (Exception e) {
            return ValidateResult.getFailedResult(this.getClass().getName(),ErrorCode.SIGNATURE_ERROR);
        }
        return p2PKHScriptSig.verifySign(tx.getHash());
    }
}
