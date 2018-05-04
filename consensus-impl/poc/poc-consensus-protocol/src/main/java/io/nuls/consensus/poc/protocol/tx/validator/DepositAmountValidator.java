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

import io.nuls.consensus.poc.protocol.constant.PocConsensusConstant;
import io.nuls.consensus.poc.protocol.model.Deposit;
import io.nuls.consensus.poc.protocol.tx.PocJoinConsensusTransaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.constant.SeverityLevelEnum;
import io.nuls.core.exception.NulsException;
import io.nuls.core.utils.log.Log;
import io.nuls.core.validate.NulsDataValidator;
import io.nuls.core.validate.ValidateResult;
import io.nuls.db.dao.DepositDataService;
import io.nuls.db.entity.DepositPo;
import io.nuls.protocol.context.NulsContext;
import io.nuls.protocol.model.Na;

import java.util.List;

/**
 * @author Niels
 * @date 2018/1/17
 */
public class DepositAmountValidator implements NulsDataValidator<PocJoinConsensusTransaction> {

    private static final DepositAmountValidator INSTANCE = new DepositAmountValidator();

    private DepositDataService depositDataService = NulsContext.getServiceBean(DepositDataService.class);

    private DepositAmountValidator() {
    }

    public static DepositAmountValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public ValidateResult validate(PocJoinConsensusTransaction data) {
        Na limit = PocConsensusConstant.ENTRUSTER_DEPOSIT_LOWER_LIMIT;
        Na max = PocConsensusConstant.SUM_OF_DEPOSIT_OF_AGENT_UPPER_LIMIT;
        List<DepositPo> list = depositDataService.getEffectiveList(null, NulsContext.getInstance().getBestHeight(), data.getTxData().getExtend().getAgentHash(), null);
        if (list == null) {
            return ValidateResult.getSuccessResult();
        }
        Na total = Na.ZERO;
        for (DepositPo cd : list) {
            total = total.add(Na.valueOf(cd.getDeposit()));
        }
        if (limit.isGreaterThan(data.getTxData().getExtend().getDeposit())) {
            return ValidateResult.getFailedResult(this.getClass().getName(),ErrorCode.DEPOSIT_NOT_ENOUGH);
        }
        if (max.isLessThan(total.add(data.getTxData().getExtend().getDeposit()))) {
            return ValidateResult.getFailedResult(this.getClass().getName(),ErrorCode.DEPOSIT_TOO_MUCH);
        }

        try {
            if (!data.getTxData().getExtend().getDeposit().equals(data.getCoinData().getTotalNa())) {
                return ValidateResult.getFailedResult(this.getClass().getName(),SeverityLevelEnum.FLAGRANT_FOUL, ErrorCode.DEPOSIT_ERROR);
            }
        } catch (NulsException e) {
            Log.error(e);
            return ValidateResult.getFailedResult(this.getClass().getName(),e.getErrorCode(),e.getMessage());
        }

        return ValidateResult.getSuccessResult();
    }

}
