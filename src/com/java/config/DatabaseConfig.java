package com.java.config;

import java.io.IOException;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@PropertySource(value="classpath:/database.properties")
@EnableTransactionManagement(proxyTargetClass=false)
public class DatabaseConfig {

	@Autowired Environment env;
	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource ds= new DriverManagerDataSource();
		ds.setUrl(env.getProperty("jdbc.url"));
		ds.setPassword(env.getProperty("jdbc.password"));
		ds.setUsername(env.getProperty("jdbc.username"));
		ds.setDriverClassName(env.getProperty("jdbc.driverClassName"));
		return ds;
	}
	
	@Bean("entityManagerFactory")
	public EntityManagerFactory emFactory() throws IOException {
		LocalContainerEntityManagerFactoryBean bean= new LocalContainerEntityManagerFactoryBean();
		bean.setJpaProperties(hibernateProperties());
		bean.setDataSource(dataSource());
		bean.setPackagesToScan("com.java.dto");
		bean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		bean.afterPropertiesSet();
		return bean.getObject();
	}

	@Bean
	public Properties hibernateProperties() {
		Properties properties= new Properties();
		properties.put(org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create");
		properties.put(org.hibernate.cfg.Environment.SHOW_SQL, "true");
		properties.put(org.hibernate.cfg.Environment.DIALECT, "org.hibernate.dialect.PostgreSQL9Dialect");
		properties.put(org.hibernate.cfg.Environment.AUTOCOMMIT, "false");
		return properties;
	}
	
	@Bean
	public JpaTransactionManager transactionManager() throws IOException {
		JpaTransactionManager tx= new JpaTransactionManager();
		tx.setEntityManagerFactory(emFactory());
		return tx;
	}
}
