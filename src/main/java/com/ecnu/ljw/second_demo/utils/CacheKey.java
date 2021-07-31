package com.ecnu.ljw.second_demo.utils;

public enum CacheKey {
    REDIS_KEY("product"),
    HASH_KEY("miaosha_v1_user_hash"),
    LIMIT_KEY("miaosha_v1_user_limit");

    private String key;
    private CacheKey(String key) {
        this.key = key;
    }
    public String getKey() {
        return key;
    }
}
