package com.ecnu.ljw.second_demo.utils;

public enum CacheKey {
    REDIS_KEY("product"),
    HASH_KEY("miaosha_v1_user_hash"),
    LIMIT_KEY("miaosha_v1_user_limit"),
    USER_HAS_ORDER("miaosha_v1_user_has_order");

    private String key;
    private CacheKey(String key) {
        this.key = key;
    }
    public String getKey() {
        return key;
    }
}
