**项目说明** 
- 项目基于springboot2.0.6 可以作为项目的子模块，也可以单独部署
- generator是项目的代码生成器，可在线生成entity、xml、dao、service，controller 减少开发任务
**配置说明** 
-generator.properties
```
  mainPath=com.teemor
  #models的包名
  package=com.teemor.models
  #默认作者
  author=
  #默认Email
  email=
  #是否去除表名的前缀 tb_hotel 映射结果为Hotel HotelService
  removePrefix=true
  tablePrefix=tb_
  #不需要生成的实体字段（继承自公共实体类）,只能用断线连接
  excludeTableFiled=id-deleted
  #java 和mysql 类型的映射关系
  tinyint=Integer
  ......
```

  
 **本地部署**
- 通过git下载源码
- 修改application.yml，更新MySQL账号和密码、数据库名称
- Eclipse、IDEA运行GeneratorApplication.java，则可启动项目
- 项目访问路径：http://10.39.1.47:8989

**演示效果图：**
![输入图片说明](https://htdrp.oss-cn-hangzhou.aliyuncs.com/common/WX20190610-152603%402x.png "aa.jpg")