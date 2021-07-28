package com.ecnu.ljw.second_demo.mapper;

import com.ecnu.ljw.second_demo.entity.Stock;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockMapper {
    
    int deleteByPrimaryKey(Integer id);

    int insert(Stock record);

    int insertSelective(Stock record);

    Stock selectByPrimaryKey(Integer id);

    Stock selectByPrimaryKeyForUpdate(Integer id);

    int updateByPrimaryKeySelective(Stock record);

    int updateByPrimaryKey(Stock record);

    int updateByOptimistic(Stock record);
}
