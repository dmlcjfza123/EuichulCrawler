package com.step.readerProcessorWriter;

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

import com.dao.MonsterDao;
import com.dto.MonsterLeaderSkillDto;
import com.util.TranslateText;

@Component
public class MonsterLeaderSkillDtoCrawlerDivide {
	
	@Autowired private static MonsterDao monsterDao;
	
	public static MonsterLeaderSkillDto getMonsterLeaderSkillDtoFromDetailPageCrawl(String monsterDetailPageUrl) {
		String url = monsterDetailPageUrl;
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
						
						
			//			if(dto.getSkillName().contains("없음") && dto.getSkillName().contains("없음")) {
			//				System.out.println("리더스킬 이름, 정보 가 없으니 insert 안함");
			//			}
			//			else {									
			//				monsterDao.insertMonsterLeaderSkillInfo(dto);
			//			}
						
						//없어도 없다고 해주는게 나을듯함. 
						//monsterDao.insertMonsterLeaderSkillInfo(dto);
						
						i++;
						return dto;
					}
				}
			}
			
			
			}
			catch (Exception e) {
			e.printStackTrace();
			return null;
			}
		
		return null;
	}
		
}

