package com.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TowerDto {
	private int towerId;
	private String name;
}
