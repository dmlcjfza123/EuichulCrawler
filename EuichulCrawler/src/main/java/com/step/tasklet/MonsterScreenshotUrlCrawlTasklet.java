package com.step.tasklet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff; 

import com.dto.DownloadAndS3UploadDto;
import com.jsoup.MonsterCrawler;
import com.util.TranslateText;

@EnableRetry
public class MonsterScreenshotUrlCrawlTasklet implements Tasklet {

	@Override
	@Retryable(value = {FileNotFoundException.class,IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		
		System.out.println("monsterScreenshotUrlCrawlStep...");
		
		String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};
		//String[] attriStrs = {"fir1","wat1","thu1","for1","lig1"};
		//String[] attriStrs = {"dar1"};
		List<String> attriList = Arrays.asList(attriStrs);
		List<String> detailHrefList = new ArrayList<String>();
		//저장해놓은 상세페이지들의 href txt 파일을 읽어서 파싱하기위함.
		for (String attristr : attriList) {
			// 출처: https://jeong-pro.tistory.com/69 [기본기를 쌓는 정아마추어 코딩블로그]
			try {
				// 파일 객체 생성
				File file = new File("C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterHrefResult\\"+attristr+"\\"+"hrefs.txt");
				// 입력 스트림 생성
				FileReader filereader = new FileReader(file);
				// 입력 버퍼 생성
				BufferedReader bufReader = new BufferedReader(filereader);
				String line = "";
				while ((line = bufReader.readLine()) != null) {
					// System.out.println("href : " + line);
					detailHrefList.add(line);
				}
				// .readLine()은 끝에 개행문자를 읽지 않는다.
				bufReader.close();
				
				for (String url : detailHrefList) {
					// 페이지 전체소스 저장
					Document doc = null;
					
					//test 용 아자토스 url
					//String url = "https://gamewith.jp/pocodun/article/show/73362";
					
					doc = Jsoup.connect(url).get();
					
					Elements els = doc.getElementsByTag("h2");
					
					//Dto 에 담아주기전에 보관용 map <몬스터이름, img주소> 혹은 <순서용key, img주소> 
					Map<String,String> MonsterImgHrefMap = new TreeMap<String, String>();

					for (Element el : els) {
						String elstr = el.text();
						if(elstr.contains("★"))
						{
							String elText = el.toString() + "\n";
							
							//이미 nextEle 가 <img> 일수도있는데 다시한번 img select하는 이유는 가끔가다가
							//https://gamewith.jp/pocodun/article/show/73142
							//얘내들 처럼 무신 스사노오 같은 새기들이 이미지가 div 안에 div안에 <img 로 있는경우가있기때문이다.
							Element img= el.nextElementSibling();
							Elements reimg = img.select("img");
							String dataOriginal = reimg.attr("data-original");

							//별제거
							elstr = elstr.replace("★", "");
							
							//숫자제거 및 value로 삽입 / 이름 key로 삽입
							char valueofRarity = elstr.charAt(0);
							elstr = elstr.replace(String.valueOf(valueofRarity), "");
							
							//몬스터이름,스크린샷 img주소 삽입
							String transMonName = TranslateText.translate(elstr);
							MonsterImgHrefMap.put(transMonName, dataOriginal);
							
							
							//폴더에 이미지 저장하기.
							String monsterIconPath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterScreenshot\\"+attristr+"\\"+transMonName+".jpg";
							
							String upLoadPath = "MonsterScreenshot/" + attristr + "/" + transMonName+".jpg";
							
							DownloadAndS3UploadDto downloadAndS3UploadDto = DownloadAndS3UploadDto.builder()
									.monsterName(transMonName)
									.url(dataOriginal)
									.downloadPath(monsterIconPath)
									.uploadPath(upLoadPath)
									.attribute(attristr)
									.isRepeat(true)
									.build();
							MonsterCrawler.downloadAndS3UploadService(downloadAndS3UploadDto);
						}
					}
				}
			} catch (FileNotFoundException e) {
				// TODO: handle exception
			} catch (IOException e) {
				System.out.println(e);
			}
			
			//System.out.println(attristr + " 속성의 href size : " + detailHrefList.size());
			//System.out.println(detailHrefList.toString());
			detailHrefList.clear();
		}
		return RepeatStatus.FINISHED;
	}

}
