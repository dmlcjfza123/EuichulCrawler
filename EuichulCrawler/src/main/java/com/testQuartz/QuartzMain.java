package com.testQuartz;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.LinkedList;
import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.testQuartz.JobChaning.HelloJob;

public class QuartzMain {

	public static void main(String[] args) {

//		SchedulerFactory schedulerFactory = new StdSchedulerFactory();
//
//		try {
//			Scheduler scheduler = schedulerFactory.getScheduler();
//
//			JobDetail job = newJob(TestJob.class).withIdentity("jobName", Scheduler.DEFAULT_GROUP).build();
//
//			Trigger trigger = newTrigger().withIdentity("trggerName", Scheduler.DEFAULT_GROUP)
//					.withSchedule(cronSchedule("0/3 * * * * ?")).build();
//
//			scheduler.scheduleJob(job, trigger);
//			scheduler.start();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		/////////////////////////////////////////////////////////////////////////////
		
		//https://homoefficio.github.io/2018/08/12/Java-Quartz-Scheduler-Job-Chaining-%EA%B5%AC%ED%98%84/
		//<Chaining 할 여러 Job 생성>
		// JobDataMap을 이용해서 원하는 정보 담기
		// Job 1 구성
		// jobDetail에는 Job의 실제 구현 내용과 Job 실행에 필요한 제반 상세 정보가 담겨 있다.
		// Job 구현 내용이 담긴 HelloJob 으로 JobDetail 생성
		//(예제에서는 편의상 3개의 Job에 모두 HelloJob.class만을 사용)
		//JobBuilder API를 참고하면 Job마다 원하는 대로 식별자를 줄 수도 있고 오류 시 재실행 옵션 등 다양하게 설정할 수 있다.
	    JobDataMap jobDataMap1 = new JobDataMap();
	    jobDataMap1.put("JobName", "Job Chain 1");
	    JobDetail jobDetail1 = newJob(HelloJob.class)
                .usingJobData(jobDataMap1) // <- jobDataMap 주입
                .build();
	    
	    // Job 2 구성
        JobDataMap jobDataMap2 = new JobDataMap();
        jobDataMap2.put("JobName", "Job Chain 2");
        JobDetail jobDetail2 = newJob(HelloJob.class)
                .usingJobData(jobDataMap2)
                .build();

        // Job 3 구성
        JobDataMap jobDataMap3 = new JobDataMap();
        jobDataMap3.put("JobName", "Job Chain 3");
        JobDetail jobDetail3 = newJob(HelloJob.class)
                .usingJobData(jobDataMap3)
                .build();
        
        
        //1)Chaining 할 모든 Job 정보를 큐에 담고,
        // 실행할 모든 Job의 JobDetail를 jobDetail1의 JobDataMap에 담는다.
        List<JobDetail> jobDetailQueue = new LinkedList<>();
        jobDetailQueue.add(jobDetail1);
        jobDetailQueue.add(jobDetail2);
        jobDetailQueue.add(jobDetail3);
        
        //2)그 큐를 처음 실행되는 Job의 JobDataMap에 담은 후에,
        // 주의사항: 아래와 같이 jopDataMap1에 저장하면 반영되지 않는다.
        // jobDataMap1.put("JobDetailQueue", jobDetailQueue);
        // 아래와 같이 jobDetail1에서 getJobDataMap()으로 새로 가져온 JobDataMap에 저장해야 한다.
        jobDetail1.getJobDataMap().put("JobDetailQueue", jobDetailQueue);

        
        //trigger에는 Job을 언제, 어떤 주기로, 언제부터 언제까지 실행할지에 대한 정보가 담겨 있다.
        // 실행 시점을 결정하는 Trigger 생성
        //TriggerBuilder API를 참고하면 Trigger도 원하는 대로 더 다양하게 구성할 수 있다.
        Trigger trigger = newTrigger()
                .build();

        //scheduler는 jobDetail과 trigger에 담긴 정보를 이용해서 실제 Job의 실행 스케줄링을 담당한다.
        // 스케줄러 실행 및 JobDetail과 Trigger 정보로 스케줄링
        Scheduler defaultScheduler;
		try {
			defaultScheduler = StdSchedulerFactory.getDefaultScheduler();
			defaultScheduler.start();
			defaultScheduler.scheduleJob(jobDetail1, trigger);
			try {
				Thread.sleep(3 * 1000);
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
