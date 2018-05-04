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
import io.nuls.core.validate.NulsDataValidator;
import io.nuls.core.validate.ValidateResult;
import io.nuls.protocol.model.Na;
import io.nuls.protocol.model.Transaction;

/**
 * @author Niels
 * @date 2017/12/19
 */
public class TxFieldValidator implements NulsDataValidator<Transaction> {

    private static final TxFieldValidator INSTANCE = new TxFieldValidator();

    public final static int MAX_REMARK_LEN = 100;
    public final static int MAX_TX_TYPE = 10000;
    public static final int MAX_TX_SIZE = 100000;

    private TxFieldValidator() {
    }

    public static TxFieldValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public ValidateResult validate(Transaction tx) {
        boolean result = true;
        do {
            if (tx == null) {
                result = false;
                break;
            }
            if (tx.getHash() == null || tx.getHash().size() == 0 || tx.getHash().size() > 70) {
                result = false;
                break;
            }
            //todo legnth  || data.getSign().getSignLength() == 0
            if (tx.getIndex() < 0 || tx.getIndex() > MAX_TX_TYPE) {
                result = false;
                break;
            }
            if (tx.getType() == 0 || tx.getType() > MAX_TX_TYPE) {
                result = false;
                break;
            }
            if (tx.getTime() == 0) {
                result = false;
                break;
            }
            if (tx.getRemark() != null && tx.getRemark().length > MAX_REMARK_LEN) {
                result = false;
                break;
            }
            if (tx.getFee() != null && tx.getFee().isGreaterThan(Na.MAX)) {
                result = false;
                break;
            }
            if (tx.getTxData() != null && tx.getTxData().size() > 1024) {
                result = false;
                break;
            }
            if (tx.size() > MAX_TX_SIZE) {
                result = false;
                break;
            }
        } while (false);
        if (!result) {
            return ValidateResult.getFailedResult(this.getClass().getName(),ErrorCode.DATA_FIELD_CHECK_ERROR);
        }
        return ValidateResult.getSuccessResult();
    }
}
