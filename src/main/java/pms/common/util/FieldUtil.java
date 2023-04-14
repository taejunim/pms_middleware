package pms.common.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Field Utility
 * <p>
 * - 필드 관련 함수 유틸리티
 */
public class FieldUtil {

    /**
     * 해당 클래스 객체의 필드 목록 추출
     *
     * @param type 클래스 타입
     * @return 필드 목록
     */
    public static <T> ArrayList<Field> getFields(Class<T> type) {
        ArrayList<Field> fields = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);

            fields.add(field);
        }

        return fields; //필드 추가
    }

    /**
     * 해당 클래스 객체의 필드 목록 추출
     * <p>
     * - 명시된 필드를 제외하여 필드 목록 추출
     *
     * @param type          클래스 타입
     * @param excludeFields 제외 필드 목록
     * @return 필드 목록
     */
    public static <T> ArrayList<Field> getFields(Class<T> type, String[] excludeFields) {
        ArrayList<Field> fields = new ArrayList<>();

        //클래스 객체의 필드 추출
        for (Field field : type.getDeclaredFields()) {
            //제외할 필드가 존재하는 경우에만 필드 제외
            if (excludeFields != null) {
                boolean isExcept = Arrays.asList(excludeFields).contains(field.getName());  //필드 제외 여부 확인

                if (!isExcept) {
                    fields.add(field);  //제외되지 않을 필드만 추가
                }
            } else {
                fields.add(field);
            }
        }

        return fields;
    }
}
