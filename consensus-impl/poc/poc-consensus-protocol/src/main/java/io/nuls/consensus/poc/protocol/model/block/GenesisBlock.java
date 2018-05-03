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
package io.nuls.consensus.poc.protocol.model.block;

import io.nuls.account.entity.Account;
import io.nuls.account.entity.Address;
import io.nuls.account.service.intf.AccountService;
import io.nuls.account.util.AccountTool;
import io.nuls.consensus.poc.protocol.constant.PocConsensusConstant;
import io.nuls.consensus.poc.protocol.utils.StringFileLoader;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.utils.crypto.Hex;
import io.nuls.core.utils.json.JSONUtils;
import io.nuls.core.utils.log.Log;
import io.nuls.core.utils.param.AssertUtil;
import io.nuls.core.validate.ValidateResult;
import io.nuls.ledger.entity.params.Coin;
import io.nuls.ledger.entity.params.CoinTransferData;
import io.nuls.ledger.entity.params.OperationType;
import io.nuls.ledger.entity.tx.CoinBaseTransaction;
import io.nuls.protocol.constant.ProtocolConstant;
import io.nuls.protocol.context.NulsContext;
import io.nuls.protocol.model.*;
import io.nuls.protocol.script.P2PKHScriptSig;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 * @date 2017/11/10
 */
public final class GenesisBlock extends Block {
    private static final String CONFIG_FILED_TIME = "time";
    private static final String CONFIG_FILED_HEIGHT = "height";
    private static final String CONFIG_FILED_TXS = "txs";
    private static final String CONFIG_FILED_ADDRESS = "address";
    private static final String CONFIG_FILED_NULS = "nuls";
    private static final String CONFIG_FILED_UNLOCK_HEIGHT = "unlockHeight";
    private static final String address = "2CjGt6mMMwZpp1ajcfyEdUP5sQv9p2D";
    public static final String priKey = "009cf05b6b3fe8c09b84c13783140c0f1958e8841f8b6f894ef69431522bc65712";

    private static GenesisBlock INSTANCE = new GenesisBlock();

    private long blockTime;

    private int status = 0;

    public static GenesisBlock getInstance() {
        if (INSTANCE.status == 0) {
            String json = null;
            try {
                json = StringFileLoader.read(PocConsensusConstant.GENESIS_BLOCK_FILE);
            } catch (NulsException e) {
                Log.error(e);
            }
            INSTANCE.init(json);
        }
        return INSTANCE;
    }

    private GenesisBlock() {

    }

    private synchronized void init(String json) {
        if (status > 0) {
            return;
        }
        Map<String, Object> jsonMap = null;
        try {
            jsonMap = JSONUtils.json2map(json);
        } catch (Exception e) {
            Log.error(e);
        }
        String time = (String) jsonMap.get(CONFIG_FILED_TIME);
        AssertUtil.canNotEmpty(time, ErrorCode.CONFIG_ERROR);
        blockTime = Long.parseLong(time);
        this.initGengsisTxs(jsonMap);
        this.fillHeader(jsonMap);
        ValidateResult validateResult = this.verify();
        if (validateResult.isFailed()) {
            throw new NulsRuntimeException(validateResult.getErrorCode(), validateResult.getMessage());
        }
        this.status = 1;
    }

    private void initGengsisTxs(Map<String, Object> jsonMap) {
        List<Map<String, Object>> list = (List<Map<String, Object>>) jsonMap.get(CONFIG_FILED_TXS);
        if (null == list || list.isEmpty()) {
            throw new NulsRuntimeException(ErrorCode.CONFIG_ERROR);
        }
        CoinTransferData data = new CoinTransferData(OperationType.COIN_BASE, Na.ZERO);
        data.setPriKey(Hex.decode(priKey));
        Na total = Na.ZERO;
        for (Map<String, Object> map : list) {
            String address = (String) map.get(CONFIG_FILED_ADDRESS);
            AssertUtil.canNotEmpty(address, ErrorCode.NULL_PARAMETER);
            Integer nuls = (Integer) map.get(CONFIG_FILED_NULS);
            AssertUtil.canNotEmpty(nuls, ErrorCode.NULL_PARAMETER);
            Integer height = (Integer) map.get(CONFIG_FILED_UNLOCK_HEIGHT);
            Coin coin = new Coin(address, Na.parseNuls(nuls), 0, height == null ? 0 : height.longValue());
            data.addTo(coin);
            total = total.add(coin.getNa());
        }
        data.setTotalNa(total);
        CoinBaseTransaction tx = null;
        try {
            tx = new CoinBaseTransaction(data, null);
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsRuntimeException(e);
        }
        tx.setTime(this.blockTime);
        tx.setFee(Na.ZERO);
        tx.setHash(NulsDigestData.calcDigestData(tx.serialize()));

        P2PKHScriptSig p2PKHScriptSig = new P2PKHScriptSig();
        Account account = null;
        try {
            account = AccountTool.createAccount(priKey);
        } catch (NulsException e) {
            Log.error(e);
        }
        AccountService accountService = NulsContext.getServiceBean(AccountService.class);
        P2PKHScriptSig scriptSig = null;
        try {
            scriptSig = accountService.createP2PKHScriptSigFromDigest(tx.getHash(), account, "");
        } catch (NulsException e) {
            Log.error(e);
        }

        tx.setScriptSig(scriptSig.serialize());

        List<Transaction> txlist = new ArrayList<>();
//        tx.setStatus(TxStatusEnum.AGREED);
        txlist.add(tx);
        setTxs(txlist);
    }


    private void fillHeader(Map<String, Object> jsonMap) {
        Integer height = (Integer) jsonMap.get(CONFIG_FILED_HEIGHT);
        AssertUtil.canNotEmpty(height, ErrorCode.CONFIG_ERROR);

        BlockHeader header = new BlockHeader();
        this.setHeader(header);
        header.setHeight(height);
        header.setTime(blockTime);

        header.setPreHash(NulsDigestData.calcDigestData(new byte[]{0}));
        header.setTxCount(this.getTxs().size());
        List<NulsDigestData> txHashList = new ArrayList<>();
        for (Transaction tx : this.getTxs()) {
            txHashList.add(tx.getHash());
        }
        header.setMerkleHash(NulsDigestData.calcMerkleDigestData(txHashList));

        BlockRoundData data = new BlockRoundData();
        data.setRoundIndex(1);
        data.setRoundStartTime(header.getTime() - ProtocolConstant.BLOCK_TIME_INTERVAL_SECOND * 1000);
        data.setConsensusMemberCount(1);
        data.setPackingIndexOfRound(1);
        header.setExtend(data.serialize());
        try {
            header.setPackingAddress(Address.fromHashs(address).getHash());
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsRuntimeException(e);
        }
        header.setHash(NulsDigestData.calcDigestData(header));

        P2PKHScriptSig p2PKHScriptSig = new P2PKHScriptSig();
        NulsSignData signData = this.signature(header.getHash().getDigestBytes());
        p2PKHScriptSig.setSignData(signData);
        p2PKHScriptSig.setPublicKey(getGenesisPubkey());
        header.setScriptSig(p2PKHScriptSig);
    }

    private NulsSignData signature(byte[] bytes) {
        AccountService service = NulsContext.getServiceBean(AccountService.class);
        return service.signDigest(bytes, ECKey.fromPrivate(new BigInteger(Hex.decode(priKey))).getPrivKeyBytes());
    }

    private byte[] getGenesisPubkey() {
        return ECKey.fromPrivate(new BigInteger(Hex.decode(priKey))).getPubKey();
    }
}

