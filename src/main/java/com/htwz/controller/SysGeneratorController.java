

package com.htwz.controller;

import com.alibaba.fastjson.JSONObject;
import com.htwz.service.SysGeneratorService;
import com.htwz.utils.PageUtils;
import com.htwz.utils.Query;
import com.htwz.utils.R;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 代码生成器
 *
 * @author lujing
 */
@Controller
@RequestMapping("/sys/generator")
public class SysGeneratorController {
    @Autowired
    private SysGeneratorService sysGeneratorService;
    
    /**
     * 列表
     */
    @ResponseBody
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils pageUtil = sysGeneratorService.queryList(new Query(params));
        
        return R.ok().put("page", pageUtil);
    }
    
    /**
     * 生成代码
     */
    @RequestMapping("/code")
    public void code(
            String tables,
            HttpServletResponse response,
            String modelName,
            String databaseName
    
    ) throws IOException {
        byte[] data = sysGeneratorService.generatorCode(tables.split(","), modelName, correctDataBaseName(databaseName));
        if(data.length ==0){
            response.setContentType("application/json; charset=UTF-8");
            R r = R.ok().put("tables", tables);
            IOUtils.write(JSONObject.toJSONBytes(r), response.getOutputStream());
            return;
        }
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"code.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        
        IOUtils.write(data, response.getOutputStream());
    }

    private String correctDataBaseName(String dataBaseName){
        if (dataBaseName == null){
            return null;
        }
        if ("null".equals(dataBaseName)){
            return null;
        }
        return dataBaseName;
    }
}
