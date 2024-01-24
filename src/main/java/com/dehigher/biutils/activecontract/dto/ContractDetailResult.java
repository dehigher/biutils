package com.dehigher.biutils.activecontract.dto;

import java.math.BigDecimal;
import java.util.List;

public class ContractDetailResult {

    private List<Pair> pairs;

    public List<Pair> getPairs() {
        return pairs;
    }

    public void setPairs(List<Pair> pairs) {
        this.pairs = pairs;
    }

    public static class Pair {

        private BigDecimal priceUsd;

        private Long pairCreatedAt;

        private Volume volume;

        private Txns txns;

        private BaseToken baseToken;

        public BigDecimal getPriceUsd() {
            return priceUsd;
        }

        public void setPriceUsd(BigDecimal priceUsd) {
            this.priceUsd = priceUsd;
        }

        public Volume getVolume() {
            return volume;
        }

        public void setVolume(Volume volume) {
            this.volume = volume;
        }

        public Txns getTxns() {
            return txns;
        }

        public void setTxns(Txns txns) {
            this.txns = txns;
        }

        public Long getPairCreatedAt() {
            return pairCreatedAt;
        }

        public void setPairCreatedAt(Long pairCreatedAt) {
            this.pairCreatedAt = pairCreatedAt;
        }

        public BaseToken getBaseToken() {
            return baseToken;
        }

        public void setBaseToken(BaseToken baseToken) {
            this.baseToken = baseToken;
        }
    }


    public static class BaseToken {

        String symbol;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
    }


    public static class Volume {

        private BigDecimal m5;


        public BigDecimal getM5() {
            return m5;
        }

        public void setM5(BigDecimal m5) {
            this.m5 = m5;
        }
    }

    public static class Txns {

        private M5 m5;


        public M5 getM5() {
            return m5;
        }

        public void setM5(M5 m5) {
            this.m5 = m5;
        }

        public static class M5{
            private int buys;

            private int sells;

            public int getBuys() {
                return buys;
            }

            public void setBuys(int buys) {
                this.buys = buys;
            }

            public int getSells() {
                return sells;
            }

            public void setSells(int sells) {
                this.sells = sells;
            }
        }

    }
}
