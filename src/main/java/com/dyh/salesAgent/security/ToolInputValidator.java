package com.dyh.salesAgent.security;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ToolInputValidator {

    private static final Set<String> VALID_REGIONS =
            Set.of("华东区", "华南区", "华北区", "西南区");

    private static final Set<String> VALID_CHART_TYPES =
            Set.of("line", "bar", "pie");

    private static final Set<String> VALID_DIMENSIONS = Set.of("region", "rep", "category");

    // 日期格式校验
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    public String validateDate(String dateStr) {
        if (dateStr == null || !DATE_PATTERN.matcher(dateStr).matches()) {
            throw new IllegalArgumentException("无效的日期格式，请使用 yyyy-MM-dd");
        }
        try {
            LocalDate.parse(dateStr);
            return dateStr;
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的日期：" + dateStr);
        }
    }

    public DateRange validateDateRange(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(validateDate(startDate));
        LocalDate end = LocalDate.parse(validateDate(endDate));
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("开始日期不得晚于结束日期");
        }
        return new DateRange(start, end);
    }

    public String validateRegionName(String regionName) {
        if (regionName == null || regionName.isBlank()) return null;
        if (!VALID_REGIONS.contains(regionName)) {
            throw new IllegalArgumentException("无效的大区名称：" + regionName +
                    "，有效值为：" + VALID_REGIONS);
        }
        return regionName;
    }

    public String validateChartType(String chartType) {
        if (!VALID_CHART_TYPES.contains(chartType)) {
            throw new IllegalArgumentException("无效的图表类型：" + chartType +
                    "，有效值为：line/bar/pie");
        }
        return chartType;
    }

    public String validateDimension(String dimension) {
        if (!VALID_DIMENSIONS.contains(dimension)) {
            throw new IllegalArgumentException("无效的维度：" + dimension +
                    "，有效值为：region/rep/category");
        }
        return dimension;
    }

    public String validateDimension(String dimension, Set<String> allowed) {
        if (dimension == null || !allowed.contains(dimension)) {
            throw new IllegalArgumentException("无效的统计维度：" + dimension + "，有效值为：" + allowed);
        }
        return dimension;
    }

    public int validateTopN(int topN) {
        if (topN < 1 || topN > 20) {
            throw new IllegalArgumentException("TopN 必须在 1 到 20 之间");
        }
        return topN;
    }

    public int validateSignedTopN(int topN) {
        if (topN == 0 || Math.abs((long) topN) > 20) {
            throw new IllegalArgumentException("TopN 必须在 -20 到 -1 或 1 到 20 之间");
        }
        return topN;
    }

    public int validateMonths(int months) {
        if (months < 1 || months > 24) {
            throw new IllegalArgumentException("月份数量必须在 1 到 24 之间");
        }
        return months;
    }

    public int validateLimit(int limit) {
        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("返回条数必须在 1 到 50 之间");
        }
        return limit;
    }

    public String validateOptionalText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(label + "长度不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    public String validateTitle(String title) {
        return validateOptionalText(title, "图表标题", 100);
    }

    public record DateRange(LocalDate start, LocalDate end) {}
}
