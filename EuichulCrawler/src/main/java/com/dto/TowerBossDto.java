package com.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TowerBossDto {
	private int towerBossId;
	private int towerId; //외래키
	private String name;
	private String iconURL;
	private int layer;
	private String limitInfo;
}
