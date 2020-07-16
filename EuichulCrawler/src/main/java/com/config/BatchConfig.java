package com.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration//Spring Batch의 모든 Job은 configuration으로 등록해서 사용.
@EnableBatchProcessing // 자동으로 jobRepository, jobLauncher ,jobRegistry ,jobExplorer 이러한 것들을 @Bean으로 등록하지않아도 디폴트로 알아서 올려준다. 만약 커스터마이징 되있으면 만든 것을 직접 @Bean으로 등록하면된다.
@ComponentScan(basePackages = {"com.scheduler", "com.step"})
public class BatchConfig {

	//스케쥴을 @Bean화 시키는동시에 Start 로 걸어주면 was 를 구동함과 동시에 자동시작되게 할 수 있다.
	@Bean
	public Scheduler scheduler() throws SchedulerException {
		Scheduler defaultScheduler = StdSchedulerFactory.getDefaultScheduler();
		defaultScheduler.start();
		return defaultScheduler;
	}
	
}
