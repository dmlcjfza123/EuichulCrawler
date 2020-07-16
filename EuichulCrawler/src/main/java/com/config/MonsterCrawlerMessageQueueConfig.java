package com.config;

import java.util.LinkedList;
import java.util.Queue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dto.DownloadAndS3UploadDto;
import com.util.DownloadAndS3UploadService;

@Configuration
public class MonsterCrawlerMessageQueueConfig {
	
//	 Queue
//
//	 - import java.util.*;
//
//	 - Queue<T> queue = new LinkedList<>();
//
//	 - add() : 큐에 삽입
//
//	 - peek() : 가장 먼저 큐에 들어간 요소 반환
//
//	 - remove() : 가장 먼저 큐에 들어간 요소 삭제하면서 반환
//
//	 - isEmpty() : 큐가 비어있는지 반환
//
//	 - size() : 큐에 있는 요소의 크기 반환
	
	@Bean
	public Queue<DownloadAndS3UploadDto> MCMessageQueue(){
		Queue<DownloadAndS3UploadDto> messageQueue = new LinkedList<>();
		return messageQueue;
	}
	
	@Bean
	public Thread DownloadAndS3UploadServiceThread() {
		DownloadAndS3UploadService downloadAndS3UploadService = new DownloadAndS3UploadService();
		Thread t1Thread = new Thread(downloadAndS3UploadService);
		t1Thread.start();
		return t1Thread;
	}
	
	@Bean
	public DownloadAndS3UploadService DownloadAndS3UploadServiceRunnable() {
		DownloadAndS3UploadService downloadAndS3UploadService = new DownloadAndS3UploadService();
		//Thread t1 = new Thread(downloadAndS3UploadService);
		//t1.start();
		//Thread t2 = new Thread(downloadAndS3UploadService);
		//t2.start();
		downloadAndS3UploadService.init();
		return downloadAndS3UploadService;
	}
	
}
