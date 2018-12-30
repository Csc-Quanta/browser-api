package org.csc.browserAPI.Helper;

import org.csc.browserAPI.gens.Additional;

import java.util.List;

/**
 * 查询报表help
 */
public interface QueryReportHelper {
    /**
     * 查询报表
     * @param reqGetReportInfo
     * @return
     */
    List<Additional.ReportResult> queryReport(Additional.ReqGetReportInfo reqGetReportInfo);
}
