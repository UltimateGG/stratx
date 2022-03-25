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
> Backtesting Mode
![Backtest GUI](gui.png "Backtest GUI")

### TODO:
- [ ] Downloader
  - [ ] Use the API to download LARGE data
  - [ ] To binary file for compact storage of huge data
  - [ ] Redo loader for new storage format
- [ ] Config file (Stop Loss, Take Profit, etc)
  - [ ] Config setup GUI
  - [ ] Make indicator settings configurable for plotting
- [x] Strategies (Containers for indicators)
- [ ] Add more indicators
  - [ ] MACD

