package com.step.readerProcessorWriter;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.dao.MonsterDao;
import com.dto.MonsterEvolutionDto;
import com.util.TranslateText;

@Component
public class MonsterEvolutionDtoCrawlerDivide {
	
	@Autowired private static MonsterDao monsterDao;
	
	public static MonsterEvolutionDto getMonsterEvolutionDtoFromDetailPageCrawl(String monsterDetailPageUrl) {
		AmazonS3 s3Client = AmazonS3ClientBuilder
				.standard()
				.withCredentials(new EnvironmentVariableCredentialsProvider())
				.withRegion(Regions.AP_NORTHEAST_2)
				.build();
		
		//String url = "https://gamewith.jp/pocodun/article/show/73009";
		String url = monsterDetailPageUrl;
		
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
				
//				//진화 재료 아이콘URL 모아두기
//				List<String> iconUrlStrsList = new ArrayList<String>();
//				String urls = "";
//				Elements imgEls = evolutionEls.select("tr").get(1).select("noscript").select("img");
//				for (Element element : imgEls) {
//					urls = element.attr("src");
//					//System.out.println(urls);
//					iconUrlStrsList.add(urls);
//				}
				
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
						
						
						//monsterDao.insertMonsterEvolutionInfo(dto);
						
						i++;
						
						return dto;
					}
				}
			}
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
		
		return null;
	}
	
}
