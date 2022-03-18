import pandas as pd
import yfinance as yf
from yahoofinancials import YahooFinancials

symbol = 'ETH-USD'
start = '2022-03-13'
end = '2022-03-18'
interval = '15m' # Opts: 1m, 5m, 15m, 30m, 1h, 1d, 1wk, 1mo
format = 'json' # Opts: json, csv

print(f'Running downloader for {symbol}')

data = yf.download(symbol, start=start, end=end, interval=interval, progress=True, rounding=True)
if format == 'csv':
    data.to_csv(f'./src/main/resources/{symbol}_{interval}_{start}.csv')
else:
    data.to_json(f'./src/main/resources/{symbol}_{interval}_{start}.json')

print('Completed')
