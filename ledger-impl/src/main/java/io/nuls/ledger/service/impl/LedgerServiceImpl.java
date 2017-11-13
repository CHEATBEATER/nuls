package io.nuls.ledger.service.impl;

import io.nuls.account.entity.Address;
import io.nuls.core.chain.entity.transaction.Transaction;
import io.nuls.core.chain.entity.transaction.TransactionOutput;
import io.nuls.ledger.entity.Balance;
import io.nuls.ledger.service.intf.LedgerService;

import java.util.List;

/**
 * Created by Niels on 2017/11/13.
 * nuls.io
 */
public class LedgerServiceImpl implements LedgerService {

    private static final LedgerService instance = new LedgerServiceImpl();

    private LedgerServiceImpl() {
    }

    public static LedgerService getInstance() {
        return instance;
    }

    @Override
    public Balance getBalance(String address) {
        return null;
    }

    @Override
    public boolean varifyTransaction(Transaction tx) {
        return false;
    }

    @Override
    public boolean transfer(Address address, String password, Address toAddress, double amount, String remark) {
        return false;
    }

    @Override
    public boolean saveTransaction(Transaction tx) {
        return false;
    }

    @Override
    public Transaction query(byte[] txid) {
        return null;
    }

    @Override
    public List<Transaction> queryListByAccount(String address, int txType, long beginTime) {
        return null;
    }

    @Override
    public List<TransactionOutput> queryNotSpent(String address, double total) {
        return null;
    }
}