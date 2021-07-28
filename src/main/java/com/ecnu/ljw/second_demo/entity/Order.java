package com.ecnu.ljw.second_demo.entity;

import java.util.Date;

import lombok.Data;

@Data
public class Order {
    
    private Integer id;

    private Integer sid;

    private String name;

    private Integer userId;

    private Date createTime;
}
