package com.config;

import java.io.File;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
@ComponentScan(basePackages = {"com.dao","com.jsoup"})
public class DBConfig {

	@Bean
	public DataSource dataSource() {
		BasicDataSource basic = new BasicDataSource();
		
		String url = "jdbc:mysql://portpolio.cyj2reko6bnu.ap-northeast-2.rds.amazonaws.com:3306/euichulportpolio?useUnicode=true&characterEncoding=utf8";
		
		basic.setUsername("admin");
		basic.setPassword("wjddmlcjf1!");
		basic.setDriverClassName("com.mysql.cj.jdbc.Driver");
		basic.setUrl(url);
		return basic;
	}

	@Bean
	public SqlSessionFactory sqlSessionFactory() throws Exception {
		SqlSessionFactoryBean sqlFac = new SqlSessionFactoryBean();
		sqlFac.setDataSource(dataSource());
		//xml에 mapper 여러개 등록시키는 방법 참고 https://atoz-develop.tistory.com/entry/MyBatis-%EC%84%A4%EC%A0%95-%ED%8C%8C%EC%9D%BC-SQL-Mapper-%EC%9E%91%EC%84%B1-%EB%B0%A9%EB%B2%95
		sqlFac.setConfigLocation(new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mybatis-config.xml")));
		sqlFac.setMapperLocations(new Resource[] {new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\Monster.xml")),
				new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\MonsterEvolution.xml")),
				new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\MonsterNormalSkill.xml")),
				new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\MonsterChainSkill.xml")),
				new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\MonsterLeaderSkill.xml")),
				new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\Tower.xml")),
				new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\TowerBoss.xml")),
				new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\TowerBossReward.xml")),
				new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\TowerFloor.xml")),
				new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\EuichulCrawler\\src\\main\\java\\com\\config\\mapper\\TowerFloorMonster.xml"))
				});
		//sqlFac.setMapperLocations(new Resource[] {new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\springEXAM\\src\\com\\config\\mapper\\User.xml"))});
		//sqlFac.setMapperLocations(new Resource[] {new FileSystemResource(new File("C:\\ictcbwd\\workspace\\Java\\NewEclipseWorkSpace\\springEXAM\\src\\com\\config\\mapper\\*.xml"))});
		//sqlFac.setConfigLocation(new ClassPathResource("classpath:WEB-INF/classes/com/config/mybatis-config.xml"));
		//sqlFac.setMapperLocations(new Resource[] {new ClassPathResource("classpath:WEB-INF/classes/com/config/GuestBook.xml")});
		return sqlFac.getObject();
	}
	
	@Bean
	public SqlSession sqlSession() throws Exception {
		return new SqlSessionTemplate(sqlSessionFactory());
	}
	
	@Bean
	public DataSourceTransactionManager dataSourceTransactionManager() {
		return new DataSourceTransactionManager(dataSource());
	}
}
