package pms.vo.history;

import lombok.Data;

@Data
public class EnergyHistoryVONew {
    private String energyNo;    //전력량 이력 번호
    private String operationHistoryType;    //운전 이력 구분
    private int operationHistoryDate;   //운전 이력 일시
    private String pcsCode;  //PCS 코드
    private String operationMode;   //운전 모드
    private String operationType;   //운영 구분
    private String scheduleNo;  //일정 번호
}
