/**
 * MIT License
 * *
 * Copyright (c) 2017-2018 nuls.io
 * *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * *
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
import io.nuls.ledger.entity.OutPutStatusEnum;
import io.nuls.ledger.entity.UtxoData;
import io.nuls.ledger.entity.UtxoInput;
import io.nuls.ledger.entity.UtxoOutput;
import io.nuls.ledger.entity.tx.AbstractCoinTransaction;
import io.nuls.ledger.service.impl.LedgerCacheService;
import io.nuls.protocol.constant.TransactionConstant;
import io.nuls.protocol.constant.TxStatusEnum;
import io.nuls.protocol.context.NulsContext;
import io.nuls.protocol.script.P2PKHScriptSig;

import java.util.Arrays;

/**
 * @author Niels
 * @date 2017/11/20
 */
public class UtxoTxInputsValidator implements NulsDataValidator<AbstractCoinTransaction> {

    private static final UtxoTxInputsValidator INSTANCE = new UtxoTxInputsValidator();

    private LedgerCacheService ledgerCacheService = LedgerCacheService.getInstance();

    private UtxoTxInputsValidator() {
    }

    public static UtxoTxInputsValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public ValidateResult validate(AbstractCoinTransaction tx) {
        if(tx.isSkipInputValidator()){
            return ValidateResult.getSuccessResult();
        }
        UtxoData data = (UtxoData) tx.getCoinData();
        for (int i = 0; i < data.getInputs().size(); i++) {
            UtxoInput input = data.getInputs().get(i);
            UtxoOutput output = ledgerCacheService.getUtxo(input.getKey());

            if (output == null && tx.getStatus() == TxStatusEnum.UNCONFIRM) {
                return ValidateResult.getFailedResult(ErrorCode.ORPHAN_TX);
            } else if (output == null) {
                return ValidateResult.getFailedResult(ErrorCode.UTXO_NOT_FOUND);
            }

            long blockHeight = tx.getBlockHeight();
            if(blockHeight<0){
                blockHeight  = NulsContext.getInstance().getBestHeight();
            }

            if (tx.getStatus() == TxStatusEnum.UNCONFIRM) {
                if (tx.getType() == TransactionConstant.TX_TYPE_STOP_AGENT) {
                    if (output.getStatus() != OutPutStatusEnum.UTXO_CONSENSUS_LOCK) {
                        return ValidateResult.getFailedResult(ErrorCode.UTXO_STATUS_CHANGE);
                    }
                } else if (!output.isUsable(blockHeight)) {
                    return ValidateResult.getFailedResult(ErrorCode.UTXO_STATUS_CHANGE);
                }
            }

            byte[] owner = output.getOwner();
            P2PKHScriptSig p2PKHScriptSig = null;
            try {
                p2PKHScriptSig = P2PKHScriptSig.createFromBytes(tx.getScriptSig());
            } catch (NulsException e) {
                return ValidateResult.getFailedResult(ErrorCode.DATA_ERROR);
            }
            byte[] user = p2PKHScriptSig.getSignerHash160();
            if (!Arrays.equals(owner, user)) {
                return ValidateResult.getFailedResult(ErrorCode.INVALID_INPUT);
            }

            return ValidateResult.getSuccessResult();
        }
        return ValidateResult.getSuccessResult();
    }

}
