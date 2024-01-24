package com.dehigher.biutils.activecontract.dto;

import java.math.BigDecimal;

public  class TopItem {

    private String address; // 合约地址

    private int txnsm5m; // 5分钟交易次数

    private BigDecimal volume5m; // 5分钟交易金额

    private BigDecimal priceUsd; // 当前价值

    private long pairCreateAt; // 合约部署时间

    private String symbol;


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTxnsm5m() {
        return txnsm5m;
    }

    public void setTxnsm5m(int txnsm5m) {
        this.txnsm5m = txnsm5m;
    }

    public BigDecimal getVolume5m() {
        return volume5m;
    }

    public void setVolume5m(BigDecimal volume5m) {
        this.volume5m = volume5m;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }

    public void setPriceUsd(BigDecimal priceUsd) {
        this.priceUsd = priceUsd;
    }

    public long getPairCreateAt() {
        return pairCreateAt;
    }

    public void setPairCreateAt(long pairCreateAt) {
        this.pairCreateAt = pairCreateAt;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}