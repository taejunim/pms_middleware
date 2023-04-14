package pms.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert Utility
 * <p>
 * - 변환 함수 유틸리티
 */
public final class ConvertUtil {
    /**
     * Camel Case 변환
     * <p>
     * - Snake Case 문자열을 Camel Case 문자열로 변환
     *
     * @param snakeCase 변환할 Snake Case 문자열
     * @return 변환된 Camel Case 문자열
     */
    public static String toCamelCase(String snakeCase) {
        Pattern pattern = Pattern.compile("_(.)");
        Matcher matcher = pattern.matcher(snakeCase.toLowerCase());

        StringBuffer camelCase = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(camelCase, matcher.group(1).toUpperCase());
        }

        matcher.appendTail(camelCase);

        return camelCase.toString();
    }

    /**
     * Snake Case 변환 (Lower Snake Case)
     * <p>
     * - Camel Case 문자열을 Snake Case 문자열로 변환
     *
     * @param camelCase 변환할 Camel Case 문자열
     * @return Snake Case 형식으로 변환된 문자열
     */
    public static String toSnakeCase(String camelCase) {
        Pattern pattern = Pattern.compile("([a-z0-9])([A-Z])");
        Matcher matcher = pattern.matcher(camelCase);

        StringBuffer snakeCase = new StringBuffer();

        while (matcher.find()) {
            String format = String.format(
                    "%s_%s",
                    matcher.group(1),
                    matcher.group(2).toLowerCase()
            );

            matcher.appendReplacement(snakeCase, format);
        }

        matcher.appendTail(snakeCase);

        return snakeCase.toString();
    }

    /**
     * Upper Snake Case 변환
     * <p>
     * - Camel Case 문자열을 Upper Snake Case 문자열로 변환
     *
     * @param camelCase 변환할 Camel Case 문자열
     * @return Upper Snake Case 형식으로 변환된 문자열
     */
    public static String toUpperSnakeCase(String camelCase) {

        return toSnakeCase(camelCase).toUpperCase();
    }
}
