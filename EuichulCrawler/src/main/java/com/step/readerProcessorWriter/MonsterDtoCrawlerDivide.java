package com.step.readerProcessorWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.dto.MonsterDto;
import com.util.TranslateText;

public class MonsterDtoCrawlerDivide {
	public static List<String> getMonsterDetailPageUrlList() {

		String[] attriStrs = {"fir1","wat1","thu1","for1","lig1","dar1"};

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
				System.out.println(e);
			} catch (IOException e) {
				System.out.println(e);
			}
		}
		
		return new ArrayList<String>(detailHrefList);
	}
	
	public static MonsterDto getMonsterDtoFromDetailPageCrawl(String monsterDetailPageUrl) {
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();	
		
		String url = monsterDetailPageUrl;
		
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
			
//			//중간체크
//			int MonCntz = 1;
//			System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//			for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//				MonsterDto valueDto = entry.getValue();
//				System.out.println(MonCntz + ". type : " + valueDto.getType());
//				System.out.println(MonCntz + ". attri : " + valueDto.getAttribute());
//				System.out.println(MonCntz + ". attri_sub : " + valueDto.getAttributeSub());
//				System.out.println(MonCntz + ". point : " + valueDto.getPoints());
//				System.out.println(MonCntz + ". bookNum : " + valueDto.getBookNum());
//				System.out.println(MonCntz + ". rarity : " + valueDto.getRarity());
//				MonCntz++;
//			}
//			System.out.println("/////////////////몬스터 기본정보넣기 - 중간체크 - 완료///////////////");
			
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
			
//			//중간체크
//			int MonCnt = 1;
//			System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//			for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//				MonsterDto valueDto = entry.getValue();
//				System.out.println(MonCnt + ". name : " + valueDto.getName());
//				System.out.println(MonCnt + ". type : " + valueDto.getType());
//				System.out.println(MonCnt + ". attri : " + valueDto.getAttribute());
//				System.out.println(MonCnt + ". attri_sub : " + valueDto.getAttributeSub());
//				System.out.println(MonCnt + ". point : " + valueDto.getPoints());
//				System.out.println(MonCnt + ". bookNum : " + valueDto.getBookNum());
//				System.out.println(MonCnt + ". rarity : " + valueDto.getRarity());
//				System.out.println(MonCnt + ". screenURL : " + valueDto.getScreenshotURL());
//				MonCnt++;
//			}
//			System.out.println("/////////////////몬스터 이름넣기, 풀스크린샷URL 넣기 완료///////////////");
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
//			int MonCnts = 1;
//			System.out.println("MonsetInfoMap Size : " + MonsterInfoMap.size());
//			for(Map.Entry<Integer, MonsterDto> entry : MonsterInfoMap.entrySet()) {
//				MonsterDto valueDto = entry.getValue();
//				System.out.println(MonCnts + ". name : " + valueDto.getName());
//				System.out.println(MonCnts + ". type : " + valueDto.getType());
//				System.out.println(MonCnts + ". attri : " + valueDto.getAttribute());
//				System.out.println(MonCnts + ". attri_sub : " + valueDto.getAttributeSub());
//				System.out.println(MonCnts + ". point : " + valueDto.getPoints());
//				System.out.println(MonCnts + ". bookNum : " + valueDto.getBookNum());
//				System.out.println(MonCnts + ". rarity : " + valueDto.getRarity());
//				MonCnts++;
//			}
//			
//			System.out.println("/////////////////몬스터 평점넣기 완료///////////////");
			
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
					//monsterDao.insertMonsterInfo(valueDto);
					
					return valueDto;
				}

				//다음 상세페이지에서 몬스터들 저장할것이기때문에 clear 해줘야함.
				//MonsterInfoMap.clear();
				
				
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return null;
	}
	
	
}
