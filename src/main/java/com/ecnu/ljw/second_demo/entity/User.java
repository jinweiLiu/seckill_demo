package com.ecnu.ljw.second_demo.entity;

public class User {
    
    private Long id;

    private String userName;

    public User(Long id, String userName) {
        this.id = id;
        this.userName = userName;
    }

    public User() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName == null ? null : userName.trim();
    }
}
