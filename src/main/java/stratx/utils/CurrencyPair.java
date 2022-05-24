package stratx.utils;

public class CurrencyPair {
    private final String crypto;
    private final String fiat;


    public CurrencyPair(String pair) {
        this.crypto = pair.substring(0, 3);
        this.fiat = pair.substring(3);
    }

    public CurrencyPair(String crypto, String fiat) {
        this.crypto = crypto;
        this.fiat = fiat;
    }

    public String getCrypto() {
        return crypto;
    }

    public String getFiat() {
        return fiat;
    }

    @Override
    public String toString() {
        return crypto + fiat;
    }
}
