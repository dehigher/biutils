package com.dehigher.biutils.base;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtils {

    private static ThreadPoolExecutor commonThreadPoolExecutor;

    static {
        commonThreadPoolExecutor = new ThreadPoolExecutor(
                5,
                12,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50),
                new ThreadFactoryBuilder().setNameFormat("common-pool-%d").build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        commonThreadPoolExecutor.prestartCoreThread();
    }

    public static ThreadPoolExecutor getCommonThreadPool(){
        return commonThreadPoolExecutor;
    }

}
