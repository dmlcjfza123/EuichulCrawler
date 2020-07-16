package com.util;

import java.io.File;



/*
 * 폴더 만들때 주의사항.
 * Asgard 와같은 보스폴더는 기존에 직접 만들어서 세팅되있어야한다. 이거해봐야 4개인데 매크로처리해서 만드는것보다 직접만드는게 나은거같아서 직접 하기로함.
 * 따라서 Asgard 폴더 같이 보스폴더 없으면 폴더생성이 안된다. 참고할것.
 */

public class Utils {
	
	//몬스터이름을가진 폴더 만들기
	public static void makeFolderOfMonsterName(String MonsterName, String Attribute) {
		String path = "C:\\ictcbwd\\workspace\\Java\\crawling\\MonsterIcon\\" +Attribute+"\\" +MonsterName;
		File Folder = new File(path);
		// 해당 디렉토리가 없을경우 디렉토리를 생성합니다.
		if (!Folder.exists()) {
			try {
				Folder.mkdir(); // 폴더 생성합니다.
				System.out.println(MonsterName + " 폴더가 생성되었습니다.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else{
			System.out.println(MonsterName + " 폴더가 이미 생성되어 있습니다.");
		}
	}
	
	
	//6p 폴더 만들기
	public static void makeFolderOf6p(String Layer, String BossFolderStr) {
 	   
        //String LayerToString = String.valueOf(Layer);
        
        String path = "C:\\ictcbwd\\workspace\\Java\\crawling\\6pResult\\" +BossFolderStr+"\\";
        //path = path + LayerToString + "Layer";
        path = path + Layer;
        File Folder = new File(path);
        
			// 해당 디렉토리가 없을경우 디렉토리를 생성합니다.
			if (!Folder.exists()) {
				try {
					Folder.mkdir(); // 폴더 생성합니다.
					System.out.println(Layer + " 폴더가 생성되었습니다.");
					
					String bossFolder = "\\BossIcon";
					String achieveFolder = "\\achieveIcon";
					
					String bossPath = path + bossFolder;
					String achievePath = path + achieveFolder;
					
					//System.out.println("bossPath : " + bossPath);
					//System.out.println("achievePath : " + achievePath);
					
					File bossFile = new File(bossPath);
					bossFile.mkdir();
					System.out.println(bossFolder + " 폴더가 생성되었습니다.");
					
					File achieveFile = new File(achievePath);
					achieveFile.mkdir();
					System.out.println(achieveFolder + " 폴더가 생성되었습니다.");
					
				} catch (Exception e) {
					e.getStackTrace();
				}
			} else {
				System.out.println(Layer + " 폴더가 이미 생성되어 있습니다.");
			}
	}
	
	//7p 폴더 만들기
	public static void makeFolderOf7p(String Layer,String BossFolderStr) {
		String path = "C:\\ictcbwd\\workspace\\Java\\crawling\\7pResult\\" +BossFolderStr+"\\";
		path = path + Layer;
		File Folder = new File(path);

		// 해당 디렉토리가 없을경우 디렉토리를 생성합니다.
		if (!Folder.exists()) {
			try {
				Folder.mkdir(); // 폴더 생성합니다.
				System.out.println(Layer + " 폴더가 생성되었습니다.");

				String floorFolder = "\\Floor";
				for(int i=1; i<=5; i++) {
					String floorPath = path + floorFolder + String.valueOf(i);
					Folder = new File(floorPath);
					Folder.mkdir(); // 폴더 생성합니다.
					System.out.println(floorFolder + String.valueOf(i) + " 폴더가 생성되었습니다.");
					
					String floorImgPath = floorPath + "\\floorImg";
					Folder = new File(floorImgPath);
					Folder.mkdir(); // 폴더 생성합니다.
					System.out.println(floorFolder + String.valueOf(i) + "\\floorImg 폴더가 생성되었습니다.");
					
					String monsterImgPath = floorPath + "\\monsterImg";
					Folder = new File(monsterImgPath);
					Folder.mkdir(); // 폴더 생성합니다.
					System.out.println(floorFolder + String.valueOf(i) + "\\monsterImg 폴더가 생성되었습니다.");
				}

			} catch (Exception e) {
				e.getStackTrace();
			}
		} else {
			System.out.println(Layer + " 폴더가 이미 생성되어 있습니다.");
		}
	}
}
