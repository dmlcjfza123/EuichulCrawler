package com.springBatchAndQuartz;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Collection;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.config.JobConfig;


/*
 * 주의
 * WAS 로 돌릴때 이 main은 Batch 돌아가는 로직이랑 아무 상관없음. 그냥 메인에서 돌려보고 싶어서 했었던것일뿐임.
 * 여기다 스레드 써도 무용지물임.
 */

//메인함수에서 spring Batch + quartz 돌려보기.
public class BatchAndQuartzRunner {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JobConfig.class);
		String[] strArr= context.getBeanDefinitionNames();
		for (String string : strArr) {
			System.out.println(string);
		}
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		JobRegistry jobRegistry = context.getBean(JobRegistry.class);
		
		System.out.println("///////////////////////////////////////////");
		Collection<String> strArray= jobRegistry.getJobNames();
		for (String string : strArray) {
			System.out.println(string);
		}
		
		JobDataMap jobDataMap = new JobDataMap();
		//jobDataMap.put("job", "simple-job");
		jobDataMap.put("job", context.getBean("job"));
		jobDataMap.put("jobLauncher", jobLauncher);
		jobDataMap.put("jobRegistry", jobRegistry);
		JobDetail jobDetail = newJob(QuartzSimpleJob.class)
	                .usingJobData(jobDataMap)
	                .build();
		
		 Trigger trigger = newTrigger()
	                .build();

        //scheduler는 jobDetail과 trigger에 담긴 정보를 이용해서 실제 Job의 실행 스케줄링을 담당한다.
        // 스케줄러 실행 및 JobDetail과 Trigger 정보로 스케줄링
        Scheduler defaultScheduler;
		try {
			defaultScheduler = StdSchedulerFactory.getDefaultScheduler();
			defaultScheduler.start();
			defaultScheduler.scheduleJob(jobDetail, trigger);
			try {
				Thread.sleep(3 * 10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  // Job이 실행될 수 있는 시간 여유를 준다
			
			// 스케줄러 종료
			defaultScheduler.shutdown(true);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
