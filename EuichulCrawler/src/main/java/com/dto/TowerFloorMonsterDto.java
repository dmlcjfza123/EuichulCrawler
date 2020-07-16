package com.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TowerFloorMonsterDto {
	private int towerFloorMonsterId;
	private int floorNum; //왜래키
	private String iconURL; 
	private int hp;
	private int layer;
}
