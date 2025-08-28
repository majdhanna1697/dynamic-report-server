package com.dynamic.report.resources;

import com.asia.api.ReportsApi;
import com.asia.model.ReportRequest;
import com.asia.model.ReportResponse;
import com.dynamic.report.annotations.RoleDetecting;
import com.dynamic.report.controllers.DynamicReportReportsController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigInteger;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class DynamicReportReportsResource implements ReportsApi {

    @Autowired
    private DynamicReportReportsController dynamicReportReportsController;

    @Override
    @RoleDetecting
    public ResponseEntity<ReportResponse> getReports(ReportRequest reportRequest) throws Exception {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        BigInteger accountId = new BigInteger((String) request.getAttribute("accountId"));
        String accountRole = (String) request.getAttribute("accountRole");
        return new ResponseEntity<>(dynamicReportReportsController.getReports(reportRequest, accountId, accountRole), HttpStatus.OK);
    }
}
