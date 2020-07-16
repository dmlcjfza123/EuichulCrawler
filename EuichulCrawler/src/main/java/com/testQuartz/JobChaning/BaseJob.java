package com.testQuartz.JobChaning;

import static org.quartz.TriggerBuilder.newTrigger;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

/*
 * 연속 실행 기능을 가질 추상 클래스인 BaseJob을 만들고, 
 * 실제 구현 내용을 담은 HelloJob은 BaseJob을 상속하게 만든다.
 * 
 * BaseJob에 템플릿 메서드 패턴을 적용해서 
 * Job 실행 전처리, Job 실행, 후처리, 다음 Job Scheduling이라는 파이프라인을 구성한다.
 */
public abstract class BaseJob implements Job {

	// execute() 메서드에 넘겨지는 JobExecutionContext에는 Job 실행에 필요한 다양한 정보를 담을 수 있다.
	// 그 중에서도 JobDataMap을 이용하면 자유롭게 Key-Value 데이터를 담을 수 있다.
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		beforeExecute(context); // Job 실행 전처리
		doExecute(context); // Job 실행
		afterExecute(context); // 후처리
		scheduleNextJob(context);// 다음 Job Scheduling
	}

	private void beforeExecute(JobExecutionContext context) {
		System.out.println("%%% Before executing job");
	}

	protected abstract void doExecute(JobExecutionContext context);

	private void afterExecute(JobExecutionContext context) {
		System.out.println("%%% After executing job");
		// 3)Job 실행이 완료되면 후처리 단계에서 실행이 완료된 Job을 큐에서 하나씩 빼주고,
		Object object = context.getJobDetail().getJobDataMap().get("JobDetailQueue");
		List<JobDetail> jobDetailQueue = (List<JobDetail>) object;

		if (jobDetailQueue.size() > 0) {
			jobDetailQueue.remove(0);
		}
	}

	private void scheduleNextJob(JobExecutionContext context) {
		System.out.println("$$$ Schedule Next Job");
		// 4)다음 Job을 실행할 때 그 큐를 다음 Job의 JobDataMap에 넣어주고 스케줄링
		Object object = context.getJobDetail().getJobDataMap().get("JobDetailQueue");
		List<JobDetail> jobDetailQueue = (List<JobDetail>) object;

		if (jobDetailQueue.size() > 0) {
			// 완료된 Job이 제거된 큐를 JobDataMap에 담고 즉시 실행하는 Trigger를 만들어서 스케줄링 한다.
			JobDetail nextJobDetail = jobDetailQueue.get(0);
			nextJobDetail.getJobDataMap().put("JobDetailQueue", jobDetailQueue);

			Trigger nowTrigger = newTrigger().startNow().build();

			try {
				// 아래의 팩토리 메서드는 이름이 같으면 여러번 호출해도 항상 동일한 스케줄러를 반환한다.
				Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
				//scheduler.start();
				scheduler.scheduleJob(nextJobDetail, nowTrigger);
			} catch (SchedulerException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
