package com.dynamic.report.controllers;

import com.asia.model.ReportRequest;
import com.asia.model.ReportResponse;
import com.asia.model.ReportResponsePaging;
import com.asia.model.ReportRow;
import com.dynamic.report.services.RSAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class DynamicReportReportsController {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public ReportResponse getReports(ReportRequest reportRequest, BigInteger accountId, String accountRole) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        MapSqlParameterSource params = new MapSqlParameterSource();

        String baseSql = buildSelectClause(reportRequest) + buildWhereClause(reportRequest, accountId, accountRole, params);
        long totalRecords = getTotalRecords(baseSql, params);
        String sql = baseSql + buildOrderByClause(reportRequest) + buildPagingClause(reportRequest, params);
        List<ReportRow> rows = jdbcTemplate.query(sql, params, this::mapRow);
        return buildResponse(rows, reportRequest, totalRecords);
    }

    private String buildSelectClause(ReportRequest reportRequest) {
        List<String> selectParts = new ArrayList<>();

        selectParts.add("id");

        if (reportRequest.getDimensions() != null) selectParts.addAll(reportRequest.getDimensions());
        if (reportRequest.getMetrics() != null) selectParts.addAll(reportRequest.getMetrics());

        return "SELECT " + String.join(", ", selectParts) + " FROM `report` WHERE 1=1 ";
    }

    private String buildWhereClause(ReportRequest reportRequest, BigInteger accountId, String accountRole, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder();

        if (reportRequest.getFilters() != null) {
            if (reportRequest.getFilters().getDateFrom() != null) {
                where.append(" AND date >= :dateFrom ");
                params.addValue("dateFrom", reportRequest.getFilters().getDateFrom());
            }
            if (reportRequest.getFilters().getDateTo() != null) {
                where.append(" AND date <= :dateTo ");
                params.addValue("dateTo", reportRequest.getFilters().getDateTo());
            }
        }

        if ("user".equals(accountRole)) {
            where.append(" AND account_id = :accountId ");
            params.addValue("accountId", accountId);
        }

        return where.toString();
    }

    private String buildOrderByClause(ReportRequest reportRequest) {
        if (reportRequest.getSorting() != null && reportRequest.getSorting().getField() != null) {
            String direction = reportRequest.getSorting().getDirection() != null ? reportRequest.getSorting().getDirection().getValue() : "ASC";
            return " ORDER BY " + reportRequest.getSorting().getField() + " " + direction;
        }
        return "";
    }

    private String buildPagingClause(ReportRequest reportRequest, MapSqlParameterSource params) {
        int page = reportRequest.getPaging() != null ? reportRequest.getPaging().getPage() : 1;
        int size = reportRequest.getPaging() != null ? reportRequest.getPaging().getSize() : 20;
        int offset = (page - 1) * size;

        params.addValue("size", size);
        params.addValue("offset", offset);

        return " LIMIT :size OFFSET :offset";
    }

    private long getTotalRecords(String baseSql, MapSqlParameterSource params) {
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS count_table";
        return jdbcTemplate.queryForObject(countSql, params, Long.class);
    }

    private ReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        ReportRow row = new ReportRow();
        row.setId(rs.getLong("id"));
        row.setAccountId(safeGetLong(rs, "account_id"));
        row.setCampaignId(safeGetLong(rs, "campaign_id"));
        row.setCountry(safeGet(rs, "country"));
        row.setPlatform(safeGet(rs, "platform"));
        row.setBrowser(safeGet(rs, "browser"));
        row.setSpent(safeGetFloat(rs, "spent"));
        row.setClicks(safeGetLong(rs, "clicks"));
        row.setImpressions(safeGetLong(rs, "impressions"));
        row.setDate(safeGetTimestamp(rs, "sys_creation_date") != null ? new Date(rs.getTimestamp("sys_creation_date").getTime()) : null);
        return row;
    }

    private Timestamp safeGetTimestamp(ResultSet rs, String column) {
        try {
            return rs.getTimestamp(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private String safeGet(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private Float safeGetFloat(ResultSet rs, String column) {
        try {
            return rs.getFloat(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private Long safeGetLong(ResultSet rs, String column) {
        try {
            return rs.getLong(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private ReportResponse buildResponse(List<ReportRow> rows, ReportRequest reportRequest, long totalRecords) {
        ReportResponse response = new ReportResponse();
        ReportResponsePaging responsePaging = new ReportResponsePaging();
        int page = reportRequest.getPaging() != null ? reportRequest.getPaging().getPage() : 1;
        int size = reportRequest.getPaging() != null ? reportRequest.getPaging().getSize() : 20;

        responsePaging.setPage(page);
        responsePaging.setSize(size);
        responsePaging.setTotalRecords(totalRecords);
        responsePaging.setTotalPages((int) Math.ceil((double) totalRecords / size));

        response.setPaging(responsePaging);
        response.setData(rows);
        return response;
    }

}
