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

import io.nuls.consensus.poc.protocol.tx.PocJoinConsensusTransaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.validate.NulsDataValidator;
import io.nuls.core.validate.ValidateResult;
import io.nuls.db.dao.AgentDataService;
import io.nuls.db.entity.AgentPo;
import io.nuls.protocol.context.NulsContext;

/**
 * @author Niels
 * @date 2017/12/6
 */
public class AgentExistValidator implements NulsDataValidator<PocJoinConsensusTransaction> {

    private static final AgentExistValidator INSTANCE = new AgentExistValidator();

    private AgentDataService agentDataService = NulsContext.getServiceBean(AgentDataService.class);

    private AgentExistValidator() {
    }

    public static final AgentExistValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public ValidateResult validate(PocJoinConsensusTransaction tx) {
        AgentPo ca = agentDataService.get(tx.getTxData().getExtend().getAgentHash());
        if (ca == null) {
            return ValidateResult.getFailedResult(this.getClass().getName(),ErrorCode.ORPHAN_TX,"Agent is not exist!");
        }
        return ValidateResult.getSuccessResult();
    }
}
