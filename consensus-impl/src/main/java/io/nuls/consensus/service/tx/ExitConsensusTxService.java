/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.consensus.service.tx;

import io.nuls.consensus.cache.manager.tx.ConfirmingTxCacheManager;
import io.nuls.consensus.constant.ConsensusStatusEnum;
import io.nuls.consensus.constant.PocConsensusConstant;
import io.nuls.consensus.entity.Consensus;
import io.nuls.consensus.entity.member.Agent;
import io.nuls.consensus.entity.member.Deposit;
import io.nuls.consensus.entity.tx.PocExitConsensusTransaction;
import io.nuls.consensus.entity.tx.PocJoinConsensusTransaction;
import io.nuls.consensus.entity.tx.RegisterAgentTransaction;
import io.nuls.consensus.event.notice.CancelConsensusNotice;
import io.nuls.consensus.event.notice.StopConsensusNotice;
import io.nuls.consensus.utils.ConsensusTool;
import io.nuls.core.chain.entity.Transaction;
import io.nuls.core.constant.TransactionConstant;
import io.nuls.core.context.NulsContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.tx.serivce.TransactionService;
import io.nuls.db.dao.AgentDataService;
import io.nuls.db.dao.DepositDataService;
import io.nuls.db.entity.AgentPo;
import io.nuls.db.entity.DepositPo;
import io.nuls.db.transactional.annotation.DbSession;
import io.nuls.event.bus.service.intf.EventBroadcaster;
import io.nuls.ledger.service.intf.LedgerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 * @date 2018/1/8
 */
public class ExitConsensusTxService implements TransactionService<PocExitConsensusTransaction> {

    private LedgerService ledgerService = NulsContext.getServiceBean(LedgerService.class);
    private AgentDataService agentDataService = NulsContext.getServiceBean(AgentDataService.class);
    private DepositDataService depositDataService = NulsContext.getServiceBean(DepositDataService.class);

    @Override
    @DbSession
    public void onRollback(PocExitConsensusTransaction tx) {
        Transaction joinTx = ledgerService.getTx(tx.getTxData());
        if (joinTx.getType() == TransactionConstant.TX_TYPE_REGISTER_AGENT) {
            RegisterAgentTransaction raTx = (RegisterAgentTransaction) joinTx;
            Consensus<Agent> ca = raTx.getTxData();
            ca.getExtend().setBlockHeight(raTx.getBlockHeight());
            ca.getExtend().setStatus(ConsensusStatusEnum.WAITING.getCode());
            AgentPo agentPo = ConsensusTool.agentToPojo(ca);
            this.agentDataService.save(agentPo);

            this.ledgerService.unlockTxRollback(tx.getTxData().getDigestHex());

            DepositPo dpo = new DepositPo();
            dpo.setAgentHash(ca.getHexHash());
            dpo.setDelHeight(0L);
            this.depositDataService.updateSelectiveByAgentHash(dpo);

            //cache deposit
            Map<String, Object> params = new HashMap<>();
            params.put("agentHash", raTx.getTxData().getHexHash());
            List<DepositPo> polist = this.depositDataService.getList(params);
            if (null != polist) {
                for (DepositPo po : polist) {
                    Consensus<Deposit> cd = ConsensusTool.fromPojo(po);
                    this.ledgerService.unlockTxRollback(po.getTxHash());
                }
            }

            CancelConsensusNotice notice = new CancelConsensusNotice();
            notice.setEventBody(tx);
            NulsContext.getServiceBean(EventBroadcaster.class).publishToLocal(notice);
            return;
        }
        PocJoinConsensusTransaction pjcTx = (PocJoinConsensusTransaction) joinTx;
        Consensus<Deposit> cd = pjcTx.getTxData();
        cd.getExtend().setStatus(ConsensusStatusEnum.IN.getCode());
        DepositPo dpo = new DepositPo();
        dpo.setId(cd.getHexHash());
        dpo.setDelHeight(0L);
        this.depositDataService.updateSelective(dpo);
        StopConsensusNotice notice = new StopConsensusNotice();
        notice.setEventBody(tx);
        NulsContext.getServiceBean(EventBroadcaster.class).publishToLocal(notice);
        this.ledgerService.unlockTxRollback(tx.getTxData().getDigestHex());
    }

    @Override
    @DbSession
    public void onCommit(PocExitConsensusTransaction tx) throws NulsException {
        Transaction joinTx = ledgerService.getTx(tx.getTxData());
        if (joinTx.getType() == TransactionConstant.TX_TYPE_REGISTER_AGENT) {
            RegisterAgentTransaction raTx = (RegisterAgentTransaction) joinTx;

            this.ledgerService.unlockTxSave(tx.getTxData().getDigestHex(), tx.getTime() + PocConsensusConstant.STOP_AGENT_DEPOSIT_LOCKED_TIME * 24 * 3600 * 1000);
            Map<String, Object> paramsMap = new HashMap<>();
            paramsMap.put("agentHash", raTx.getTxData().getHexHash());
            List<DepositPo> poList = depositDataService.getList(paramsMap);
            for (DepositPo po : poList) {
                this.ledgerService.unlockTxSave(po.getTxHash(), 0);
            }
            this.agentDataService.deleteById(raTx.getTxData().getHexHash(), tx.getBlockHeight());
            DepositPo delPo = new DepositPo();
            delPo.setAgentHash(raTx.getTxData().getHexHash());
            delPo.setDelHeight(tx.getBlockHeight());
            this.depositDataService.deleteByAgentHash(delPo);
            return;
        }
        PocJoinConsensusTransaction pjcTx = (PocJoinConsensusTransaction) joinTx;
        Consensus<Deposit> cd = pjcTx.getTxData();
        DepositPo dpo = new DepositPo();
        dpo.setDelHeight(tx.getBlockHeight());
        dpo.setId(cd.getHexHash());
        this.depositDataService.deleteById(dpo);
        this.ledgerService.unlockTxSave(tx.getTxData().getDigestHex(), 0);
    }

    @Override
    @DbSession
    public void onApproval(PocExitConsensusTransaction tx) {
        Transaction joinTx = ledgerService.getTx(tx.getTxData());
        if (joinTx == null) {
            joinTx = ConfirmingTxCacheManager.getInstance().getTx(tx.getTxData());
        }
        if (joinTx.getType() == TransactionConstant.TX_TYPE_REGISTER_AGENT) {
            this.ledgerService.unlockTxApprove(tx.getTxData().getDigestHex(), tx.getTime() + PocConsensusConstant.STOP_AGENT_DEPOSIT_LOCKED_TIME * 24 * 3600 * 1000);
            return;
        }
        this.ledgerService.unlockTxApprove(tx.getTxData().getDigestHex(), 0);
    }
}
