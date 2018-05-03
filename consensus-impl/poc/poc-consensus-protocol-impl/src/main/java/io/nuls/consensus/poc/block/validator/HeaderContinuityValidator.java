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
package io.nuls.consensus.poc.block.validator;

import io.nuls.consensus.poc.protocol.model.block.BlockRoundData;
import io.nuls.consensus.poc.protocol.service.BlockService;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.utils.log.BlockLog;
import io.nuls.core.utils.log.Log;
import io.nuls.core.validate.NulsDataValidator;
import io.nuls.core.validate.ValidateResult;
import io.nuls.protocol.constant.ProtocolConstant;
import io.nuls.protocol.context.NulsContext;
import io.nuls.protocol.model.BlockHeader;
import io.nuls.protocol.model.NulsDigestData;

/**
 * @author Niels
 * @date 2017/11/17
 */
public class HeaderContinuityValidator implements NulsDataValidator<BlockHeader> {
    private static final String ERROR_MESSAGE = "block continuity check failed";
    public static final HeaderContinuityValidator INSTANCE = new HeaderContinuityValidator();

    private HeaderContinuityValidator() {
    }

    public static HeaderContinuityValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public ValidateResult validate(BlockHeader header) {
        ValidateResult result = ValidateResult.getSuccessResult();
        boolean failed = false;
        do {
            if (header.getHeight() == 0) {
                failed = !header.getPreHash().equals(NulsDigestData.calcDigestData(new byte[]{0}));
                break;
            }
            BlockHeader preHeader = null;
            try {
                preHeader = NulsContext.getServiceBean(BlockService.class).getBlockHeader(header.getPreHash().getDigestHex());
            } catch (NulsException e) {
                Log.error(e);
                return ValidateResult.getFailedResult(ErrorCode.ORPHAN_BLOCK);
            }
            if (null == preHeader) {
                return ValidateResult.getFailedResult(ErrorCode.ORPHAN_BLOCK);
            }
            BlockRoundData roundData = new BlockRoundData();
            roundData.parse(header.getExtend());
            long shouldTime = roundData.getRoundStartTime() + roundData.getPackingIndexOfRound() * ProtocolConstant.BLOCK_TIME_INTERVAL_SECOND * 1000;
            long difference = header.getTime() - shouldTime;
            long timeout = ProtocolConstant.BLOCK_TIME_INTERVAL_SECOND * 1000;
            failed = difference > timeout || difference < -timeout;
            if (failed) {
                BlockLog.debug("header validate failed:" + header.getHeight() + " , time difference：" + difference);
                break;
            }
        } while (false);
        if (failed) {
            result = ValidateResult.getFailedResult(ERROR_MESSAGE);
        }
        return result;
    }
}
