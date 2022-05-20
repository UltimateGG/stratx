## StratX
#### Crypto trading & strategy testing bot
StratX is a crypto trading bot made in Java.
There are a few modes:
- Downloader
  - Used to download historical data from the exchange,
    can download huge amounts of data.
- Backtesting
  - Used to backtest strategies on downloaded data and show results.
- Simulation
  - Used to simulate strategies on live data, without real money. 
- Live trading (WIP)
  - Trade real money on live data. 
> Backtesting Mode
![Backtest GUI](gui.png "Backtest GUI")

### TODO:

- [x] Config file (Stop Loss, Take Profit, etc)
  - [ ] Config setup GUI?
- [X] Simulation trading mode
- [ ] Live trading mode
- [ ] Not sure if we need candle gui anymore, just so glitchy
- [X] Package into executable jar
- [x] Downloader
- [x] Strategies (Containers for indicators)

