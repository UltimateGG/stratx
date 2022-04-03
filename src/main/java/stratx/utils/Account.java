package stratx.utils;

import stratx.StratX;

import java.util.ArrayList;

public class Account {
    private final double initialBalance;
    private double balance;
    private final ArrayList<Trade> trades = new ArrayList<>();
    private int openTrades = 0;

    public Account(double balance) {
        this.balance = balance;
        this.initialBalance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void openTrade(Trade trade) {
        trades.add(trade);
        balance -= trade.getEntryAmountUSD();
        openTrades++;
        StratX.trace("New Balance: ${} ({} open trades)\n", MathUtils.COMMAS_2F.format(balance), openTrades);
    }

    public void closeTrade(Trade trade, String reason) {
        trade.close(reason);
        balance += trade.getCurrentUSDWorth();
        openTrades--;
        StratX.trace("New Balance: ${} ({} open trades)\n", MathUtils.COMMAS_2F.format(balance), openTrades);
    }

    public ArrayList<Trade> getTrades() {
        return trades;
    }

    public int getOpenTrades() {
        return openTrades;
    }

    public void reset() {
        balance = initialBalance;
        openTrades = 0;
        trades.clear();
    }
}
