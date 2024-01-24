package com.dehigher.biutils.activecontract.dto;

import com.dehigher.biutils.base.TimeUtils;

import java.util.List;

public class PerRoundContractsLog {

    private long startBlock;

    private long endBlock;

    private long stringTime;

    private long endTime;

    private List<String> addresses;


    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("开始区块：").append(startBlock);
        sb.append("\n");
        sb.append("结束区块：").append(endBlock);
        sb.append("\n");
        sb.append("开始时间：").append(TimeUtils.format(stringTime));
        sb.append("\n");
        sb.append("结束时间：").append(TimeUtils.format(endTime));
        sb.append("\n");
        for(String address:addresses){
            sb.append(address).append("\n");
        }
        return sb.toString();
    }
}
