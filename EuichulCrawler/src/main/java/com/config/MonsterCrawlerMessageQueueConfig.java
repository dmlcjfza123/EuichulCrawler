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
	
	//스레드 구현방식 version1. 스레드 자체를 Bean화 하면 사용자가 정의한 Runnable 인터페이스 부분을 컨트롤하기가 힘드므로 지양한다.
//	@Bean
//	public Thread DownloadAndS3UploadServiceThread() {
//		DownloadAndS3UploadService downloadAndS3UploadService = new DownloadAndS3UploadService();
//		Thread t1Thread = new Thread(downloadAndS3UploadService);
//		t1Thread.start();
//		return t1Thread;
//	}
	
	//스레드 구현방식 version2. 스레드 자체를 Bean화 하지않고, Runnable을 구현한 클래스를 Bean화 함으로써 Thread를 필드변수로 잡아놓으면 기타 사용자정의사항까지 부여해서 컨트롤할 수 있으므로 지향한다.
	//즉, Runnable을 구현한 클래스에서 Thread를 직접 돌리는 방식을 추천한다.
	@Bean
	public DownloadAndS3UploadService DownloadAndS3UploadServiceRunnable() {
		DownloadAndS3UploadService downloadAndS3UploadService = new DownloadAndS3UploadService();
		//Thread t1 = new Thread(downloadAndS3UploadService);
		//t1.start();
		//Thread t2 = new Thread(downloadAndS3UploadService);
		//t2.start();
		//스레드 생성및 시작을 Runnable을 구현한 클래스에서 진행한다. (스레드 생성및 작동방식은 취향을 탄다. 이방법말고도 생성자를 만들어서 스레드생성 및 start하는 방식도 있다.)
		downloadAndS3UploadService.init();
		return downloadAndS3UploadService;
	}
	
}
