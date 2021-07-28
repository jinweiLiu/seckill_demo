package com.ecnu.ljw.second_demo.utils;

public enum CacheKey {
    REDIS_KY("product");

    private String key;
    private CacheKey(String key) {
        this.key = key;
    }
    public String getKey() {
        return key;
    }
}
