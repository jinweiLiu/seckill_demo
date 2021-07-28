package com.ecnu.ljw.second_demo.mapper;

import com.ecnu.ljw.second_demo.entity.Order;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {
    
    int deleteByPrimaryKey(Integer id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);
}
