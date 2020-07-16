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
import com.dto.MonsterNormalSkillDto;
import com.util.TranslateText;

@Component
public class MonsterNormalSkillDtoCrawlerDivide {
	@Autowired private static MonsterDao monsterDao;

	public static MonsterNormalSkillDto getMonsterNormalSkillDtoFromDetailPageCrawl(String monsterDetailPageUrl) {
		
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
		
			
			///////////////////////////몬스터 스킬 정보 테이블 관련 파싱 시작 //////////////////////////////
				
			//스킬정보 파싱
			Elements skillEls = doc.select("div.pd_skill");
			
			//<순서구분용key,MonsterNormalSkillDto>
			Map<Integer,List<MonsterNormalSkillDto>> monsterNormalSkillInfoMap= new TreeMap<Integer, List<MonsterNormalSkillDto>>();
			MonsterNormalSkillDto monsterNormalSkillDto = MonsterNormalSkillDto.builder().build();
			List<MonsterNormalSkillDto> monsterNormalSkillList = new ArrayList<MonsterNormalSkillDto>();
			
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
			
			///////////////////////////////////////몬스터 일반스킬 파싱 완료 /////////////////////////////////
			System.out.println("");
//			System.out.println("///////////////////////중간체크 일반스킬/체인스킬 정보 ///////////////////////");
//				//중간체크
//				System.out.println("일반스킬 맵 사이즈 : " + monsterNormalSkillInfoMap.size());
//				//일반스킬정보
//				for (Map.Entry<Integer, List<MonsterNormalSkillDto>> entry : monsterNormalSkillInfoMap.entrySet()) {
//					int i = entry.getKey();
//					List<MonsterNormalSkillDto> tempList = entry.getValue();
//					System.out.println(i + ". 일반 스킬 입력된 개수 : " + tempList.size());
//					for (MonsterNormalSkillDto dto : tempList) {
//						System.out.println(i + ". 일반스킬 이름 : " + dto.getSkillName());
//						System.out.println(i + ". 일반스킬 한계돌파 : " + dto.getSkillType());
//						System.out.println(i + ". 일반스킬 발동수 : " + dto.getNumberOfTriggers());
//						System.out.println(i + ". 일반스킬 스킬정보 : " + dto.getSkillInfo());
//					}
//					System.out.println();
//				}
				
				
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
							
							//monsterDao.insertMonsterNormalSkillInfo(dto);
							
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
