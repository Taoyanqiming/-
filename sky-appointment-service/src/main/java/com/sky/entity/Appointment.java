package com.sky.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer appointmentId;
    private Integer userId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;
    private String visitReason;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
