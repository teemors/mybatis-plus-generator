package com.htwz.utils;

import com.htwz.entity.ColumnEntity;
import com.htwz.entity.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 *
 * @author lujing
 * @date 2016年12月19日 下午11:40:24
 */
public class GenUtils {
    
    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("template/Entity.java.vm");
        templates.add("template/query.java.vm");
        templates.add("template/Dao.java.vm");
        templates.add("template/Dao.xml.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/ServiceImpl.java.vm");
        templates.add("template/Controller.java.vm");
//        templates.add("template/menu.sql.vm");

//        templates.add("template/index.vue.vm");
//        templates.add("template/add-or-update.vue.vm");
        
        return templates;
    }
    
    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip, String modelName) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getString("tablePrefix"));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));
        String excludeTableFiled = config.getString("excludeTableFiled");
        
        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));
            
            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));
            
            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType");
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && attrType.equals("BigDecimal")) {
                hasBigDecimal = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }
            //去掉公共的字段
            if (StringUtils.isNotBlank(columnEntity.getColumnName()) && excludeTableFiled.contains(columnEntity.getColumnName())) {
                continue;
            }
            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);
        
        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }
        
        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "com.htwz" : mainPath;
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("mainPath", mainPath);
        map.put("package", config.getString("package"));
        map.put("moduleName", modelName);
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);
    
        Integer outFileType = config.getInt("outFileType");
        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);
           if(outFileType == 1){
               outFileToTarget(modelName, config, tableEntity, template, sw);
               
           }else {
               downloadFile(zip, modelName, config, tableEntity, template, sw);
           }
           
        }
    }
    
    private static void outFileToTarget(String modelName, Configuration config, TableEntity tableEntity, String template, StringWriter sw) {
        String filePath = getFileName(template, tableEntity.getClassName(), config.getString("package"), modelName);
        
        File file = createFile(filePath);
        
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(sw.toString().getBytes());
            fileOutputStream.close();
            System.out.println("模板" + template + "解析成功 ，代码位置：" + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("模板 [ " + template + " ] 解析错误");
        }
    }
    
    private static void downloadFile(ZipOutputStream zip, String modelName, Configuration config, TableEntity tableEntity, String template, StringWriter sw) {
        try {
            //添加到zip
            zip.putNextEntry(new ZipEntry(getFileName(template, tableEntity.getClassName(), config.getString("mainPath"), modelName)));
            IOUtils.write(sw.toString(), zip, "UTF-8");
            IOUtils.closeQuietly(sw);
            zip.closeEntry();
        } catch (IOException e) {
            throw new RRException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
        }
    }
    
    
    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }
    
    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        boolean removeProfix = getConfig().getBoolean("removePrefix");
       
    
        if (removeProfix) {
            int indexFirstLine = tableName.indexOf("_");
            tableName = tableName.substring(indexFirstLine);
        }
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return columnToJava(tableName);
    }
    
    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }
    
    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName, String moduleName) {
        String packagePath;
        Integer outFileType = getConfig().getInt("outFileType");
        if(outFileType == 1 ){
            String outPath = getConfig().getString("outPath");
            packagePath  = outPath + File.separator + "out" +  File.separator;
        }else {
             packagePath = "main" + File.separator + "java" + File.separator;
        }
    
        
        
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator;
        }
        
        
        if (template.contains("Entity.java.vm")) {
            return packagePath + "models" + File.separator + "entity" + File.separator + moduleName + File.separator + className + ".java";
        }
        
        if (template.contains("query.java.vm")) {
            return packagePath + "models" + File.separator + "request" + File.separator + moduleName + File.separator + className + "Request.java";
        }
        
        if (template.contains("Dao.java.vm")) {
            return packagePath + "dao" + File.separator + moduleName + File.separator + className + "Mapper.java";
        }
        
        if (template.contains("Service.java.vm")) {
            return packagePath + "service" + File.separator + moduleName + File.separator + className + "Service.java";
        }
        
        if (template.contains("ServiceImpl.java.vm")) {
            return packagePath + "service" + File.separator + moduleName + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }
        
        if (template.contains("Controller.java.vm")) {
            return packagePath + "controller" + File.separator + moduleName + File.separator + className + "Controller.java";
        }
        
        if (template.contains("Dao.xml.vm")) {
            return packagePath + "dao" + File.separator + moduleName + File.separator + className + "Mapper.xml";
            
        }
        
        
        return null;
    }
    
    
    /**
     * 创建多级目录文件
     *
     * @param path 文件路径
     * @throws IOException
     */
    private static File createFile(String path) {
        if (StringUtils.isNotEmpty(path)) {
            File file = new File(path);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            return file;
        }
        
        return null;
    }
    
    
}
