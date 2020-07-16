package com.testBatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class JobRunnerConfiguration {

	//Batch를 실행하기 위한 JobLauncherTestUtils를 Bean으로 등록
	@Bean
    public JobLauncherTestUtils utils(){
        return new JobLauncherTestUtils();
    }
	
}
