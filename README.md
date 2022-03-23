## StratX
#### Strategy testing bot
StratX is a crypto trading strategy testing bot made in Java. It uses historical data to backtest your strategy and in the future, I plan to make it find the best strategy settings by brute force.

![Backtest GUI](gui.png "Backtest GUI")

### TODO:
- [ ] Config file (Stop Loss, Take Profit, etc)
- [x] Strategies (Containers for indicators)
- [ ] Add more indicators

- [ ] Downloader mode
  - [ ] Better downloader (Binance api?)
  - [ ] To binary file for compact storage of huge data

- [x] Backtest GUI:
  - [x] Chart with candlesticks
  - [x] Visualizer showing trades
  - [X] Show indicators on chart (RSI, EMA, etc)
  
- [x] Back testing:
  - [x] Fake account for testing trades
  - [x] Descriptive result output

