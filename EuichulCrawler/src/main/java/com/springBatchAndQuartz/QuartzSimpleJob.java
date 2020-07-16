package com.springBatchAndQuartz;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.LocalDateTime;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

//Quartz 에서 Quartz 용 잡 을 구현시켜주는데, 이 잡안에서는 스프링 배치의 잡을 구동신켜주면 Spring Batch +  Quartz 가 완성된다.
public class QuartzSimpleJob implements Job {
	
	private static int Count = 0; 
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		System.out.println("QuartzSimpleJob execute...");
		
		//원래는 jobRegistry에 등록된 이름(value) 를 가져와야하기때문에, job으로 key를 get 하는거지만, 지금 JobRegistry 이슈때문에 가져와 지지가 않음. 따라서 job에 value를 현재 string으로 등록된 이름이 아니라, object 로 job 자체를 넘겨주고 잇다. 
		//String jobName = (String) context.getJobDetail().getJobDataMap().get("job");
		JobLauncher jobLauncher = (JobLauncher)context.getJobDetail().getJobDataMap().get("jobLauncher");
		//JobRegistry jobRegistry = (JobRegistry)context.getJobDetail().getJobDataMap().get("jobRegistry");
		
		try {
			//jobLauncher 로 run 시킬때, 잡파라미터를 넘겨줘야하는데, null로 넘기면 아무동작도 하지않는다. null 인식이안됨. 따라서 기본 디폴트로 넘겨주기위해 getUniqueJobParameters 호출시켜 넘겨준다. 이로직은 실제 개발자들이 짠 로직 그냥 컨씨컨븨 한것이다.
			System.out.println("돌아라돌아라" + (++Count));
			if((org.springframework.batch.core.Job) context.getJobDetail().getJobDataMap().get("job") != null) {
				org.springframework.batch.core.Job check = (org.springframework.batch.core.Job) context.getJobDetail().getJobDataMap().get("job");
				System.out.println("job 이름 : " + check.getName());
			}
			jobLauncher.run((org.springframework.batch.core.Job) context.getJobDetail().getJobDataMap().get("job"), getUniqueJobParameters());
		} catch (JobExecutionAlreadyRunningException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JobRestartException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JobInstanceAlreadyCompleteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JobParametersInvalidException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	//잡 파라미터에 아무값이나 랜덤으로 넣어서 돌려주는 로직인가보다. 실제개발자가 만든거라 왜이런지까진 모름.
	public JobParameters getUniqueJobParameters() {
		Map<String, JobParameter> parameters = new HashMap<>();
		//parameters.put("random", new JobParameter((long) (Math.random() * 1000000)));
		parameters.put("version", new JobParameter(LocalDateTime.now().toString()));
		return new JobParameters(parameters);
	}

}
