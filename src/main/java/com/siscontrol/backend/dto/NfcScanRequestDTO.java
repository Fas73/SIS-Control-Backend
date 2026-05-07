package com.siscontrol.backend.dto;
import lombok.Data;

@Data
public class NfcScanRequestDTO {

    private Long executionId;
    private String nfcTagCode;

}
