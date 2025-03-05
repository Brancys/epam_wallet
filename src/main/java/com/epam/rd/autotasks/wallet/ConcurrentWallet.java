package com.epam.rd.autotasks.wallet;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentWallet implements Wallet {
    private final List<Account> accounts;
    private final PaymentLog paymentLog;
    private final Lock walletLock = new ReentrantLock();

    public ConcurrentWallet(List<Account> accounts, PaymentLog paymentLog) {
        this.accounts = accounts;
        this.paymentLog = paymentLog;
    }

    @Override
    public void pay(String recipient, long amount) throws ShortageOfMoneyException {
        Account selectedAccount = null;

        // Lock the entire wallet to safely find an account
        walletLock.lock();
        try {
            for (Account account : accounts) {
                if (account.balance() >= amount) {
                    selectedAccount = account;
                    break;
                }
            }

            if (selectedAccount == null) {
                throw new ShortageOfMoneyException(recipient, amount);
            }

            // Lock the selected account before making changes
            selectedAccount.lock().lock();
        } finally {
            walletLock.unlock(); // Release the wallet lock after selecting an account
        }

        try {
            selectedAccount.pay(amount);
            paymentLog.add(selectedAccount, recipient, amount);
        } finally {
            selectedAccount.lock().unlock();
        }
    }

}