package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AppointmentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer appointmentId;
    private Integer userId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;
    private String visitReason;
    //1=待审批，2=已通过，3=已拒绝
    private Integer status;
    private LocalDateTime updateTime;
}
