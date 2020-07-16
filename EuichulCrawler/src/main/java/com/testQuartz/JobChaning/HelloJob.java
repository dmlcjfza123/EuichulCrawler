package com.testQuartz.JobChaning;

import java.util.Date;

import org.quartz.JobExecutionContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelloJob extends BaseJob{
	 @Override
	    protected void doExecute(JobExecutionContext context) {
	       // log.info("### Hello Job is being executed!");
		 System.out.println("Job Executed [" + new Date(System.currentTimeMillis()) + "]");
		 //HelloJob 클래스도 JobDataMap에 담긴 정보를 사용 : Job Chain 1 이 콘솔에 찍힌다.
		 System.out.println(context.getJobDetail().getJobDataMap().get("JobName").toString());
	    }
}
