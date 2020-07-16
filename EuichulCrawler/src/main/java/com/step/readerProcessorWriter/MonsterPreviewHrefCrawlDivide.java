package com.step.readerProcessorWriter;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

import com.dto.MonsterPreviewHrefCrawlStepDto;
import com.util.TranslateText;

public class MonsterPreviewHrefCrawlDivide {

	public static MonsterPreviewHrefCrawlStepDto getMonsterPreviewHrefCrawlStepDto(String paramAttriStr){
		String url = "https://gamewith.jp/pocodun/article/show/72816";

		// 페이지 전체소스 저장
		Document doc = null;

		// 검색용 값 설정
		String[] reaStrs = { "rea7", "rea6", "rea5" };
		List<String> reaList = Arrays.asList(reaStrs);

		// 중복 제거용 map
		TreeMap<String, List<String>> hrefRsltMap = new TreeMap<String, List<String>>();

		try {
			doc = Jsoup.connect(url).get();
			// doc = Jsoup.parse(input, "UTF-8");

			// test용 파싱할 태그들 html 파일로 구성하기위한 변수.
			// BufferedOutputStream bs = null;

			// String HrefFilePath =
			// "C:\\ictcbwd\\workspace\\Java\\crawling\\3pOutputHrefs7.html";
			// bs = new BufferedOutputStream(new FileOutputStream(HrefFilePath));

			// Elements elems = doc.select("table.sorttable").select("a");
			Elements elems = doc.select("tr.w-idb-element");
			// System.out.println("size : " + elems.size());
			// bs.write(elems.toString().getBytes());
			String attriStr = paramAttriStr;
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

						// 최초 등록되는 것들의 cnt 값 확인

						for (Element element : em) {
							Elements el = element.getElementsByAttribute("href");
							// System.out.println("el size : " + el.size());
							String hrefStr = el.attr("href") + "\n";
							// System.out.println(hrefStr);

							// bs.write(hrefStr.getBytes());

							String MonsterName = element.getElementsByTag("a").text();
							MonsterName = TranslateText.translate(MonsterName);
							MonsterName += " / ";
							// System.out.println(MonsterName);

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
								System.out.println(AddCnt + ". 레어도 : " + reaStr + " 속성 : " + attriStr + " map에 넣는중");
								System.out.printf(
										AddCnt + ". 레어도 : " + reaStr + " 속성 : " + attriStr + " href : " + hrefStr);
								System.out.println(AddCnt + ". 레어도 : " + reaStr + " 속성 : " + attriStr + " 몬스터 이름 : "
										+ MonsterName);
								hrefRsltMap.put(hrefStr, tempLst);
								AddCnt++;
							}

						}

					}
				}

			}
			MonsterPreviewHrefCrawlStepDto monsterPreviewHrefCrawlStepDto = MonsterPreviewHrefCrawlStepDto.builder().attriStr(attriStr).hrefRsltMap(hrefRsltMap).build();
			return monsterPreviewHrefCrawlStepDto;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void printTxtFileFromhrefRsltMap(MonsterPreviewHrefCrawlStepDto monsterPreviewHrefCrawlStepDto) {
		
		String attriStr = monsterPreviewHrefCrawlStepDto.getAttriStr();
		
		TreeMap<String, List<String>> hrefRsltMap = monsterPreviewHrefCrawlStepDto.getHrefRsltMap();
		
		// 레어7 6 5 에 관한 속성 한개 치를 다 돌았다.
		// 해당 속성의 7 6 5 href결과 값들을 txt로 저장시킨다.
		BufferedOutputStream bs = null;
		//확인용 Cnt
		int hrefOutCnt = 1;
		String rslpath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterHrefResult\\" + attriStr
				+ "\\hrefs.txt";
		//bs = new BufferedOutputStream(new FileOutputStream(rslpath, true));
		//BufferedOutputStream 은 출력하면 잘림. 버퍼크기가 디폴트로 부족한거같음 그래서 DataOutputStream 쓰면 안짤림 // https://enspring.tistory.com/492
		DataOutputStream os;
		try {
			os = new DataOutputStream(new FileOutputStream(rslpath, true));
			
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
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		
		// 해당속성의 7 6 5 몬스터이름 값들을 txt 로 저장시킨다.
		String monsterNamepath = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterHrefResult\\" + attriStr
				+ "\\MonsterNames.txt";
		try {
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
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
