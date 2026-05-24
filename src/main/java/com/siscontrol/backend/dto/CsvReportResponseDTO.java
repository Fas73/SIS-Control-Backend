package com.siscontrol.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsvReportResponseDTO {
    private String fileName;
    private String filePath;
    private String downloadUrl;
    private LocalDateTime generatedAt;
    private int rows;
}
