import pandas as pd
import yfinance as yf
from yahoofinancials import YahooFinancials

symbol = 'BTC-USD'
start = '2022-01-13'
end = '2022-01-18'
interval = '5m' # Opts: 1m, 3m, 5m, 15m, 30m, 1h, 1d, 1wk, 1mo, 3mo
format = 'json' # Opts: json, csv

print(f'Running downloader for {symbol}')

data = yf.download(symbol, start=start, end=end, interval=interval, progress=True, rounding=True)
if format == 'csv':
    data.to_csv(f'./src/main/resources/{symbol}_{interval}_{start}.csv')
else:
    data.to_json(f'./src/main/resources/{symbol}_{interval}_{start}.json')

print('Completed')
