package com.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TowerBossRewardDto {
	private int towerBossRewardId;
	private int layer; //왜래키
	private String iconURL;
}
