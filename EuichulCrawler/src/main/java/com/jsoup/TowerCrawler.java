package com.jsoup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.IOUtils;
import com.config.DBConfig;
import com.dao.MonsterDao;
import com.dao.TowerDao;
import com.dto.TowerBossDto;
import com.dto.TowerBossRewardDto;
import com.dto.TowerFloorDto;
import com.dto.TowerFloorMonsterDto;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.util.TranslateText;
import com.util.Utils;


@Component
public class TowerCrawler {
	
	@Autowired
	private TowerDao towerDao;
	
	public TowerDao getTowerDao() {
		return towerDao;
	}

	public static void main(String[] args) {
		//TowerCrawler jc = new TowerCrawler();
		
		//타워 크롤링 함수 호출. 6p
		//jc.towerPreviewCrawl();
		
		//타워 크롤링 함수 호출. 7p - 플로어 이미지 크롤링
		//jc.towerDetailCrawlFloorImg();
		
		//타워 크롤링 함수 호출. 7p - 몬스터 이미지,hp 크롤링 
		//towerDetailCrawlFloorImg() 함수가 선행되어야한다. 해당 함수에서 폴더를 만들고있기때문.
		//아니면 폴더가 이미 존재했을때는 아래 함수만 호출해도 상관없다.
		//jc.towerDetailCrawlMonsterInfo();
		
		/////////////////////////////////////////////////////////////////////////
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DBConfig.class)) {
			TowerCrawler jc = context.getBean(TowerCrawler.class);
			//- 미리보기 페이지에서 만든 로직  = 탑보스테이블(보스이름만빼고) , 탑 보스 보상정보 테이블에 insert.
			//jc.towerBossAndRewardInfoCrawlAndInsertDB();
			
			//- 상세페이지 토대로 보스이름만 파싱 로직 새로 만든거 + update 로 탑보스 보상정보 테이블에 보스 이름 넣기.
			//jc.towerBossInfoNameCrawlAndUpdateDB();
			
			//보스아이콘  S3에 업로드하기.
			//jc.settingAwsAndBossIconUrlFileUploadToS3();
			
			//보상 아이콘 S3에 업로드하기.
			//jc.settingAwsAndBossRewardIconUrlFileUploadToS3();
			
			//////////////////////////////////////////////////////////////////////////
			
			//towerDetailCrawlFloorImg 에서 플로어 이미지 다운로드 완료. 
			//TowerFloordto 작업해서 insert 할것
			//jc.towerDetailCrawlFloorImg();
			
			//towerDetailCrawlMonsterInfo 에서 몬스터 이미지 다운로드 완료. 
			//TowerFloorMonsterdto 작업해서 insert 할것
			//jc.towerDetailCrawlMonsterInfo();
			
			//플로어이미지들 S3에 올리기.
			//jc.settingAwsAndFloorImgFileUploadToS3();
			
			//플로어 몬스터 이미지 S3에 올리기.
			//jc.settingAwsAndFloorMonsterImgFileUploadToS3();
		}
	}
	
	
	//해당층들의 플로어이미지 파싱된거 리스트에 담아둠 - 나중에 DB에 담아야될 수도 있어서. - 7p관련 함수에서 사용중.
	//왠지 Map<"층수명"(String) , "플로어이미지str들"(List<String) > 으로 관리 해야할것같긴함. - 우선 이건 나중에 DB에 저장할일이 생기면 이렇게 관리할것.
	List<String> imgStrList = new ArrayList<String>();
	
	//hp정보를 리스트에 담은뒤 임시 복사생성자로 아래 map 에 담아주기위한 임시용 리스트.
	List<Integer> hpList = new ArrayList<Integer>();
	//hp정보를 <층수, hp리스트> 형태로 관리해야하기때문에 Map 으로관리.
	Map<Integer,List<Integer>> hpMap = new HashMap<Integer, List<Integer>>();
	
	
	//각 플로어에 등장하는 몬스터 이미지 S3에 올리기
	public void settingAwsAndFloorMonsterImgFileUploadToS3() {
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		String basePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\7pResult\\Asgard\\";
		
		for(int k=11; k<=30; k++) {
			System.out.println(k + "층");
			for(int j=1; j<=5; j++) {
				String floorPath = basePath + k +"층\\" +"Floor"+ j +"\\monsterImg";
				
				//가져온 폴더명 저장 
				//List<String> fileNamesList = new ArrayList<String>();
				//디렉토리 파일 전체목록 가져오기.
				File path = new File(floorPath);
				File[] fileList = path.listFiles();
				int fileCnt = 1;
				if(fileList.length > 0){
				    for(int i=0; i < fileList.length; i++){
				  		//System.out.println(fileList[i].toString()) ; //풀경로추출
				  		String fileName = fileList[i].getName(); //파일이름만 추출
				  		System.out.println(k + "층 " + j + "플로어  / 파일이름 : " + fileName);
				  		fileCnt++;
				  		//fileNamesList.add(fileName);
				  		
				  		
				  		//가져올 파일경로
						String newPath = floorPath + "\\" + fileName;
						
						File file = new File(newPath);
						
						//putObject 메소드로 (만들어놓은 Bucket의이름, bucket안에 저장될경로, 업로드할파일변수) 업로드하기.
						//putObejct의 문제점 : 파일쓰기작업은 매우 무거운 작업이다. 업로드할곳은 S3인데 로컬에도 파일이 저장되기때문에 쓸데없이 자원이 낭비된다.
						//문제점 해결 inputStream을 받는 putObject 메소드 활용하기.
						FileInputStream input;
						try {
							input = new FileInputStream(file);
							MultipartFile multipartFile;
							try {
								//File 을 multipartFile 로 변환.
								multipartFile = new MockMultipartFile("file",file.getName(),"image/jpeg", IOUtils.toByteArray(input));
								
								ObjectMetadata metadata = new ObjectMetadata();
								metadata.setContentType(MediaType.IMAGE_JPEG_VALUE);
								metadata.setContentLength(multipartFile.getSize());
								
								//inputStream을 매개변수로 받는 경우, metadata를 보내야한다. 
								//이유 : inputStream을 통해 Byte만 전달되기때문에, 해당파일에대한 정보가 없기때문이다. 따라서, metadata를 전달해 파일에대한 정보를 추가로 전달해줘야한다.
								//폴더명을 몬스터이름으로 안하는이유 : 몬스터이름뒤에 . 이 붙으면 폴더 만들어질때 . 은 안써짐. 그런데 몬스터이름..jpg 이렇게 저장되서 폴더명이랑 파일명이 달라져버림. 따라서 폴더명에 몬스터이름 넣으면안됨.
								//업로드할 파일경로
								String upLoadPath = "TowerFloorMonsterImg/Asgard/"+ k +"층/Floor"+ j + "/" + fileName;
								System.out.println("업로드 된 파일 경로 : " + upLoadPath);
								s3Client.putObject("euichul-pocorong-imgs", upLoadPath, multipartFile.getInputStream(), metadata);
								
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
				  		
				  		
				  		
				    }
				}
			}
		}
	}
	
	//7p 크롤링 - 몬스터 이미지,hp 부분
	//7p 크롤링이 되기위해서는, towerPreviewCrawl() 함수에서 파싱한 hrefList 리스트안에 href 들이 저장되어있어야한다.
	public void towerDetailCrawlMonsterInfo() {
		// towerPreviewCrawl() 함수가 실행되지 않고 독립적으로 towerDetailCrawl() 실행하는경우,
		// towerPreviewCrawl() 함수에서 저장해놓은 txt 파일을 읽어서 hrefList에 저장해두고 파싱하기위함.
		if (hrefList.size() == 0) {
			// 출처: https://jeong-pro.tistory.com/69 [기본기를 쌓는 정아마추어 코딩블로그]
			try {
				// 파일 객체 생성
				File file = new File("C:\\ictcbwd\\workspace\\Java\\crawling\\6pHref.txt");
				// 입력 스트림 생성
				FileReader filereader = new FileReader(file);
				// 입력 버퍼 생성
				BufferedReader bufReader = new BufferedReader(filereader);
				String line = "";
				while ((line = bufReader.readLine()) != null) {
					// System.out.println("href : " + line);
					hrefList.add(line);
				}
				// .readLine()은 끝에 개행문자를 읽지 않는다.
				bufReader.close();
			} catch (FileNotFoundException e) {
				// TODO: handle exception
			} catch (IOException e) {
				System.out.println(e);
			}
		}
		
		
		//맵<층, 멀티맵<플로어,리스트<Dto>>
		Map<Integer, Multimap<Integer, List<TowerFloorMonsterDto>>> towerFloorMonsterDtoMap = new TreeMap<Integer, Multimap<Integer, List<TowerFloorMonsterDto>>>();
		Multimap<Integer, List<TowerFloorMonsterDto>> towerFloorMonsterDtoMultiMap = ArrayListMultimap.create();
		List<TowerFloorMonsterDto> towerFloorMonsterDtoList = new ArrayList<TowerFloorMonsterDto>();
		TowerFloorMonsterDto towerFloorMonsterDto = TowerFloorMonsterDto.builder().build();
		
		
		//맵<층, 멀티맵<플로어, hp>> layerAndFloorAndHpMap
		Map<Integer, Multimap<Integer,Integer>> layerAndFloorAndHpMap = new TreeMap<Integer, Multimap<Integer,Integer>>();
		Multimap<Integer,Integer> layerAndFloorAndHpMultiMap = ArrayListMultimap.create();
		
		//맵<층, 멀티맵<플로어, url>> layerAndFloorAndUrlMap
		Map<Integer, Multimap<Integer,String>> layerAndFloorAndUrlMap = new TreeMap<Integer, Multimap<Integer,String>>();
		Multimap<Integer,String> layerAndFloorAndUrlMultiMap = ArrayListMultimap.create();
		
		
		//보스폴더명은 저장된 list에 따라서 바꿔주어야한다.
		String BossFolderStr = "Asgard";
		//폴더 저장용 층수
		int layerCnt = 30;
		//리스트 안에 저장된 href 들 하나씩 들어가서 파싱해오기.
		for (String strs : hrefList) {
			
			String url = strs;
			//30층
			//String url = "https://gamewith.jp/pocodun/article/show/122276";
			//12층
			//String url = "https://gamewith.jp/pocodun/article/show/141726";
			
			Document doc = null;
			
			//플로어 층수 체크용도 폴더구분용.
			int FloorCntCheck = 1;
			
			try {
				doc = Jsoup.connect(url).get();
				
				Elements elementMonster = doc.select("h3");
				
	    		for (Element elem : elementMonster) {
	    			//h3 태그로 검색된것들중에 적의HP 라는 일본어가 담긴 태그를 탐색한다.
	    			Elements hpText =  elem.getElementsContainingText("敵のHP");
	    			
	    			String str = hpText.toString() +"\n";
	    			
	    			//만약 h3 태그로 검색된것들중에 적의HP라는 일본어가 담겨있지않다면 hpText 의 size가 0이기때문에 size가 0이아닌것들이 적의hp가 있는것이다.
	    			if(hpText.size() != 0) {
	    				
	    				for (Element el : hpText) {
	    					
	    					//적의 hp 다음에 존재하는 div 테이블 만 파싱한다. 여기에 몬스터정보가 담겨있고 다른 테이블들은 용병추천 이딴거라 필요가없음.
							Element table = el.nextElementSibling();
							List<String> HpTexts= table.select("td").eachText();
							
							//System.out.println();
							for (String strs1 : HpTexts) {
								strs1 = strs1.replace(",", "");
								
								//숫자인지 체크해서 숫자면 hpList 에 저장시켜준다.
								boolean isNum =true;
								for(int i=0; i<strs1.length(); i++) {
									if(!Character.isDigit(strs1.charAt(i))) {
										isNum = false;
										break;
									}
								}
								
								if(isNum) {
									//System.out.println(layerCnt + "층 " + FloorCntCheck + " 플로어 / hp : " + strs1);
									hpList.add(Integer.valueOf(strs1));
									//System.out.println(hpList.toString());
									
									layerAndFloorAndHpMultiMap.put(FloorCntCheck, Integer.valueOf(strs1));
									System.out.println(layerCnt + "층 " + FloorCntCheck + " 플로어 / hp : " + layerAndFloorAndHpMultiMap.get(FloorCntCheck));
								}
							}
							
							
							String tdHp = table.select("td").text() +"\n";
							
							//이미지 소스만 따기위해 noscript 로 검색
							Elements img = table.select("noscript");
							
							int monsterimgCnt = 1;
							
							for (Element es : img) {
								Elements elems = es.getElementsByAttribute("src");
								String inUrl = elems.attr("src");
								
								//inUrl += "\n";
								
								//System.out.println(layerCnt + "층 " + FloorCntCheck + "플로어 / url : " + inUrl);
								layerAndFloorAndUrlMultiMap.put(FloorCntCheck, inUrl);
								System.out.println(layerCnt + "층 " + FloorCntCheck + "플로어 / url : " + layerAndFloorAndUrlMultiMap.get(FloorCntCheck));
								
//								//몬스터 이미지 저장
//								URL imgUrl = new URL(inUrl);
//								HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
//								//System.out.println(conn.getContentLength());
//								
//								InputStream is = conn.getInputStream();
//								BufferedInputStream bis = new BufferedInputStream(is);
//								
//								//보스의경우 BossIcon 폴더에								
//								String FilePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\7pResult\\"+BossFolderStr+"\\"+layerCnt+"층\\Floor"+FloorCntCheck+"\\monsterImg\\Monster"+monsterimgCnt+"Img.jpg";
//									
//								FileOutputStream os = new FileOutputStream(FilePath);
//								BufferedOutputStream bos = new BufferedOutputStream(os);
//								
//								int byteImg;
//								
//								byte[] buf = new byte[conn.getContentLength()];
//								while((byteImg = bis.read(buf)) != -1) {
//									bos.write(buf,0,byteImg);
//								}
//								
//								monsterimgCnt++;
//								
//								bos.close();
//								os.close();
//								bis.close();
//								is.close();
							}
							
							
						}
	    				//플로어 층이 증가될때마다 하나씩 올린다.
	    				FloorCntCheck ++;
	    				
	    			}
	    			
				}
	    		
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//층이 바뀌기전에 해당 층의 hp리스트들을 저장시킨다.
			hpMap.put(layerCnt, new ArrayList<Integer>(hpList));
			
			layerAndFloorAndHpMap.put(layerCnt, ArrayListMultimap.create(layerAndFloorAndHpMultiMap));
			layerAndFloorAndHpMultiMap.clear();
			
			layerAndFloorAndUrlMap.put(layerCnt, ArrayListMultimap.create(layerAndFloorAndUrlMultiMap));
			layerAndFloorAndUrlMultiMap.clear();
			
			layerCnt--;	
			
			//다시 담아주기위해 hpList 초기화
			hpList.clear();
			
			//10층 즉, 1층데이터 파싱못하게 break
			if(layerCnt == 10) {
				break;
			}
		}
		//각층의 저장된 hp정보 확인
		for (Map.Entry<Integer, List<Integer>> entry : hpMap.entrySet()) {
			int key =entry.getKey();
			List<Integer> value = entry.getValue();
			
			//System.out.println("층 : " + key);
			//System.out.println(value.toString());
		}
		//System.out.println(hpMap.toString());
				
		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		
		//<층, <플로어,hp>> 확인
		//귀찮으니까 Dto 만들고 여기서 set 후에 insert 하는걸로.
		TowerFloorMonsterDto towerFloorMonsterDto2 = TowerFloorMonsterDto.builder().build();
		TowerDao towerDao = getTowerDao();
		
		System.out.println("//////////////////// <층, <플로어,hp>> 확인  + DB에 insert ///////////////////");
		for (Map.Entry<Integer, Multimap<Integer,Integer>> entry : layerAndFloorAndHpMap.entrySet()) {
			int Layer = entry.getKey();
			
			Multimap<Integer,Integer> floorHpMultimap = entry.getValue();
			Iterator<Integer> iter = floorHpMultimap.keys().iterator();
			Iterator<Integer> iterVal = floorHpMultimap.values().iterator();
			
			//int floorHpMultimapSize = floorHpMultimap.size();
			
			Multimap<Integer,String> floorUrlMultimap = layerAndFloorAndUrlMap.get(Layer);
			System.out.println("floorUrlMultimap Size : " + floorUrlMultimap.size());
			Iterator<String> iterUrl =  floorUrlMultimap.values().iterator();
			
			//현재 플로어 수 와 이전 플로어 수를 비교해 플로어수 가 같을때는 i 의 값이 그대로 ++ 되면 되지만, 
			//만약,현재 플로어 수가 증가되어 이전 플로어 수와 다르다면, i 의값은 다시 1부터 시작하게 만들면된다. 그래야 S3에 올려진 몬스터 아이콘의 key 를 순회할 수 있음.
			int nowFloorNum = 0;
			int prevFloorNum = 0;
			
			int i = 1;
			//url , 플로어 수 , hp 하나라도 존재하는 항목이라면 세팅해줘야하므로 이렇게 돌린다. - 근데 사실 url 은 내가 s3꺼 입력시킬거니까 확인할 필요가 없음.
			//while(iterUrl.hasNext() || iter.hasNext() || iterVal.hasNext()) {
			while(iter.hasNext() || iterVal.hasNext()) {
				
				//Dto에 layer 세팅
				towerFloorMonsterDto2.setLayer(Layer);
				
				//우선 없을때를 대비해 기본값 잡아놓고,
				int floorNum = 0;
				int hp = 0;
				//String url = null;
				
				//있다고하는 것들만 받아온다.
				if(iter.hasNext()) {
					floorNum = iter.next();
					//만약,현재 플로어 수가 증가되어 이전 플로어 수와 다르다면, i 의값은 다시 1부터 시작하게 만들면된다. 그래야 S3에 올려진 몬스터 아이콘의 key 를 순회할 수 있음.
					nowFloorNum = floorNum;
					if(nowFloorNum != prevFloorNum) {
						i = 1;
						prevFloorNum = nowFloorNum;
					}
				}
				towerFloorMonsterDto2.setFloorNum(floorNum);
				
				if(iterVal.hasNext()) {
					hp = iterVal.next();
				}
				towerFloorMonsterDto2.setHp(hp);
				
//				if(iterUrl.hasNext()) {					
//					url = iterUrl.next();
//				}
				
				//S3에 올려진 플로어의 몬스터 이미지들의 주소를 넣어준다.
				//https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/TowerFloorMonsterImg/Asgard/26층/Floor5/Monster1Img.jpg
				String baseUrl = "https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/TowerFloorMonsterImg/Asgard/";
				String fullUrl = baseUrl + Layer +"층/Floor"+ floorNum + "/Monster"+ i +"Img.jpg";
				String key = "TowerFloorMonsterImg/Asgard/" + Layer +"층/Floor"+ floorNum + "/Monster"+ i +"Img.jpg";
				System.out.println("key : " + key);
				try {
					URL awsUrl = s3Client.getUrl("euichul-pocorong-imgs", key);
	                HttpURLConnection connection = (HttpURLConnection)awsUrl.openConnection();
	                connection.setRequestMethod("GET");
	                connection.connect();
	                int code = connection.getResponseCode();
	                
	                //System.out.println("icon response code : " + code);
	                
	                if(code == 200) {
	                	towerFloorMonsterDto2.setIconURL(fullUrl);
	                	//System.out.println("FloorImg Url : " + towerFloorDto.getFloorURL());
	                }
	                else {
	                	System.out.println("else) icon response code : " + code);
	                	fullUrl = null;
	                	towerFloorMonsterDto2.setIconURL(fullUrl);
	                }
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				//DB에 insert
				towerDao.insertTowerFloorMonsterInfo(towerFloorMonsterDto2);
				
				System.out.println(i + ". " + Layer + "층 " + towerFloorMonsterDto2.getFloorNum() + "플로어 / hp :  " + towerFloorMonsterDto2.getHp());
				System.out.println(i + ". " + Layer + "층 " + towerFloorMonsterDto2.getFloorNum() + "플로어 / url :  " + towerFloorMonsterDto2.getIconURL());
				
				towerFloorMonsterDto2 = TowerFloorMonsterDto.builder().build();
				
				i++;
				
			}
		}
		
		
	}
	
	//플로어 이미지 S3에 올리기
	public void settingAwsAndFloorImgFileUploadToS3() {
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		
		String basePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\7pResult\\Asgard\\";
		
		for(int k=11; k<=30; k++) {
			System.out.println(k + "층");
			for(int j=1; j<=5; j++) {
				String floorPath = basePath + k +"층\\" +"Floor"+ j +"\\floorImg";
				
				//가져온 폴더명 저장 
				List<String> fileNamesList = new ArrayList<String>();
				//디렉토리 파일 전체목록 가져오기.
				File path = new File(floorPath);
				File[] fileList = path.listFiles();
				int fileCnt = 1;
				if(fileList.length > 0){
				    for(int i=0; i < fileList.length; i++){
				  		//System.out.println(fileList[i].toString()) ; //풀경로추출
				  		String fileName = fileList[i].getName(); //파일이름만 추출
				  		//System.out.println(fileCnt +" . " + fileName);
				  		fileCnt++;
				  		fileNamesList.add(fileName);
				  		
				  		
				  		//가져올 파일경로
						String newPath = floorPath + "\\" + fileName;
						
						File file = new File(newPath);
						
						//putObject 메소드로 (만들어놓은 Bucket의이름, bucket안에 저장될경로, 업로드할파일변수) 업로드하기.
						//putObejct의 문제점 : 파일쓰기작업은 매우 무거운 작업이다. 업로드할곳은 S3인데 로컬에도 파일이 저장되기때문에 쓸데없이 자원이 낭비된다.
						//문제점 해결 inputStream을 받는 putObject 메소드 활용하기.
						FileInputStream input;
						try {
							input = new FileInputStream(file);
							MultipartFile multipartFile;
							try {
								//File 을 multipartFile 로 변환.
								multipartFile = new MockMultipartFile("file",file.getName(),"image/jpeg", IOUtils.toByteArray(input));
								
								ObjectMetadata metadata = new ObjectMetadata();
								metadata.setContentType(MediaType.IMAGE_JPEG_VALUE);
								metadata.setContentLength(multipartFile.getSize());
								
								//inputStream을 매개변수로 받는 경우, metadata를 보내야한다. 
								//이유 : inputStream을 통해 Byte만 전달되기때문에, 해당파일에대한 정보가 없기때문이다. 따라서, metadata를 전달해 파일에대한 정보를 추가로 전달해줘야한다.
								//폴더명을 몬스터이름으로 안하는이유 : 몬스터이름뒤에 . 이 붙으면 폴더 만들어질때 . 은 안써짐. 그런데 몬스터이름..jpg 이렇게 저장되서 폴더명이랑 파일명이 달라져버림. 따라서 폴더명에 몬스터이름 넣으면안됨.
								//업로드할 파일경로
								String upLoadPath = "TowerFloorImg/Asgard/"+ k +"층/"+ fileName;
								System.out.println("업로드 할 파일 이름 : " + fileName);
								s3Client.putObject("euichul-pocorong-imgs", upLoadPath, multipartFile.getInputStream(), metadata);
								
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
				  		
				  		
				  		
				    }
				}
			}
		}
		
		
		
	}
	
	//7p 크롤링 - 플로어 이미지 부분
	//7p 크롤링이 되기위해서는, towerPreviewCrawl() 함수에서 파싱한 hrefList 리스트안에 href 들이 저장되어있어야한다.
	public void towerDetailCrawlFloorImg() {
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	

		//towerPreviewCrawl() 함수가 실행되지 않고 독립적으로 towerDetailCrawl() 실행하는경우, 
		//towerPreviewCrawl() 함수에서 저장해놓은 txt 파일을 읽어서 hrefList에 저장해두고 파싱하기위함.
		int layerCnt = 30;
		if (hrefList.size() == 0) {
			// 출처: https://jeong-pro.tistory.com/69 [기본기를 쌓는 정아마추어 코딩블로그]
			try {
				// 파일 객체 생성
				File file = new File("C:\\ictcbwd\\workspace\\Java\\crawling\\6pHref.txt");
				// 입력 스트림 생성
				FileReader filereader = new FileReader(file);
				// 입력 버퍼 생성
				BufferedReader bufReader = new BufferedReader(filereader);
				String line = "";
				while ((line = bufReader.readLine()) != null) {
					//System.out.println("href : " + line);
					if(layerCnt > 10)
						hrefList.add(line);
					layerCnt--;
				}
				// .readLine()은 끝에 개행문자를 읽지 않는다.
				bufReader.close();
			} catch (FileNotFoundException e) {
				// TODO: handle exception
			} catch (IOException e) {
				System.out.println(e);
			}
		}
		
		//보스폴더명은 저장된 list에 따라서 바꿔주어야한다.
		String BossFolderStr = "Asgard";
		
		
		//리스트 안에 저장된 href 들 하나씩 들어가서 파싱해오기.
		
		//<층수 , <플로어수,플로어이미지> > 
		Map<Integer, Map<Integer,String>> towerLayerAndFloorMap = new TreeMap<Integer, Map<Integer,String>>();
		Map<Integer,String> towerFloorMap = new TreeMap<Integer, String>();
		String tempStr = "";
		for (String strs : hrefList) {
			//hrefList 안에 String 값들 보기.
			//System.out.println("hrefList : " + strs);
			
						
			String url = strs;
			//30층
			//String url = "https://gamewith.jp/pocodun/article/show/122276";
			//12층
			//String url = "https://gamewith.jp/pocodun/article/show/141726";
		
			//27층 test
			//String url = "https://gamewith.jp/pocodun/article/show/122229";
			
			Document doc = null;
			
			try {
				doc = Jsoup.connect(url).get();
				//System.out.println(url);
				
				
				//해당 층수 파싱
				Elements pager = doc.select("div.pager");
				
				tempStr = "";
				//1층의 경우 div.pager 가 없어서 1층인지 파악이 안되기때문에 div.pager가 있는 30~11 까지만 파싱.
				if(pager.size() != 0) {
					String pageLayer = doc.select("div.pager").select("td").get(1).text();
					//System.out.println("pageLayer : " + pageLayer);
					
					//층수 번역
					tempStr = TranslateText.translate(pageLayer);
					tempStr = tempStr.replace(" ","");
					
					//해당층 상세공략 정보 폴더만들기
					//Utils.makeFolderOf7p(tempStr,BossFolderStr);
				}
				
				
				
				
				//플로어이미지
				//Elements elementFloor = doc.select("div.w-article-img");
				Elements elementFloor = doc.select("div.js-accordion-area.accordion-area");
				
				//플로어 이미지 파싱
				//층수확인
				//System.out.println(tempStr);
				tempStr = tempStr.replace("층", "");
				
				int oddCnt =1;
				int floorFolderCnt=1;
				
				for (Element element : elementFloor) {
					
					//짝수번째는 접어져있는 이미지라 필요가없다. 펼처져있는 이미지를 가져오기위해 홀수번째 이미지들만 파싱함.
					//최대 플로어는 5개이므로 (접어진이미지 + 펼처진이미지) * 5 를해도 10이다.
					//따라서 11층같은경우 플로어 이미지가 2개밖에 없지만, 30층같은경우 플로어 이미지가 10개이기때문에, 10개까진 카운팅해준다.
					//if(oddCnt %2 == 1 && oddCnt <= 10) {
					  if(element.previousElementSibling().text().contains("フロア")) {
						//System.out.println("oddCnt" + oddCnt);
						Elements els = element.getElementsByAttributeValueContaining("data-original","akamaized");
						String inUrl = els.attr("data-original");
						
						//System.out.println(inUrl );
						
						towerFloorMap.put(oddCnt, inUrl);
						
//						//플로어 이미지 저장
//						URL imgUrl = new URL(inUrl);
//						HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
//						//System.out.println(conn.getContentLength());
//						
//						InputStream is = conn.getInputStream();
//						BufferedInputStream bis = new BufferedInputStream(is);
//						
//						//보스의경우 BossIcon 폴더에								
//						String FilePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\7pResult\\"+BossFolderStr+"\\"+layerCnt+"층\\Floor"+floorFolderCnt+"\\floorImg\\Floor"+floorFolderCnt+"Img.jpg";
//							
//						FileOutputStream os = new FileOutputStream(FilePath);
//						BufferedOutputStream bos = new BufferedOutputStream(os);
//						
//						int byteImg;
//						
//						byte[] buf = new byte[conn.getContentLength()];
//						while((byteImg = bis.read(buf)) != -1) {
//							bos.write(buf,0,byteImg);
//						}
//						
//						floorFolderCnt++;
//						
//						bos.close();
//						os.close();
//						bis.close();
//						is.close();
					
						oddCnt++;
					}
					
					
					//따라서 플로어 이미지 10개까지 훑었으면 그이상의 이미지들은 필요하지않으니 for문을 나가도 좋다.
					if(oddCnt > 10)
						break;
					
				}
				
				towerLayerAndFloorMap.put(Integer.valueOf(tempStr), new TreeMap<Integer, String>(towerFloorMap));
				towerFloorMap.clear();
				
	    		
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			layerCnt--;
		}
		
		System.out.println("///////////////////////// towerLayerAndFloorMap 확인 ///////////////////");
		
		//한층에 플로어 수, 플로어 이미지 여러개.
		Map<Integer, List<TowerFloorDto>> towerFloorDtosMap = new TreeMap<Integer, List<TowerFloorDto>>();
		List<TowerFloorDto> towerFloorDtoLists = new ArrayList<TowerFloorDto>();
		TowerFloorDto towerFloorDto = TowerFloorDto.builder().build();
		
		//towerLayerAndFloorMap 확인 및 Dto 삽입준비
		for (Map.Entry<Integer, Map<Integer,String>> entry : towerLayerAndFloorMap.entrySet()) {
			int layer = entry.getKey();
			//System.out.println(layer + "층");
			Map<Integer,String> floorNumAndFloorURL = entry.getValue();
			for (Map.Entry<Integer, String> en : floorNumAndFloorURL.entrySet()) {
				int floorNum = en.getKey();
				String floorUrl = en.getValue();
				//System.out.println(floorNum + " 플로어 / URL : " + floorUrl);
				
				//Dto 세팅
				towerFloorDto.setLayer(layer);
				towerFloorDto.setFloorNum(floorNum);
				
				//https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/TowerFloorImg/Asgard/11층/Floor1Img.jpg
				String baseUrl = "https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/TowerFloorImg/Asgard/";
				String fullUrl = baseUrl + layer +"층/Floor"+ floorNum +"Img.jpg";
				String key = "TowerFloorImg/Asgard/" + layer +"층/Floor"+ floorNum +"Img.jpg";
				try {
					URL awsUrl = s3Client.getUrl("euichul-pocorong-imgs", key);
	                HttpURLConnection connection = (HttpURLConnection)awsUrl.openConnection();
	                connection.setRequestMethod("GET");
	                connection.connect();
	                int code = connection.getResponseCode();
	                
	                //System.out.println("icon response code : " + code);
	                
	                if(code == 200) {
	                	towerFloorDto.setFloorURL(fullUrl);
	                	//System.out.println("FloorImg Url : " + towerFloorDto.getFloorURL());
	                }
	                else {
	                	fullUrl = null;
	                	towerFloorDto.setFloorURL(fullUrl);
	                }
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				//세팅된 Dto Lis에 넣기
				towerFloorDtoLists.add(towerFloorDto);
				towerFloorDto = TowerFloorDto.builder().build();
				
			}
			
			//세팅된 List Map에 넣기
			towerFloorDtosMap.put(layer, new ArrayList<TowerFloorDto>(towerFloorDtoLists));
			towerFloorDtoLists.clear();
		}
		
		TowerDao towerDao = getTowerDao();
		System.out.println("////////////TowrFloor Table에 insert 하기 ///////////////////");
		for (Map.Entry<Integer, List<TowerFloorDto>> entry : towerFloorDtosMap.entrySet()) {
			int key = entry.getKey();
			List<TowerFloorDto> dtolist = entry.getValue();
			for (TowerFloorDto dto : dtolist) {
				System.out.println(key + ". 층 층수 : " + dto.getLayer());
				System.out.println(key + ". 층 플로어 수 : " + dto.getFloorNum());
				System.out.println(key + ". 층 플로어 이미지 : " + dto.getFloorURL());
				
				towerDao.insertTowerFloorInfo(dto);
			}
		}
		
		
		
	}
	
	//제한정보 파싱된거 리스트에 담아둠 - 나중에 DB에 담아야 될수도 있어서. - TowerCrawl 함수에서 사용중.
	List<String> translateLimitInfoList = new ArrayList<String>();
	
	//해당층들의 href 파싱된거 리스트에 담아둠 - 나중에 DB에 담아야될 수도 있어서. - TowerCrawl 함수에서 사용중.
	List<String> hrefList = new ArrayList<String>();
	
	//탑 보스 보상아이콘 S3에 업로드하기.
	public void settingAwsAndBossRewardIconUrlFileUploadToS3() {
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		String basePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\6pResult\\";
		String bossName = "Asgard";
		String bossIconFullPath = "";
		
		//가져온 폴더명 저장 층별로 보상아이콘이 여러개 있기때문에 Map 에 넣기.
		Map<Integer, List<String>> fileNamesMap = new TreeMap<Integer, List<String>>(); 
		List<String> fileNamesList = new ArrayList<String>();
		
		try {
			for(int j=11; j<=30; j++) {
				bossIconFullPath = basePath + bossName +"\\"+ j +"층공략\\"+"achieveIcon";
				
				//디렉토리 파일 전체목록 가져오기.
				File path = new File(bossIconFullPath);
				File[] fileList = path.listFiles();
				//해당폴더안에 들어있는 파일개수 세는거라서, 1. 만 반복되면 해당 폴더에 파일이 1개라서 그런것임.
				int fileCnt = 1;
				if(fileList.length > 0){
					for(int i=0; i < fileList.length; i++){
						//System.out.println(fileList[i].toString()) ; //풀경로추출
						String fileName = fileList[i].getName(); //파일이름만 추출
						//System.out.println(fileCnt +" . " + fileName);
						fileCnt++;
						fileNamesList.add(fileName);
					}
					fileNamesMap.put(j, new ArrayList<String>(fileNamesList));
					//리스트 맵에 넣었으니 다음을 위해 초기화.
					fileNamesList.clear();
				}
			}
			
			//중간 체크
			System.out.println("fileNamesMap size : " + fileNamesMap.size());
			for (Map.Entry<Integer, List<String>> entry : fileNamesMap.entrySet()) {
				int Layer = entry.getKey();
				List<String> fileList = entry.getValue();
				System.out.println(Layer + ". 층 보상 아이콘 개수 : " + fileList.size());
				for (String string : fileList) {
					System.out.println(Layer + ". 층 아이콘 파일 명 : " + string);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		for (Map.Entry<Integer, List<String>> entry : fileNamesMap.entrySet()) {
			int LayerCnt = entry.getKey();
			//가져올 파일경로
			List<String> folderStrList = entry.getValue();
			for (String folderStr : folderStrList) {
				
				String newPath = basePath + bossName + "\\" + LayerCnt + "층공략\\achieveIcon\\" + folderStr;
				
				File file = new File(newPath);
				
				FileInputStream input;
				try {
					input = new FileInputStream(file);
					MultipartFile multipartFile;
					try {
						//File 을 multipartFile 로 변환.
						multipartFile = new MockMultipartFile("file",file.getName(),"image/jpeg", IOUtils.toByteArray(input));
						
						ObjectMetadata metadata = new ObjectMetadata();
						metadata.setContentType(MediaType.IMAGE_JPEG_VALUE);
						metadata.setContentLength(multipartFile.getSize());
						
						//inputStream을 매개변수로 받는 경우, metadata를 보내야한다. 
						//이유 : inputStream을 통해 Byte만 전달되기때문에, 해당파일에대한 정보가 없기때문이다. 따라서, metadata를 전달해 파일에대한 정보를 추가로 전달해줘야한다.
						//폴더명을 몬스터이름으로 안하는이유 : 몬스터이름뒤에 . 이 붙으면 폴더 만들어질때 . 은 안써짐. 그런데 몬스터이름..jpg 이렇게 저장되서 폴더명이랑 파일명이 달라져버림. 따라서 폴더명에 몬스터이름 넣으면안됨.
						//업로드할 파일경로
						String upLoadPath = "TowerBossRewardIcon/" + bossName + "/" + LayerCnt +"층/"+ folderStr;
						System.out.println("업로드 할 파일 이름 : " + folderStr);
						s3Client.putObject("euichul-pocorong-imgs", upLoadPath, multipartFile.getInputStream(), metadata);
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}
		
		
	}
	
	//탑 보스 아이콘 S3에 업로드하기. 
	public void settingAwsAndBossIconUrlFileUploadToS3() {
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		String basePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\6pResult\\";
		String bossName = "Asgard";
		String bossIconFullPath = "";
		
		//가져온 폴더명 저장
		List<String> fileNamesList = new ArrayList<String>();
		
		for(int j=11; j<=30; j++) {
			bossIconFullPath = basePath + bossName +"\\"+ j +"층공략\\"+"BossIcon";
			
			
			//디렉토리 파일 전체목록 가져오기.
			File path = new File(bossIconFullPath);
			File[] fileList = path.listFiles();
			//해당폴더안에 들어있는 파일개수 세는거라서, 1. 만 반복되면 해당 폴더에 파일이 1개라서 그런것임.
			int fileCnt = 1;
			if(fileList.length > 0){
				for(int i=0; i < fileList.length; i++){
					//System.out.println(fileList[i].toString()) ; //풀경로추출
					String fileName = fileList[i].getName(); //파일이름만 추출
					System.out.println(fileCnt +" . " + fileName);
					fileCnt++;
					fileNamesList.add(fileName);
				}
			}
		}
		
		int LayerCnt = 11;
		for (String folderStr : fileNamesList) {
			
			
			//가져올 파일경로
			String newPath = basePath + bossName + "\\" + LayerCnt + "층공략\\BossIcon\\" + folderStr;
			
			File file = new File(newPath);
			
			FileInputStream input;
			try {
				input = new FileInputStream(file);
				MultipartFile multipartFile;
				try {
					//File 을 multipartFile 로 변환.
					multipartFile = new MockMultipartFile("file",file.getName(),"image/jpeg", IOUtils.toByteArray(input));
					
					ObjectMetadata metadata = new ObjectMetadata();
					metadata.setContentType(MediaType.IMAGE_JPEG_VALUE);
					metadata.setContentLength(multipartFile.getSize());
					
					//inputStream을 매개변수로 받는 경우, metadata를 보내야한다. 
					//이유 : inputStream을 통해 Byte만 전달되기때문에, 해당파일에대한 정보가 없기때문이다. 따라서, metadata를 전달해 파일에대한 정보를 추가로 전달해줘야한다.
					//폴더명을 몬스터이름으로 안하는이유 : 몬스터이름뒤에 . 이 붙으면 폴더 만들어질때 . 은 안써짐. 그런데 몬스터이름..jpg 이렇게 저장되서 폴더명이랑 파일명이 달라져버림. 따라서 폴더명에 몬스터이름 넣으면안됨.
					//업로드할 파일경로
					String upLoadPath = "TowerBossIcon/" + bossName + "/" + folderStr;
					System.out.println("업로드 할 파일 이름 : " + folderStr);
					s3Client.putObject("euichul-pocorong-imgs", upLoadPath, multipartFile.getInputStream(), metadata);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			LayerCnt++;
		}
		
	}
	
	
	
	//상세페이지 크롤링 - 보스이름만 파싱 = 탑보스 테이블에 보스이름만 update로 넣기.
	public void towerBossInfoNameCrawlAndUpdateDB() {
		
		TowerDao towerDao = getTowerDao();
		
		//Map<층수,href> 로 저장
		Map<Integer,String> hrefsMap = new TreeMap<Integer, String>();
		// 파일 객체 생성
		File file = new File("C:\\ictcbwd\\workspace\\Java\\crawling\\6pHref.txt");
		// 입력 스트림 생성
		FileReader filereader;
		
		//층수
		int LayerCnt = 30;
		try {
			filereader = new FileReader(file);
			
			// 입력 버퍼 생성
			BufferedReader bufReader = new BufferedReader(filereader);
			String line = "";
			while ((line = bufReader.readLine()) != null) {
				//System.out.println("href : " + line);
				//hrefList.add(line);
				hrefsMap.put(LayerCnt, line);
				//System.out.println(line);
				LayerCnt--;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		//int Layer = href
		for (Map.Entry<Integer, String> entry : hrefsMap.entrySet()) {
			//크롤링 할 test용 url
			//String url = "https://gamewith.jp/pocodun/article/show/122276";
			String url = entry.getValue();
			
			int Layer = entry.getKey();
			if(Layer == 10)
				continue;
			
			// 페이지 전체소스 저장
			Document doc = null;

			try {
				doc = Jsoup.connect(url).get();
				
				Elements elementh3 = doc.select("h3");
				for (Element el : elementh3) {
					if(el.text().contains("ボス情報"))
					{
						Element element = el.nextElementSibling();
						Element bossNameEle = element.select("tr").get(0).select("td").get(0);
						
						String bossNameStr = TranslateText.translate(bossNameEle.text());
						System.out.println(Layer + ". 층 보스이름 : " + bossNameStr);
						
						//update 로 whre절의 층수가 같은 데이터에 보스이름을 넣어준다.
						towerDao.updateTowerBossInfoName(Layer, bossNameStr);
						
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	//6p 미리보기페이지 크롤링 + 상세페이지 보스이름 파싱 = 탑보스 테이블, 탑 보스 보상정보 테이블 
	public void towerBossAndRewardInfoCrawlAndInsertDB() {
		TowerDao towerDao = getTowerDao();
		
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		// 크롤링 할 url - 현재 Asgard 프리뷰 페이지.
		String url = "https://gamewith.jp/pocodun/article/show/122072";
		// 크롤링할 url에 맞춰 폴더도 변경
		String BossFolderStr = "Asgard";
		
		//페이지 전체소스 저장
		Document doc = null;

		//Map<층,제한정보> 파싱
		Map<Integer,String> limitInfoMap = new TreeMap<Integer, String>();
		
		
		try {
			doc = Jsoup.connect(url).get();
			
			//////////////////////////미리 보기 페이지에서 층수, 제한정보, 
			//div의 클래스가 pd_tower_tips 인것들 싹다 모음. 모든 검색은 이아래에서 파싱된다. 가장큰 틀.
			Elements element = doc.select("div.pd_tower_tips");   
			
//			//a 태그안에 적힌 층수 text 파싱.
//    		String str = element.select("a").text();
//    		//공백 없애기
//    		str = str.replace(" ", "");
//    		//System.out.println(str);
//    		List<String> listStr = new ArrayList<String>();
//    		int start = 0;
//    		//층수+일본어 조합이 5글자라서 5글자마다 잘라서 리스트에 저장 리스트에 적힌 요소들이 폴더명이 될것임.
//    		int last = 5;
//    		while(last <= str.length()){
//    			String temp = str.substring(start,last);
//    			//System.out.println(temp);
//    			listStr.add(temp);
//    			start = last;
//    			last+=5;
//    		}
    		
    		
    		
    		Elements trs = element.select("tr");
    		//System.out.println(trs.toString());
    		
    		boolean firstCheck = true;
    		
    		int layerCnt = 30;
    		int achieveCnt = 1;
    		for(int i=0; i<trs.size(); i++) { //
    			//Elements img = element.get(0).select("td").get(0).select("img");
    			firstCheck =true;
    			
    			//i번째 tr의 td들 파싱 -> 층마다로 묶임.
    			Elements sr = element.select("tr").get(i).select("td");
    			//찾아지는 td의 개수 파악
    			//System.out.println(sr.size());
    			//td가 없으면 th만 있는거라서 작업할 필요가 없으니 pass
    			if(sr.size() == 0) {
    				continue;
    			}
    			//td가 있으면 img 태그 찾고 해당 data-original 속성값 찾아서 해당 이미지 파싱.
    			else {
    				//프리뷰 페이지에서 파싱하고자하는 층수는 1~30 모든층이 아닌, 11~30 층수 이다.
    				//따라서 10층이라고 표기되는 1층 정보가 파싱되는것을 방지하기위해 11층까지만 파싱한다. 
    				if(layerCnt >= 11) {
    					//해당 층들의 상세공략 페이지 파싱용 href 저장.
    					String hrefStrs = sr.first().select("a").attr("href");
//    					//해당층들의 href 파싱된거 리스트에 담아둠 - 나중에 DB에 담아야될 수도 있어서.
//    					List<String> hrefList = new ArrayList<String>();
    					hrefList.add(hrefStrs);
    					
    					/*
    					//href 내용들 txt 로 저장하기.
    					hrefStrs += "\n"; //개행추가하기.
    					BufferedOutputStream bs = null;
    					bs = new BufferedOutputStream(new FileOutputStream("C:\\ictcbwd\\workspace\\Java\\crawling\\6pHref.txt",true));
    					bs.write(hrefStrs.getBytes());
    					bs.close();
    					 */
    					
    					//제한정보 파싱 - 제한정보는 tr들의 마지막(last) td들에 존재.
    					String translateStr = TranslateText.translate(sr.last().text());
//    					//제한정보 파싱된거 리스트에 담아둠 - 나중에 DB에 담아야 될수도 있어서.
//    					List<String> translateLimitInfoList = new ArrayList<String>();
    					translateLimitInfoList.add(translateStr);
    					
    					//<층수,제한정보> 파싱.
    					limitInfoMap.put(layerCnt, translateStr);
    					
    					//한층에대한 이미지크롤링 끝났으니, 다음층 폴더찾기위해 --
    					layerCnt--;
    					//다음층 폴더에 achice Icon 값을 다시 1부터 적재
    					achieveCnt = 1;
    				}
    			}
    		}
    		
//    		//층수,제한정보 파싱한거 확인
//    		for (Map.Entry<Integer, String> entry : limitInfoMap.entrySet()) {
//				int key = entry.getKey();
//				String value = entry.getValue();
//				
//				System.out.println(key + "층. 제한정보 : " + value);
//			}
    		
    		
    		//탑 보스 테이블Dto에 - 층수, 제한정보 일단 넣어두기. 
    		//S3 에 보스 아이콘 URL 올리고나서 여기서 Dto에도 넣어주기.
    		//Map<층수,Dto> 탑 보스 정보 테이블 Map
    		TowerBossDto towerBossDto = TowerBossDto.builder().build();
    		Map<Integer, TowerBossDto> towerBossDtoMap = new TreeMap<Integer, TowerBossDto>();
    		for (Map.Entry<Integer, String> entry : limitInfoMap.entrySet()) {
    			int layer = entry.getKey();
				String limitStr = entry.getValue();
				
				//타워 아이디 값 세팅 - 원래는 조회해서 같은 이름의 보스이름을 가진 아이디를 받아와야하지만, 지금은 Asgard 만 넣을것이므로 아이디값 1 로넣어주면됨.
				towerBossDto.setTowerId(1);
				
				//층 정보 세팅
				towerBossDto.setLayer(layer);
				//제한정보 세팅
				towerBossDto.setLimitInfo(limitStr);
				
				//https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/TowerBossIcon/Asgard/11boss.jpg
				String baseIconUrl = "https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/TowerBossIcon/";
				String fullIconUrl = baseIconUrl + BossFolderStr + "/" + layer + "boss.jpg";
				String key = "TowerBossIcon/" + BossFolderStr + "/" + layer + "boss.jpg";
				URL awsUrl = s3Client.getUrl("euichul-pocorong-imgs", key);
                HttpURLConnection connection = (HttpURLConnection)awsUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int code = connection.getResponseCode();
                
                if(code == 200) {
                	towerBossDto.setIconURL(fullIconUrl);
                }
                else {
                	fullIconUrl = null;
                	towerBossDto.setIconURL(fullIconUrl);
                }
				
				//보스아이콘URL 세팅
				
				//towerBossDtoMap에 담아두기.
				towerBossDtoMap.put(layer, towerBossDto);
				
				//다음 층수의 Dto 세팅하기위해 초기화.
				towerBossDto = TowerBossDto.builder().build();
				
			}
    		
    		//넣은 towerBossDtoMap 확인.
    		System.out.println("//////////// TowerBossInfo 테이블에 Insert 하기 ////////////////");
    		for (Map.Entry<Integer, TowerBossDto> entry : towerBossDtoMap.entrySet()) {
    			int layer = entry.getKey();
    			TowerBossDto dto = entry.getValue();
    			
    			//System.out.println("key 값 : " + layer);
    			System.out.println(layer + ". 층 타워 아이디 : " + dto.getTowerId());
    			System.out.println(layer + ". 층 층 수정 보 : " + dto.getLayer());
    			System.out.println(layer + ". 층 제한정보 : " + dto.getLimitInfo());
    			System.out.println(layer + ". 층 보스 아이콘URL : " + dto.getIconURL());
    			
    			towerDao.insertTowerBossInfo(dto);
    		}
    		////////////////////////////////////////////////////////////////////////////////////////////////////
    		
    		//가져온 폴더명 저장 층별로 보상아이콘이 여러개 있기때문에 Map 에 넣기.
    		Map<Integer, Integer> fileNamesMap = new TreeMap<Integer, Integer>(); 
    		List<String> fileNamesList = new ArrayList<String>();
    		
    		String basePath2 = "C:\\ictcbwd\\workspace\\Java\\crawling\\6pResult\\";
    		String bossName2 = "Asgard";
    		String bossIconFullPath2 = "";
    		
    		for(int j=11; j<=30; j++) {
    			bossIconFullPath2 = basePath2 + bossName2 +"\\"+ j +"층공략\\"+"achieveIcon";
    			
    			//디렉토리 파일 전체목록 가져오기.
    			File path = new File(bossIconFullPath2);
    			File[] fileList = path.listFiles();
    			//해당폴더안에 들어있는 파일개수 세는거라서, 1. 만 반복되면 해당 폴더에 파일이 1개라서 그런것임.
    			int fileCnt = 1;
    			if(fileList.length > 0){
    				for(int i=0; i < fileList.length; i++){
    					//System.out.println(fileList[i].toString()) ; //풀경로추출
    					String fileName = fileList[i].getName(); //파일이름만 추출
    					//System.out.println(fileCnt +" . " + fileName);
    					fileCnt++;
    					fileNamesList.add(fileName);
    				}
    				fileNamesMap.put(j, fileNamesList.size());
    				//리스트 맵에 넣었으니 다음을 위해 초기화.
    				fileNamesList.clear();
    			}
    		}
    		
    		
    		//탑 보스 보상정보 테이블 Dto 에 - 층수 일단 넣어두기.
    		//S3에 (보상) 아이콘URL 올리고나서 여기서 Dto에도 넣어주기.
    		//꼭 이함수 안에서 했어야하는건 아니긴함.
    		TowerBossRewardDto towerBossRewardDto = TowerBossRewardDto.builder().build();
    		List<TowerBossRewardDto> towerBossRewardList = new ArrayList<TowerBossRewardDto>();
    		Map<Integer, List<TowerBossRewardDto>> towerBossRewardDtoMap = new TreeMap<Integer, List<TowerBossRewardDto>>();
    		for (Map.Entry<Integer, Integer> entry : fileNamesMap.entrySet()) {
    			int layer = entry.getKey();
    			
    			//해당 층의 보상아이콘 개수만큼 for문을 돌려준다. Dto를 해당아이콘들의 Url 별로 만들어 넣어줘야하기때문.
    			for(int i=1; i<=entry.getValue(); i++) {
    				//층 정보 세팅
        			towerBossRewardDto.setLayer(layer);
        			
    				//보상 아이콘URL 세팅
        			String baseIconUrl = "https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/TowerBossRewardIcon/";
    				String fullIconUrl = baseIconUrl + BossFolderStr + "/" + layer + "층/" + i +"achieve.jpg";
    				String key = "TowerBossRewardIcon/" + BossFolderStr + "/" + layer + "층/" + i +"achieve.jpg";
    				URL awsUrl = s3Client.getUrl("euichul-pocorong-imgs", key);
                    HttpURLConnection connection = (HttpURLConnection)awsUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    int code = connection.getResponseCode();
                    
                    if(code == 200) {
                    	towerBossRewardDto.setIconURL(fullIconUrl);
                    }
                    else {
                    	fullIconUrl = null;
                    	towerBossRewardDto.setIconURL(fullIconUrl);
                    }
                    
                    towerBossRewardList.add(towerBossRewardDto);
                    towerBossRewardDto = TowerBossRewardDto.builder().build();
    			}
				
				//towerBossRewardDtoMap에 담아두기.
				towerBossRewardDtoMap.put(layer, new ArrayList<TowerBossRewardDto>(towerBossRewardList));
				
				//다음 층수의 Dto List들 세팅하기위해 초기화.
				towerBossRewardList.clear();
				
			}
    		System.out.println();
    		System.out.println("////////////////////towerBossRewardInfo 테이블에 데이터 넣기 ///////////////");
    		//넣은 towerBossRewardDtoMap 확인.
    		for (Map.Entry<Integer, List<TowerBossRewardDto>> entry : towerBossRewardDtoMap.entrySet()) {
    			int layer = entry.getKey();
    			List<TowerBossRewardDto> dtoList = entry.getValue();
    			for (TowerBossRewardDto dto : dtoList) {					
    				//System.out.println("key 값 : " + layer);
    				System.out.println(layer + ". 층 층 수정 보 : " + dto.getLayer());
    				System.out.println(layer + ". 층 보상 아이콘URL : " + dto.getIconURL());
    				
    				towerDao.insertTowerBossRewardInfo(dto);
				}
    			
    		}
    		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//6p 크롤링
	public void towerPreviewCrawl() {
	
		//크롤링 할 url
		String url = "https://gamewith.jp/pocodun/article/show/122072";
		//크롤링할 url에 맞춰 폴더도 변경
		String BossFolderStr = "Asgard";
		
		//String url = "https://gamewith.jp/pocodun/article/show/127603";
		//String BossFolderStr = "Catastropy";
		
		//페이지 전체소스 저장
		Document doc = null;
		
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//div의 클래스가 pd_tower_tips 인것들 싹다 모음. 모든 검색은 이아래에서 파싱된다. 가장큰 틀.
		Elements element = doc.select("div.pd_tower_tips");    

		System.out.println("============================================================");

		
		//test용 파싱할 태그들 html 파일로 구성하기위한 변수.
		//BufferedOutputStream bs = null;
    	try {
    		// https://coding-factory.tistory.com/282
    		//test 용 파싱한 태그들 html 파일로 구성.
//    		bs = new BufferedOutputStream(new FileOutputStream("C:\\ictcbwd\\workspace\\Java\\crawling\\6pOutput.html"));
//    		
//    		for (Element elem : element) {
//    			String str = elem.toString();
//				System.out.println(str);
//				bs.write(str.getBytes()); //Byte형으로만 넣을 수 있음					
//			}
    		
    		    		
    		
    		//a 태그안에 적힌 층수 text 파싱.
    		//a 태그안에 적힌 text 싹다 모음.
    		String str = element.select("a").text();
    		//공백 없애기
    		str = str.replace(" ", "");
    		//System.out.println(str);
    		List<String> listStr = new ArrayList<String>();
    		int start = 0;
    		//층수+일본어 조합이 5글자라서 5글자마다 잘라서 리스트에 저장 리스트에 적힌 요소들이 폴더명이 될것임.
    		int last = 5;
    		while(last <= str.length()){
    			String temp = str.substring(start,last);
    			//System.out.println(temp);
    			listStr.add(temp);
    			start = last;
    			last+=5;
    		}
    		
//    		//층수 text 를 폴더명으로 구성.
//    		for (String strs : listStr) {
//    			//폴더명으로 들어갈거 번역
//    			String tempStr = TranslateText.translate(strs);
//    			tempStr = tempStr.replace(" ","");
//    			//폴더명으로 지정할 string 값, 해당보스폴더명 파라미터로 넘기기
//				Utils.makeFolderOf6p(tempStr,BossFolderStr);
//    			
//			}
    		
    		
    		/////////////////////////////////이미지 크롤링///////////////////////////////////////
    		
    		  
    		Elements trs = element.select("tr");
    		//System.out.println(trs.toString());
    		
    		boolean firstCheck = true;
    		
    		int layerCnt = 30;
    		int achieveCnt = 1;
    		for(int i=0; i<trs.size(); i++) { //
    			//Elements img = element.get(0).select("td").get(0).select("img");
    			firstCheck =true;
    			
    			//i번째 tr의 td들 파싱 -> 층마다로 묶임.
    			Elements sr = element.select("tr").get(i).select("td");
    			//찾아지는 td의 개수 파악
    			//System.out.println(sr.size());
    			//td가 없으면 th만 있는거라서 작업할 필요가 없으니 pass
    			if(sr.size() == 0) {
    				continue;
    			}
    			//td가 있으면 img 태그 찾고 해당 data-original 속성값 찾아서 해당 이미지 파싱.
    			else {
    				
    				//해당 층들의 상세공략 페이지 파싱용 href 저장.
    				String hrefStrs = sr.first().select("a").attr("href");
    				System.out.println("href : " + hrefStrs);
    				//System.out.println(sr.first().toString());
//    				//해당층들의 href 파싱된거 리스트에 담아둠 - 나중에 DB에 담아야될 수도 있어서.
//    				List<String> hrefList = new ArrayList<String>();
    				hrefList.add(hrefStrs);
    				/*
    				//href 내용들 txt 로 저장하기.
    				hrefStrs += "\n"; //개행추가하기.
    				BufferedOutputStream bs = null;
    				bs = new BufferedOutputStream(new FileOutputStream("C:\\ictcbwd\\workspace\\Java\\crawling\\6pHref.txt",true));
    				bs.write(hrefStrs.getBytes());
    				bs.close();
    				*/
    				
    				//제한정보 파싱 - 제한정보는 tr들의 마지막(last) td들에 존재.
    				String translateStr = TranslateText.translate(sr.last().text());
//    				//제한정보 파싱된거 리스트에 담아둠 - 나중에 DB에 담아야 될수도 있어서.
//    				List<String> translateLimitInfoList = new ArrayList<String>();
    				translateLimitInfoList.add(translateStr);
    				
    				
    				Elements eles = sr.select("img");
    				for (Element elem : eles) {
						
						Elements elems = elem.getElementsByAttribute("data-original");
						
						if(elems.size() != 0)
						for (Element els : elems) {
							String inUrl = els.attr("data-original");
							
							//System.out.println("inUrl : " + inUrl);
						
							URL imgUrl = new URL(inUrl);
							HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
							//System.out.println(conn.getContentLength());
							
							InputStream is = conn.getInputStream();
							BufferedInputStream bis = new BufferedInputStream(is);
							
							String FilePath = "";
							//보스의경우 BossIcon 폴더에
							if(firstCheck == true) {								
								FilePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\6pResult\\"+BossFolderStr+"\\"+layerCnt+"층공략\\Bossicon\\"+layerCnt+"boss.jpg";
								firstCheck = false;
							}
							//나머지 보상들은 achieveIcon 폴더에
							else {
								//System.out.println("achieveCnt : "+ achieveCnt);
								FilePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\6pResult\\"+BossFolderStr+"\\"+layerCnt+"층공략\\achieveIcon\\"+achieveCnt+"achieve.jpg";
								achieveCnt++;
							}
							FileOutputStream os = new FileOutputStream(FilePath);
							BufferedOutputStream bos = new BufferedOutputStream(os);
							
							int byteImg;
							
							byte[] buf = new byte[conn.getContentLength()];
							while((byteImg = bis.read(buf)) != -1) {
								bos.write(buf,0,byteImg);
							}
							
							
							bos.close();
							os.close();
							bis.close();
							is.close();
						}
					}
    				//한층에대한 이미지크롤링 끝났으니, 다음층 폴더찾기위해 --
    				if(layerCnt > 0) {layerCnt--;}
    				//다음층 폴더에 achice Icon 값을 다시 1부터 적재
    				achieveCnt = 1;
    			}
    		}
    		

    	} catch (Exception e) {
                    e.getStackTrace();
    		// TODO: handle exception
    	}finally {
//    		try {
//				bs.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} //반드시 닫는다.
    	} 

    	
		System.out.println("============================================================");
	
	}
}
