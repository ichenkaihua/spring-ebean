# Spring-ebean
ebean-orm是个神奇的框架，他功能强大，又灵活多变，还保持了轻量级的体积。简单来讲，他相比于hibernate来说，他是小巧灵活的；相比mybatis来说，他支持jpa标准，意味着可以方便的使用注解，但他也是支持mybatis式的源生sql。个人认为，
中小项目使用ebean-orm比其他框架省心。具体使用方式，请上[ebean-orm官网][1]查看。
## 为何有这篇博客
spring是个非常优秀的框架，ebean-orm也是个优秀的开源项目，但知名度相对于hibernate和mybatis小得多。spring能否集成ebean-orm？当然能！虽然ebean官网提供了教程，但是教程非常简短，有些坑还得留意。
## 框架与开发工具
* 框架:spring4+springmvc4+ebean-orm6
* 开发环境:idea,最智能的java开发工具
* 构建工具:gradle，最灵活的构建工具

## 项目配置

### 包结构
> com.chenkaihua.springebean.entity:实体映射类<br>
> com.chenkaihua.springebean.controller:前端控制器，处理请求<br>
> com.chenkaihua.springebean.service:处理逻辑，事物在这里起作用

### gradle配置
`build.gradle`是gradle项目的配置文件，和maven的`pom.xml`类似，在`build.gradle`里管理依赖，设置插件，甚至直接调用`java api`，总之，非常强大。
ebeaon-orm有个麻烦点的地方，叫`Enhancement操作`，这个`Enhancement操作`用于修改实体bean，如果项目没有`Enhancement`，那启动时会报错`xxx.User not Enhancement ?`。`ebean-orm`提供了多种方式`Enhancement`，其中有ant方式`Enhancement`，gradle完全支持ant，因此就有必要通过ant完成自动`Enhancement`操作

1. 在`build.gradle`里添加一个`configuration`，这相当于自定义了一个依赖组，`compile`、`testCompile`就是两个`configuration`
2. 然后在`dependencies`闭包里定义这个依赖组的依赖,依赖`org.avaje.ebeanorm:avaje-ebeanorm-agent:4.7.1`
3. 定义一个ant task，用于` enhancement`
4. 定义一个闭包`ebeanEnhance`，用于调用ant的ebeanEnhance task
5. 配置在编译完java类之后，调用闭包，完成`ebeanEnhance`

代码如下:

```gradle
  configurations{
      ebeanagent
  }
  dependencies{
    ebeanagent 'org.avaje.ebeanorm:avaje-ebeanorm-agent:4.7.1'
  }
  def ebeanEnhance = {dir, packages ->
      println dir
      println packages
      println '============================================'
      println '  Enhance ebean classes....' + dir
      println '============================================'
      ant.ebeanEnhance(classSource: dir,
              packages: packages,
              transformArgs: "debug=10")
      println 'Enhance ebean end....................'
  }
  
  compileJava.doLast {
      ebeanEnhance(destinationDir, "com.chenkaihua.springebean.entity.*")
  }
```
### spring配置
两个spring配置文件
* `spring-config.xml`:用于root context
* `spring-mvc-config.xml`:用于mvc context
这里只讲`spring-config`部分，其他可参照源码。
`spring-config`:包含了`spring-ebean.xml`配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:context="http://www.springframework.org/schema/context"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:tx="http://www.springframework.org/schema/tx"
	xmlns:aop="http://www.springframework.org/schema/aop"
	xsi:schemaLocation="
        http://www.springframework.org/schema/beans     
        http://www.springframework.org/schema/beans/spring-beans-4.2.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context-4.2.xsd
        http://www.springframework.org/schema/aop
        http://www.springframework.org/schema/aop/spring-aop-4.1.xsd
        http://www.springframework.org/schema/tx
        http://www.springframework.org/schema/tx/spring-tx-4.1.xsd
        ">

	<!-- 扫描注解，除去web层注解，web层注解在mvc配置中扫描 -->
	<context:component-scan
		base-package="com.chenkaihua.springebean.service">
		<context:exclude-filter type="annotation"
			expression="org.springframework.stereotype.Controller" />
		<context:exclude-filter type="annotation"
			expression="org.springframework.web.bind.annotation.RestController" />
	</context:component-scan>

	<!-- 开启AOP监听 只对当前配置文件有效 -->
	<aop:aspectj-autoproxy expose-proxy="true" proxy-target-class="true" />


	<tx:annotation-driven transaction-manager="transactionManager"  />

	<import resource="spring-ebean.xml"/>
</beans>
```
`spring-ebean`集成了spring事物，由spring管理事物
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xmlns:tx="http://www.springframework.org/schema/tx"
	   xmlns:aop="http://www.springframework.org/schema/aop"
	   xsi:schemaLocation="
        http://www.springframework.org/schema/beans     
        http://www.springframework.org/schema/beans/spring-beans-4.1.xsd
        http://www.springframework.org/schema/aop
        http://www.springframework.org/schema/aop/spring-aop-4.1.xsd
        http://www.springframework.org/schema/tx
        http://www.springframework.org/schema/tx/spring-tx-4.1.xsd
        ">



	
	<bean id="dataSource" class="org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy">
		<constructor-arg>
			<bean class="org.springframework.jdbc.datasource.SimpleDriverDataSource">
				<property name="driverClass" value="org.h2.Driver" />
				<property name="url"
						  value="jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1" />
			</bean>
		</constructor-arg>
	</bean>




	<!--  Transaction Manager -->
	<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
		<property name="dataSource" ref="dataSource"/>
	</bean>

	<bean id="serverConfig" class="com.avaje.ebean.config.ServerConfig">
		<property name="externalTransactionManager">
			<bean class="com.avaje.ebean.springsupport.txn.SpringAwareJdbcTransactionManager"/>
		</property>
		<property name="defaultServer" value="true"/>

		<property name="namingConvention">
			<bean class="com.avaje.ebean.config.UnderscoreNamingConvention"/>
		</property>
		<property name="name" value="ebeanServer"/>

		<property name="packages">
			<list>
				<value>com.chenkaihua.springebean.entity</value>
			</list>
		</property>

		<property name="dataSource" ref="dataSource"/>
		<!--<property name="disableClasspathSearch" value="true"/>-->
		<!--是否生成sql文件-->
		<property name="ddlGenerate" value="true"/>
		<!--时候启动时读取sql文件，并执行-->
		<property name="ddlRun" value="true"/>
	</bean>

	<!-- Ebean server -->
	<bean id="ebeanServer" class="com.avaje.ebean.springsupport.factory.EbeanServerFactoryBean">
		<property name="serverConfig" ref="serverConfig"/>
	</bean>

	<aop:aspectj-autoproxy  />

	<aop:config>
		<aop:pointcut id="appService"
					  expression="execution(* com.chenkaihua.springebean..*Service*.*(..))" />
		<aop:advisor advice-ref="txAdvice" pointcut-ref="appService" />
	</aop:config>

	<tx:advice id="txAdvice" transaction-manager="transactionManager">
		<tx:attributes>
			<tx:method name="select*" read-only="true" />
			<tx:method name="find*" read-only="true" />
			<tx:method name="get*" read-only="true" />
			<tx:method name="*" />
			<tx:method name="sava*"  />
			<tx:method name="update*" />
			<tx:method name="delete*" />
		</tx:attributes>
	</tx:advice>


</beans>
```
这里需要**注意**的是，`transactionManager`里的事物必须是`org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy`，`LazyConnectionDataSourceProxy`对真实的datasource进行代理，以前没注意到这点，导致事物一直不起作用。
## 使用`ebean-orm`
```java
package com.chenkaihua.springebean.service;

import com.avaje.ebean.EbeanServer;
import com.chenkaihua.springebean.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by chenkaihua on 15-10-29.
 */
@Service
public class UserService {


    @Autowired
    EbeanServer ebeanServer;

    public void save(User user){
        ebeanServer.save(user);
    }

    public void saveOnThrowException(User user){
        ebeanServer.save(user);
        throw new IllegalArgumentException("非法参数！！");
    }


    public List<User> users(){
        return ebeanServer.find(User.class).findList();
    }

}

```
如果controller调用`saveOnThrowException`方法，则会抛出异常，事物回滚，说明spring事物起作用了。
## 项目运行
```groovy
# 启动项目
./gradlew appStart
# 插入数据
curl http://localhost:8080/users/save
# 插入数据（抛出exception，事物回滚）
curl http://localhost:8080/users/saveE
# 浏览所以数据
curl http://localhost:8080/users
# 关闭server
./gradlew appStop
```

[1]:http://http://ebean-orm.github.io/