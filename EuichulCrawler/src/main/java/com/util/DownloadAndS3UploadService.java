package com.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Queue;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.IOUtils;
import com.dto.DownloadAndS3UploadDto;

@WebListener
public class DownloadAndS3UploadService implements Runnable, ServletContextListener {

	@Autowired
	private Queue<DownloadAndS3UploadDto> mcMessageQueue;

	private boolean stopFlag = true;
	private boolean isCompelete = false;
	
	private Thread thread;
	
	public void init() {
		thread = new Thread(this);
		thread.start();
	}
	
	//웹어플리케이션 시작시
	public void contextInitialized(ServletContextEvent sce) {
		
	}

	//웹어플리케이션 종료시
	public void contextDestroyed(ServletContextEvent sce) {
		stopFlag = false;
		while(isCompelete) {
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void interrupte() {
		thread.interrupt();
	}
	
	
	@Override
	public void run() {

		DownloadAndS3UploadDto dto = null;

		while(stopFlag) {
			isCompelete = true;
			//synchronized (mcMessageQueue) {
				if(mcMessageQueue.size() == 0) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
				}
				else {
					dto = mcMessageQueue.poll();
				}
			//}
			
			//sync 가 없다면 else 안으로 들어가도 상관없음.
			if(dto != null) {
				// 다운로드
				try {
					URL imgUrl = new URL(dto.getUrl());

					HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();

					InputStream is = conn.getInputStream();
					BufferedInputStream bis = new BufferedInputStream(is);

					FileOutputStream fos = new FileOutputStream(dto.getDownloadPath());
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					int byteImg;

					byte[] buf = new byte[conn.getContentLength()];
					while ((byteImg = bis.read(buf)) != -1) {
						bos.write(buf, 0, byteImg);
					}

					bos.close();
					fos.close();
					bis.close();
					is.close();
				} catch (IOException ioEx) {
					//ioEx.printStackTrace();
				}

				// 업로드
				AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
						.withCredentials(new EnvironmentVariableCredentialsProvider()).withRegion(Regions.AP_NORTHEAST_2)
						.build();

				// 가져올 파일경로
				String newPath = dto.getDownloadPath();

				File file = new File(newPath);

				// putObject 메소드로 (만들어놓은 Bucket의이름, bucket안에 저장될경로, 업로드할파일변수) 업로드하기.
				// putObejct의 문제점 : 파일쓰기작업은 매우 무거운 작업이다. 업로드할곳은 S3인데 로컬에도 파일이 저장되기때문에 쓸데없이 자원이
				// 낭비된다.
				// 문제점 해결 inputStream을 받는 putObject 메소드 활용하기.
				FileInputStream input;
				try {
					input = new FileInputStream(file);
					MultipartFile multipartFile;

					// File 을 multipartFile 로 변환.
					multipartFile = new MockMultipartFile("file", file.getName(), "image/jpeg", IOUtils.toByteArray(input));

					ObjectMetadata metadata = new ObjectMetadata();
					metadata.setContentType(MediaType.IMAGE_JPEG_VALUE);
					metadata.setContentLength(multipartFile.getSize());

					// inputStream을 매개변수로 받는 경우, metadata를 보내야한다.
					// 이유 : inputStream을 통해 Byte만 전달되기때문에, 해당파일에대한 정보가 없기때문이다. 따라서, metadata를 전달해
					// 파일에대한 정보를 추가로 전달해줘야한다.
					// 폴더명을 몬스터이름으로 안하는이유 : 몬스터이름뒤에 . 이 붙으면 폴더 만들어질때 . 은 안써짐. 그런데 몬스터이름..jpg 이렇게
					// 저장되서 폴더명이랑 파일명이 달라져버림. 따라서 폴더명에 몬스터이름 넣으면안됨.
					// 업로드할 파일경로
					String upLoadPath = dto.getUploadPath();
					// System.out.println("업로드 할 파일 이름 : " + dto.getMonsterName() + ".jpg");
					s3Client.putObject("euichul-pocorong-imgs", upLoadPath, multipartFile.getInputStream(), metadata);

				} catch (IOException ioEx) {
					//ioEx.printStackTrace();
				}
				finally {
					dto = null;
				}
			}
			isCompelete = false;
		}
		
		
		//자원정리
		

	}

}
