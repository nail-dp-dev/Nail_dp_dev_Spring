package com.backend.naildp.dto.archive;

import com.backend.naildp.common.Boundary;

import lombok.Getter;

@Getter
public class ArchiveRequestDto {
	private String archiveName;
	private Boundary boundary;
}
