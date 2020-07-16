package com.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonsterLeaderSkillDto {
	private int monsterLeaderSkillId;
	private int monsterId;
	private String skillType;//한계돌파전,한계돌파후
	private String skillInfo; //스킬 설명
	private String skillName; //스킬 이름
}
