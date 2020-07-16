package com.scheduler;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
//import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.quartz.Trigger;
import org.quartz.CronTrigger;
import static org.quartz.CronScheduleBuilder.cronSchedule;

import com.springBatchAndQuartz.QuartzSimpleJob;

//@Component 화 시킴으로인해서, @PostConstruct 가 실행될때, 만들어져있는 job들이 모두 스케줄에  자동으로 알아서 걸리게 할수있다.
@Component
public class SchedulerRegistry {
	//jobLauncher 와 JobRegistry는 이미  @EnableBatchProcessing 어노테이션에 의해서 자동으로 디폴트 애들이 올라간다.
	//따라서 Bean화 시켜주지 않아도 @Resource 로 자동주입 가능하다.
	@Resource
	private JobLauncher jobLauncher;
	@Resource
	private JobRegistry jobRegistry;
	@Resource
	private Scheduler scheduler;
	
	
	//JobRegistry에 등록된 job 들은 아래 for문에서 이름값으로 가져와 돌려주면 되는데,
	//지금 jobRegistry 에 job은 등록이 되는데, job에 설정한 이름이 등록되지 않아서, 이름으로 가져오기가 어렵기때문에,
	// @Bean 으로 등록되어있는 job을 @Resource 로 자동주입받아, 동적으로 registJob 시켜주어 스케줄에 job을 등록시켜주면 된다.
	@Resource
	private Job job;
	
	@PostConstruct
	public void Init() throws SchedulerException, NoSuchJobException {
		//원래 이로직으로 돌리는게 맞는데 위에 말한 이유때문에 잡을 주입 받아서 돌리고있음.
		System.out.println("PostConstruct Init() ...");
		/*
		 * for(String jobName : jobRegistry.getJobNames()) { 
		 * JobDataMap jobDataMap = new JobDataMap(); 
		 * 
		 * jobDataMap.put("job", jobRegistry.getJob(jobName)); 
		 * jobDataMap.put("jobLauncher", jobLauncher);
		 * jobDataMap.put("jobRegistry", jobRegistry); 
		 * JobDetail jobDetail = newJob(QuartzSimpleJob.class) 
		 * 						.usingJobData(jobDataMap) .build();
		 * 
		 * Trigger trigger = newTrigger() .build();
		 * 
		 * scheduler.scheduleJob(jobDetail, trigger); }
		 */
		
//		Trigger trigger = newTrigger()
//                .build();
//		registJob(job, trigger);
		
		//크론표현식을 써보기위해 CronTrigger 써보기.
		//CronTrigger cronTrigger = new CronTrigger("0/5 * * * * ?");//5초마다
		Trigger cronTrigger = newTrigger().withSchedule(cronSchedule("0/5 * * * * ?")).build();
		//CronTrigger cronTrigger = new CronTrigger("trigger1","group1","0/5 * * * * ?");//5초마다
		registJob(job, cronTrigger);
	}
	
	//was 가실행중일때, 버튼을누른다면 해당 메소드를 호출시켜 동적으로 job을 걸어줄수있다.
	public void registJob(Job job, Trigger trigger) throws SchedulerException {
		System.out.println("registJob() ...");
		JobDataMap jobDataMap = new JobDataMap();
		
		//지금은 주입받은 job 오브젝트를 넣어줄것이기때문. 만약 jobRegistry의 이름등록 문제가 해결된다면 job이라는 key에 job의 이름을 value로 등록시켜주면되겠다.
		jobDataMap.put("job", job); //jobDataMap.put("job", job.getName());
		jobDataMap.put("jobLauncher", jobLauncher);
		jobDataMap.put("jobRegistry", jobRegistry);
		JobDetail jobDetail = newJob(QuartzSimpleJob.class)
	                .usingJobData(jobDataMap)
	                .build();
		scheduler.scheduleJob(jobDetail, trigger);
	}
	
	public void registJob(Job job, CronTrigger trigger) throws SchedulerException {
		System.out.println("registJob() ...");
		JobDataMap jobDataMap = new JobDataMap();
		
		//지금은 주입받은 job 오브젝트를 넣어줄것이기때문. 만약 jobRegistry의 이름등록 문제가 해결된다면 job이라는 key에 job의 이름을 value로 등록시켜주면되겠다.
		jobDataMap.put("job", job); //jobDataMap.put("job", job.getName());
		jobDataMap.put("jobLauncher", jobLauncher);
		jobDataMap.put("jobRegistry", jobRegistry);
		JobDetail jobDetail = newJob(QuartzSimpleJob.class)
	                .usingJobData(jobDataMap)
	                .build();
		scheduler.scheduleJob(jobDetail, (Trigger) trigger);
	}
}
