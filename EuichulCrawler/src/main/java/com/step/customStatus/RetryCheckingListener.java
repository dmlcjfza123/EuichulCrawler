package com.step.customStatus;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;

public class RetryCheckingListener extends StepExecutionListenerSupport {
	 public ExitStatus afterStep(StepExecution stepExecution) {
		 
		 
	 }
}
