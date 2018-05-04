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
package io.nuls.consensus.poc.block.validator.header;

import io.nuls.core.utils.log.Log;
import io.nuls.core.validate.NulsDataValidator;
import io.nuls.core.validate.ValidateResult;
import io.nuls.protocol.model.BlockHeader;
import io.nuls.protocol.model.NulsDigestData;
import io.nuls.protocol.script.P2PKHScriptSig;

/**
 * @author Niels
 * @date 2018/1/11
 */
public class HeaderHashValidator implements NulsDataValidator<BlockHeader> {

    private static final HeaderHashValidator INSTANCE = new HeaderHashValidator();
    private static final String ERROR_MESSAGE = "block header hash check failed";

    private HeaderHashValidator() {
    }

    @Override
    public ValidateResult validate(BlockHeader data) {
        ValidateResult result = ValidateResult.getSuccessResult();
        NulsDigestData hash = data.getHash();
        P2PKHScriptSig scriptSig = data.getScriptSig();
        NulsDigestData cfmHash = null;
        try {
            BlockHeader newHeader = new BlockHeader();
            newHeader.parse(data.serialize());
            cfmHash = newHeader.getHash();
        } catch (Exception e) {
            Log.error(e);
        }finally {
            data.setScriptSig(scriptSig);
        }
        if (!cfmHash.getDigestHex().equals(hash.getDigestHex())) {
            result = ValidateResult.getFailedResult(this.getClass().getName(),ERROR_MESSAGE);
        }
        return result;
    }

    public static HeaderHashValidator getInstance() {
        return INSTANCE;
    }
}
