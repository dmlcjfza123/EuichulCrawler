package com.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonsterEvolutionDto {
	private int monsterEvolutionId;
	//MonsterDto에 데이터를 입력시킬때, MonsterEvolutionDto 에도 같이 데이터를 입력시킬것이므로,
	//monsterId 를 굳이, MONSTER_INFO 테이블에서 select 후 검색해서 가져온 데이터를 넣어줄 필요는 없다.
	//select 해서 가져와야하는경우는, jsp에서 입력된 DB정보를 가져와 입혀주기위해 테이블을 조회 하게될때이다.
	private int monsterId;
	private String name;
	private int requiredLuck;
	private String iconURL;
}
