package com.testQuartz.notUsed;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.scheduling.quartz.QuartzJobBean;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class MyJobTwo extends QuartzJobBean {

	public static final String COUNT = "count";
	private String name;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		int cnt = dataMap.getInt(COUNT);
		JobKey jobKey = context.getJobDetail().getKey();
		System.out.println(jobKey + ": " + name + ": " + cnt);
		cnt++;
		dataMap.put(COUNT, cnt);

	}

	public void setName(String name) {
		this.name = name;
	}

}
