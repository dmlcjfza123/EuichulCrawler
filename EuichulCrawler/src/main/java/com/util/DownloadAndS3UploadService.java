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
import com.jsoup.MonsterCrawler;

//리스너는 필요한곳마다 달아주는것보다, 따로 리스너를 상속받을 클래스를 지정한뒤 구현해서 종료시 실행되어야하는 사항들을 모두 한곳에서 제어해주는것이 관점지향방식에서 좋다.(단일책임원칙?)
@WebListener
public class DownloadAndS3UploadService implements Runnable, ServletContextListener {

	@Autowired
	//Runnable클래스에서는 static 선언을 할필요가없다. static(글로벌)하게 선언하지않고 IOC컨테이너에서 제공해주는 Bean을 바탕으로 작업하면되니까 static이 들어갈 필요가 없다.
	private Queue<DownloadAndS3UploadDto> mcMessageQueue;

	//run thread 제어
	private boolean stopFlag = true;
	//was thread 제어
	private boolean isCompelete = false;
	
	
	//스레드 생성및 시작을 러너블에서 진행한다.
	private Thread thread;
	
	//Runnable에서 직접 스레드까지 생성해서 돌리는방식.
	public void init() {
		//멀티스레드로 병렬처리하고싶으면 이부분에서 스레드를 여러개 생성해서 돌리면됨. 하지만 스레드를 여러개할경우 스레드풀을 권장한다.
		thread = new Thread(this);
		thread.start();
	}
	
	//웹어플리케이션 시작시
	public void contextInitialized(ServletContextEvent sce) {
		
	}

	//웹어플리케이션 종료시 - 들어오는 리스너, 아직 WAS가 종료되지않았고 종료될거라는 신호를 알려주는 리스너 함수.
	public void contextDestroyed(ServletContextEvent sce) {
		
		//더이상 큐에 Add 되지 못하도록 제어변수의 값을 false로 바꿔준다.
		MonsterCrawler.setMQAddFlagChange();
		
		stopFlag = false;
		//웹어플리케이션 종료시 진행중이던 처리건을 처리한뒤에 종료될 수 있도록 디스트로이를 날리는 was의 스레드에 sleep으로 인터럽트를 건다. - WAS 종료를 잡아두기위함.
		while(isCompelete) {
			try {
				//이 Thread 명령어는 WAS에서 제공하는 스레드에관한 sleep 명령이다. 우리가  필드변수로 제어하는 thread에대한 제어가 아니다. 필드변수로 선언한 스레드 Thread. 제어는 run 안에서만 작동한다.
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				//큐에서 처리못한 데이터 즉, 큐에 담겨있는 데이터가 남아있으면 특정파일에 데이터를 모두 담아놓고, 위 함수인 contextInitialized 에서 다시 큐에 해당 파일에대한 내용을 읽어서 넣은뒤 이 자료부터 처리하게 하는 방법이있고,
				//다른방법으로는, contextDestroyed 신호가 왔을때 큐에담겨있는 데이터를 모두 처리한뒤에 끝나게끔 로직을 짜주면된다. 단, 큐에 데이터를 넣는 입력부에서 더이상 넣지못하게 플래그를 넣어준다음, 남은데이터만 처리할수있게 해줘야한다. 
			}
		}
	}
	
	//contextDestroyed에서 이함수를 호출시킴으로써 인터럽트를 직접 걸어주는 방법도있고, 아니면 인터럽트를 안걸고 sleep 하는 방법도 있음. 이방법도 인터럽트를 거는것과 동일하다. 이유 : catch 에 InterruptedException 걸리는거보면 인터럽트거는것이기때문.
//	public void interrupte() {
//		thread.interrupt();
//	}
	
	
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
						//인위적으로 인터럽트를 가해서 cpu자원의 낭비를 막기위한 용도이기때문에 인터럽트발생시 처리를 꼭 해줘야하는 부분이 아니다. 비워놔도 된다.
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
					//네트워크지연, 네트워크 단절 혹은 아예 없거나, 이런상황에서 발생되는데, 해당 크롤링의 기준이되는 서버가 어떤 성향을 갖고있는지 모르다보니까,
					//보통 retry를 3번정도 실행해주는 방식으로 처리한다. 그럼에도 처리가 불가능한경우 모아놓고 처리를 직접 해주는 상황이 발생된다.
					//connection timeout이 걸리면 retry를 할수있게 처리해주자.
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
					//네트워크지연, 네트워크 단절 혹은 아예 없거나, 이런상황에서 발생되는데, 해당 크롤링의 기준이되는 서버가 어떤 성향을 갖고있는지 모르다보니까,
					//보통 retry를 3번정도 실행해주는 방식으로 처리한다. 그럼에도 처리가 불가능한경우 모아놓고 처리를 직접 해주는 상황이 발생된다.
					//connection timeout이 걸리면 retry를 할수있게 처리해주자.
					//그런데 지금은 다운로드한 파일이 존재할때 업로드를 할수있으므로 다운로드 retry 가 실패하면 업로드 retry는 할 필요가 없다.
				}
				finally {
					dto = null;
				}
			}
			//큐에남은 데이터가 없을때 false를 주게끔 변경하자.
			if(mcMessageQueue.size() == 0)
				isCompelete = false;
		}

	}

}
