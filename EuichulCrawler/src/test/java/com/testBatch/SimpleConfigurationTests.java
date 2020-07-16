package com.testBatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.config.JobConfig;

@RunWith(SpringJUnit4ClassRunner.class)
//BatchConfig, JobRunnerConfiguration을 @Contextconfiguration을 이용해 테스트에 필요한 Config Bean으로 등록
@ContextConfiguration(classes = { JobConfig.class, JobRunnerConfiguration.class})
public class SimpleConfigurationTests {

	//Bean으로 등록된 JobLauncherTestUtils를 주입(@Autowired) 받아 Batch Job(BatchConfig)을 실행
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    public void testLaunchJob() throws Exception {
    	//이때 JobLauncherTestUtils은 SimpleJobLauncher를 이용해 Batch Job을 실행
        jobLauncherTestUtils.launchJob();
    }
    
}