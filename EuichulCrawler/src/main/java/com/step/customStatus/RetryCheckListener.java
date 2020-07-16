package com.step.customStatus;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.RetryListener;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;

public class RetryCheckListener extends StepExecutionListenerSupport implements RetryListener {
	public ExitStatus afterStep(StepExecution stepExecution) {
		String exitCode = stepExecution.getExitStatus().getExitCode();
		if (!exitCode.equals(ExitStatus.FAILED.getExitCode()) 
				&& stepExecution.getSkipCount() > 0) {
			stepExecution.get
			return new ExitStatus("COMPLETED WITH SKIPS");
		}
	}
}
