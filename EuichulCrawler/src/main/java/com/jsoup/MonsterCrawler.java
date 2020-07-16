package com.jsoup;

import java.io.BufferedInputStream;

import org.springframework.retry.annotation.EnableRetry;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.config.DBConfig;
import com.dao.MonsterDao;
import com.dto.DownloadAndS3UploadDto;
import com.dto.MonsterChainSkillDto;
import com.dto.MonsterDto;
import com.dto.MonsterEvolutionDto;
import com.dto.MonsterLeaderSkillDto;
import com.dto.MonsterNormalSkillDto;
import com.sun.jdi.connect.spi.Connection;
import com.util.TranslateText;

//@Component
public class MonsterCrawler {
//	public static void main(String[] args) {
//
//		//MonsterCrawler mc = new MonsterCrawler();
//		
//		// 3p - 몬스터 미리보기 에서 몬스터 상세페이지로 이동 할 수 있는 Href 만 먼저 뽑는다. + 몬스터 정보테이블의 아이콘 URL 도 여기서 파싱.
//		//mc.monsterPreviewHrefCrawl();
//		
//		// 3p - 몬스터 미리보기 에서 몬스터 정보 테이블에 들어갈 ICON_URL 만 따로 파싱한다. <몬스터이름,아이콘URL> 로 파싱하니까,
//		//나중에 몬스터 정보 테이블에 넣을때, 해당 몬스터랑 같은 이름 인 거에 URL 넣으면 됨.
//		//해당 함수 안에서 폴더에 아이콘.jpg 도 따놨음.
//		//mc.monsterPreviewIconUrlCrawl();
//		
//		// 4p
//		//mc.monsterDetailCrawl();
//		
//		//aws 의 S3 저장소를 활용하여 몬스터이름.jpg 아이콘들 저장하기.
//		//mc.settingAwsAndMonsterIconUrlFileUploadToS3();
//
//		//몬스터 스크린샷 부분만 따로 파싱해서 폴더안에 이미지.jpg 형태로 다운로드 해놓기.
//		//mc.monsterScreenshotUrlCrawlAndDownload();
//		
//		//aws 의 S3 저장소를 활용하여 몬스터이름.jpg 스크린샷 URL 저장하기.
//		//mc.settingAwsAndScreenshotUrlFileUploadToS3();
//		
//		//몬스터 진화재료 <이름,아이콘> 부분만 따로 파싱
//		//mc.monsterEvolutionIconUrlCrawl();
//		//위에서 몬스터 진화재료 파싱한거 txt에 직접 저장해놓고 해당 txt 불러와서 이미지 다운로드.
//		//mc.monsterEvolutionIconUrlDownload();
//		
//		//aws 의 S3 저장소를 활용하여 몬스터이름.jpg 진화재료 아이콘들 저장하기.
//		//mc.settingAwsAndEvolutionIconFileUploadToS3();
//		
//		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DBConfig.class)) {
//			MonsterCrawler mc = context.getBean(MonsterCrawler.class);
//			//MonsterDto에 S3에 저장한 아이콘URL 과 풀스크린샷 URL 저장후 DB에 저장하기.
//			//mc.getObjectUrlAndSetMonsterDtoAndInsertDB();
//			
//			//몬스터이름만 다시 파싱해서, MonsterEvolutionDto 랑 짬뽕한다음, MONSTER_EVOLUTION_INFO에 저장하기
//			//mc.getObjectUrlAndSetMonsterEvolutionDtoAndInsertDB();
//			
//			//몬스터이름만 다시 파싱해서, 일반,체인,리더 스킬 Dto에 MonsterId 값 넣고 각각의 테이블에 Insert시키기.
//			mc.setMonsterNormalChainLeaderSkillDtoAndInsertDB();
//		}
//	}
	
	@Autowired private static Queue<DownloadAndS3UploadDto> mcMessageQueue;
	@Autowired private MonsterDao monsterDao;
	
	public MonsterDao getMonsterDao() {
		return monsterDao;
	}
	
	public void setMonsterNormalChainLeaderSkillDtoAndInsertDB() {
		//주의 사항 : Dto에 MONSTER_INFO 테이블의 MONSTER_ID 값도 넣어야함.
		MonsterDao monsterDao = getMonsterDao();
		
		String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};
		
		List<String> attriList = Arrays.asList(attriStrs);
		//List<String> detailHrefList = new ArrayList<String>();
		Map<String,String> detailHrefMap = new TreeMap<String, String>();
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
					//detailHrefList.add(line);
					detailHrefMap.put(line, line);
				}
				// .readLine()은 끝에 개행문자를 읽지 않는다.
				bufReader.close();
			} catch (FileNotFoundException e) {
				// TODO: handle exception
			} catch (IOException e) {
				System.out.println(e);
			}
		}
		
		/////////////////////////////////////파싱 시작////////////////////////////////////
		for(Map.Entry<String, String> detailHrefMapEntry : detailHrefMap.entrySet()) {
			
			String url = detailHrefMapEntry.getKey();
			//String url = "https://gamewith.jp/pocodun/article/show/73009";
			//String url = "https://gamewith.jp/pocodun/article/show/73362";
			//String url = "https://gamewith.jp/pocodun/article/show/100682";
		
		
			Document doc = null;
			
			try {
				doc = Jsoup.connect(url).get();
				
				//몬스터순서번호, 몬스터이름 파싱 (레어도는 아래에서 다시 파싱중)
				Elements els = doc.getElementsByTag("h2");
		
				Map<Integer,String> MonsterNameMap = new TreeMap<Integer,String>();
				
				int NumberCnt=1;
				for (Element el : els) {
					String elstr = el.text();
					if(elstr.contains("★"))
					{
						String elText = el.toString() + "\n";
						
						//별제거
						elstr = elstr.replace("★", "");
						//System.out.println(elstr);
						
						//숫자제거 및 value로 삽입 / 이름 key로 삽입
						char valueofRarity = elstr.charAt(0);
						elstr = elstr.replace(String.valueOf(valueofRarity), "");
						
						MonsterNameMap.put(NumberCnt,TranslateText.translate(elstr));
						NumberCnt++;
					}
				}
				
				//중간체크
				System.out.println();
				System.out.println("/////////////////MonsterNameMap에 몬스터 이름넣은거 확인///////////////");
				System.out.println("MonsterNameMap Size : " + MonsterNameMap.size());
				for(Map.Entry<Integer, String> entry : MonsterNameMap.entrySet()) {
					int MonCnt = entry.getKey();
					String MonsterName = entry.getValue();
					System.out.println(MonCnt + ". name : " + MonsterName);
				}
				
				///////////////////////////////////////////////////////////////////////////////
			
				
				///////////////////////////몬스터 스킬 정보 테이블 관련 파싱 시작 //////////////////////////////
					
				//스킬정보 파싱
				Elements skillEls = doc.select("div.pd_skill");
				
				//<순서구분용key,MonsterNormalSkillDto>
				Map<Integer,List<MonsterNormalSkillDto>> monsterNormalSkillInfoMap= new TreeMap<Integer, List<MonsterNormalSkillDto>>();
				MonsterNormalSkillDto monsterNormalSkillDto = MonsterNormalSkillDto.builder().build();
				List<MonsterNormalSkillDto> monsterNormalSkillList = new ArrayList<MonsterNormalSkillDto>();
				
				//<순서구분용key,MonsterChainSkillDto>
				Map<Integer,List<MonsterChainSkillDto>> monsterChainSkillInfoMap= new TreeMap<Integer, List<MonsterChainSkillDto>>();
				MonsterChainSkillDto monsterChainSkillDto = MonsterChainSkillDto.builder().build();
				List<MonsterChainSkillDto> monsterChainSkillList = new ArrayList<MonsterChainSkillDto>();
				
				
				//순서구분용 key가 될 cnt
				int MonsterCntNumbers = 1;
				//<h3>기술정보 / <h3>체인스킬정보 총2개가 끝나면 몬스터 하나가 끝난것. -> 2가 0이되면 순서구분용 key 를 ++ 하면 됨.
				int h3Cnt = 0;
				
				//현재 h3 tag 가 무엇인지 확인용도
				String nowH3TagText = "";
				//<h4>기술이름 2개면 한계돌파 도 저장시켜줘야함.
				int h4Cnt =0;
				//<td>의 한계돌파 정보도 있으면 저장시켜줘야함.
				int pd_skillCnt = 0;
				//같은 체인스킬이름인데 발동수랑 스킬설명만 다른경우, 체인스킬이름이 또 다시 파싱되지않으므로, 체인스킬이름을 임시저장해준뒤, 이 정보를 넣어준다.
				String tempChainSkillName = "";
				for (Element element : skillEls) {
					if(skillEls.size() !=0 ) {
						Element prevh4 = element.previousElementSibling().previousElementSibling();
						
						//스킬 구분 정보 - 일반기술/체인스킬
						if(prevh4 !=null) {
							String transPrevh4 = TranslateText.translate(prevh4.toString());
							transPrevh4 += "\n";
							//System.out.println(prevh4.toString());
							
							//os.write(transPrevh4.getBytes());
							
							//'기술'이 들어가면 '일반' 으로 분류
							if(transPrevh4.contains("기술")) {
								//System.out.println("일반 스킬 정보로 분류");
								nowH3TagText = "일반";
								
							}
							//'체인'이 들어가면 '체인' 으로 분류
							else if(transPrevh4.contains("체인")) {
								//System.out.println("체인 스킬 정보로 분류");
								nowH3TagText = "체인";
								
							}
							
							//h3가 하나씩 입력될때마다 h3를 올려준다. h3태그가 나왔다는걸 의미.
							h3Cnt++;
							
							//h3에 다시 들어왔다는것은, 다음 일반/체인 스킬 정보를 파싱하기위한 한돌 전,후 구분을 다시 처음부터 해도된다는것이다.   
							h4Cnt = 0;
							pd_skillCnt = 0;
							//h3에 다시들어왔으니까, 체인스킬이름을 다시 갈아줘야한다.
							tempChainSkillName = "";
							
							//기술정보/체인스킬정보 <h3> 2개가 끝났다면
							if(h3Cnt == 3) {
								h3Cnt = 1;
								//h4Cnt = 0;
								//pd_skillCnt = 0;
								
								//여기서 저장 하고 있는 이유 : h3는 2로 다들어왔어도 h3 아래있는 h4 나 div 를 돌고있기때문에 h3가 3이됬을때 다른몬스터 이제 들어간다는것이므로 이때 이전에 파싱한것들 저장하는것이다. 
								//몬스터 일반스킬정보/체인스킬정보 구성한거 저장해놓고 다음몬스터 스킬정보 저장할 준비. 
								monsterNormalSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterNormalSkillDto>(monsterNormalSkillList));
								monsterNormalSkillList.clear();
								
								
								monsterChainSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterChainSkillDto>(monsterChainSkillList));
								monsterChainSkillList.clear();
								
								MonsterCntNumbers++;
							}
						}
						
						//스킬이름 <h4>
						Element prevh3 = element.previousElementSibling();
						//Element prevh3 = prevh4.nextElementSibling();
						if(prevh3 != null) {
							String transPrevh3tag = TranslateText.translate(prevh3.toString());
							String transPrevh3 = TranslateText.translate(prevh3.text());
							//System.out.println("스킬이름 번역확인 : " + transPrevh3);
							//transPrevh3 += "\n";
							//System.out.println(prevh3.toString());
				
							String transn = transPrevh3tag + "\n";
							//os.write(transn.getBytes());
							
							//h3 가 한번 나왔을때는 일반 스킬 정보에 스킬이름을 넣는다.
							if(h3Cnt == 1) {
							//if(nowH3TagText.equals("일반")) {
								switch (h4Cnt) {
								case 0://h4가 처음 등장했을때는 한계돌파 전 스킬정보이다.
									monsterNormalSkillDto.setSkillType("한계돌파전");
									monsterNormalSkillDto.setSkillName(transPrevh3);
									//System.out.println("한계돌파 전 일반 스킬이름 입력 : " + transPrevh3);
									break;
								case 1://h4가 두번째로 등장했을때는 한계돌파 후 스킬정보이다.
									monsterNormalSkillDto.setSkillType("한계돌파후");
									monsterNormalSkillDto.setSkillName(transPrevh3);
									//System.out.println("한계돌파 후 일반 스킬이름  입력 : " + transPrevh3);
									break;
								default:
									break;
								}
								
								h4Cnt++;
							}
							//h3가 두번째로 등장했을때는 체인스킬 정보에 스킬이름 넣는다.
							else if(h3Cnt == 2) {									
							//else if(nowH3TagText.equals("체인")) {
								switch (h4Cnt) {
								case 0://h4가 처음 등장했을때는 한계돌파 전 스킬정보이다.
									monsterChainSkillDto.setSkillType("한계돌파전");
									monsterChainSkillDto.setSkillName(transPrevh3);
									//체인스킬 이름이 같은경우, 여기에 다시 들어오지않고, 아래에서 발동수,스킬정보 만 다시파싱되기때문에, 임시로 저장해두고, 같은 이름을 저장해줄수있게해준다.
									tempChainSkillName = transPrevh3;
									//System.out.println("한계돌파 전 체인 스킬이름  입력 : " + transPrevh3);
									break;
								case 1://h4가 두번째로 등장했을때는 한계돌파 후 스킬정보이다.
									monsterChainSkillDto.setSkillType("한계돌파후");
									monsterChainSkillDto.setSkillName(transPrevh3);
									//체인스킬 이름이 같은경우, 여기에 다시 들어오지않고, 아래에서 발동수,스킬정보 만 다시파싱되기때문에, 임시로 저장해두고, 같은 이름을 저장해줄수있게해준다.
									tempChainSkillName = transPrevh3;
									//System.out.println("한계돌파 후 체인 스킬이름  입력 : " + transPrevh3);
									break;
								default:
									break;
								}
								
								h4Cnt++;
							}
							
							if(h4Cnt == 2) {
								h4Cnt =0;
							}
						}
						
						//발동수, 스킬정보
						String transpdskill = TranslateText.translate(element.toString());
						transpdskill +="\n";
						//System.out.println(element.toString());
						
						//os.write(transpdskill.getBytes());
						Elements tds = element.select("tr").get(1).select("td");
						if(tds == null) {
							System.out.println("tds 가 null");
						}
						else if(tds != null) {
							
							//td 가 한번 나왔을때는 일반 스킬 정보에 스킬이름을 넣는다.
							if(h3Cnt == 1) {
							//if(nowH3TagText.equals("일반")) {
								switch (pd_skillCnt) {
								case 0://pd_skill이 처음 등장했을때는 한계돌파 전 스킬정보이다.
									monsterNormalSkillDto.setSkillType("한계돌파전");
									monsterNormalSkillDto.setNumberOfTriggers(Integer.valueOf(tds.first().text()));
									monsterNormalSkillDto.setSkillInfo(TranslateText.translate(tds.last().text()));
									//System.out.println("한계돌파 전 일반 스킬정보 번역 확인 : "+ monsterNormalSkillDto.getSkillInfo());
									monsterNormalSkillList.add(monsterNormalSkillDto);
									monsterNormalSkillDto = MonsterNormalSkillDto.builder().build();
									break;
								case 1://pd_skill이 두번째로 등장했을때는 한계돌파 후 스킬정보이다.
									monsterNormalSkillDto.setSkillType("한계돌파후");
									monsterNormalSkillDto.setNumberOfTriggers(Integer.valueOf(tds.first().text()));
									monsterNormalSkillDto.setSkillInfo(TranslateText.translate(tds.last().text()));
									//System.out.println("한계돌파 후 일반 스킬정보 번역 확인 : "+ monsterNormalSkillDto.getSkillInfo());
									monsterNormalSkillList.add(monsterNormalSkillDto);
									monsterNormalSkillDto = MonsterNormalSkillDto.builder().build();
									break;
								default:
									break;
								}
								
								pd_skillCnt++;
								//System.out.println("일반스킬 정보 번역에서 pd_skillCnt 올림");
							}
							//h3가 두번째로 등장했을때는 체인스킬 정보에 스킬이름 넣는다.
							else if(h3Cnt == 2) {
							//else if(nowH3TagText.equals("체인")) {
								//System.out.println("h3Cnt : " + h3Cnt + " pd_skillCnt : " + pd_skillCnt);
								switch (pd_skillCnt) {
								case 0://pd_skill이 처음 등장했을때는 한계돌파 전 스킬정보이다.
									monsterChainSkillDto.setSkillType("한계돌파전");
									monsterChainSkillDto.setNumberOfTriggers(Integer.valueOf(tds.first().text()));
									monsterChainSkillDto.setSkillInfo(TranslateText.translate(tds.last().text()));
									//System.out.println("한계돌파 전 체인 스킬정보 번역 확인 : "+ monsterChainSkillDto.getSkillInfo());
									monsterChainSkillList.add(monsterChainSkillDto);
									monsterChainSkillDto = MonsterChainSkillDto.builder().build();
									
									//발동 체인이 한개만 있으면 위에서 끝내고 마치면 되는데,
									//체인스킬의 경우 발동 체인이 가령 ex) 5 , 9 ... 얼만큼 더 있을 수 있는지 모른다.
									int trSize = element.select("tr").size();
									if(trSize >= 3) {
										for(int i=2; i<trSize; i++) {
											Elements newtds = element.select("tr").get(i).select("td");
											monsterChainSkillDto.setSkillType("한계돌파전");
											monsterChainSkillDto.setNumberOfTriggers(Integer.valueOf(newtds.first().text()));
											monsterChainSkillDto.setSkillInfo(TranslateText.translate(newtds.last().text()));
											//System.out.println("한계돌파 전 체인 스킬정보 번역 확인 : "+ monsterChainSkillDto.getSkillInfo());
											//만약 체인 스킬 이름이 등록되지 않았다면, 체인스킬이름은같은데 발동수와 설명만 다른 상황이니까, 저장해놓은 체인스킬이름으로 매꿔준다.
											if(monsterChainSkillDto.getSkillName() == "" || monsterChainSkillDto.getSkillName() == null) {
												monsterChainSkillDto.setSkillName(tempChainSkillName);
											}
											monsterChainSkillList.add(monsterChainSkillDto);
											monsterChainSkillDto = MonsterChainSkillDto.builder().build();
										}
									}
									break;
								case 1://pd_skill이 두번째로 등장했을때는 한계돌파 후 스킬정보이다.
									monsterChainSkillDto.setSkillType("한계돌파후");
									monsterChainSkillDto.setNumberOfTriggers(Integer.valueOf(tds.first().text()));
									monsterChainSkillDto.setSkillInfo(TranslateText.translate(tds.last().text()));
									//System.out.println("한계돌파 후 체인 스킬정보 번역 확인 : "+ monsterChainSkillDto.getSkillInfo());
									monsterChainSkillList.add(monsterChainSkillDto);
									monsterChainSkillDto = MonsterChainSkillDto.builder().build();
									
									//발동 체인이 한개만 있으면 위에서 끝내고 마치면 되는데,
									//체인스킬의 경우 발동 체인이 가령 ex) 5 , 9 ... 얼만큼 더 있을 수 있는지 모른다.
									trSize = element.select("tr").size();
									if(trSize >= 3) {
										for(int i=2; i<trSize; i++) {
											Elements newtds = element.select("tr").get(i).select("td");
											monsterChainSkillDto.setSkillType("한계돌파후");
											monsterChainSkillDto.setNumberOfTriggers(Integer.valueOf(newtds.first().text()));
											monsterChainSkillDto.setSkillInfo(TranslateText.translate(newtds.last().text()));
											//System.out.println("한계돌파 후 체인 스킬정보 번역 확인 : "+ monsterChainSkillDto.getSkillInfo());
											//만약 체인 스킬 이름이 등록되지 않았다면, 체인스킬이름은같은데 발동수와 설명만 다른 상황이니까, 저장해놓은 체인스킬이름으로 매꿔준다.
											if(monsterChainSkillDto.getSkillName() == "" || monsterChainSkillDto.getSkillName() == null) {
												monsterChainSkillDto.setSkillName(tempChainSkillName);
											}
											monsterChainSkillList.add(monsterChainSkillDto);
											monsterChainSkillDto = MonsterChainSkillDto.builder().build();
										}
									}
									break;
								default:
									break;
								}
								
								pd_skillCnt++;
							}
							
							if(pd_skillCnt == 2) {
								pd_skillCnt =0;
							}
						}
						
						//기술정보/체인스킬정보 <h3> 2개가 끝났다면
						if(h3Cnt == 2) {
															
	//						//몬스터 일반스킬정보/체인스킬정보 구성한거 저장해놓고 다음몬스터 스킬정보 저장할 준비. 
	//						monsterNormalSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterNormalSkillDto>(monsterNormalSkillList));
	//						//monsterNormalSkillDto = MonsterNormalSkillDto.builder().build();
	//						monsterNormalSkillList.clear();
	//						
	//						
	//						monsterChainSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterChainSkillDto>(monsterChainSkillList));
	//						//monsterChainSkillDto = MonsterChainSkillDto.builder().build();
	//						monsterChainSkillList.clear();
							
						}
					}
				}
				
				//for문을 빠져나와서 마지막에 저장된 요소들을 저장해주는 로직이 필요하다.
				monsterNormalSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterNormalSkillDto>(monsterNormalSkillList));
				monsterChainSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterChainSkillDto>(monsterChainSkillList));
				
				///////////////////////////////////////몬스터 일반스킬, 체인스킬 파싱 완료 /////////////////////////////////
				System.out.println("");
//				System.out.println("///////////////////////중간체크 일반스킬/체인스킬 정보 ///////////////////////");
//					//중간체크
//					System.out.println("일반스킬 맵 사이즈 : " + monsterNormalSkillInfoMap.size());
//					//일반스킬정보
//					for (Map.Entry<Integer, List<MonsterNormalSkillDto>> entry : monsterNormalSkillInfoMap.entrySet()) {
//						int i = entry.getKey();
//						List<MonsterNormalSkillDto> tempList = entry.getValue();
//						System.out.println(i + ". 일반 스킬 입력된 개수 : " + tempList.size());
//						for (MonsterNormalSkillDto dto : tempList) {
//							System.out.println(i + ". 일반스킬 이름 : " + dto.getSkillName());
//							System.out.println(i + ". 일반스킬 한계돌파 : " + dto.getSkillType());
//							System.out.println(i + ". 일반스킬 발동수 : " + dto.getNumberOfTriggers());
//							System.out.println(i + ". 일반스킬 스킬정보 : " + dto.getSkillInfo());
//						}
//						System.out.println();
//					}
					
					
					///////////////////MonsterNameMap 이랑  monsterEvolutionInfoMap 짬뽕 ///////////////
					Map<String,List<MonsterNormalSkillDto>> mixedMonNormalSkillMap = new TreeMap<String, List<MonsterNormalSkillDto>>();
					//짬뽕할 기준 맵 : monsterNormalSkillInfoMap 으로 해야함. 
					//이유 : 몬스터의 이름은 있을수있지만 스킬정보는 없을수도있다. 우리는 몬스터이름이 아닌 스킬정보를 입력할것이므로 기준이된다.
					for (Map.Entry<Integer, List<MonsterNormalSkillDto>> entry : monsterNormalSkillInfoMap.entrySet()) {
						int i = entry.getKey();
						List<MonsterNormalSkillDto> valueList = entry.getValue();
						
						String keyStr = MonsterNameMap.get(i);
						mixedMonNormalSkillMap.put(keyStr, valueList);
					}
					//굳이 짬뽕할 필요 없이 바로 DB에 넣어도 되지만, 체크하기위해 넣음.
					System.out.println("//////////////////일반스킬 DB에 넣기 ///////////////////////");
					System.out.println("일반 스킬 입력된 개수 : " + mixedMonNormalSkillMap.size());
					for (Map.Entry<String, List<MonsterNormalSkillDto>> entry : mixedMonNormalSkillMap.entrySet()) {
						//DB에서 해당 이름을 가진 몬스터의 아이디값을 MONSTER_INFO 테이블에서 가져온뒤,
						
						int monsterId = 0;
						System.out.println("DB에 검색할 몬스터 이름 : " + entry.getKey());
						monsterId = monsterDao.getMonsterId(entry.getKey());
						//System.out.println("monsterId : " + monsterId);
						
						//존재하지 않으면 -1이 반환되기때문에 -1이 아닐때만 넣으면됨.
						if(monsterId != -1) {
							//MONSTER_EVOLUTION_INFO 테이블에 아이디값을 포함해서 넣는다.
							List<MonsterNormalSkillDto> valuedto = entry.getValue();
							int i=1;
							for (MonsterNormalSkillDto dto : valuedto) {
								dto.setMonsterId(monsterId);
								
								System.out.println(i + ". monsterId : " + dto.getMonsterId());
								System.out.println(i + ". 일반스킬 이름 : " + dto.getSkillName());
								System.out.println(i + ". 일반스킬 한계돌파 : " + dto.getSkillType());
								System.out.println(i + ". 일반스킬 발동수 : " + dto.getNumberOfTriggers());
								System.out.println(i + ". 일반스킬 스킬정보 : " + dto.getSkillInfo());
								
								monsterDao.insertMonsterNormalSkillInfo(dto);
								
								i++;
							}
						}
					}
					
					//System.out.println("/////////////////// 일반 스킬 정보 확인 끝 ///////////////////");
					//중간체크
					//System.out.println("체인스킬 맵 사이즈 : " + monsterChainSkillInfoMap.size());
					
					//체인스킬정보
//					for (Map.Entry<Integer, List<MonsterChainSkillDto>> entry : monsterChainSkillInfoMap.entrySet()) {
//						int i = entry.getKey();
//						List<MonsterChainSkillDto> tempList = entry.getValue();
//						for (MonsterChainSkillDto dto : tempList) {
//							System.out.println(i + ". 체인스킬 이름 : " + dto.getSkillName());
//							System.out.println(i + ". 체인스킬 한계돌파 : " + dto.getSkillType());
//							System.out.println(i + ". 체인스킬 발동수 : " + dto.getNumberOfTriggers());
//							System.out.println(i + ". 체인스킬 스킬정보 : " + dto.getSkillInfo());
//						}
//						System.out.println();
//					}
					
					//System.out.println("/////////////////// 체인 스킬 정보 확인 끝 ///////////////////");
					
					System.out.println();
						///////////////////MonsterNameMap 이랑  monsterEvolutionInfoMap 짬뽕 ///////////////
						Map<String,List<MonsterChainSkillDto>> mixedMonChainSkillMap = new TreeMap<String, List<MonsterChainSkillDto>>();
						//짬뽕할 기준 맵 : monsterNormalSkillInfoMap 으로 해야함. 
						//이유 : 몬스터의 이름은 있을수있지만 스킬정보는 없을수도있다. 우리는 몬스터이름이 아닌 스킬정보를 입력할것이므로 기준이된다.
						for (Map.Entry<Integer, List<MonsterChainSkillDto>> entry : monsterChainSkillInfoMap.entrySet()) {
							int i = entry.getKey();
							List<MonsterChainSkillDto> valueList = entry.getValue();
							
							String keyStr = MonsterNameMap.get(i);
							mixedMonChainSkillMap.put(keyStr, valueList);
						}
						//굳이 짬뽕할 필요 없이 바로 DB에 넣어도 되지만, 체크하기위해 넣음.
						System.out.println("//////////////////체인스킬 DB에 넣기 ///////////////////////");
						System.out.println("체인 스킬 입력된 개수 : " + mixedMonChainSkillMap.size());
						for (Map.Entry<String, List<MonsterChainSkillDto>> entry : mixedMonChainSkillMap.entrySet()) {
							//DB에서 해당 이름을 가진 몬스터의 아이디값을 MONSTER_INFO 테이블에서 가져온뒤,
							
							int monsterId = 0;
							System.out.println("DB에 검색할 몬스터 이름 : " + entry.getKey());
							monsterId = monsterDao.getMonsterId(entry.getKey());
							//System.out.println("monsterId : " + monsterId);
							
							//존재하지 않으면 -1이 반환되기때문에 -1이 아닐때만 넣으면됨.
							if(monsterId != -1) {
								//MONSTER_EVOLUTION_INFO 테이블에 아이디값을 포함해서 넣는다.
								List<MonsterChainSkillDto> valuedto = entry.getValue();
								int i=1;
								for (MonsterChainSkillDto dto : valuedto) {
									dto.setMonsterId(monsterId);
									
									System.out.println(i + ". monsterId : " + dto.getMonsterId());
									System.out.println(i + ". 체인스킬 이름 : " + dto.getSkillName());
									System.out.println(i + ". 체인스킬 한계돌파 : " + dto.getSkillType());
									System.out.println(i + ". 체인스킬 발동수 : " + dto.getNumberOfTriggers());
									System.out.println(i + ". 체인스킬 스킬정보 : " + dto.getSkillInfo());
									
									monsterDao.insertMonsterChainSkillInfo(dto);
									
									i++;
								}
							}
						}
					
					
					//////////////////////////////////리더 스킬 정보 파싱 시작 ////////////////////////////////
					
					//<순서구분용key,MonsterLeaderSkillDto>
					Map<Integer,List<MonsterLeaderSkillDto>> monsterLeaderSkillInfoMap= new TreeMap<Integer, List<MonsterLeaderSkillDto>>();
					MonsterLeaderSkillDto monsterLeaderSkillDto = MonsterLeaderSkillDto.builder().build();
					List<MonsterLeaderSkillDto> monsterLeaderSkillList = new ArrayList<MonsterLeaderSkillDto>();
					
					//DataOutputStream os = new DataOutputStream(new FileOutputStream(HrefFilePath));
					Elements LeaderskillEls = doc.select("h3");
					
					//순서 구분용 key
					int MonCnts =0;
					for (Element element : LeaderskillEls) {
						//String LeaderStr = TranslateText.translate(element.text());
						String LeaderStr = element.text();
						//리더 스킬 정보 라는 h3 태그 인 경우에만
						//리더스킬정보 : リーダースキル情報
						if(LeaderStr.contains("リーダースキル情報")) {
							MonCnts++;
							String h3trans = TranslateText.translate(element.toString()) + "\n";
							//System.out.println("h3trans : " + h3trans);
							//os.write(h3trans.getBytes());
							
							//다음 태그인 h4 태그 : 리더스킬 이름 을 파싱
							Element h3nexth4 = element.nextElementSibling();
							String h4trans =TranslateText.translate(h3nexth4.toString())+ "\n" ; 
							if(h3nexth4 != null) {
								//os.write(h4trans.getBytes());
								//<h4> 스탠 보호 </h4>  - 리더스킬 이름 파싱
								monsterLeaderSkillDto.setSkillType("한계돌파전");
								monsterLeaderSkillDto.setSkillName(TranslateText.translate(h3nexth4.text()));
								//System.out.println(MonCnts + ". 한계돌파 전 리더스킬 이름 : "+ monsterLeaderSkillDto.getSkillName());
								
								//h4 태그가 있으므로 table 파싱
								Element LeaderSkilltable = h3nexth4.nextElementSibling();
								String tabletrans = TranslateText.translate(LeaderSkilltable.toString()) + "\n";
								//System.out.println(tabletrans);
								//os.write(tabletrans.getBytes());
								//<td> 리더스킬 정보 <td> 파싱
								String LeaderSkillInfoStr = LeaderSkilltable.select("tr").get(1).select("td").text();
								monsterLeaderSkillDto.setSkillInfo(TranslateText.translate(LeaderSkillInfoStr));
								//System.out.println(MonCnts + ". 한계돌파 전 리더스킬 정보 : "+ monsterLeaderSkillDto.getSkillInfo());
								
								//우선 한계돌파 전 으로 저장된 리더스킬 정보를 리스트에 보관한다.
								monsterLeaderSkillList.add(monsterLeaderSkillDto);
								//이제 한계돌파 후 로 저장될 것이있을경우를 대비해 준비한다.
								monsterLeaderSkillDto = MonsterLeaderSkillDto.builder().build();
								
								//만약 한계돌파 이후 리더 스킬 정보가 있다라면, table 태그 다음에 a->div 안에 h4 태그가 또 있을것이다.
								Element checkNextLeaderh4 = LeaderSkilltable.nextElementSibling();
								if(checkNextLeaderh4 != null) {
									//만약 이태그가 a 태그 라면, 한계돌파 정보가 있다는것이므로 한번더 파싱해주자.
									String checkh4tagStr = checkNextLeaderh4.toString();
									char checkatag = checkh4tagStr.charAt(1);
									char a = 'a';
									//만약 <a 로 시작하는 태그라면,
									if(checkatag == a) {
										Element aNextdivtag = checkNextLeaderh4.nextElementSibling();
										String divtagStrs = TranslateText.translate(aNextdivtag.toString()) + "\n";
										//os.write(divtagStrs.getBytes());
										
										//System.out.println(MonCnts + ". 한계돌파 후 리더스킬 이름 : " + TranslateText.translate(aNextdivtag.select("h4").text()));
										//한계돌파후 리더스킬 이름, 정보 파싱
										monsterLeaderSkillDto.setSkillType("한계돌파후");
										monsterLeaderSkillDto.setSkillName(TranslateText.translate(aNextdivtag.select("h4").text()));
										//System.out.println(MonCnts + ". 한계돌파 후 리더스킬 정보 : " + TranslateText.translate(aNextdivtag.select("table").select("tr").get(1).select("td").text()));
										monsterLeaderSkillDto.setSkillInfo(TranslateText.translate(aNextdivtag.select("table").select("tr").get(1).select("td").text()));
										
										//한계돌파 데이터 파싱이 끝났으니 다시 리스트에 저장해주자
										monsterLeaderSkillList.add(monsterLeaderSkillDto);
										monsterLeaderSkillDto = MonsterLeaderSkillDto.builder().build();
									}
									
								}
								//한계돌파 후 리더스킬 이름과 정보가 있던 말던 리더스킬이름과 정보 파싱된것들을 저장은 시켜줘야한다.
								//한계돌파 이후 에 해당되는 if문이 지난다음에 정보를 저장해주면된다.
								//몬스터 한마리에대한 리더스킬 파싱이 끝났으니 맵에 저장해주자.
								monsterLeaderSkillInfoMap.put(MonCnts, new ArrayList<MonsterLeaderSkillDto>(monsterLeaderSkillList));
								//그리고 다음 몬스터의 리스트를 준비하기위해 clear
								monsterLeaderSkillList.clear();
							}
							
						}
					}
					
					////////////////////////////////// 몬스터 리더스킬 정보 파싱 완료 ////////////////////
					//중간체크
	//				System.out.println();
	//				System.out.println("////////////////리더 스킬 중간 체크 ////////////////////////");
	//				for (Map.Entry<Integer, List<MonsterLeaderSkillDto>> entry : monsterLeaderSkillInfoMap.entrySet()) {
	//					int i = entry.getKey();
	//					List<MonsterLeaderSkillDto> tempList = entry.getValue();
	//					for (MonsterLeaderSkillDto dto : tempList) {
	//						System.out.println(i + ". 리더스킬 이름 : " + dto.getSkillName());
	//						System.out.println(i + ". 리더스킬 한계돌파 : " + dto.getSkillType());
	//						System.out.println(i + ". 리더스킬 스킬정보 : " + dto.getSkillInfo());
	//					}
	//					System.out.println();
	//				}
					
					////////////////////////////////////////////////////////////////////////////
					System.out.println();
					///////////////////MonsterNameMap 이랑  monsterEvolutionInfoMap 짬뽕 ///////////////
					Map<String,List<MonsterLeaderSkillDto>> mixedMonLeaderSkillMap = new TreeMap<String, List<MonsterLeaderSkillDto>>();
					//짬뽕할 기준 맵 : monsterNormalSkillInfoMap 으로 해야함. 
					//이유 : 몬스터의 이름은 있을수있지만 스킬정보는 없을수도있다. 우리는 몬스터이름이 아닌 스킬정보를 입력할것이므로 기준이된다.
					for (Map.Entry<Integer, List<MonsterLeaderSkillDto>> entry : monsterLeaderSkillInfoMap.entrySet()) {
						int i = entry.getKey();
						List<MonsterLeaderSkillDto> valueList = entry.getValue();
						
						String keyStr = MonsterNameMap.get(i);
						mixedMonLeaderSkillMap.put(keyStr, valueList);
					}
					//굳이 짬뽕할 필요 없이 바로 DB에 넣어도 되지만, 체크하기위해 넣음.
					System.out.println("//////////////////리더스킬 DB에 넣기 ///////////////////////");
					System.out.println("체인 스킬 입력된 개수 : " + mixedMonLeaderSkillMap.size());
					for (Map.Entry<String, List<MonsterLeaderSkillDto>> entry : mixedMonLeaderSkillMap.entrySet()) {
						//DB에서 해당 이름을 가진 몬스터의 아이디값을 MONSTER_INFO 테이블에서 가져온뒤,
						
						int monsterId = 0;
						System.out.println("DB에 검색할 몬스터 이름 : " + entry.getKey());
						monsterId = monsterDao.getMonsterId(entry.getKey());
						//System.out.println("monsterId : " + monsterId);
						
						//존재하지 않으면 -1이 반환되기때문에 -1이 아닐때만 넣으면됨.
						if(monsterId != -1) {
							//MONSTER_EVOLUTION_INFO 테이블에 아이디값을 포함해서 넣는다.
							List<MonsterLeaderSkillDto> valuedto = entry.getValue();
							int i=1;
							for (MonsterLeaderSkillDto dto : valuedto) {
								dto.setMonsterId(monsterId);
								
								System.out.println(i + ". monsterId : " + dto.getMonsterId());
								System.out.println(i + ". 리더스킬 이름 : " + dto.getSkillName());
								System.out.println(i + ". 리더스킬 한계돌파 : " + dto.getSkillType());
								System.out.println(i + ". 리더스킬 스킬정보 : " + dto.getSkillInfo());
								
								
//								if(dto.getSkillName().contains("없음") && dto.getSkillName().contains("없음")) {
//									System.out.println("리더스킬 이름, 정보 가 없으니 insert 안함");
//								}
//								else {									
//									monsterDao.insertMonsterLeaderSkillInfo(dto);
//								}
								
								//없어도 없다고 해주는게 나을듯함. 
								monsterDao.insertMonsterLeaderSkillInfo(dto);
								
								i++;
							}
						}
					}
				
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void getObjectUrlAndSetMonsterEvolutionDtoAndInsertDB() {
		
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();
		
		//주의 사항 : Dto에 MONSTER_INFO 테이블의 MONSTER_ID 값도 넣어야함.
		MonsterDao monsterDao = getMonsterDao();
		
		String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};
		
		List<String> attriList = Arrays.asList(attriStrs);
		//List<String> detailHrefList = new ArrayList<String>();
		Map<String,String> detailHrefMap = new TreeMap<String, String>();
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
					//detailHrefList.add(line);
					detailHrefMap.put(line, line);
				}
				// .readLine()은 끝에 개행문자를 읽지 않는다.
				bufReader.close();
			} catch (FileNotFoundException e) {
				// TODO: handle exception
			} catch (IOException e) {
				System.out.println(e);
			}
		}
		
		/////////////////////////////////////파싱 시작////////////////////////////////////
		for(Map.Entry<String, String> detailHrefMapEntry : detailHrefMap.entrySet()) {
			//test
			//String url = "https://gamewith.jp/pocodun/article/show/73700";
			String url = detailHrefMapEntry.getKey();
			//String url = "https://gamewith.jp/pocodun/article/show/73009";
			
			Document doc = null;
			
			try {
				doc = Jsoup.connect(url).get();
			
			
				//몬스터순서번호, 몬스터이름 파싱 (레어도는 아래에서 다시 파싱중)
				Elements els = doc.getElementsByTag("h2");
		
				Map<Integer,String> MonsterNameMap = new TreeMap<Integer,String>();
				
				int NumberCnt=1;
				for (Element el : els) {
					String elstr = el.text();
					if(elstr.contains("★"))
					{
						String elText = el.toString() + "\n";
						
						//별제거
						elstr = elstr.replace("★", "");
						//System.out.println(elstr);
						
						//숫자제거 및 value로 삽입 / 이름 key로 삽입
						char valueofRarity = elstr.charAt(0);
						elstr = elstr.replace(String.valueOf(valueofRarity), "");
						
						MonsterNameMap.put(NumberCnt,TranslateText.translate(elstr));
						NumberCnt++;
					}
				}
				
				//중간체크
				System.out.println();
				System.out.println("/////////////////MonsterNameMap에 몬스터 이름넣은거 확인///////////////");
				System.out.println("MonsterNameMap Size : " + MonsterNameMap.size());
				for(Map.Entry<Integer, String> entry : MonsterNameMap.entrySet()) {
					int MonCnt = entry.getKey();
					String MonsterName = entry.getValue();
					System.out.println(MonCnt + ". name : " + MonsterName);
				}
				
				///////////////////////////////////////////////////////////////////////////////
				
				//몬스터 진화정보 파싱
				//<순서구분용key,MonsterLeaderSkillDto>
				Map<Integer,List<MonsterEvolutionDto>> monsterEvolutionInfoMap= new TreeMap<Integer, List<MonsterEvolutionDto>>();
				MonsterEvolutionDto monsterEvolutionDto = MonsterEvolutionDto.builder().build();
				List<MonsterEvolutionDto> monsterEvolutionList = new ArrayList<MonsterEvolutionDto>();
				
				int MonCount = 1;
				
				Elements evolutionElss = doc.select("div.pd_simple_table_col5");
				
				for (Element evolutionEls : evolutionElss) {
					
					//진화 재료 개수 모아두기
					List<String> requiredLuckthsStrList = evolutionEls.select("tr").get(0).select("th").eachText();
					
					//진화 재료 이름 모아두기
					List<String> MonNameList = evolutionEls.select("tr").get(1).select("td").eachText();
					
//					//진화 재료 아이콘URL 모아두기
//					List<String> iconUrlStrsList = new ArrayList<String>();
//					String urls = "";
//					Elements imgEls = evolutionEls.select("tr").get(1).select("noscript").select("img");
//					for (Element element : imgEls) {
//						urls = element.attr("src");
//						//System.out.println(urls);
//						iconUrlStrsList.add(urls);
//					}
					
					System.out.println("MonNameList size : " + MonNameList.size());
					System.out.println("requiredLuckthsStrList size : " + requiredLuckthsStrList.size());
					
					//몬스터 진화재료 정보 저장하기.
					//두개의 리스트는 사이즈가 다를 수있다. 몬스터이름은 있는데 개수는 없다거나 이런식으로. 
					//따라서 가장 큰 사이즈로 기준 사이즈를 잡아준뒤, 사이즈가 넘어가는 java.lang.IndexOutOfBoundsException 이 발생할경우를 대비해 사이즈가 작을때만 get 하는걸로 하면된다.
					int totalListSize = (MonNameList.size() <= requiredLuckthsStrList.size()) ? requiredLuckthsStrList.size() : MonNameList.size();
					
					for(int i=0; i<totalListSize; i++) {
						String evolutionMonName = null;
						if( i < MonNameList.size()) {							
							evolutionMonName = TranslateText.translate(MonNameList.get(i));
						}
						System.out.println("dto에 set 하기전 진화재료 이름 : " + evolutionMonName);
						monsterEvolutionDto.setName(evolutionMonName);
						
						int requiredLuckCnt = 0;
						if( i < requiredLuckthsStrList.size()) {							
							requiredLuckCnt = Integer.valueOf(requiredLuckthsStrList.get(i));
						}
						System.out.println("dto에 set 하기전 진화재료 개수 : " + requiredLuckCnt);
						monsterEvolutionDto.setRequiredLuck(requiredLuckCnt);
						
						//monsterEvolutionDto.setIconURL(iconUrlStrsList.get(i));
						
						//dto가 완성될때마다 dto를 list에 저장한다
						monsterEvolutionList.add(monsterEvolutionDto);
						//dto 를 다시 초기화시킨다.
						monsterEvolutionDto = MonsterEvolutionDto.builder().build();
					}
					
					//완성된 Dto를 Map에 저장시킨다.
					monsterEvolutionInfoMap.put(MonCount, new ArrayList<MonsterEvolutionDto>(monsterEvolutionList));
					//리스트를 초기화시킨다.
					monsterEvolutionList.clear();
					
					MonCount++;
				}
				
				////////////////////////////몬스터 진화정보재료 중간체크///////////////////////////////
				
	//			System.out.println("////////////////몬스터 진화 재료 중간 체크 ////////////////////////");
	//			for (Map.Entry<Integer, List<MonsterEvolutionDto>> entry : monsterEvolutionInfoMap.entrySet()) {
	//				int i = entry.getKey();
	//				List<MonsterEvolutionDto> tempList = entry.getValue();
	//				for (MonsterEvolutionDto dto : tempList) {
	//					System.out.println(i + ". 진화재료 이름 : " + dto.getName());
	//					System.out.println(i + ". 진화재료 개수 : " + dto.getRequiredLuck());
	//					System.out.println(i + ". 진화재료 URL : " + dto.getIconURL());
	//				}
	//				System.out.println();
	//			}
				
				///////////////////MonsterNameMap 이랑  monsterEvolutionInfoMap 짬뽕 ///////////////
				Map<String,List<MonsterEvolutionDto>> mixedMonEvolutionMap = new TreeMap<String, List<MonsterEvolutionDto>>();
				//짬뽕할 기준 맵 : monsterEvolutionInfoMap 으로 해야함. 
				//이유 : 7성으로 진화하기위한 재료가 있을수있지만, 6성으로 진화하기위한 재료는 없을수있다. 따라서 여기서는 사이즈가 1 , 그런데 몬스터이름이 담긴 맵은 사이즈가 2다. 우리에게 필요한 정보는 진화재료가 있는 몬스터의 이름이기때문
				for (Map.Entry<Integer, List<MonsterEvolutionDto>> entry : monsterEvolutionInfoMap.entrySet()) {
					int i = entry.getKey();
					List<MonsterEvolutionDto> valueList = entry.getValue();
					
					String keyStr = MonsterNameMap.get(i);
					mixedMonEvolutionMap.put(keyStr, valueList);
				}
				//굳이 짬뽕할 필요 없이 바로 DB에 넣어도 되지만, 체크하기위해 넣음.
				System.out.println("////////////////// DB에 넣기 ///////////////////////");
				for (Map.Entry<String, List<MonsterEvolutionDto>> entry : mixedMonEvolutionMap.entrySet()) {
					//DB에서 해당 이름을 가진 몬스터의 아이디값을 MONSTER_INFO 테이블에서 가져온뒤,
					
					int monsterId = 0;
					System.out.println("DB에 검색할 몬스터 이름 : " + entry.getKey());
					monsterId = monsterDao.getMonsterId(entry.getKey());
					//System.out.println("monsterId : " + monsterId);
					
					//존재하지 않으면 -1이 반환되기때문에 -1이 아닐때만 넣으면됨.
					if(monsterId != -1) {
						//MONSTER_EVOLUTION_INFO 테이블에 아이디값을 포함해서 넣는다.
						List<MonsterEvolutionDto> valuedto = entry.getValue();
						int i=1;
						for (MonsterEvolutionDto dto : valuedto) {
							dto.setMonsterId(monsterId);
							
							System.out.println(i + ". monsterId : " + dto.getMonsterId());
							System.out.println(i + ". 진화재료 이름 : " + dto.getName());
							//System.out.println(i + ". 진화재료 개수 : " + dto.getRequiredLuck());
							//System.out.println(i + ". 진화재료 URL : " + dto.getIconURL());
							
							//진화재료 iconUrl 을 S3의 객체URL로 다시세팅
							//https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/MonsterEvolutionIcon/
							String baseIconUrl = "https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/MonsterEvolutionIcon/";
							String fullIconUrl = baseIconUrl + dto.getName() + ".jpg";
							String key = "MonsterEvolutionIcon/" + dto.getName() + ".jpg";
							URL awsUrl = s3Client.getUrl("euichul-pocorong-imgs", key);
			                HttpURLConnection connection = (HttpURLConnection)awsUrl.openConnection();
			                connection.setRequestMethod("GET");
			                connection.connect();
			                int code = connection.getResponseCode();
			                
			                //System.out.println("icon response code : " + code);
			                
			                if(code == 200) {
			                	dto.setIconURL(fullIconUrl);
			                	System.out.println(i + ". 진화재료 iconUrl : " + dto.getIconURL());
			                }
			                else {
			                	fullIconUrl = null;
			                	dto.setIconURL(fullIconUrl);
			                }
							
							
							monsterDao.insertMonsterEvolutionInfo(dto);
							
							i++;
						}
					}
				}
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	}
	
	public void getObjectUrlAndSetMonsterDtoAndInsertDB() {
		MonsterDao monsterDao = getMonsterDao();
		
		S3Object fullObject = null, objectPortion = null, headerOverrideObject = null;
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		
		String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};
		
		List<String> attriList = Arrays.asList(attriStrs);
		//List<String> detailHrefList = new ArrayList<String>();
		Map<String,String> detailHrefMap = new TreeMap<String, String>();
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
					//detailHrefList.add(line);
					detailHrefMap.put(line, line);
				}
				// .readLine()은 끝에 개행문자를 읽지 않는다.
				bufReader.close();
			} catch (FileNotFoundException e) {
				// TODO: handle exception
			} catch (IOException e) {
				System.out.println(e);
			}
		}
			
		/*
		 지금 href.txt 들 돌면서 속성별로 리스트 하나에 몰아넣고 리스트 하나씩 몬스터 기본정보 돌고있을텐데, 이렇게 하다보니까,
		가령 스사노오 - 목 / 스사노오 - 어둠 이렇게 있다보니까
		다른 속성의 href.txt 에도 스사노오 https://gamewith.jp/pocodun/article/show/73142 가있고
		다른 속성의 href.txt 에도 스사노오 https://gamewith.jp/pocodun/article/show/73142 가 있으므로
		지금 속성하나씩 돌면서 href 훑어서 리스트에 저장해서 이렇게 돌리지말고,
		어차피 지금은 속성별로 몬스터 를 분류해서 DB에 넣고 이러지는 않기때문에, 애초에 처음부터 href.txt 속성별로 싹다 먼저 돌면서
		리스트 대신에, Map<String,String> 에 다가 key,value 양 쪽에 href 값을 넣게끔해서 중복을 막아주면 될것같다.
		 */
		//for (String url : detailHrefList) {
		for(Map.Entry<String, String> detailHrefMapEntry : detailHrefMap.entrySet()) {
				//test 용 아자토스 url
				//String url = "https://gamewith.jp/pocodun/article/show/73362";
				//test
				//String url = "https://gamewith.jp/pocodun/article/show/73700";
				//test
				//String url = "https://gamewith.jp/pocodun/article/show/122443";
				
				String url = detailHrefMapEntry.getKey();
			
				Document doc = null;
				
				try {
					doc = Jsoup.connect(url).get();
					
					//기본 정보 파싱 - 속성,부속성,타입,도감번호,레어도
					Map<Integer,MonsterDto> MonsterInfoMap = new TreeMap<Integer, MonsterDto>(); 
					MonsterDto tempMonsterDto = MonsterDto.builder().build();
					
					//Elements tables = doc.select("div.pd_simple_table_col3").select("table");
					Elements h3els = doc.select("h3");
					//os.write(tables.toString().getBytes());
					
					//몬스터의 평점 작업 해주려면 평점 관리 맵 key와 같이 맞춰줘야함.
					int MonsterNumberCnt =1;
					for (Element element : h3els) {
						//기본정보 라는 h3가 포함되어있으면 다음 tag로 오는 table 만 파싱.
						if(element.text().contains("基本情報"))
						{
							Elements tableEls = element.nextElementSibling().select("table");
							Elements tds = tableEls.select("tr").select("td");
							//os.write(tds.toString().getBytes());
							//기본정보 테이블에서 제공되고있는 정보를 순서대로 나열
							
							//1,1,2,4,5 로 출력되면 정상.
							int MonsterTextNumberCnt=1;
							for (Element element2 : tds) {
								
								//System.out.println(MonsterNumberCnt + ". text : " + element2.text());
								String headStr = (MonsterTextNumberCnt + ". text : ");
								String testStr = element2.text() +"\n";
								String ResultStr = headStr + testStr;
								//os.write(ResultStr.getBytes());
								
								String texts = element2.text();
								//System.out.println("ResultStr : " + ResultStr);
								
								
								//-가 아니라는건 작성이 되어있다는것. 작성되있을 경우에만 해준다.
								if(!texts.equals("-")) {
									
									switch (MonsterTextNumberCnt) {
									case 1:
										//속성/부속성
										String[] attris = texts.split("/");
										
										if(attris.length == 2) {
											//System.out.println("0 : " + TranslateText.translate(attris[0]));
											tempMonsterDto.setAttribute(TranslateText.translate(attris[0]));
											//System.out.println(MonsterTextNumberCnt + ". attri : " + tempMonsterDto.getAttribute());
											
											//System.out.println("1 : " + TranslateText.translate(attris[1]));
											tempMonsterDto.setAttributeSub(TranslateText.translate(attris[1]));
											//System.out.println(MonsterTextNumberCnt + ". attri_sub : " + tempMonsterDto.getAttributeSub());
										}
										else if(attris.length == 1) {
											//System.out.println("0 : " + TranslateText.translate(attris[0]));
											tempMonsterDto.setAttribute(TranslateText.translate(attris[0]));
											//System.out.println(MonsterTextNumberCnt + ". attri : " + tempMonsterDto.getAttribute());
										}
										
										break;
									case 2:
										//타입(ex) 신)
										tempMonsterDto.setType(TranslateText.translate(texts));
										//System.out.println(MonsterTextNumberCnt + ". type : " + tempMonsterDto.getType());
										break;
									case 3:
										//공격? 이라는 칸인데 다 - 라서 안해줘도될듯
										break;
									case 4:
										//도감넘버
										tempMonsterDto.setBookNum(Integer.valueOf(texts));
										//System.out.println(MonsterTextNumberCnt + ". bookNum : " + tempMonsterDto.getBookNum());
										break;
									case 5:
										//레어도
										String rarity = texts.replace("★", "");
										tempMonsterDto.setRarity(Integer.valueOf(rarity));
										//System.out.println(MonsterTextNumberCnt + ". rarity : " + tempMonsterDto.getRarity());
										break;
									case 6:
										//소울 어떤던전에서 획득하면되는지인데 필요없어보임 일단 여지를 남겨두겠음.
										break;
										
									default:
										break;
									}
								}
								MonsterTextNumberCnt++;
							}
							String Enter = "\n";
							//os.write(Enter.getBytes());
							
							
							//작성된 Dto 맵에 넣고나서 다시 초기화
							MonsterInfoMap.put(MonsterNumberCnt, tempMonsterDto);
							
							//작성완료된 Dto 확인
							//System.out.println(MonsterNumberCnt + ". type : " + tempMonsterDto.getType());
							//System.out.println(MonsterNumberCnt + ". attri : " + tempMonsterDto.getAttribute());
							//System.out.println(MonsterNumberCnt + ". attri_sub : " + tempMonsterDto.getAttributeSub());
							//System.out.println(MonsterNumberCnt + ". bookNum : " + tempMonsterDto.getBookNum());
							//System.out.println(MonsterNumberCnt + ". rarity : " + tempMonsterDto.getRarity());
							
							MonsterNumberCnt++;
							
							tempMonsterDto = MonsterDto.builder().build();
						}
					}
					//System.out.println("/////////////////몬스터 기본정보넣기 완료///////////////");
					
//					//중간체크
//					int MonCntz = 1;
//					System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//					for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//						MonsterDto valueDto = entry.getValue();
//						System.out.println(MonCntz + ". type : " + valueDto.getType());
//						System.out.println(MonCntz + ". attri : " + valueDto.getAttribute());
//						System.out.println(MonCntz + ". attri_sub : " + valueDto.getAttributeSub());
//						System.out.println(MonCntz + ". point : " + valueDto.getPoints());
//						System.out.println(MonCntz + ". bookNum : " + valueDto.getBookNum());
//						System.out.println(MonCntz + ". rarity : " + valueDto.getRarity());
//						MonCntz++;
//					}
//					System.out.println("/////////////////몬스터 기본정보넣기 - 중간체크 - 완료///////////////");
					
					///////////////////////////////////////////////////////////////////////////////
					
					
					//몬스터 이름, 레어도 파싱 -> 몬스터순서번호, 몬스터이름 파싱 (레어도는 아래에서 다시 파싱중)
					Elements els = doc.getElementsByTag("h2");
					//System.out.println( "els : " + els.toString());
					//os.write(els.toString().getBytes());
					//Map<String,Integer> MonsterNameMap = new TreeMap<String,Integer>();
					//Map<String,MonsterDto> MonsterNameMap = new TreeMap<String,MonsterDto>();
					Map<Integer,String> MonsterNameMap = new TreeMap<Integer,String>();
					
					//Dto 에 담아주기전에 보관용 map <몬스터이름, img주소> 혹은 <순서용key, img주소> 
					Map<Integer,String> MonsterImgHrefMap = new TreeMap<Integer, String>();
					
					int NumberCnt=1;
					for (Element el : els) {
						String elstr = el.text();
						if(elstr.contains("★"))
						{
							String elText = el.toString() + "\n";
							//os.write(elText.getBytes());
							//System.out.println(elText);
							
							Element img= el.nextElementSibling();
							String dataOriginal = img.attr("data-original");
							//os.write(img.toString().getBytes());
							//System.out.println(img.toString());
							//System.out.println(dataOriginal);
							//os.write(dataOriginal.getBytes());
							
							//순서용키,img주소 삽입
							MonsterImgHrefMap.put(NumberCnt, dataOriginal);
							
							//System.out.println(elstr + " / 별 있음");
							
							//별제거
							elstr = elstr.replace("★", "");
							//System.out.println(elstr);
							
							//숫자제거 및 value로 삽입 / 이름 key로 삽입
							char valueofRarity = elstr.charAt(0);
							elstr = elstr.replace(String.valueOf(valueofRarity), "");
							//System.out.println("key : " + elstr + " / value : " + valueofRarity);
							//MonsterNameMap.put(elstr, Integer.valueOf(valueofRarity));
							//MonsterNameMap.put(elstr, MonsterDto.builder().rarity(Integer.valueOf(valueofRarity)).name(elstr).build());
							MonsterNameMap.put(NumberCnt,elstr);
							NumberCnt++;
						}
						else {
							//System.out.println(elstr + " / 별없음");
						}
					}
					
					//os.write(MonsterNameMap.toString().getBytes());
					
					//System.out.println("맵 순서 체크 : " + MonsterNameMap.toString());
					
					//System.out.println("맵 이미지주소 체크 : " + MonsterImgHrefMap.toString());
					//////////////////////////////////////////////////////////////////////////////
					//각각의 map에서 같은 순서번호 갖고있는것들을 찾아 dto에 몬스터이름 넣기. , 풀스크린샷 이미지 주소도 넣기.
					for(int i=1; i<=MonsterInfoMap.size(); i++) {
						
						//기존에 저장된 몬스터 정보맵의 dto 를 가져온다.
						MonsterDto tempDto = MonsterInfoMap.get(i);
						
						//몬스터이름을 관리중이던 map에서 이름을 가져와 번역한뒤 dto에 넣어준다.
						tempDto.setName(TranslateText.translate(MonsterNameMap.get(i)));
						
						//풀스크린샷 이미지주소를 관리중이던 map에서 주소를 가져와 dto에 넣어준다.
						tempDto.setScreenshotURL(MonsterImgHrefMap.get(i));
						
						//세팅된 dto를 다시 MonsterInfo Map의 value로 넣는다.
						MonsterInfoMap.put(i, tempDto);
					}
					
//					//중간체크
//					int MonCnt = 1;
//					System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//					for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//						MonsterDto valueDto = entry.getValue();
//						System.out.println(MonCnt + ". name : " + valueDto.getName());
//						System.out.println(MonCnt + ". type : " + valueDto.getType());
//						System.out.println(MonCnt + ". attri : " + valueDto.getAttribute());
//						System.out.println(MonCnt + ". attri_sub : " + valueDto.getAttributeSub());
//						System.out.println(MonCnt + ". point : " + valueDto.getPoints());
//						System.out.println(MonCnt + ". bookNum : " + valueDto.getBookNum());
//						System.out.println(MonCnt + ". rarity : " + valueDto.getRarity());
//						System.out.println(MonCnt + ". screenURL : " + valueDto.getScreenshotURL());
//						MonCnt++;
//					}
//					System.out.println("/////////////////몬스터 이름넣기, 풀스크린샷URL 넣기 완료///////////////");
					///////////////////////////////////////////////////////////////////////////////
					
					//몬스터이름, 평점 파싱 -> 평점은 몬스터이름으로 파싱해야함 : 7,6,5 모두가 평점이 매겨져있는건 아니기때문
					Map<String,Float> MonsterPointMap = new TreeMap<String,Float>();
					//Elements els = doc.select("span.bolder");
					Elements elss = doc.select("table");
					//Elements els = doc.select("tr");
					Element nextTr = null;
					for (Element element : elss) {
						if(element.text().contains("評価点")) {
							//System.out.println("if문 평가 들어옴");
							
							Elements trs = element.select("tr").select("td");
							int cnt =0;
							String key = "";
							Float value = 0.f;
							for (Element elem : trs) {
								if(cnt%2 == 0) {
									key = TranslateText.translate(elem.text());
									//System.out.println("key : " + key);
								}
								else if(cnt%2 == 1) {
									String floatStr = elem.select("span.bolder").text();
									//만약에 스사노오 같은 //https://gamewith.jp/pocodun/article/show/73142
									//개같은 8.5(가칭) 이라는 평점이 붙어있을 수가 있어서 만약 붙어있다면 가칭이라는 일본어를 제거해준다.
									//장난 늑대 소녀 보레아스 //https://gamewith.jp/pocodun/article/show/173684
									if(floatStr.contains("(仮)")) {
										floatStr = floatStr.replace("(仮)", "");
									}
									//'푸' 처럼 https://gamewith.jp/pocodun/article/show/122443
									//평점이 '-' 처리되어있으면 0으로 처리한다.
									//아테나 [에레스토] //https://gamewith.jp/pocodun/article/show/73823
									//금기의 신 토르 //https://gamewith.jp/pocodun/article/show/163249
									if(floatStr.contains("-")) {
										floatStr = "0";
									}
									value = Float.valueOf(floatStr);
									//System.out.println("value : " + value);
									
									MonsterPointMap.put(key, value);
								}
								cnt++;
							}

							//os.write(MonsterPointMap.toString().getBytes());
							
							break;
						}
						
					}
					///////////////////////////////////////////////////////////////
					
					//MonsterInfo 맵과 pointMap에서 같은 이름을 갖고있는 것들을 찾아, dto 에 평점 넣기.
					for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
						//담겨있던 dto를 꺼낸다
						MonsterDto valueDto = entry.getValue();
						//System.out.println("valueDto 의 name : " + valueDto.getName());
						//담겨있떤 dto의 이름과 동일한 이름을 찾아서 value값(point)를 가져온다.
						//주의 : 평점이 없는 레어도 6,5 몬스터의 경우도 있으니 키값이 있는지 확인해서 가져와야한다.
						if(MonsterPointMap.containsKey(valueDto.getName())) {							
							float point =  MonsterPointMap.get(valueDto.getName());
							//담겨있던 dto의 point에 값을 입력시켜준다.
							valueDto.setPoints(point);
							//valueDto를 다시 담아준다.
							MonsterInfoMap.put(entry.getKey(), valueDto);
						}
					}
					
					//////////////////////////////////////////////////////////////////////
					
					
					//중간체크
//					int MonCnts = 1;
//					System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//					for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//						MonsterDto valueDto = entry.getValue();
//						System.out.println(MonCnts + ". name : " + valueDto.getName());
//						System.out.println(MonCnts + ". type : " + valueDto.getType());
//						System.out.println(MonCnts + ". attri : " + valueDto.getAttribute());
//						System.out.println(MonCnts + ". attri_sub : " + valueDto.getAttributeSub());
//						System.out.println(MonCnts + ". point : " + valueDto.getPoints());
//						System.out.println(MonCnts + ". bookNum : " + valueDto.getBookNum());
//						System.out.println(MonCnts + ". rarity : " + valueDto.getRarity());
//						MonCnts++;
//					}
//					
//					System.out.println("/////////////////몬스터 평점넣기 완료///////////////");
					
					//////////////////////////////////////////////////////////////////////
					
					//hp, 공격력 파싱 - 최대상태 
					int MonsterCntNum = 1; //Map 키 저장용도, MonsterInfoMap 에서 같은 키로 비교사용.
					Map<Integer,List<Integer>> MonsterHpAtkMap = new TreeMap<Integer, List<Integer>>();
					List<Integer> HpAtkList = new ArrayList<Integer>();
					Elements statusEls = doc.select("div.pd_status").select("table");
					if(statusEls.size() !=0 ) {
						//os.write(statusEls.toString().getBytes());
						for (Element element : statusEls) {
							Elements tds = element.select("tr").select("td");
							int tdCnt = 0;
							for (Element element2 : tds) {
								if(tdCnt == 0 ) {
									String text = element2.text();
									//System.out.println(tdCnt + ". hp : " + text);
									//os.write(text.getBytes());
									
									HpAtkList.add(Integer.valueOf(text));
								}
								else if(tdCnt == 2) {
									String text = element2.text();
									//System.out.println(tdCnt + ". atk : " + text);
									
									HpAtkList.add(Integer.valueOf(text));
								}
								
								tdCnt++;
								if(tdCnt == 3)
									break;
							}
							//os.write(tds.toString().getBytes());
							
							MonsterHpAtkMap.put(MonsterCntNum, new ArrayList<Integer>(HpAtkList));
							
							HpAtkList.clear();
							
							MonsterCntNum++;
						}
					}
					
					//System.out.println("///////////////////////// MonsterHpAtkMap 확인하기 ////////////////////////");
					//중간체크
					for (Map.Entry<Integer, List<Integer>> entry : MonsterHpAtkMap.entrySet()) {
						int key = entry.getKey();
						//System.out.println("key : " + key);
						List<Integer> valueList =entry.getValue();
						
						//System.out.println(key + ". hp : " + valueList.get(0) + " / Atk : " + valueList.get(1));
						
						//같은키가 있다면
						if(MonsterInfoMap.containsKey(key)) {
							//기존에 저장된 몬스터 정보맵의 dto 를 가져온다.
							MonsterDto tempDto = MonsterInfoMap.get(key);
							tempDto.setHp(valueList.get(0));
							tempDto.setAttack(valueList.get(1));
							
							//세팅된 dto를 다시 MonsterInfo Map의 value로 넣는다.
							MonsterInfoMap.put(key, tempDto);
						}
					}
					//System.out.println("/////////////////몬스터 hp,Attack넣기 완료///////////////");
					
					////////////////////////몬스터 아이콘 URL , 풀스크린샷 URL Dto에 세팅////////////////////////
			
		            String baseIconUrl = "https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/MonsterIcon/";
		            String baseScreenshotUrl = "https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/MonsterScreenshot/";
					
		            for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
		            	//dto 를 꺼내서
		            	MonsterDto valueDto = entry.getValue();
		            	String MonsterNameStr = valueDto.getName();
		            	//System.out.println("추출한 몬스터 이름 : " + MonsterNameStr);
		            	
		            	String attributeStr = valueDto.getAttribute();
		                
		                
		                switch (attributeStr) {
		    			case "불":
		    				attributeStr = "fir1";
		    				break;
		    			case "물":
		    				attributeStr = "wat1";
		    				break;
		    			case "번개":
		    				attributeStr = "thu1";
		    				break;
		    			case "숲":
		    				attributeStr = "for1";
		    				break;
		    			case "빛":
		    				attributeStr = "lig1";
		    				break;
		    			case "어둠":
		    				attributeStr = "dar1";
		    				break;

		    			default:
		    				break;
		    			}
		                
		                //System.out.println("추출할 몬스터 속성 : " + attributeStr);
		                
		                //Dto에 몬스터 아이콘 URL 세팅
		                //https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/MonsterIcon/fir1/狂滅 조물주 아자 토스.jpg
		                //이러한 형태로 저장해주면됨. 건드려야될부분 : 속성 / 몬스터이름
		                //### Error updating database.  Cause: java.sql.SQLException: Incorrect string value: '\xE7\x8B\x82\xE6\xBB\x85...' for column 'ICON_URL' at row 1
		                //오류 해결방법 : 테이블의 컬럼명에대한 charSET 을 utf-8 로 싹다 바꿔줘야함. 이유 : 개같은 일본어 때문에.
		                // 즉, 일본어가 들어가는 DB의 column 들은 모두 charSET 을 utf-8로 바꾸어줄것.
		                String fullIconUrl = baseIconUrl + attributeStr + "/" + MonsterNameStr + ".jpg";
		                //String fullIconUrl = baseIconUrl + attributeStr + "/" ;//+ MonsterNameStr + ".jpg";
		                
		                //S3에 등록된 url 인지 아닌지에대한 판단은 응답 수신을 보고 200 이면 넣고, 403이면 안넣어주면된다.
		                //ex) 등록되지않은 6성 아자토스 아이콘 
						//https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/MonsterIcon/fir1/장님 신 아자 토스.jpg
		                String key = "MonsterIcon/" + attributeStr + "/" + MonsterNameStr + ".jpg";
		                //fullObject = s3Client.getObject(new GetObjectRequest("euichul-pocorong-imgs", key));
		                //System.out.println("Content-Type: " + fullObject.getObjectMetadata().getContentType());
		                //System.out.println(fullObject.getObjectContent());
		                URL awsUrl = s3Client.getUrl("euichul-pocorong-imgs", key);
		                HttpURLConnection connection = (HttpURLConnection)awsUrl.openConnection();
		                connection.setRequestMethod("GET");
		                connection.connect();
		                int code = connection.getResponseCode();
		                
		                //System.out.println("icon response code : " + code);
		                
		                if(code == 200) {
		                	valueDto.setIconURL(fullIconUrl);
		                }
		                else {
		                	fullIconUrl = null;
		                	valueDto.setIconURL(fullIconUrl);
		                }
		                
		                //Dto에 몬스터 풀스크린샷 URL 세팅
		                //https://euichul-pocorong-imgs.s3.ap-northeast-2.amazonaws.com/MonsterScreenshot/fir1/狂滅 조물주 아자 토스.jpg
		                //이러한 형태로 저장해주면됨. 건드려야될부분 : 속성 / 몬스터이름 
		                String fullScreenshotUrl = baseScreenshotUrl + attributeStr + "/" + MonsterNameStr + ".jpg";
		                
		                key = "MonsterScreenshot/" + attributeStr + "/" + MonsterNameStr + ".jpg";
		                awsUrl = s3Client.getUrl("euichul-pocorong-imgs", key);
		                connection = (HttpURLConnection)awsUrl.openConnection();
		                connection.setRequestMethod("GET");
		                connection.connect();
		                code = connection.getResponseCode();
		                
		                //System.out.println("screenshot response code : " + code);
		                
		                if(code == 200) {
		                	valueDto.setScreenshotURL(fullScreenshotUrl);
		                }
		                else {
		                	fullScreenshotUrl = null;
		                	valueDto.setScreenshotURL(fullScreenshotUrl);
		                }
		                
		                
		                
		            }
		            
		            
						//////////////////////////몬스터 정보 테이블 관련 파싱 끝///////////////////////////////////
		            System.out.println();
		            System.out.println("////////////////////////  DB  에 입력 할 정보 /////////////////////");
						//중간체크
						int MonCnts = 1;
						//System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
						for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
							MonsterDto valueDto = entry.getValue();
							System.out.println(MonCnts + ". name : " + valueDto.getName());
							//System.out.println(MonCnts + ". type : " + valueDto.getType());
							System.out.println(MonCnts + ". attri : " + valueDto.getAttribute());
							//System.out.println(MonCnts + ". attri_sub : " + valueDto.getAttributeSub());
							System.out.println(MonCnts + ". point : " + valueDto.getPoints());
							//System.out.println(MonCnts + ". bookNum : " + valueDto.getBookNum());
							System.out.println(MonCnts + ". rarity : " + valueDto.getRarity());
							//System.out.println(MonCnts + ". hp : " + valueDto.getHp());
							//System.out.println(MonCnts + ". Attack : " + valueDto.getAttack());
							//System.out.println(MonCnts + ". iconURL : " + valueDto.getIconURL());
							//System.out.println(MonCnts + ". screenURL : " + valueDto.getScreenshotURL());
							MonCnts++;
							
							//모두 저장되었으니 DB에 insert 시키기.
							monsterDao.insertMonsterInfo(valueDto);
						}

						//다음 상세페이지에서 몬스터들 저장할것이기때문에 clear 해줘야함.
						MonsterInfoMap.clear();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
			}
//			//다음 attribute 에대한 href 리스트 가져오려면 초기화
//			detailHrefList.clear();
//		}
		
		
		
		
	}
	
	public void settingAwsAndEvolutionIconFileUploadToS3() {
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		String basePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterEvolutionIcon\\";
		
		//가져온 폴더명 저장 
		List<String> fileNamesList = new ArrayList<String>();
		//디렉토리 파일 전체목록 가져오기.
		File path = new File(basePath);
		File[] fileList = path.listFiles();
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
		
		//각각의 속성명 폴더에서 가져온 파일 이름들을 다시 순회.
		for (String folderStr : fileNamesList) {
			
			//가져올 파일경로
			String newPath = basePath + folderStr;
			
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
					String upLoadPath = "MonsterEvolutionIcon/"+ folderStr;
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
	
	public void monsterEvolutionIconUrlDownload() {
		
		String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};
		List<String> attriList = Arrays.asList(attriStrs);
		
		List<String> MonsterNameList = new ArrayList<String>();
		List<String> IconUrlList = new ArrayList<String>();
		
		//url 겹치는거 방지용. , 몬스터이름
		Map<String,String> iconUrlAndMonNameMap = new TreeMap<String, String>();
		
		//저장해놓은 상세페이지들의 href txt 파일을 읽어서 파싱하기위함.
		// 출처: https://jeong-pro.tistory.com/69 [기본기를 쌓는 정아마추어 코딩블로그]
		try {
			// 파일 객체 생성
			File file = new File(
					"C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterEvolutionIcon\\MonsterEvolutionNameAndUrl.txt");
			// 입력 스트림 생성
			FileReader filereader = new FileReader(file);
			// 입력 버퍼 생성
			BufferedReader bufReader = new BufferedReader(filereader);
			String line = "";
			int cnt = 0;
			while ((line = bufReader.readLine()) != null) {
				if(cnt % 2==0) { //짝수면 몬스터이름
					MonsterNameList.add(line);
				}
				else if(cnt % 2 == 1) { //홀수면 url
					IconUrlList.add(line);
				}
				cnt++;
			}
			// .readLine()은 끝에 개행문자를 읽지 않는다.
			bufReader.close();
		} catch (FileNotFoundException e) {
			// TODO: handle exception
		} catch (IOException e) {
			System.out.println(e);
		}
		
		int urlListSize = IconUrlList.size();
		int nameListSize = MonsterNameList.size();
		
		
		if(urlListSize == nameListSize) {
			for(int i=0; i<urlListSize; i++) {
				iconUrlAndMonNameMap.put(IconUrlList.get(i), MonsterNameList.get(i));
			}
			//171개 아니고 169 개인 이유, UrlTemp.txt 에 있는 맨위에 몬스터이름 - 가 에노리아 여서 그냥 뺏음.
			//169개가 아니고 167 개인 이유, 마오부루후 는 없음. 마오루부루후 겠지.
			//마오부루후
			//https://gamewith.akamaized.net/article_tools/pocodun/gacha/_m_i.png 이거뺀거임
			System.out.println("map size 167 개 맞나 확인 size : " + iconUrlAndMonNameMap.size());
		}
		else {
			System.out.println("List들의 size 가 다릅니다");
			System.out.println("nameList size " + nameListSize);
			System.out.println("iconList size " + urlListSize);
		}
		
		//iconUrlAndMonNameMap에 저장된거 토대로 몬스터 이미지 다운로드 
		
			
			for (Map.Entry<String, String> entry : iconUrlAndMonNameMap.entrySet()) {
				
				String MonsterName = entry.getValue();
				String iconUrl = entry.getKey();
				
				//폴더에 이미지 저장하기.
				String monsterIconPath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterEvolutionIcon\\"+MonsterName+".jpg";
				
				//플로어 이미지 저장
				System.out.println("URL 요청전 몬스터 이름 : " + MonsterName + " / 이미지주소 : " + iconUrl);
				URL imgUrl;
				try {
					imgUrl = new URL(iconUrl);
					
					HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
					//System.out.println(conn.getContentLength());
					
					//참고 : https://triest.tistory.com/14
					// TimeOut 시간 (서버 접속시 연결 시간) - 40초로 늘려주기.
					conn.setConnectTimeout(40000);
					 
					// TimeOut 시간 (Read시 연결 시간)
					conn.setReadTimeout(40000);
					
					InputStream is = conn.getInputStream();
					BufferedInputStream bis = new BufferedInputStream(is);
						
					FileOutputStream fos = new FileOutputStream(monsterIconPath);
					BufferedOutputStream bos = new BufferedOutputStream(fos);
					
					int byteImg;
					
					byte[] buf = new byte[conn.getContentLength()];
					while((byteImg = bis.read(buf)) != -1) {
						bos.write(buf,0,byteImg);
					}
					
					bos.close();
					fos.close();
					bis.close();
					is.close();
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

		
	}
	
	public static void monsterEvolutionIconUrlCrawl() {
		
		/*
		 * url 오류 나면 무시해도됨 
		 * ex)
		 * WARNING: exception thrown while executing request
			java.net.SocketTimeoutException: Read timed out
		 * 이유 : 어차피 강림몬스터,강습몬스터, 진화재료 몬스터들 이름,아이콘url 따는거라서
		 * 		겹치는 것들이 많을것임.
		 * 		따라서, 솔직히 href 다 훑을필요 없고, 그냥 7,6,5 애들 몇마리씩 속성별로 훑으면됨. 속성별로는 진화재료가 다르긴하니까.
		 */

		String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};
		//String[] attriStrs = {"fir1","wat1","thu1","for1","lig1"};
		//String[] attriStrs = {"dar1"};
		List<String> attriList = Arrays.asList(attriStrs);
		List<String> detailHrefList = new ArrayList<String>();
		
		//진화재료 몬스터이름,아이콘URL 파싱하기
		Map<String,String> evolutionMonIconMap = new TreeMap<String, String>();
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
			} catch (FileNotFoundException e) {
				// TODO: handle exception
			} catch (IOException e) {
				System.out.println(e);
			}
					
					
			for (String url : detailHrefList) {
				
				int MonCount = 1;
				
				//test 용 아자토스 url
				//String url = "https://gamewith.jp/pocodun/article/show/73362";
				
				System.out.println("url : " + url);
				
				// 페이지 전체소스 저장
				Document doc = null;
				
				try {
					
					doc = Jsoup.connect(url).get();
					
					Elements evolutionElss = doc.select("div.pd_simple_table_col5");
					
					for (Element evolutionEls : evolutionElss) {
						
						//진화 재료 이름 모아두기
						List<String> MonNameList = evolutionEls.select("tr").get(1).select("td").eachText();
						
						//진화 재료 아이콘URL 모아두기
						List<String> iconUrlStrsList = new ArrayList<String>();
						String urls = "";
						Elements imgEls = evolutionEls.select("tr").get(1).select("noscript").select("img");
						for (Element element : imgEls) {
							urls = element.attr("src");
							//System.out.println(urls);
							iconUrlStrsList.add(urls);
						}
						
						//몬스터 진화재료 정보 저장하기.
						//세개의 리스트 모두 사이즈는 같을것이므로 하나를 기준으로 사이즈 잡기.
						int totalListSize = iconUrlStrsList.size();
						
						for(int i=0; i<totalListSize; i++) {
							//진화재료의 이름을 key로 해서 중복을 막고 URL을 저장시킨다.
							evolutionMonIconMap.put(TranslateText.translate(MonNameList.get(i)), iconUrlStrsList.get(i));
						}
						
						MonCount++;
					}
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			detailHrefList.clear();
		}
		
		
			////////////////////////////몬스터 진화정보재료 중간체크///////////////////////////////
			System.out.println();
			System.out.println("////////////////몬스터 진화 재료 중간 체크 ////////////////////////");
			int cntMons = 1;
			for (Map.Entry<String, String> entry : evolutionMonIconMap.entrySet()) {
				System.out.println(cntMons + ". 진화재료 이름 : " + entry.getKey());
				System.out.println(cntMons + ". 진화재료 URL : " + entry.getValue());
				cntMons++;
				
				
				String monsterIconPath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterEvolutionIcon\\"+entry.getKey()+".jpg";
				String upLoadPath = "MonsterEvolutionIcon/"+ entry.getKey() +".jpg";
				
				DownloadAndS3UploadDto downloadAndS3UploadDto = DownloadAndS3UploadDto.builder()
						.monsterName(entry.getKey())
						.url(entry.getValue())
						.downloadPath(monsterIconPath)
						.uploadPath(upLoadPath)
						.isRepeat(false)
						.build();
				//downloadAndS3UploadService(downloadAndS3UploadDto);
				//기존에는 바로 처리했었다면 메시지 큐를 이용해서 다른 스레드를 통해 돌릴 수 있도록 Dto를 큐에 담는다.
				mcMessageQueue.add(downloadAndS3UploadDto);
				
				
			//	//폴더에 이미지 저장하기.
			//	String monsterIconPath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterScreenshot\\"+attristr+"\\"+transMonName+".jpg";
			//	
			//	//플로어 이미지 저장
			//	System.out.println("URL 요청전 몬스터 이름 : " + transMonName + " / 이미지주소 : " + dataOriginal);
			//	URL imgUrl = new URL(dataOriginal);
			//	HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
			//	//System.out.println(conn.getContentLength());
			//	
			//	InputStream is = conn.getInputStream();
			//	BufferedInputStream bis = new BufferedInputStream(is);
			//		
			//	FileOutputStream fos = new FileOutputStream(monsterIconPath);
			//	BufferedOutputStream bos = new BufferedOutputStream(fos);
			//	
			//	int byteImg;
			//	
			//	byte[] buf = new byte[conn.getContentLength()];
			//	while((byteImg = bis.read(buf)) != -1) {
			//		bos.write(buf,0,byteImg);
			//	}
			//	
			//	bos.close();
			//	fos.close();
			//	bis.close();
			//	is.close();
			}
		
	}
	
	//aws 의 S3 저장소를 활용하여 몬스터이름.jpg 스크린샷 URL 저장하기.
	public void settingAwsAndScreenshotUrlFileUploadToS3() {
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		String basePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterScreenshot\\";
		
		//실제 파일들의 파일경로 세팅하기.
				String[] attributeFolderName = {"fir1","wat1","thu1","for1","lig1","dar1"};
				List<String> attriList = Arrays.asList(attributeFolderName);
				for (String attriStr : attriList) {
					
					//가져온 폴더명 저장 - ver. 애들 이름 잘가져와지는지 확인할것.
					List<String> fileNamesList = new ArrayList<String>();
					//디렉토리 파일 전체목록 가져오기.
					File path = new File(basePath + attriStr);
					File[] fileList = path.listFiles();
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
					//각각의 속성명 폴더에서 가져온 파일 이름들을 다시 순회.
					for (String folderStr : fileNamesList) {
						
						//가져올 파일경로
						String newPath = basePath + attriStr + "\\" + folderStr;
						
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
								String upLoadPath = "MonsterScreenshot/" + attriStr + "/" + folderStr;
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
					//순회가 끝난 리스트 클리어
					fileNamesList.clear();
				}
	}
	
	//몬스터 스크린샷 부분만 따로 파싱해서 폴더안에 이미지.jpg 형태로 다운로드 해놓기.
	
	public static void monsterScreenshotUrlCrawlAndDownload() {
		
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
			} catch (FileNotFoundException e) {
				// TODO: handle exception
			} catch (IOException e) {
				System.out.println(e);
			}
			
			
			for (String url : detailHrefList) {
				// 페이지 전체소스 저장
				Document doc = null;
				
				//test 용 아자토스 url
				//String url = "https://gamewith.jp/pocodun/article/show/73362";
				
				try {
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
							//downloadAndS3UploadService(downloadAndS3UploadDto);
							//기존에는 바로 처리했었다면 메시지 큐를 이용해서 다른 스레드를 통해 돌릴 수 있도록 Dto를 큐에 담는다.
							mcMessageQueue.add(downloadAndS3UploadDto);
							
							
							//플로어 이미지 저장
//							System.out.println("URL 요청전 몬스터 이름 : " + transMonName + " / 이미지주소 : " + dataOriginal);
//							URL imgUrl = new URL(dataOriginal);
//							HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
//							//System.out.println(conn.getContentLength());
//							
//							InputStream is = conn.getInputStream();
//							BufferedInputStream bis = new BufferedInputStream(is);
//								
//							FileOutputStream fos = new FileOutputStream(monsterIconPath);
//							BufferedOutputStream bos = new BufferedOutputStream(fos);
//							
//							int byteImg;
//							
//							byte[] buf = new byte[conn.getContentLength()];
//							while((byteImg = bis.read(buf)) != -1) {
//								bos.write(buf,0,byteImg);
//							}
//							
//							bos.close();
//							fos.close();
//							bis.close();
//							is.close();
							
							
							
						}
					}
					
					//System.out.println("맵 이미지주소 체크 : " + MonsterImgHrefMap.toString());
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					
					e.printStackTrace();
				}
			}
			
			//System.out.println(attristr + " 속성의 href size : " + detailHrefList.size());
			//System.out.println(detailHrefList.toString());
			detailHrefList.clear();
		}
	}
	
	public void settingAwsAndMonsterIconUrlFileUploadToS3() {
		// s3 사용해보기
		// https://galid1.tistory.com/590

		// version1. 발급받은 key를 적어서 credentials 가져오기 
		// credentials 세팅 - accessKey와 secretKey 는 절대 노출되어서는 안됨. 노출하면 재발급받을것.
//		String accessKey = "AKIA5TAFDJJPTLHFNIPL";
//		String secretKey = "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";
//		
//		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

		// 첫번째 : aws에 요청할 Client 객체 생성
//		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
//				.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.AP_NORTHEAST_2)
//				.build();
		
		//version2. 보안처리 코드상에 명시적으로 적지않고, 환경변수에 등록해서 가져오기 
		//고급시스템설정 - 환경변수 - 내 사용자 변수에 - 변수명 : AWS_ACCESS_KEY_ID 랑 변수명 : AWS_SECRET_ACCESS_KEY 만들어서 위에 적힌 값 들 넣어주면 됨.
		//EnvironmentVariableCredentialsProvider 를 넘겨주면 해당객체가 자동으로 알맞은 환경변수를 불러와 AccessKey를 설정해준다
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();		
		
		
		// 두번째 : Bucket(Object(업로드 할 파일들)를 담는 그릇(디렉토리)) 생성하기.
		//CreateBucketRequest 를 매개변수로 받는 createBucket() 메소드는 AWS에서 설정할 수 있는 설정정보 담아 생성 요청가능.
		//예를들면, withAccessControlList() 메소드로 Access 권한 설정을 담아 버킷을 생성할수있다.
		//일단 디폴트로 간단하게 생성하겠다.
		//그런데 aws 사이트에서 직접 버킷을 손수 만들었기때문에 해당 코드는 실행될 필요가 없다.
		//s3Client.createBucket("euichul-pocorong-imgs"); 
		
		// 세번째 : Object 업로드
		// 파일 정보 얻어오기. - 몬스터이름의 폴더명 - 몬스터이름.jpg 로 되있는 파일을 가져오자.
		//test 로 fir1 폴더의 길가메쉬 .jpg 얻어오기.
		//File file = new File("C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterIcon\\fir1\\원래 검도부 싸움 대장 길가 메쉬\\원래 검도부 싸움 대장 길가 메쉬.jpg");
		String basePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterIcon\\";
		
		
		//실제 파일들의 파일경로 세팅하기.
		String[] attributeFolderName = {"fir1","wat1","thu1","for1","lig1","dar1"};
		List<String> attriList = Arrays.asList(attributeFolderName);
		for (String attriStr : attriList) {
			
			//가져온 폴더명 저장 - ver. 애들 이름 잘가져와지는지 확인할것.
			List<String> fileNamesList = new ArrayList<String>();
			//디렉토리 파일 전체목록 가져오기.
			File path = new File(basePath + attriStr);
			File[] fileList = path.listFiles();
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
			//각각의 속성명 폴더에서 가져온 파일 이름들을 다시 순회.
			for (String folderStr : fileNamesList) {
				
				//가져올 파일경로
				String newPath = basePath + attriStr + "\\" + folderStr;
				
				File file = new File(newPath);
				
				//System.out.println("파일생성 시도");
				//file.createNewFile();
				//System.out.println("파일생성 완료");
				
				//putObject 메소드로 (만들어놓은 Bucket의이름, bucket안에 저장될경로, 업로드할파일변수) 업로드하기.
				//putObejct의 문제점 : 파일쓰기작업은 매우 무거운 작업이다. 업로드할곳은 S3인데 로컬에도 파일이 저장되기때문에 쓸데없이 자원이 낭비된다.
				//s3Client.putObject("euichul-pocorong-imgs", "test/원래 검도부 싸움 대장 길가 메쉬.jpg", file);
				
				//문제점 해결
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
						String upLoadPath = "MonsterIcon/" + attriStr + "/" + folderStr;
						s3Client.putObject("euichul-pocorong-imgs", upLoadPath, multipartFile.getInputStream(), metadata);
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			//순회가 끝난 리스트 클리어
			fileNamesList.clear();
		}
		
	}
	//메세지 큐 서비스 - 다운로드 & s3 에 업로드
	public static void downloadAndS3UploadService(DownloadAndS3UploadDto dto) {
		
		//다운로드
		try {
			URL imgUrl = new URL(dto.getUrl());
			
			HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
			
			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
				
			FileOutputStream fos = new FileOutputStream(dto.getDownloadPath());
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			
			int byteImg;
			
			byte[] buf = new byte[conn.getContentLength()];
			while((byteImg = bis.read(buf)) != -1) {
				bos.write(buf,0,byteImg);
			}
			
			bos.close();
			fos.close();
			bis.close();
			is.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//업로드
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();
		
		//가져올 파일경로
		String newPath = dto.getDownloadPath();
		
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
				String upLoadPath = dto.getUploadPath();
				System.out.println("업로드 할 파일 이름 : " + dto.getMonsterName() + ".jpg");
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
	
	//3p - 몬스터 상세페이지로 이동할 수 있는 Href 뽑았던 로직 변형해서 <몬스터이름,아이콘URL> 파싱하기
	public static void monsterPreviewIconUrlCrawl() {
		String url = "https://gamewith.jp/pocodun/article/show/72816";

		// 페이지 전체소스 저장
		Document doc = null;

		// 검색용 값 설정
		String[] reaStrs = { "rea7", "rea6", "rea5" };
		List<String> reaList = Arrays.asList(reaStrs);

		 String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};
		// String[] attriStrs = {"lig1","dar1"};
		//String[] attriStrs = { "for1" };
		List<String> attriList = Arrays.asList(attriStrs);
		
		//<몬스터이름,아이콘URL> 파싱 - 여기에 담아둘테니 나중에 MonsterInfoDto 의 아이콘Url 에 넣어주면되겠다.
		//그리고 혹시몰라서 임시로 IconUrl.txt 따로 속성마다 뺴뒀음
		Map<String,String> hrefRsltMap = new TreeMap<String, String>();
		
		DataOutputStream os = null;
		try {
			doc = Jsoup.connect(url).get();
			
			Elements elems = doc.select("tr.w-idb-element");
			
			String rslpath="";
			
			for (String attriStr : attriList) {
				int AddCnt = 1;
				for (String reaStr : reaList) {
					System.out.println("//////////////////// 속성 : " + attriStr + " 레어도 : " + reaStr + "/////////////////");
					for (Element ele : elems) {

						String val = reaStr + " " + attriStr;
						Elements em = ele.getElementsByAttributeValueContaining("class", val);
						
						if (em.size() != 0) {
							for (Element element : em) {
								
								String MonsterName = element.getElementsByTag("a").text();
								MonsterName = TranslateText.translate(MonsterName);
								
								
								String iconUrlStrs = element.select("img").attr("data-original");
								String rsltStrs = AddCnt + ". 이름 : " + MonsterName +" Url : " + iconUrlStrs + "\n";
								System.out.println(AddCnt + ". 이름 : " + MonsterName +" Url : " + iconUrlStrs);
								
								AddCnt++;
								
								hrefRsltMap.put(MonsterName, iconUrlStrs);
								
								
								
								/////////////////////////////////////////////////////////////////////////////
								
								//txt 파일에 이름, url 뽑기.
								//rslpath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterHrefResult\\" + attriStr
								//		+ "\\IconUrls.txt";
								
								//os = new DataOutputStream(new FileOutputStream(rslpath, true));
								//os.write(rsltStrs.getBytes());
								
								///////////////////////////////////////////////////////////////////////////////
								
								//몬스터이름의 폴더명 만들고, 그안에 아이콘 이미지 뽑기.
								//Utils.makeFolderOfMonsterName(MonsterName, attriStr);
								String monsterIconPath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterIcon\\"+attriStr+"\\"+MonsterName+".jpg";
								
								String upLoadPath = "MonsterIcon/" + attriStr + "/" + MonsterName+".jpg";
								
								DownloadAndS3UploadDto downloadAndS3UploadDto = DownloadAndS3UploadDto.builder()
										.monsterName(MonsterName)
										.url(iconUrlStrs)
										.downloadPath(monsterIconPath)
										.uploadPath(upLoadPath)
										.attribute(attriStr)
										.isRepeat(true)
										.build();
								//downloadAndS3UploadService(downloadAndS3UploadDto);
								//기존에는 바로 처리했었다면 메시지 큐를 이용해서 다른 스레드를 통해 돌릴 수 있도록 Dto를 큐에 담는다.
								mcMessageQueue.add(downloadAndS3UploadDto);
//								
//								//플로어 이미지 저장
//								URL imgUrl = new URL(iconUrlStrs);
//								HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
//								//System.out.println(conn.getContentLength());
//								
//								InputStream is = conn.getInputStream();
//								BufferedInputStream bis = new BufferedInputStream(is);
//									
//								FileOutputStream fos = new FileOutputStream(monsterIconPath);
//								BufferedOutputStream bos = new BufferedOutputStream(fos);
//								
//								int byteImg;
//								
//								byte[] buf = new byte[conn.getContentLength()];
//								while((byteImg = bis.read(buf)) != -1) {
//									bos.write(buf,0,byteImg);
//								}
//								
//								bos.close();
//								fos.close();
//								bis.close();
//								is.close();
							}
								
						}
					}
				}
			}
			
			
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
//			try {
//				os.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		
		
	}

	//뽑은 Href 로 몬스터 상세정보 페이지 파싱 - 몬스터정보테이블, 몬스터일반,체인,리더 스킬정보테이블 , 몬스터 진화재료 테이블 모두 파싱.
	public void monsterDetailCrawl() {
		//test 용 아자토스 url
		String url = "https://gamewith.jp/pocodun/article/show/73362";
		//test 용 레이븐 url
		//String url = "https://gamewith.jp/pocodun/article/show/100667";
		
		// 페이지 전체소스 저장
				Document doc = null;
				
				try {
					doc = Jsoup.connect(url).get();
					
					String HrefFilePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\4pOutput5.html";
					//DataOutputStream os = new DataOutputStream(new FileOutputStream(HrefFilePath));
					
/////////////////////////////////////////////////////////////////////////////////////////////////////////
					
					//기본 정보 파싱 - 속성,부속성,타입,도감번호,레어도
					Map<Integer,MonsterDto> MonsterInfoMap = new TreeMap<Integer, MonsterDto>(); 
					MonsterDto tempMonsterDto = MonsterDto.builder().build();
					
					Elements tables = doc.select("div.pd_simple_table_col3").select("table");
					//os.write(tables.toString().getBytes());
					
					//몬스터의 평점 작업 해주려면 평점 관리 맵 key와 같이 맞춰줘야함.
					int MonsterNumberCnt =1;
					for (Element element : tables) {
						Elements tds = element.select("tr").select("td");
						//os.write(tds.toString().getBytes());
						//기본정보 테이블에서 제공되고있는 정보를 순서대로 나열
						
						//1,1,2,4,5 로 출력되면 정상.
						int MonsterTextNumberCnt=1;
						for (Element element2 : tds) {
							
							//System.out.println(MonsterNumberCnt + ". text : " + element2.text());
							String headStr = (MonsterTextNumberCnt + ". text : ");
							String testStr = element2.text() +"\n";
							String ResultStr = headStr + testStr;
							//os.write(ResultStr.getBytes());
							
							String texts = element2.text();
							
							//-가 아니라는건 작성이 되어있다는것. 작성되있을 경우에만 해준다.
							if(!texts.equals("-")) {
								
								switch (MonsterTextNumberCnt) {
								case 1:
									//속성/부속성
									String[] attris = texts.split("/");
									
									if(attris.length == 2) {
										//System.out.println("0 : " + TranslateText.translate(attris[0]));
										tempMonsterDto.setAttribute(TranslateText.translate(attris[0]));
										//System.out.println(MonsterTextNumberCnt + ". attri : " + tempMonsterDto.getAttribute());
									
										//System.out.println("1 : " + TranslateText.translate(attris[1]));
										tempMonsterDto.setAttributeSub(TranslateText.translate(attris[1]));
										//System.out.println(MonsterTextNumberCnt + ". attri_sub : " + tempMonsterDto.getAttributeSub());
									}
									else if(attris.length == 1) {
										//System.out.println("0 : " + TranslateText.translate(attris[0]));
										tempMonsterDto.setAttribute(TranslateText.translate(attris[0]));
										//System.out.println(MonsterTextNumberCnt + ". attri : " + tempMonsterDto.getAttribute());
									}
								
									break;
								case 2:
									//타입(ex) 신)
									tempMonsterDto.setType(TranslateText.translate(texts));
									//System.out.println(MonsterTextNumberCnt + ". type : " + tempMonsterDto.getType());
									break;
								case 3:
									//공격? 이라는 칸인데 다 - 라서 안해줘도될듯
									break;
								case 4:
									//도감넘버
									tempMonsterDto.setBookNum(Integer.valueOf(texts));
									//System.out.println(MonsterTextNumberCnt + ". bookNum : " + tempMonsterDto.getBookNum());
									break;
								case 5:
									//레어도
									String rarity = texts.replace("★", "");
									tempMonsterDto.setRarity(Integer.valueOf(rarity));
									//System.out.println(MonsterTextNumberCnt + ". rarity : " + tempMonsterDto.getRarity());
									break;
								case 6:
									//소울 어떤던전에서 획득하면되는지인데 필요없어보임 일단 여지를 남겨두겠음.
									break;
									
								default:
									break;
								}
							}
							MonsterTextNumberCnt++;
						}
						String Enter = "\n";
						//os.write(Enter.getBytes());
						
						
						//작성된 Dto 맵에 넣고나서 다시 초기화
						MonsterInfoMap.put(MonsterNumberCnt, tempMonsterDto);

						//작성완료된 Dto 확인
						//System.out.println(MonsterNumberCnt + ". type : " + tempMonsterDto.getType());
						//System.out.println(MonsterNumberCnt + ". attri : " + tempMonsterDto.getAttribute());
						//System.out.println(MonsterNumberCnt + ". attri_sub : " + tempMonsterDto.getAttributeSub());
						//System.out.println(MonsterNumberCnt + ". bookNum : " + tempMonsterDto.getBookNum());
						//System.out.println(MonsterNumberCnt + ". rarity : " + tempMonsterDto.getRarity());
						
						MonsterNumberCnt++;
						
						tempMonsterDto = MonsterDto.builder().build();
					}
					//System.out.println("/////////////////몬스터 기본정보넣기 완료///////////////");
					
//					//중간체크
//					int MonCntz = 1;
//					System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//					for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//						MonsterDto valueDto = entry.getValue();
//						System.out.println(MonCntz + ". type : " + valueDto.getType());
//						System.out.println(MonCntz + ". attri : " + valueDto.getAttribute());
//						System.out.println(MonCntz + ". attri_sub : " + valueDto.getAttributeSub());
//						System.out.println(MonCntz + ". point : " + valueDto.getPoints());
//						System.out.println(MonCntz + ". bookNum : " + valueDto.getBookNum());
//						System.out.println(MonCntz + ". rarity : " + valueDto.getRarity());
//						MonCntz++;
//					}
//					System.out.println("/////////////////몬스터 기본정보넣기 - 중간체크 - 완료///////////////");
					
					///////////////////////////////////////////////////////////////////////////////
					
					
					//몬스터 이름, 레어도 파싱 -> 몬스터순서번호, 몬스터이름 파싱 (레어도는 아래에서 다시 파싱중)
					Elements els = doc.getElementsByTag("h2");
					//System.out.println( "els : " + els.toString());
					//os.write(els.toString().getBytes());
					//Map<String,Integer> MonsterNameMap = new TreeMap<String,Integer>();
					//Map<String,MonsterDto> MonsterNameMap = new TreeMap<String,MonsterDto>();
					Map<Integer,String> MonsterNameMap = new TreeMap<Integer,String>();
					
					//Dto 에 담아주기전에 보관용 map <몬스터이름, img주소> 혹은 <순서용key, img주소> 
					Map<Integer,String> MonsterImgHrefMap = new TreeMap<Integer, String>();
					
					int NumberCnt=1;
					for (Element el : els) {
						String elstr = el.text();
						if(elstr.contains("★"))
						{
							String elText = el.toString() + "\n";
							//os.write(elText.getBytes());
							//System.out.println(elText);
							
							Element img= el.nextElementSibling();
							String dataOriginal = img.attr("data-original");
							//os.write(img.toString().getBytes());
							//System.out.println(img.toString());
							//System.out.println(dataOriginal);
							//os.write(dataOriginal.getBytes());
							
							//순서용키,img주소 삽입
							MonsterImgHrefMap.put(NumberCnt, dataOriginal);
							
							//System.out.println(elstr + " / 별 있음");
							
							//별제거
							elstr = elstr.replace("★", "");
							//System.out.println(elstr);
							
							//숫자제거 및 value로 삽입 / 이름 key로 삽입
							char valueofRarity = elstr.charAt(0);
							elstr = elstr.replace(String.valueOf(valueofRarity), "");
							//System.out.println("key : " + elstr + " / value : " + valueofRarity);
							//MonsterNameMap.put(elstr, Integer.valueOf(valueofRarity));
							//MonsterNameMap.put(elstr, MonsterDto.builder().rarity(Integer.valueOf(valueofRarity)).name(elstr).build());
							MonsterNameMap.put(NumberCnt,elstr);
							NumberCnt++;
						}
						else {
							//System.out.println(elstr + " / 별없음");
						}
					}
					
					//os.write(MonsterNameMap.toString().getBytes());
					
					//System.out.println("맵 순서 체크 : " + MonsterNameMap.toString());
					
					//System.out.println("맵 이미지주소 체크 : " + MonsterImgHrefMap.toString());
					//////////////////////////////////////////////////////////////////////////////
					//각각의 map에서 같은 순서번호 갖고있는것들을 찾아 dto에 몬스터이름 넣기. , 풀스크린샷 이미지 주소도 넣기.
					for(int i=1; i<=MonsterInfoMap.size(); i++) {
						
						//기존에 저장된 몬스터 정보맵의 dto 를 가져온다.
						MonsterDto tempDto = MonsterInfoMap.get(i);
						
						//몬스터이름을 관리중이던 map에서 이름을 가져와 번역한뒤 dto에 넣어준다.
						tempDto.setName(TranslateText.translate(MonsterNameMap.get(i)));
						
						//풀스크린샷 이미지주소를 관리중이던 map에서 주소를 가져와 dto에 넣어준다.
						tempDto.setScreenshotURL(MonsterImgHrefMap.get(i));
						
						//세팅된 dto를 다시 MonsterInfo Map의 value로 넣는다.
						MonsterInfoMap.put(i, tempDto);
					}
					
//					//중간체크
//					int MonCnt = 1;
//					System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//					for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//						MonsterDto valueDto = entry.getValue();
//						System.out.println(MonCnt + ". name : " + valueDto.getName());
//						System.out.println(MonCnt + ". type : " + valueDto.getType());
//						System.out.println(MonCnt + ". attri : " + valueDto.getAttribute());
//						System.out.println(MonCnt + ". attri_sub : " + valueDto.getAttributeSub());
//						System.out.println(MonCnt + ". point : " + valueDto.getPoints());
//						System.out.println(MonCnt + ". bookNum : " + valueDto.getBookNum());
//						System.out.println(MonCnt + ". rarity : " + valueDto.getRarity());
//						System.out.println(MonCnt + ". screenURL : " + valueDto.getScreenshotURL());
//						MonCnt++;
//					}
//					System.out.println("/////////////////몬스터 이름넣기, 풀스크린샷URL 넣기 완료///////////////");
					///////////////////////////////////////////////////////////////////////////////
					
					//몬스터이름, 평점 파싱 -> 평점은 몬스터이름으로 파싱해야함 : 7,6,5 모두가 평점이 매겨져있는건 아니기때문
					Map<String,Float> MonsterPointMap = new TreeMap<String,Float>();
					//Elements els = doc.select("span.bolder");
					Elements elss = doc.select("table");
					//Elements els = doc.select("tr");
					Element nextTr = null;
					for (Element element : elss) {
						if(element.text().contains("評価点")) {
							//System.out.println("if문 평가 들어옴");
							
							Elements trs = element.select("tr").select("td");
							int cnt =0;
							String key = "";
							Float value = 0.f;
							for (Element elem : trs) {
								if(cnt%2 == 0) {
									key = TranslateText.translate(elem.text());
									//System.out.println("key : " + key);
								}
								else if(cnt%2 == 1) {
									value = Float.valueOf(elem.select("span.bolder").text());
									//System.out.println("value : " + value);
									
									MonsterPointMap.put(key, value);
								}
								cnt++;
							}

							//os.write(MonsterPointMap.toString().getBytes());
							
							break;
						}
						
					}
					///////////////////////////////////////////////////////////////
					
					//MonsterInfo 맵과 pointMap에서 같은 이름을 갖고있는 것들을 찾아, dto 에 평점 넣기.
					for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
						//담겨있던 dto를 꺼낸다
						MonsterDto valueDto = entry.getValue();
						//System.out.println("valueDto 의 name : " + valueDto.getName());
						//담겨있떤 dto의 이름과 동일한 이름을 찾아서 value값(point)를 가져온다.
						//주의 : 평점이 없는 레어도 6,5 몬스터의 경우도 있으니 키값이 있는지 확인해서 가져와야한다.
						if(MonsterPointMap.containsKey(valueDto.getName())) {							
							float point =  MonsterPointMap.get(valueDto.getName());
							//담겨있던 dto의 point에 값을 입력시켜준다.
							valueDto.setPoints(point);
							//valueDto를 다시 담아준다.
							MonsterInfoMap.put(entry.getKey(), valueDto);
						}
					}
					
					//////////////////////////////////////////////////////////////////////
					
					
					//중간체크
//					int MonCnts = 1;
//					System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//					for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//						MonsterDto valueDto = entry.getValue();
//						System.out.println(MonCnts + ". name : " + valueDto.getName());
//						System.out.println(MonCnts + ". type : " + valueDto.getType());
//						System.out.println(MonCnts + ". attri : " + valueDto.getAttribute());
//						System.out.println(MonCnts + ". attri_sub : " + valueDto.getAttributeSub());
//						System.out.println(MonCnts + ". point : " + valueDto.getPoints());
//						System.out.println(MonCnts + ". bookNum : " + valueDto.getBookNum());
//						System.out.println(MonCnts + ". rarity : " + valueDto.getRarity());
//						MonCnts++;
//					}
//					
//					System.out.println("/////////////////몬스터 평점넣기 완료///////////////");
					
					//////////////////////////////////////////////////////////////////////
					
					//hp, 공격력 파싱 - 최대상태 
					int MonsterCntNum = 1; //Map 키 저장용도, MonsterInfoMap 에서 같은 키로 비교사용.
					Map<Integer,List<Integer>> MonsterHpAtkMap = new TreeMap<Integer, List<Integer>>();
					List<Integer> HpAtkList = new ArrayList<Integer>();
					Elements statusEls = doc.select("div.pd_status").select("table");
					if(statusEls.size() !=0 ) {
						//os.write(statusEls.toString().getBytes());
						for (Element element : statusEls) {
							Elements tds = element.select("tr").select("td");
							int tdCnt = 0;
							for (Element element2 : tds) {
								if(tdCnt == 0 ) {
									String text = element2.text();
									//System.out.println(tdCnt + ". hp : " + text);
									//os.write(text.getBytes());
									
									HpAtkList.add(Integer.valueOf(text));
								}
								else if(tdCnt == 2) {
									String text = element2.text();
									//System.out.println(tdCnt + ". atk : " + text);
									
									HpAtkList.add(Integer.valueOf(text));
								}
								
								tdCnt++;
								if(tdCnt == 3)
									break;
							}
							//os.write(tds.toString().getBytes());
							
							MonsterHpAtkMap.put(MonsterCntNum, new ArrayList<Integer>(HpAtkList));
							
							HpAtkList.clear();
							
							MonsterCntNum++;
						}
					}
					
					//System.out.println("///////////////////////// MonsterHpAtkMap 확인하기 ////////////////////////");
					//중간체크
					for (Map.Entry<Integer, List<Integer>> entry : MonsterHpAtkMap.entrySet()) {
						int key = entry.getKey();
						//System.out.println("key : " + key);
						List<Integer> valueList =entry.getValue();
						
						//System.out.println(key + ". hp : " + valueList.get(0) + " / Atk : " + valueList.get(1));
						
						//같은키가 있다면
						if(MonsterInfoMap.containsKey(key)) {
							//기존에 저장된 몬스터 정보맵의 dto 를 가져온다.
							MonsterDto tempDto = MonsterInfoMap.get(key);
							tempDto.setHp(valueList.get(0));
							tempDto.setAttack(valueList.get(1));
							
							//세팅된 dto를 다시 MonsterInfo Map의 value로 넣는다.
							MonsterInfoMap.put(key, tempDto);
						}
					}
					
//					//중간체크
//					int MonCnts = 1;
//					System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//					for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//						MonsterDto valueDto = entry.getValue();
//						System.out.println(MonCnts + ". name : " + valueDto.getName());
//						System.out.println(MonCnts + ". type : " + valueDto.getType());
//						System.out.println(MonCnts + ". attri : " + valueDto.getAttribute());
//						System.out.println(MonCnts + ". attri_sub : " + valueDto.getAttributeSub());
//						System.out.println(MonCnts + ". point : " + valueDto.getPoints());
//						System.out.println(MonCnts + ". bookNum : " + valueDto.getBookNum());
//						System.out.println(MonCnts + ". rarity : " + valueDto.getRarity());
//						System.out.println(MonCnts + ". hp : " + valueDto.getHp());
//						System.out.println(MonCnts + ". Attack : " + valueDto.getAttack());
//						System.out.println(MonCnts + ". screenURL : " + valueDto.getScreenshotURL());
//						MonCnts++;
//					}
//					
//					System.out.println("/////////////////몬스터 hp,Attack넣기 완료///////////////");
					
					////////////////////////////몬스터 정보 테이블 관련 파싱 끝///////////////////////////////////
					
					///////////////////////////몬스터 스킬 정보 테이블 관련 파싱 시작 //////////////////////////////
					
					//스킬정보 파싱
					//DataOutputStream os = new DataOutputStream(new FileOutputStream(HrefFilePath));
					Elements skillEls = doc.select("div.pd_skill");//.select("table");
					
					//<순서구분용key,MonsterNormalSkillDto>
					Map<Integer,List<MonsterNormalSkillDto>> monsterNormalSkillInfoMap= new TreeMap<Integer, List<MonsterNormalSkillDto>>();
					MonsterNormalSkillDto monsterNormalSkillDto = MonsterNormalSkillDto.builder().build();
					List<MonsterNormalSkillDto> monsterNormalSkillList = new ArrayList<MonsterNormalSkillDto>();
					
					//<순서구분용key,MonsterChainSkillDto>
					Map<Integer,List<MonsterChainSkillDto>> monsterChainSkillInfoMap= new TreeMap<Integer, List<MonsterChainSkillDto>>();
					MonsterChainSkillDto monsterChainSkillDto = MonsterChainSkillDto.builder().build();
					List<MonsterChainSkillDto> monsterChainSkillList = new ArrayList<MonsterChainSkillDto>();
					
					
					//순서구분용 key가 될 cnt
					int MonsterCntNumbers = 1;
					//<h3>기술정보 / <h3>체인스킬정보 총2개가 끝나면 몬스터 하나가 끝난것. -> 2가 0이되면 순서구분용 key 를 ++ 하면 됨.
					int h3Cnt = 0;
					
					//현재 h3 tag 가 무엇인지 확인용도
					String nowH3TagText = "";
					//<h4>기술이름 2개면 한계돌파 도 저장시켜줘야함.
					int h4Cnt =0;
					//<td>의 한계돌파 정보도 있으면 저장시켜줘야함.
					int pd_skillCnt = 0;
					//같은 체인스킬이름인데 발동수랑 스킬설명만 다른경우, 체인스킬이름이 또 다시 파싱되지않으므로, 체인스킬이름을 임시저장해준뒤, 이 정보를 넣어준다.
					String tempChainSkillName = "";
					for (Element element : skillEls) {
						if(skillEls.size() !=0 ) {
							Element prevh4 = element.previousElementSibling().previousElementSibling();
							
							//스킬 구분 정보 - 일반기술/체인스킬
							if(prevh4 !=null) {
								String transPrevh4 = TranslateText.translate(prevh4.toString());
								transPrevh4 += "\n";
								//System.out.println(prevh4.toString());
								
								//os.write(transPrevh4.getBytes());
								
								//'기술'이 들어가면 '일반' 으로 분류
								if(transPrevh4.contains("기술")) {
									//System.out.println("일반 스킬 정보로 분류");
									nowH3TagText = "일반";
									
								}
								//'체인'이 들어가면 '체인' 으로 분류
								else if(transPrevh4.contains("체인")) {
									//System.out.println("체인 스킬 정보로 분류");
									nowH3TagText = "체인";
									
								}
								
								//h3가 하나씩 입력될때마다 h3를 올려준다. h3태그가 나왔다는걸 의미.
								h3Cnt++;
								
								//h3에 다시 들어왔다는것은, 다음 일반/체인 스킬 정보를 파싱하기위한 한돌 전,후 구분을 다시 처음부터 해도된다는것이다.   
								h4Cnt = 0;
								pd_skillCnt = 0;
								//h3에 다시들어왔으니까, 체인스킬이름을 다시 갈아줘야한다.
								tempChainSkillName = "";
								
								//기술정보/체인스킬정보 <h3> 2개가 끝났다면
								if(h3Cnt == 3) {
									h3Cnt = 1;
									//h4Cnt = 0;
									//pd_skillCnt = 0;
									
									//여기서 저장 하고 있는 이유 : h3는 2로 다들어왔어도 h3 아래있는 h4 나 div 를 돌고있기때문에 h3가 3이됬을때 다른몬스터 이제 들어간다는것이므로 이때 이전에 파싱한것들 저장하는것이다. 
									//몬스터 일반스킬정보/체인스킬정보 구성한거 저장해놓고 다음몬스터 스킬정보 저장할 준비. 
									monsterNormalSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterNormalSkillDto>(monsterNormalSkillList));
									monsterNormalSkillList.clear();
									
									
									monsterChainSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterChainSkillDto>(monsterChainSkillList));
									monsterChainSkillList.clear();
									
									MonsterCntNumbers++;
								}
							}
							
							//스킬이름 <h4>
							Element prevh3 = element.previousElementSibling();
							//Element prevh3 = prevh4.nextElementSibling();
							if(prevh3 != null) {
								String transPrevh3tag = TranslateText.translate(prevh3.toString());
								String transPrevh3 = TranslateText.translate(prevh3.text());
								//System.out.println("스킬이름 번역확인 : " + transPrevh3);
								//transPrevh3 += "\n";
								//System.out.println(prevh3.toString());
					
								String transn = transPrevh3tag + "\n";
								//os.write(transn.getBytes());
								
								//h3 가 한번 나왔을때는 일반 스킬 정보에 스킬이름을 넣는다.
								if(h3Cnt == 1) {
								//if(nowH3TagText.equals("일반")) {
									switch (h4Cnt) {
									case 0://h4가 처음 등장했을때는 한계돌파 전 스킬정보이다.
										monsterNormalSkillDto.setSkillType("한계돌파전");
										monsterNormalSkillDto.setSkillName(transPrevh3);
										//System.out.println("한계돌파 전 일반 스킬이름 입력 : " + transPrevh3);
										break;
									case 1://h4가 두번째로 등장했을때는 한계돌파 후 스킬정보이다.
										monsterNormalSkillDto.setSkillType("한계돌파후");
										monsterNormalSkillDto.setSkillName(transPrevh3);
										//System.out.println("한계돌파 후 일반 스킬이름  입력 : " + transPrevh3);
										break;
									default:
										break;
									}
									
									h4Cnt++;
								}
								//h3가 두번째로 등장했을때는 체인스킬 정보에 스킬이름 넣는다.
								else if(h3Cnt == 2) {									
								//else if(nowH3TagText.equals("체인")) {
									switch (h4Cnt) {
									case 0://h4가 처음 등장했을때는 한계돌파 전 스킬정보이다.
										monsterChainSkillDto.setSkillType("한계돌파전");
										monsterChainSkillDto.setSkillName(transPrevh3);
										//체인스킬 이름이 같은경우, 여기에 다시 들어오지않고, 아래에서 발동수,스킬정보 만 다시파싱되기때문에, 임시로 저장해두고, 같은 이름을 저장해줄수있게해준다.
										tempChainSkillName = transPrevh3;
										//System.out.println("한계돌파 전 체인 스킬이름  입력 : " + transPrevh3);
										break;
									case 1://h4가 두번째로 등장했을때는 한계돌파 후 스킬정보이다.
										monsterChainSkillDto.setSkillType("한계돌파후");
										monsterChainSkillDto.setSkillName(transPrevh3);
										//체인스킬 이름이 같은경우, 여기에 다시 들어오지않고, 아래에서 발동수,스킬정보 만 다시파싱되기때문에, 임시로 저장해두고, 같은 이름을 저장해줄수있게해준다.
										tempChainSkillName = transPrevh3;
										//System.out.println("한계돌파 후 체인 스킬이름  입력 : " + transPrevh3);
										break;
									default:
										break;
									}
									
									h4Cnt++;
								}
								
								if(h4Cnt == 2) {
									h4Cnt =0;
								}
							}
							
							//발동수, 스킬정보
							String transpdskill = TranslateText.translate(element.toString());
							transpdskill +="\n";
							//System.out.println(element.toString());
							
							//os.write(transpdskill.getBytes());
							Elements tds = element.select("tr").get(1).select("td");
							if(tds == null) {
								System.out.println("tds 가 null");
							}
							else if(tds != null) {
								
								//td 가 한번 나왔을때는 일반 스킬 정보에 스킬이름을 넣는다.
								if(h3Cnt == 1) {
								//if(nowH3TagText.equals("일반")) {
									switch (pd_skillCnt) {
									case 0://pd_skill이 처음 등장했을때는 한계돌파 전 스킬정보이다.
										monsterNormalSkillDto.setSkillType("한계돌파전");
										monsterNormalSkillDto.setNumberOfTriggers(Integer.valueOf(tds.first().text()));
										monsterNormalSkillDto.setSkillInfo(TranslateText.translate(tds.last().text()));
										//System.out.println("한계돌파 전 일반 스킬정보 번역 확인 : "+ monsterNormalSkillDto.getSkillInfo());
										monsterNormalSkillList.add(monsterNormalSkillDto);
										monsterNormalSkillDto = MonsterNormalSkillDto.builder().build();
										break;
									case 1://pd_skill이 두번째로 등장했을때는 한계돌파 후 스킬정보이다.
										monsterNormalSkillDto.setSkillType("한계돌파후");
										monsterNormalSkillDto.setNumberOfTriggers(Integer.valueOf(tds.first().text()));
										monsterNormalSkillDto.setSkillInfo(TranslateText.translate(tds.last().text()));
										//System.out.println("한계돌파 후 일반 스킬정보 번역 확인 : "+ monsterNormalSkillDto.getSkillInfo());
										monsterNormalSkillList.add(monsterNormalSkillDto);
										monsterNormalSkillDto = MonsterNormalSkillDto.builder().build();
										break;
									default:
										break;
									}
									
									pd_skillCnt++;
									//System.out.println("일반스킬 정보 번역에서 pd_skillCnt 올림");
								}
								//h3가 두번째로 등장했을때는 체인스킬 정보에 스킬이름 넣는다.
								else if(h3Cnt == 2) {
								//else if(nowH3TagText.equals("체인")) {
									//System.out.println("h3Cnt : " + h3Cnt + " pd_skillCnt : " + pd_skillCnt);
									switch (pd_skillCnt) {
									case 0://pd_skill이 처음 등장했을때는 한계돌파 전 스킬정보이다.
										monsterChainSkillDto.setSkillType("한계돌파전");
										monsterChainSkillDto.setNumberOfTriggers(Integer.valueOf(tds.first().text()));
										monsterChainSkillDto.setSkillInfo(TranslateText.translate(tds.last().text()));
										//System.out.println("한계돌파 전 체인 스킬정보 번역 확인 : "+ monsterChainSkillDto.getSkillInfo());
										monsterChainSkillList.add(monsterChainSkillDto);
										monsterChainSkillDto = MonsterChainSkillDto.builder().build();
										
										//발동 체인이 한개만 있으면 위에서 끝내고 마치면 되는데,
										//체인스킬의 경우 발동 체인이 가령 ex) 5 , 9 ... 얼만큼 더 있을 수 있는지 모른다.
										int trSize = element.select("tr").size();
										if(trSize >= 3) {
											for(int i=2; i<trSize; i++) {
												Elements newtds = element.select("tr").get(i).select("td");
												monsterChainSkillDto.setSkillType("한계돌파전");
												monsterChainSkillDto.setNumberOfTriggers(Integer.valueOf(newtds.first().text()));
												monsterChainSkillDto.setSkillInfo(TranslateText.translate(newtds.last().text()));
												//System.out.println("한계돌파 전 체인 스킬정보 번역 확인 : "+ monsterChainSkillDto.getSkillInfo());
												//만약 체인 스킬 이름이 등록되지 않았다면, 체인스킬이름은같은데 발동수와 설명만 다른 상황이니까, 저장해놓은 체인스킬이름으로 매꿔준다.
												if(monsterChainSkillDto.getSkillName() == "" || monsterChainSkillDto.getSkillName() == null) {
													monsterChainSkillDto.setSkillName(tempChainSkillName);
												}
												monsterChainSkillList.add(monsterChainSkillDto);
												monsterChainSkillDto = MonsterChainSkillDto.builder().build();
											}
										}
										break;
									case 1://pd_skill이 두번째로 등장했을때는 한계돌파 후 스킬정보이다.
										monsterChainSkillDto.setSkillType("한계돌파후");
										monsterChainSkillDto.setNumberOfTriggers(Integer.valueOf(tds.first().text()));
										monsterChainSkillDto.setSkillInfo(TranslateText.translate(tds.last().text()));
										//System.out.println("한계돌파 후 체인 스킬정보 번역 확인 : "+ monsterChainSkillDto.getSkillInfo());
										monsterChainSkillList.add(monsterChainSkillDto);
										monsterChainSkillDto = MonsterChainSkillDto.builder().build();
										
										//발동 체인이 한개만 있으면 위에서 끝내고 마치면 되는데,
										//체인스킬의 경우 발동 체인이 가령 ex) 5 , 9 ... 얼만큼 더 있을 수 있는지 모른다.
										trSize = element.select("tr").size();
										if(trSize >= 3) {
											for(int i=2; i<trSize; i++) {
												Elements newtds = element.select("tr").get(i).select("td");
												monsterChainSkillDto.setSkillType("한계돌파후");
												monsterChainSkillDto.setNumberOfTriggers(Integer.valueOf(newtds.first().text()));
												monsterChainSkillDto.setSkillInfo(TranslateText.translate(newtds.last().text()));
												//System.out.println("한계돌파 후 체인 스킬정보 번역 확인 : "+ monsterChainSkillDto.getSkillInfo());
												//만약 체인 스킬 이름이 등록되지 않았다면, 체인스킬이름은같은데 발동수와 설명만 다른 상황이니까, 저장해놓은 체인스킬이름으로 매꿔준다.
												if(monsterChainSkillDto.getSkillName() == "" || monsterChainSkillDto.getSkillName() == null) {
													monsterChainSkillDto.setSkillName(tempChainSkillName);
												}
												monsterChainSkillList.add(monsterChainSkillDto);
												monsterChainSkillDto = MonsterChainSkillDto.builder().build();
											}
										}
										break;
									default:
										break;
									}
									
									pd_skillCnt++;
								}
								
								if(pd_skillCnt == 2) {
									pd_skillCnt =0;
								}
							}
							
							//기술정보/체인스킬정보 <h3> 2개가 끝났다면
							if(h3Cnt == 2) {
																
//								//몬스터 일반스킬정보/체인스킬정보 구성한거 저장해놓고 다음몬스터 스킬정보 저장할 준비. 
//								monsterNormalSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterNormalSkillDto>(monsterNormalSkillList));
//								//monsterNormalSkillDto = MonsterNormalSkillDto.builder().build();
//								monsterNormalSkillList.clear();
//								
//								
//								monsterChainSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterChainSkillDto>(monsterChainSkillList));
//								//monsterChainSkillDto = MonsterChainSkillDto.builder().build();
//								monsterChainSkillList.clear();
								
							}
						}
					}
					
					//for문을 빠져나와서 마지막에 저장된 요소들을 저장해주는 로직이 필요하다.
					monsterNormalSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterNormalSkillDto>(monsterNormalSkillList));
					monsterChainSkillInfoMap.put(MonsterCntNumbers, new ArrayList<MonsterChainSkillDto>(monsterChainSkillList));
					
					///////////////////////////////////////몬스터 일반스킬, 체인스킬 파싱 완료 /////////////////////////////////
//					System.out.println("");
//					System.out.println("///////////////////////중간체크 일반스킬/체인스킬 정보 ///////////////////////");
//						//중간체크
//						System.out.println("일반스킬 맵 사이즈 : " + monsterNormalSkillInfoMap.size());
//						//일반스킬정보
//						for (Map.Entry<Integer, List<MonsterNormalSkillDto>> entry : monsterNormalSkillInfoMap.entrySet()) {
//							int i = entry.getKey();
//							List<MonsterNormalSkillDto> tempList = entry.getValue();
//							System.out.println(i + ". 일반 스킬 입력된 개수 : " + tempList.size());
//							for (MonsterNormalSkillDto dto : tempList) {
//								System.out.println(i + ". 일반스킬 이름 : " + dto.getSkillName());
//								System.out.println(i + ". 일반스킬 한계돌파 : " + dto.getSkillType());
//								System.out.println(i + ". 일반스킬 발동수 : " + dto.getNumberOfTriggers());
//								System.out.println(i + ". 일반스킬 스킬정보 : " + dto.getSkillInfo());
//							}
//							System.out.println();
//						}
//						
//						System.out.println("/////////////////// 일반 스킬 정보 확인 끝 ///////////////////");
//						//중간체크
//						System.out.println("체인스킬 맵 사이즈 : " + monsterChainSkillInfoMap.size());
//						
//						//체인스킬정보
//						for (Map.Entry<Integer, List<MonsterChainSkillDto>> entry : monsterChainSkillInfoMap.entrySet()) {
//							int i = entry.getKey();
//							List<MonsterChainSkillDto> tempList = entry.getValue();
//							for (MonsterChainSkillDto dto : tempList) {
//								System.out.println(i + ". 체인스킬 이름 : " + dto.getSkillName());
//								System.out.println(i + ". 체인스킬 한계돌파 : " + dto.getSkillType());
//								System.out.println(i + ". 체인스킬 발동수 : " + dto.getNumberOfTriggers());
//								System.out.println(i + ". 체인스킬 스킬정보 : " + dto.getSkillInfo());
//							}
//							System.out.println();
//						}
//						
//						System.out.println("/////////////////// 체인 스킬 정보 확인 끝 ///////////////////");
					
						//////////////////////////////////리더 스킬 정보 파싱 시작 ////////////////////////////////
						
						//<순서구분용key,MonsterLeaderSkillDto>
						Map<Integer,List<MonsterLeaderSkillDto>> monsterLeaderSkillInfoMap= new TreeMap<Integer, List<MonsterLeaderSkillDto>>();
						MonsterLeaderSkillDto monsterLeaderSkillDto = MonsterLeaderSkillDto.builder().build();
						List<MonsterLeaderSkillDto> monsterLeaderSkillList = new ArrayList<MonsterLeaderSkillDto>();
						
						//DataOutputStream os = new DataOutputStream(new FileOutputStream(HrefFilePath));
						Elements LeaderskillEls = doc.select("h3");
						
						//순서 구분용 key
						int MonCnts =0;
						for (Element element : LeaderskillEls) {
							String LeaderStr = TranslateText.translate(element.text());
							//리더 스킬 정보 라는 h3 태그 인 경우에만
							if(LeaderStr.contains("리더")) {
								MonCnts++;
								String h3trans = TranslateText.translate(element.toString()) + "\n";
								//os.write(h3trans.getBytes());
								
								//다음 태그인 h4 태그 : 리더스킬 이름 을 파싱
								Element h3nexth4 = element.nextElementSibling();
								String h4trans =TranslateText.translate(h3nexth4.toString())+ "\n" ; 
								if(h3nexth4 != null) {
									//os.write(h4trans.getBytes());
									//<h4> 스탠 보호 </h4>  - 리더스킬 이름 파싱
									monsterLeaderSkillDto.setSkillType("한계돌파전");
									monsterLeaderSkillDto.setSkillName(TranslateText.translate(h3nexth4.text()));
									//System.out.println(MonCnts + ". 한계돌파 전 리더스킬 이름 : "+ monsterLeaderSkillDto.getSkillName());
									
									//h4 태그가 있으므로 table 파싱
									Element LeaderSkilltable = h3nexth4.nextElementSibling();
									String tabletrans = TranslateText.translate(LeaderSkilltable.toString()) + "\n";
									//os.write(tabletrans.getBytes());
									//<td> 리더스킬 정보 <td> 파싱
									String LeaderSkillInfoStr = LeaderSkilltable.select("tr").get(1).select("td").text();
									monsterLeaderSkillDto.setSkillInfo(TranslateText.translate(LeaderSkillInfoStr));
									//System.out.println(MonCnts + ". 한계돌파 전 리더스킬 정보 : "+ monsterLeaderSkillDto.getSkillInfo());
									
									//우선 한계돌파 전 으로 저장된 리더스킬 정보를 리스트에 보관한다.
									monsterLeaderSkillList.add(monsterLeaderSkillDto);
									//이제 한계돌파 후 로 저장될 것이있을경우를 대비해 준비한다.
									monsterLeaderSkillDto = MonsterLeaderSkillDto.builder().build();
									
									//만약 한계돌파 이후 리더 스킬 정보가 있다라면, table 태그 다음에 a->div 안에 h4 태그가 또 있을것이다.
									Element checkNextLeaderh4 = LeaderSkilltable.nextElementSibling();
									if(checkNextLeaderh4 != null) {
										//만약 이태그가 a 태그 라면, 한계돌파 정보가 있다는것이므로 한번더 파싱해주자.
										String checkh4tagStr = checkNextLeaderh4.toString();
										char checkatag = checkh4tagStr.charAt(1);
										char a = 'a';
										//만약 <a 로 시작하는 태그라면,
										if(checkatag == a) {
											Element aNextdivtag = checkNextLeaderh4.nextElementSibling();
											String divtagStrs = TranslateText.translate(aNextdivtag.toString()) + "\n";
											//os.write(divtagStrs.getBytes());
											
											//System.out.println(MonCnts + ". 한계돌파 후 리더스킬 이름 : " + TranslateText.translate(aNextdivtag.select("h4").text()));
											//한계돌파후 리더스킬 이름, 정보 파싱
											monsterLeaderSkillDto.setSkillType("한계돌파후");
											monsterLeaderSkillDto.setSkillName(TranslateText.translate(aNextdivtag.select("h4").text()));
											//System.out.println(MonCnts + ". 한계돌파 후 리더스킬 정보 : " + TranslateText.translate(aNextdivtag.select("table").select("tr").get(1).select("td").text()));
											monsterLeaderSkillDto.setSkillInfo(TranslateText.translate(aNextdivtag.select("table").select("tr").get(1).select("td").text()));
											
											//한계돌파 데이터 파싱이 끝났으니 다시 리스트에 저장해주자
											monsterLeaderSkillList.add(monsterLeaderSkillDto);
											monsterLeaderSkillDto = MonsterLeaderSkillDto.builder().build();
										}
										
									}
									//한계돌파 후 리더스킬 이름과 정보가 있던 말던 리더스킬이름과 정보 파싱된것들을 저장은 시켜줘야한다.
									//한계돌파 이후 에 해당되는 if문이 지난다음에 정보를 저장해주면된다.
									//몬스터 한마리에대한 리더스킬 파싱이 끝났으니 맵에 저장해주자.
									monsterLeaderSkillInfoMap.put(MonCnts, new ArrayList<MonsterLeaderSkillDto>(monsterLeaderSkillList));
									//그리고 다음 몬스터의 리스트를 준비하기위해 clear
									monsterLeaderSkillList.clear();
								}
								
							}
						}
						
						////////////////////////////////// 몬스터 리더스킬 정보 파싱 완료 ////////////////////
						//중간체크
//						System.out.println();
//						System.out.println("////////////////리더 스킬 중간 체크 ////////////////////////");
//						for (Map.Entry<Integer, List<MonsterLeaderSkillDto>> entry : monsterLeaderSkillInfoMap.entrySet()) {
//							int i = entry.getKey();
//							List<MonsterLeaderSkillDto> tempList = entry.getValue();
//							for (MonsterLeaderSkillDto dto : tempList) {
//								System.out.println(i + ". 리더스킬 이름 : " + dto.getSkillName());
//								System.out.println(i + ". 리더스킬 한계돌파 : " + dto.getSkillType());
//								System.out.println(i + ". 리더스킬 스킬정보 : " + dto.getSkillInfo());
//							}
//							System.out.println();
//						}
						
						////////////////////////////////////////////////////////////////////////////
						
						//몬스터 진화정보 파싱
						//<순서구분용key,MonsterLeaderSkillDto>
						Map<Integer,List<MonsterEvolutionDto>> monsterEvolutionInfoMap= new TreeMap<Integer, List<MonsterEvolutionDto>>();
						MonsterEvolutionDto monsterEvolutionDto = MonsterEvolutionDto.builder().build();
						List<MonsterEvolutionDto> monsterEvolutionList = new ArrayList<MonsterEvolutionDto>();
						
						DataOutputStream os = new DataOutputStream(new FileOutputStream(HrefFilePath));
						int MonCount = 1;
						
						Elements evolutionElss = doc.select("div.pd_simple_table_col5");
						
						//os.write(TranslateText.translate(evolutionEls.toString()).getBytes());
						
						for (Element evolutionEls : evolutionElss) {
							
							//진화 재료 개수 모아두기
							List<String> requiredLuckthsStrList = evolutionEls.select("tr").get(0).select("th").eachText();
							
							//진화 재료 이름 모아두기
							List<String> MonNameList = evolutionEls.select("tr").get(1).select("td").eachText();
							
							//진화 재료 아이콘URL 모아두기
							List<String> iconUrlStrsList = new ArrayList<String>();
							String urls = "";
							Elements imgEls = evolutionEls.select("tr").get(1).select("noscript").select("img");
							for (Element element : imgEls) {
								urls = element.attr("src");
								//System.out.println(urls);
								iconUrlStrsList.add(urls);
							}
							
							//몬스터 진화재료 정보 저장하기.
							//세개의 리스트 모두 사이즈는 같을것이므로 하나를 기준으로 사이즈 잡기.
							int totalListSize = iconUrlStrsList.size();
							
							for(int i=0; i<totalListSize; i++) {
								monsterEvolutionDto.setName(TranslateText.translate(MonNameList.get(i)));
								monsterEvolutionDto.setRequiredLuck(Integer.valueOf(requiredLuckthsStrList.get(i)));
								monsterEvolutionDto.setIconURL(iconUrlStrsList.get(i));
								
								//dto가 완성될때마다 dto를 list에 저장한다
								monsterEvolutionList.add(monsterEvolutionDto);
								//dto 를 다시 초기화시킨다.
								monsterEvolutionDto = MonsterEvolutionDto.builder().build();
							}
							
							//완성된 Dto를 Map에 저장시킨다.
							monsterEvolutionInfoMap.put(MonCount, new ArrayList<MonsterEvolutionDto>(monsterEvolutionList));
							//리스트를 초기화시킨다.
							monsterEvolutionList.clear();
							
							MonCount++;
						}
						
						////////////////////////////몬스터 진화정보재료 중간체크///////////////////////////////
						System.out.println();
						System.out.println("////////////////몬스터 진화 재료 중간 체크 ////////////////////////");
						for (Map.Entry<Integer, List<MonsterEvolutionDto>> entry : monsterEvolutionInfoMap.entrySet()) {
							int i = entry.getKey();
							List<MonsterEvolutionDto> tempList = entry.getValue();
							for (MonsterEvolutionDto dto : tempList) {
								System.out.println(i + ". 진화재료 이름 : " + dto.getName());
								System.out.println(i + ". 진화재료 개수 : " + dto.getRequiredLuck());
								System.out.println(i + ". 진화재료 URL : " + dto.getIconURL());
							}
							System.out.println();
						}
						
						
						
				}//end of try
				catch (Exception e) {
					e.printStackTrace();
				}
	}
	
	// 3p - 몬스터 상세페이지로 이동할 수 있는 Href 먼저 뽑기위한 함수 - href들 text로 저장중임
	public static void monsterPreviewHrefCrawl() {
		// 크롤링 할 url test
		//String url = "https://gamewith.jp/pocodun/article/show/72816#";
		String url = "https://gamewith.jp/pocodun/article/show/72816";
		//test url
		//String url = "https://gamewith.jp/pocodun/article/show/72816#fir1,fir2,wat2,thu2,for2,lig2,dar2,rea7";
		
		//String url = resultOfSeleniumPageSource;
		
		//String url = "https%3A%2F%2Fgamewith.jp%2Fpocodun%2Farticle%2Fshow%2F72816%23fir1%2Cfir2%2Cwat2%2Cthu2%2Cfor2%2Clig2%2Cdar2%2Crea7";
		
//		String fir1Encode="";
//		try {
//			fir1Encode = URLEncoder.encode("fir1","utf-8");
//			//fir1Encode = URLDecoder.decode("fir1","utf-8");
//		} catch (UnsupportedEncodingException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		System.out.println(fir1Encode);
		
		//File input = new File("C:\\ictcbwd\\workspace\\Java\\crawling\\testHrefURL.html");

		// 페이지 전체소스 저장
		Document doc = null;
		
		//검색용 값 설정
		String[] reaStrs = {"rea7","rea6","rea5"};
		List<String> reaList = Arrays.asList(reaStrs);
		
		String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};
		//String[] attriStrs = {"lig1","dar1"};
		//String[] attriStrs = {"for1"};
		List<String> attriList = Arrays.asList(attriStrs);
		
		//중복 제거용 map
		//Map<String,List<String>> hrefRsltMap = new HashMap<String, List<String>>();
		Map<String,List<String>> hrefRsltMap = new TreeMap<String, List<String>>();
		
		try {
			doc = Jsoup.connect(url).get();
			//doc = Jsoup.parse(input, "UTF-8");
			
			//test용 파싱할 태그들 html 파일로 구성하기위한 변수.
			//BufferedOutputStream bs = null;
			
			//String HrefFilePath = "C:\\ictcbwd\\workspace\\Java\\crawling\\3pOutputHrefs7.html";
			//bs = new BufferedOutputStream(new FileOutputStream(HrefFilePath));
			
			//Elements elems = doc.select("table.sorttable").select("a");
			Elements elems = doc.select("tr.w-idb-element");
			//System.out.println("size : " + elems.size());
			//bs.write(elems.toString().getBytes());
			for (String attriStr : attriList) {
				int AddCnt = 1;
				for (String reaStr : reaList) {
					for (Element ele : elems) {
						// https://gamewith.jp/pocodun/article/show/72816#fir1,fir2,wat2,thu2,for2,lig2,dar2,rea7
						// 는 아래와 같이 검색할 수 있다. 2에 해당되는 것들은 적어주지 않아도 모두 파싱되기때문에, 주속성이 fir1인것만 파싱하면 부속성은
						// 다걸려서 파싱된다.
						// test1
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea7 fir1");
						// //117
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea7 wat1");
						// //114
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea7 thu1");
						// //114
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea7 for1");
						// //109
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea7
						// lig1");//17
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea7
						// dar1");//18

						// test2 - 이하 검색되는 결과는 7성에서 파싱된 몬스터 중에 6성의 내용이 포함되있으면 아래에 6성으로 긁어도 같은 결과가 나오기때문에
						// 6성까지만 진화가 있는 몬스터만 나오질 않으므로, 개수를 파악하는것은 무의미하다.
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea6 fir1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea6 wat1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea6 thu1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea6 for1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea6 lig1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea6 dar1");

						// test3
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea5 fir1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea5 wat1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea5 thu1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea5 for1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea5 lig1");
						// Elements em = ele.getElementsByAttributeValueContaining("class","rea5 dar1");

						// 실제 위 test 들을 돌리는 로직

						String val = reaStr + " " + attriStr;
						Elements em = ele.getElementsByAttributeValueContaining("class", val);

						if (em.size() != 0) {

							// System.out.println("em size : " + em.size());
							// System.out.println(em.toString());
							
							//최초 등록되는 것들의 cnt 값 확인
							
							for (Element element : em) {
								Elements el = element.getElementsByAttribute("href");
								// System.out.println("el size : " + el.size());
								String hrefStr = el.attr("href") + "\n";
								// System.out.println(hrefStr);

								// bs.write(hrefStr.getBytes());

								String MonsterName = element.getElementsByTag("a").text();
								MonsterName = TranslateText.translate(MonsterName);
								MonsterName+=" / ";
								//System.out.println(MonsterName);

								// 중복 결과 값 제거 - 키 이용
								// 아마 만약 해당 href 값을 가진 key가 존재한다면
								if (hrefRsltMap.containsKey(hrefStr)) {
									// list에 값을 더해준다. 이렇게하면 레어도 7->6->5 순으로 도니까 신라7 신라6 신라5 이름이 담기게됨.
									hrefRsltMap.get(hrefStr).add(MonsterName);
								}
								// 키가 없어서 최초 등록 하는거라면 그냥 넣어준다.
								else {
									List<String> tempLst = new ArrayList<String>();
									tempLst.add(MonsterName);
									System.out.println();
									System.out.println(AddCnt + ". 레어도 : "+ reaStr +" 속성 : " + attriStr + " map에 넣는중");
									System.out.printf(AddCnt + ". 레어도 : " + reaStr +" 속성 : " + attriStr + " href : " + hrefStr);
									System.out.println(AddCnt + ". 레어도 : "+ reaStr +" 속성 : " + attriStr + " 몬스터 이름 : " + MonsterName);
									hrefRsltMap.put(hrefStr, tempLst);
									AddCnt++;
								}

							}

						}
					}
					
				}
				
				System.out.println("////////////////////////////////////////text 작업 전 /////////////////////////////////////////////");
				Thread.sleep(3000);
				
				// 레어7 6 5 에 관한 속성 한개 치를 다 돌았다.
				// 해당 속성의 7 6 5 href결과 값들을 txt로 저장시킨다.
				BufferedOutputStream bs = null;
				//확인용 Cnt
				int hrefOutCnt = 1;
				String rslpath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterHrefResult\\" + attriStr
						+ "\\hrefs.txt";
				//bs = new BufferedOutputStream(new FileOutputStream(rslpath, true));
				//BufferedOutputStream 은 출력하면 잘림. 버퍼크기가 디폴트로 부족한거같음 그래서 DataOutputStream 쓰면 안짤림 // https://enspring.tistory.com/492
				DataOutputStream os = new DataOutputStream(new FileOutputStream(rslpath, true));
				// href가저장된 맵을 순회하면서 txt에 값을 저장한다.
				for (Map.Entry<String, List<String>> entry : hrefRsltMap.entrySet()) {
					String key = entry.getKey();
					System.out.println(hrefOutCnt + ". 속성 : "+ attriStr +" href 텍스트 출력중");
					System.out.println(hrefOutCnt + ". 속성 : "+ attriStr +" href : " + key);
					//bs.write(key.getBytes());
					os.write(key.getBytes());
					hrefOutCnt++;
				}
				
				os.close();
				
				
				
				Thread.sleep(7000);
				
				// 해당속성의 7 6 5 몬스터이름 값들을 txt 로 저장시킨다.
				String monsterNamepath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterHrefResult\\" + attriStr
						+ "\\MonsterNames.txt";
				bs = new BufferedOutputStream(new FileOutputStream(monsterNamepath, true));
				// href가저장된 맵을 순회하면서 txt에 값을 저장한다.
				//확인용 Cnt
				int MonsterNameOutCnt = 1;
				for (Map.Entry<String, List<String>> entry : hrefRsltMap.entrySet()) {
					List<String> value = entry.getValue();
					int valueSize = value.size();
					int sizeCnt = 1;
					for (String strs : value) {
						//해당키의 마지막번째 이름뒤에 \n 을 붙여서 개행되면서 저장될수있게한다.
						if(valueSize == sizeCnt) {
							strs += "\n";
						}
						System.out.println(MonsterNameOutCnt + ". 속성 : "+ attriStr +" 몬스터이름 텍스트 출력중");
						System.out.println(MonsterNameOutCnt + ". 속성 : "+ attriStr +" 몬스터이름 : " + strs);
						bs.write(strs.getBytes());
						sizeCnt++;
					}
					MonsterNameOutCnt++;
				}
				bs.close();

				// 저장이 완료된 맵은 초기화 시킨다. 다음 속성의 7 6 5 담아주기 위해서
				hrefRsltMap.clear();
			}
			
			//bs.write(elems.toString().getBytes());
			
			//bs.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
