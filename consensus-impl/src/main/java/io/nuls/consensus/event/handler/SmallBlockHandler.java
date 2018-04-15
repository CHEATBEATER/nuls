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
package io.nuls.consensus.event.handler;

import io.nuls.account.entity.Address;
import io.nuls.consensus.cache.manager.block.TemporaryCacheManager;
import io.nuls.consensus.cache.manager.tx.TxCacheManager;
import io.nuls.consensus.constant.DownloadStatus;
import io.nuls.consensus.constant.PocConsensusConstant;
import io.nuls.consensus.entity.GetTxGroupParam;
import io.nuls.consensus.event.GetTxGroupRequest;
import io.nuls.consensus.event.SmallBlockEvent;
import io.nuls.consensus.event.notice.AssembledBlockNotice;
import io.nuls.consensus.manager.BlockManager;
import io.nuls.consensus.service.intf.DownloadService;
import io.nuls.consensus.utils.ConsensusTool;
import io.nuls.core.chain.entity.*;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.context.NulsContext;
import io.nuls.core.utils.date.TimeService;
import io.nuls.core.utils.log.BlockLog;
import io.nuls.core.utils.log.Log;
import io.nuls.core.validate.ValidateResult;
import io.nuls.event.bus.handler.AbstractEventHandler;
import io.nuls.event.bus.service.intf.EventBroadcaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author facjas
 * @date 2017/11/16
 */
public class SmallBlockHandler extends AbstractEventHandler<SmallBlockEvent> {

    private TemporaryCacheManager temporaryCacheManagerBak = TemporaryCacheManager.getInstance();
    private BlockManager blockManager = BlockManager.getInstance();
    private EventBroadcaster eventBroadcaster = NulsContext.getServiceBean(EventBroadcaster.class);
    private DownloadService downloadService = NulsContext.getServiceBean(DownloadService.class);


    private TxCacheManager txCacheManager = TxCacheManager.TX_CACHE_MANAGER;

    @Override
    public void onEvent(SmallBlockEvent event, String fromId) {
        SmallBlock smallBlock = event.getEventBody();
        if (null == smallBlock) {
            Log.warn("recieved a null smallBlock!");
            return;
        }
        if(downloadService.getStatus() != DownloadStatus.SUCCESS) {
            Log.warn("recieved a smallBlock , and downloading skip");
            return;
        }
        BlockHeader header = smallBlock.getHeader();
        BlockLog.info("recieve new block from(" + fromId + "), tx count : " + header.getTxCount() + " , tx pool count : " + txCacheManager.getReceivedCacheMap().size() + " & " +txCacheManager.getOrphanCacheMap().size() + " , header height:" + header.getHeight() + ", preHash:" + header.getPreHash() + " , hash:" + header.getHash() + ", address:" + Address.fromHashs(header.getPackingAddress()));

//        Block theBlock = blockManager.getBlock(header.getHash().getDigestHex());
//        if (null != theBlock) {
//            return;
//        }

        //todo checkIt
        if ((TimeService.currentTimeMillis() - header.getTime()) > PocConsensusConstant.BLOCK_TIME_INTERVAL_SECOND * 1000L) {
            Log.info("It's too late:hash:" + header.getHash() + ", height:" + header.getHeight() + ", packer:" + Address.fromHashs(header.getPackingAddress()));
            return;
        }

        ValidateResult result = header.verify();
        boolean isOrphan = result.getErrorCode() == ErrorCode.ORPHAN_TX || result.getErrorCode() == ErrorCode.ORPHAN_BLOCK;

        BlockLog.debug("verify block result: " + result.isSuccess() + " , verify message : " + result.getMessage() + " , isOrphan : " + isOrphan);

        if (result.isFailed() && (!isOrphan || (NulsContext.getInstance().getBestHeight() - header.getHeight()) > PocConsensusConstant.CONFIRM_BLOCK_COUNT)) {
            BlockLog.warn("discard a SmallBlock:" + smallBlock.getHeader().getHash() + ", from:" + fromId + " ,reason:" + result.getMessage());
            return;
        }
        Map<String, Transaction> txMap = new HashMap<>();
        for (Transaction tx : smallBlock.getSubTxList()) {
            txMap.put(tx.getHash().getDigestHex(), tx);
        }
        List<NulsDigestData> needHashList = new ArrayList<>();
        StringBuilder str = new StringBuilder("smallblock("+smallBlock.getHeader().getHash()+") contains:\n");
        for (NulsDigestData hash : smallBlock.getTxHashList()) {
            Transaction tx = this.txCacheManager.getTx(hash);
            str.append("hash:"+hash + ",\n");
            if (null == tx && txMap.get(hash) == null) {
                needHashList.add(hash);
                continue;
            }
            txMap.put(tx.getHash().getDigestHex(), tx);
        }
        BlockLog.info(str.toString());
        if (!needHashList.isEmpty()) {
            GetTxGroupRequest request = new GetTxGroupRequest();
            GetTxGroupParam param = new GetTxGroupParam();
            param.setBlockHash(header.getHash());
            for (NulsDigestData hash : needHashList) {
                param.addHash(hash);
            }
            request.setEventBody(param);
            this.eventBroadcaster.sendToNode(request, fromId);
            temporaryCacheManagerBak.cacheSmallBlock(smallBlock);
            return;
        }
        Block block = ConsensusTool.assemblyBlock(header, txMap, smallBlock.getTxHashList());
        boolean needForward = blockManager.addBlock(block, true, fromId);
        if(needForward) {
            SmallBlockEvent newBlockEvent = new SmallBlockEvent();
            newBlockEvent.setEventBody(smallBlock);
            List<String> addressList = eventBroadcaster.broadcastHashAndCache(newBlockEvent,  fromId);
//            for (String address : addressList) {
//                BlockLog.debug("forward blockHeader:(" + address + ")" + header.getHeight() + ", hash:" + header.getHash() + ", preHash:" + header.getPreHash() + ", packing:" + Address.fromHashs(header.getPackingAddress()));
//            }
        }

        AssembledBlockNotice notice = new AssembledBlockNotice();
        notice.setEventBody(header);
        eventBroadcaster.publishToLocal(notice);
    }
}
