package com.dto;

import java.util.List;
import java.util.TreeMap;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonsterPreviewHrefCrawlStepDto {
	private TreeMap<String, List<String>> hrefRsltMap;
	private String attriStr;
}
