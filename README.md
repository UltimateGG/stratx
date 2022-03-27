## StratX
#### Crypto trading & strategy testing bot
StratX is a crypto trading bot made in Java. Currently, the bot only backtests
strategies and does not trade. There are a few modes:
- Downloader
  - Used to download historical data from the exchange,
    can download huge amounts of data.
- Backtesting
  - Used to backtest strategies on downloaded data and show results.
- ~~Live trading~~ (TODO)
- ~~Simulation~~ (TODO)
> Backtesting Mode
![Backtest GUI](gui.png "Backtest GUI")

### TODO:

- [ ] Broker fees & taxes settings
- [ ] Config file (Stop Loss, Take Profit, etc)
  - [ ] Config setup GUI
  - [ ] Make indicator settings configurable for plotting
- [ ] Add more indicators
  - [ ] MACD
- [ ] Simulation trading mode
- [ ] Live trading mode
- [X] Log files for trades
- [x] Downloader
- [x] Strategies (Containers for indicators)

