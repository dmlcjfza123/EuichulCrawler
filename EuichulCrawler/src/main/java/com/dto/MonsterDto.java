package com.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonsterDto {
	private int monsterId;
	private String iconURL;
	private String name;
	private String type;
	private String attribute;
	private String attributeSub;
	private float points;
	private int bookNum;
	private int rarity;
	private int hp;
	private int attack;
	private String screenshotURL;
}
