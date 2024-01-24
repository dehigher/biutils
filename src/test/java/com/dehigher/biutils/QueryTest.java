package com.dehigher.biutils;

import com.dehigher.biutils.activecontract.ContractQueryExecutor;
import org.junit.Test;

public class QueryTest {


    @Test
    public void test() {
        ContractQueryExecutor queryExecutor = new ContractQueryExecutor(1000 * 20);
        queryExecutor.start();

        while (true){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
