package stratx.utils;

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


    }

    public void closeTrade(Trade trade, Candlestick exit, String reason) {
        trade.close(exit, reason);
        balance += trade.getProfit() + trade.getEntryAmountUSD();
        openTrades--;
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
