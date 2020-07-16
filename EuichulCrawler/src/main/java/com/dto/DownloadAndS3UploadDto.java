package com.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadAndS3UploadDto {
	private String monsterName;
	private String url;
	private String downloadPath;
	private String uploadPath;
	private String attribute;
	private boolean isRepeat;
}
