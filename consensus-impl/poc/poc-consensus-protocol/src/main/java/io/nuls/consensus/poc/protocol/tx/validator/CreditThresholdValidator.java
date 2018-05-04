/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.consensus.poc.protocol.tx.validator;

import io.nuls.consensus.poc.protocol.constant.PunishType;
import io.nuls.consensus.poc.protocol.tx.PocJoinConsensusTransaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.utils.log.Log;
import io.nuls.core.validate.NulsDataValidator;
import io.nuls.core.validate.ValidateResult;
import io.nuls.db.dao.PunishLogDataService;
import io.nuls.db.entity.PunishLogPo;
import io.nuls.ledger.service.intf.LedgerService;
import io.nuls.protocol.constant.TransactionConstant;
import io.nuls.protocol.context.NulsContext;
import io.nuls.protocol.model.Transaction;

import java.util.List;

/**
 * @author Niels
 * @date 2017/12/19
 */
public class CreditThresholdValidator implements NulsDataValidator<PocJoinConsensusTransaction> {

    private static final CreditThresholdValidator INSTANCE = new CreditThresholdValidator();
    private PunishLogDataService punishLogDataService = NulsContext.getInstance().getServiceBean(PunishLogDataService.class);

    private CreditThresholdValidator() {
    }

    public static CreditThresholdValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public ValidateResult validate(PocJoinConsensusTransaction data) {
        String address = data.getTxData().getAddress();
        long count = 0;
        try {
            count = punishLogDataService.getCountByType(address, PunishType.RED.getCode());
        } catch (Exception e) {
            Log.error(e);
        }
        if (count == 0) {
            return ValidateResult.getSuccessResult();
        }
        return ValidateResult.getFailedResult(this.getClass().getName(),ErrorCode.LACK_OF_CREDIT);
    }
}
