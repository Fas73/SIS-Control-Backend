package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckpointDTO {
    private Long id;
    private String name;
    private String locationDescription;
    private String nfcTagCode;
    private Long installationId;
}