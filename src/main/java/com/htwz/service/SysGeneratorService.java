/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.htwz.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htwz.dao.GeneratorDao;
import com.htwz.utils.GenUtils;
import com.htwz.utils.PageUtils;
import com.htwz.utils.Query;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 *
 * @author lujing
 */
@Service
public class SysGeneratorService {
    @Autowired
    private GeneratorDao generatorDao;
    
    public PageUtils queryList(Query query) {
        
        Page<?> page = PageHelper.startPage(query.getPage(), query.getLimit());
        List<Map<String, Object>> list = generatorDao.queryList(query);
        return new PageUtils(list, (int) page.getTotal(), query.getLimit(), query.getPage());
    }
    
    public Map<String, String> queryTable(String tableName, String databaseName) {
        return generatorDao.queryTable(tableName, databaseName);
    }
    
    public List<Map<String, String>> queryColumns(String tableName, String databaseName) {
        return generatorDao.queryColumns(tableName, databaseName);
    }
    
    public byte[] generatorCode(String[] tableNames, String modelName, String databaseName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        
        for (String tableName : tableNames) {
            //查询表信息
            Map<String, String> table = queryTable(tableName, databaseName);
            //查询列信息
            List<Map<String, String>> columns = queryColumns(tableName, databaseName);
            //生成代码
            GenUtils.generatorCode(table, columns, zip, modelName);
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }
}
