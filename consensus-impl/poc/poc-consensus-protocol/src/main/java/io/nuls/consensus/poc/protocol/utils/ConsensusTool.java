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
package io.nuls.consensus.poc.protocol.utils;

import io.nuls.account.entity.Account;
import io.nuls.account.entity.Address;
import io.nuls.account.service.intf.AccountService;
import io.nuls.consensus.poc.protocol.constant.PocConsensusConstant;
import io.nuls.consensus.poc.protocol.model.*;
import io.nuls.consensus.poc.protocol.model.block.BlockData;
import io.nuls.consensus.poc.protocol.model.block.BlockRoundData;
import io.nuls.consensus.poc.protocol.model.meeting.ConsensusReward;
import io.nuls.consensus.poc.protocol.tx.YellowPunishTransaction;
import io.nuls.consensus.poc.protocol.tx.entity.YellowPunishData;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.utils.calc.DoubleUtils;
import io.nuls.core.utils.date.TimeService;
import io.nuls.core.utils.log.Log;
import io.nuls.db.entity.AgentPo;
import io.nuls.db.entity.BlockHeaderPo;
import io.nuls.db.entity.DepositPo;
import io.nuls.ledger.entity.params.Coin;
import io.nuls.ledger.entity.params.CoinTransferData;
import io.nuls.ledger.entity.params.OperationType;
import io.nuls.ledger.entity.tx.CoinBaseTransaction;
import io.nuls.protocol.constant.TransactionConstant;
import io.nuls.protocol.context.NulsContext;
import io.nuls.protocol.event.entity.Consensus;
import io.nuls.protocol.model.*;
import io.nuls.protocol.script.P2PKHScriptSig;
import io.nuls.protocol.utils.io.NulsByteBuffer;

import java.io.IOException;
import java.util.*;

/**
 * @author Niels
 * @date 2017/12/6
 */
public class ConsensusTool {
    private static AccountService accountService = NulsContext.getServiceBean(AccountService.class);

    /**
     * Deep clone proxy node objects
     * 深度克隆代理节点对象
     *
     * @param ca the agent ocbject
     * @return the object cloned
     */
    public static Consensus<Agent> copyConsensusAgent(Consensus<Agent> ca) {
        ConsensusAgentImpl consensusAgent = new ConsensusAgentImpl();
        consensusAgent.setDelHeight(ca.getDelHeight());
        consensusAgent.setAddress(ca.getAddress());
        consensusAgent.setHash(ca.getHash());
        Agent agent = new Agent();
        agent.setCreditVal(ca.getExtend().getCreditVal());
        agent.setStatus(ca.getExtend().getStatus());
        agent.setTxHash(ca.getExtend().getTxHash());
        agent.setBlockHeight(ca.getExtend().getBlockHeight());
        agent.setTotalDeposit(ca.getExtend().getTotalDeposit());
        agent.setAgentName(ca.getExtend().getAgentName());
        agent.setCommissionRate(ca.getExtend().getCommissionRate());
        agent.setDeposit(ca.getExtend().getDeposit());
        agent.setIntroduction(ca.getExtend().getIntroduction());
        agent.setPackingAddress(ca.getExtend().getPackingAddress());
        agent.setStartTime(ca.getExtend().getStartTime());
        consensusAgent.setExtend(agent);
        return consensusAgent;
    }

    /**
     * Deep clone consensus deposit objects
     * 深度克隆委托对象
     *
     * @param cd the deposit ocbject
     * @return the object cloned
     */
    public static Consensus<Deposit> copyConsensusDeposit(Consensus<Deposit> cd) {
        ConsensusDepositImpl consensusDeposit = new ConsensusDepositImpl();
        consensusDeposit.setDelHeight(cd.getDelHeight());
        consensusDeposit.setAddress(cd.getAddress());
        consensusDeposit.setHash(cd.getHash());
        Deposit deposit = new Deposit();
        deposit.setBlockHeight(cd.getExtend().getBlockHeight());
        deposit.setAgentHash(cd.getExtend().getAgentHash());
        deposit.setDeposit(cd.getExtend().getDeposit());
        deposit.setStartTime(cd.getExtend().getStartTime());
        deposit.setStatus(cd.getExtend().getStatus());
        deposit.setTxHash(cd.getExtend().getTxHash());
        consensusDeposit.setExtend(deposit);
        return consensusDeposit;

    }

    public static Consensus<Agent> fromPojo(AgentPo po) {
        if (null == po) {
            return null;
        }
        Agent agent = new Agent();
        agent.setDeposit(Na.valueOf(po.getDeposit()));
        agent.setCommissionRate(po.getCommissionRate());
        agent.setPackingAddress(po.getPackingAddress());
        agent.setIntroduction(po.getRemark());
        agent.setStartTime(po.getStartTime());
        agent.setStatus(po.getStatus());
        agent.setAgentName(po.getAgentName());
        agent.setBlockHeight(po.getBlockHeight());
        agent.setTxHash(po.getTxHash());
        Consensus<Agent> ca = new ConsensusAgentImpl();
        ca.setDelHeight(po.getDelHeight());
        ca.setAddress(po.getAgentAddress());
        ca.setHash(NulsDigestData.fromDigestHex(po.getId()));
        ca.setExtend(agent);
        return ca;
    }

    public static Consensus<Deposit> fromPojo(DepositPo po) {
        if (null == po) {
            return null;
        }
        Consensus<Deposit> ca = new ConsensusDepositImpl();
        ca.setAddress(po.getAddress());
        ca.setDelHeight(po.getDelHeight());
        Deposit deposit = new Deposit();
        deposit.setAgentHash(po.getAgentHash());
        deposit.setDeposit(Na.valueOf(po.getDeposit()));
        deposit.setStartTime(po.getTime());
        deposit.setTxHash(po.getTxHash());
        deposit.setBlockHeight(po.getBlockHeight());
        ca.setHash(NulsDigestData.fromDigestHex(po.getId()));
        ca.setExtend(deposit);
        return ca;
    }

    public static AgentPo agentToPojo(Consensus<Agent> bean) {
        if (null == bean) {
            return null;
        }
        AgentPo po = new AgentPo();
        po.setAgentAddress(bean.getAddress());
        po.setBlockHeight(bean.getExtend().getBlockHeight());
        po.setId(bean.getHexHash());
        po.setDeposit(bean.getExtend().getDeposit().getValue());
        po.setStartTime(bean.getExtend().getStartTime());
        po.setRemark(bean.getExtend().getIntroduction());
        po.setPackingAddress(bean.getExtend().getPackingAddress());
        po.setStatus(bean.getExtend().getStatus());
        po.setAgentName(bean.getExtend().getAgentName());
        po.setCommissionRate(bean.getExtend().getCommissionRate());
        po.setDelHeight(bean.getDelHeight());
        po.setTxHash(bean.getExtend().getTxHash());
        return po;
    }

    public static DepositPo depositToPojo(Consensus<Deposit> bean, String txHash) {
        if (null == bean) {
            return null;
        }
        DepositPo po = new DepositPo();
        po.setAddress(bean.getAddress());
        po.setDeposit(bean.getExtend().getDeposit().getValue());
        po.setTime(bean.getExtend().getStartTime());
        po.setAgentHash(bean.getExtend().getAgentHash());
        po.setId(bean.getHexHash());
        po.setTxHash(txHash);
        po.setDelHeight(bean.getDelHeight());
        po.setBlockHeight(bean.getExtend().getBlockHeight());
        return po;
    }

    public static Block createBlock(BlockData blockData, Account account) throws NulsException {
        if (null == account) {
            throw new NulsRuntimeException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        Block block = new Block();
        block.setTxs(blockData.getTxList());
        BlockHeader header = new BlockHeader();
        block.setHeader(header);
        try {
            block.getHeader().setExtend(blockData.getRoundData().serialize());
        } catch (IOException e) {
            Log.error(e);
        }
        header.setHeight(blockData.getHeight());
        header.setTime(blockData.getTime());
        header.setPreHash(blockData.getPreHash());
        header.setTxCount(blockData.getTxList().size());
        List<NulsDigestData> txHashList = new ArrayList<>();
        for (int i = 0; i < blockData.getTxList().size(); i++) {
            Transaction tx = blockData.getTxList().get(i);
            tx.setBlockHeight(header.getHeight());
            txHashList.add(tx.getHash());
        }
        header.setPackingAddress(account.getAddress().getHash());
        header.setMerkleHash(NulsDigestData.calcMerkleDigestData(txHashList));
        header.setHash(NulsDigestData.calcDigestData(block.getHeader()));
        P2PKHScriptSig scriptSig = new P2PKHScriptSig();
        NulsSignData signData = accountService.signDigest(header.getHash(), account, NulsContext.getCachedPasswordOfWallet());
        scriptSig.setSignData(signData);
        scriptSig.setPublicKey(account.getPubKey());
        header.setScriptSig(scriptSig);
        return block;
    }


    public static CoinBaseTransaction createCoinBaseTx(MeetingMember member, List<Transaction> txList, MeetingRound localRound, long unlockHeight) {
        CoinTransferData data = new CoinTransferData(OperationType.COIN_BASE, Na.ZERO);
        List<ConsensusReward> rewardList = calcReward(txList, member, localRound);
        Na total = Na.ZERO;
        for (int i = 0; i < rewardList.size(); i++) {
            ConsensusReward reward = rewardList.get(i);
            Coin coin = new Coin(reward.getAddress(), reward.getReward(), 0, unlockHeight);
            data.addTo(coin);
            total = total.add(reward.getReward());
        }
        data.setTotalNa(total);
        CoinBaseTransaction tx;
        try {
            tx = new CoinBaseTransaction(data, null);
            tx.setTime(member.getPackEndTime());
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsRuntimeException(e);
        }
        tx.setFee(Na.ZERO);
        tx.setHash(NulsDigestData.calcDigestData(tx));
        return tx;
    }

    private static List<ConsensusReward> calcReward(List<Transaction> txList, MeetingMember self, MeetingRound localRound) {
        List<ConsensusReward> rewardList = new ArrayList<>();
        if (self.getOwnDeposit().getValue() == Na.ZERO.getValue()) {
            long totalFee = 0;
            for (Transaction tx : txList) {
                totalFee += tx.getFee().getValue();
            }
            if (totalFee == 0L) {
                return rewardList;
            }
            double caReward = totalFee;
            ConsensusReward agentReword = new ConsensusReward();
            agentReword.setAddress(self.getAgentAddress());
            agentReword.setReward(Na.valueOf((long) caReward));
            rewardList.add(agentReword);
            return rewardList;
        }
        long totalFee = 0;
        for (Transaction tx : txList) {
            totalFee += tx.getFee().getValue();
        }
        double totalAll = DoubleUtils.mul(localRound.getMemberCount(), PocConsensusConstant.BLOCK_REWARD.getValue());
        double commissionRate = DoubleUtils.div(self.getCommissionRate(), 100, 2);
        double agentWeight = DoubleUtils.mul(self.getOwnDeposit().getValue() + self.getTotalDeposit().getValue(), self.getCalcCreditVal());
        double blockReword = totalFee;
        if (localRound.getTotalWeight() > 0d && agentWeight > 0d) {
            blockReword = DoubleUtils.sum(blockReword, DoubleUtils.mul(totalAll, DoubleUtils.div(agentWeight, localRound.getTotalWeight())));
        }

        if (blockReword == 0d) {
            return rewardList;
        }

        ConsensusReward agentReword = new ConsensusReward();
        agentReword.setAddress(self.getAgentAddress());

        long realTotalAllDeposit = self.getOwnDeposit().getValue() + self.getTotalDeposit().getValue();
        double caReward = DoubleUtils.mul(blockReword, DoubleUtils.div(self.getOwnDeposit().getValue(), realTotalAllDeposit));
        List<String> addressList = new ArrayList<>();
        Map<String, ConsensusReward> rewardMap = new HashMap<>();
        for (Consensus<Deposit> cd : self.getDepositList()) {
            double weight = DoubleUtils.div(cd.getExtend().getDeposit().getValue(), realTotalAllDeposit);
            if (cd.getAddress().equals(self.getAgentAddress())) {
                caReward = caReward + DoubleUtils.mul(blockReword, weight);
            } else {
                ConsensusReward depositReward = rewardMap.get(cd.getAddress());
                if (null == depositReward) {
                    depositReward = new ConsensusReward();
                    depositReward.setAddress(cd.getAddress());
                    rewardMap.put(cd.getAddress(), depositReward);
                    addressList.add(cd.getAddress());
                }
                double reward = DoubleUtils.mul(blockReword, weight);
                double fee = DoubleUtils.mul(reward, commissionRate);
                caReward = caReward + fee;
                double hisReward = DoubleUtils.sub(reward, fee);
                depositReward.setReward(depositReward.getReward().add(Na.valueOf(DoubleUtils.longValue(hisReward))));
            }
        }
        agentReword.setReward(Na.valueOf(DoubleUtils.longValue(caReward)));
        rewardList.add(agentReword);
        Collections.sort(addressList);
        for (String address : addressList) {
            rewardList.add(rewardMap.get(address));
        }
        return rewardList;
    }


    public static YellowPunishTransaction createYellowPunishTx(Block preBlock, MeetingMember self, MeetingRound round) throws NulsException, IOException {
        BlockRoundData preBlockRoundData = new BlockRoundData(preBlock.getHeader().getExtend());
        if (self.getRoundIndex() - preBlockRoundData.getRoundIndex() > 1) {
            return null;
        }

        int yellowCount = 0;
        if (self.getRoundIndex() == preBlockRoundData.getRoundIndex() && self.getPackingIndexOfRound() != preBlockRoundData.getPackingIndexOfRound() + 1) {
            yellowCount = self.getPackingIndexOfRound() - preBlockRoundData.getPackingIndexOfRound() - 1;
        }

        if (self.getRoundIndex() != preBlockRoundData.getRoundIndex() && (self.getPackingIndexOfRound() != 1 || preBlockRoundData.getPackingIndexOfRound() != preBlockRoundData.getConsensusMemberCount())) {
            yellowCount = self.getPackingIndexOfRound() + preBlockRoundData.getConsensusMemberCount() - preBlockRoundData.getPackingIndexOfRound() - 1;
        }

        if (yellowCount == 0) {
            return null;
        }

        List<Address> addressList = new ArrayList<>();
        for (int i = 1; i <= yellowCount; i++) {
            int index = self.getPackingIndexOfRound() - i;
            if (index > 0) {
                addressList.add(Address.fromHashs(round.getMember(index).getAgentAddress()));
            } else {
                MeetingRound preRound = round.getPreRound();
                addressList.add(Address.fromHashs(preRound.getMember(index + preRound.getMemberCount()).getAgentAddress()));
            }
        }
        if (addressList.isEmpty()) {
            return null;
        }
        YellowPunishTransaction punishTx = new YellowPunishTransaction();
        YellowPunishData data = new YellowPunishData();
        data.setAddressList(addressList);
        data.setHeight(preBlock.getHeader().getHeight() + 1);
        punishTx.setTxData(data);
        punishTx.setTime(self.getPackEndTime());
        punishTx.setFee(Na.ZERO);
        punishTx.setHash(NulsDigestData.calcDigestData(punishTx));
        return punishTx;
    }

    public static Block assemblyBlock(BlockHeader header, Map<String, Transaction> txMap, List<NulsDigestData> txHashList) {
        Block block = new Block();
        block.setHeader(header);
        List<Transaction> txs = new ArrayList<>();
        for (NulsDigestData txHash : txHashList) {
            Transaction tx = txMap.get(txHash.getDigestHex());
            tx.setBlockHeight(header.getHeight());
            if (null == tx) {
                throw new NulsRuntimeException(ErrorCode.DATA_ERROR);
            }
            txs.add(tx);
        }
        block.setTxs(txs);
        return block;
    }

    public static SmallBlock getSmallBlock(Block block) {
        SmallBlock smallBlock = new SmallBlock();
        smallBlock.setHeader(block.getHeader());
        List<NulsDigestData> txHashList = new ArrayList<>();
        for (Transaction tx : block.getTxs()) {
            txHashList.add(tx.getHash());
            if (tx.getType() == TransactionConstant.TX_TYPE_COIN_BASE ||
                    tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH ||
                    tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
                smallBlock.addConsensusTx(tx);
            }
        }
        smallBlock.setTxHashList(txHashList);
        return smallBlock;
    }
}

