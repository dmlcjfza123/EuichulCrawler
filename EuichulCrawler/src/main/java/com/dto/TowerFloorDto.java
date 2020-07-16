package com.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TowerFloorDto {
	private int towerFloorId;
	private int layer; //왜래키
	private int floorNum; //층수
	private String floorURL;
}
