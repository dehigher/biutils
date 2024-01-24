package com.dehigher.biutils;

import com.dehigher.biutils.activecontract.ContractQueryExecutor;

public class Main {
    public static void main(String[] args) {
        ContractQueryExecutor executor = new ContractQueryExecutor(1000 * 20);
        executor.start();

        while (true){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}