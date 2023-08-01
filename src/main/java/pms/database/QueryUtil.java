package pms.database;

import pms.common.util.FieldUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryUtil {

    /**
     * Insert Values 쿼리문 생성
     * <p>
     * - 1건의 Insert Values
     *
     * @param object Values 클래스
     * @return Insert Values 쿼리문
     */
    public static String createInsertValue(Object object) {

        return setInsertValues(object).toString();
    }

    /**
     * 다중 Insert Values 쿼리문 생성
     * <p>
     * - 1건 이상의 Insert Values
     *
     * @param objectList Values 클래스 목록
     * @return 다중 Insert Values 쿼리문
     */
    public static String createInsertValues(List<Object> objectList) {
        StringBuilder values = new StringBuilder(); //등록 필드 값

        for (Object object : objectList) {
            values.append(setInsertValues(object));
            values.append(", ");    //값 행 뒤에 콤마 추가
        }

        values.delete(values.length() - 2, values.length());    //마지막 값 끝의 공백 및 콤마 제거

        return values.toString();
    }

    /**
     * 클래스 필드에 해당하는 값 호출
     * <p>
     * - 필드 타입에 따라 값 설정하여 반환
     *
     * @param object 클래스
     * @param field  클래스 필드
     * @return 필드 타입에 따른 필드 값
     */
    private static Object getFieldValue(Object object, Field field) {
        Object value = null;

        try {
            Object getValue = field.get(object);   //해당 필드의 값 호출
            //호출한 값의 NULL 구분
            if (getValue != null) {
                //데이터 타입 별 설정
                if (field.getType().equals(String.class)) {
                    value = "'" + getValue + "'";   //String 형식인 경우 따옴표 추가
                } else if (field.getType().equals(int.class)) {
                    value = getValue;
                } else if (field.getType().equals(float.class)) {
                    value = getValue;
                } else if (field.getType().equals(long.class)) {
                    value = getValue;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return value;
    }

    /**
     * Insert 명령어의 Values 설정
     *
     * @param object 클래스
     * @return Insert Values
     */
    private static StringBuilder setInsertValues(Object object) {
        StringBuilder values = new StringBuilder(); //등록 필드 값
        ArrayList<Field> fields = FieldUtil.getFields(object.getClass());   //필드 목록

        for (Field field : fields) {
            Object fieldValue = getFieldValue(object, field);
            values.append(fieldValue).append(", ");
        }

        values.delete(values.length() - 2, values.length());    //마지막 값 끝의 공백 및 콤마 제거
        values.insert(0, "(").insert(values.length(), ")"); //값 처음과 끝에 괄호 추가

        return values;
    }

    /**
     * 추가 조건문 생성
     * <p>
     * - WHERE 절 이후 조건문 추가
     *
     * @param parameterMap Parameter 목록
     * @return 추가 조건문
     */
    public static String addConditionSql(Map<String, List<String>> parameterMap) {
        StringBuilder conditionSql = new StringBuilder();

        for (String parameterKey : parameterMap.keySet()) {
            StringBuilder parameterValues = new StringBuilder();

            for (String parameterValue : parameterMap.get(parameterKey)) {
                if (parameterValues.toString().isEmpty()) {
                    parameterValues.append(parameterValue);
                } else {
                    parameterValues.append(", ").append(parameterValue);
                }
            }

            conditionSql.append(" AND ").append(parameterKey).append(" IN (");
            conditionSql.append(parameterValues).append(")");
        }

        return conditionSql.toString();
    }
}
