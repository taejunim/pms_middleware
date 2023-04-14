package pms.vo.system;

import lombok.Data;

/**
 * 시스템 공통 코드 정보
 */
@Data
public class CommonCodeVO {
    private String groupCd; //그룹 코드
    private String code;    //코드
    private String groupNm; //그룹 명
    private String name;    //코드 명
    private int sort;   //정렬 순서
    private String data1;   //참조 값 1
    private String data2;   //참조 값 2
}
