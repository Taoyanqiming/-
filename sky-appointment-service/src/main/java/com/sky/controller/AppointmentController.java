package com.sky.controller;


import com.sky.dto.AppointmentDTO;
import com.sky.entity.Appointment;
import com.sky.result.Result;
import com.sky.service.AppointmentService;
import com.sky.vo.AppointVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/appoint")

public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;
    /**
     * 用户提交预约申请
     * @param appointmentDTO 预约信息
     * @return 操作结果
     */
    @PostMapping("/submit")
    public Result<Integer> submit(@RequestBody AppointmentDTO appointmentDTO) {
            Integer appointmentId = appointmentService.submit(appointmentDTO);
            return Result.success(appointmentId,"预约成功");

    }

    /**
     * 修改预约信息
     * @param appointmentDTO 预约信息
     * @return 操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody AppointmentDTO appointmentDTO, HttpServletRequest request) {
        appointmentDTO.setUserId(Integer.valueOf( request.getHeader("X-User-Id")));
        appointmentService.update(appointmentDTO);

        return Result.successMsg("修改成功");
    }

    /**
     * 管理员审核预约
     * @return 操作结果
     */
    @PutMapping("/approve")
    public Result approve(AppointmentDTO appointmentDTO) {
        appointmentService.approve(appointmentDTO);
        return Result.successMsg("审核成功");
    }

    /**
     * 根据条件查询预约记录
     * @return 预约列表
     */
    @PostMapping("/list")
    public Result<List<Appointment>> list(@RequestBody AppointmentDTO appointmentDTO) {
        List<Appointment> appointments =appointmentService.getByUserId(appointmentDTO);
        return Result.success(appointments,"查询成功");
    }

    /**
     * 根据ID查询预约
     * @param appointmentId 预约ID
     * @return 预约信息
     */
    @GetMapping("/{appointmentId}")
    public Result<AppointVO> getById(@PathVariable Integer appointmentId) {
        AppointVO appointment = appointmentService.getById(appointmentId);
        return Result.success(appointment,"查询成功");
    }
}