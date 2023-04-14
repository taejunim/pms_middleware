package pms.vo.device;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import lombok.Data;

import java.util.List;

@Data
public class PcsVO {
    private String pcsCode; //PCS 코드
    private int regDate;    //등록 일시
    private String operationStatus; //운전 상태
    private String operationModeStatus; //운전 모드 상태
    private float outputPower;  //출력 전력
    private float rsLineVoltage;    //계통 R-S 선간전압
    private float stLineVoltage;    //계통 S-T 선간전압
    private float trLineVoltage;    //계통 T-R 선간전압
    private float rPhaseCurrent;    //계통 R상 전류
    private float sPhaseCurrent;    //계통 S상 전류
    private float tPhaseCurrent;    //계통 T상 전류
    private float frequency;    //계통 주파수
    private float dcLinkVoltage;    //DC-Link 전압
    private float batteryVoltage;   //배터리 전압
    private float batteryCurrent;   //배터리 전류
    private float igbtTemperature1; //IGBT 온도 1
    private float igbtTemperature2; //IGBT 온도 2
    private float igbtTemperature3; //IGBT 온도 3
    private String acMainMcStatus;  //AC 메인 전자접촉기 상태
    private String dcMainMcStatus;  //DC 메인 전자접촉기 상태
    private float accumulatedChargeEnergy;  //누적 충전 전력량
    private float accumulatedDischargeEnergy;   //누적 방전 전력량
    private int referencePower; //기준 전력 값
    private String emergencyStopFlag;   //비상정지 발생 여부
    private String warningFlag; //경고 여부
    private String faultFlag;   //결함 여부

    @Data
    public static class RequestItem {
        private String address; //레지스터 주소 (HEX 코드)
        private int register;   //레지스터 주소
        private int size;   //데이터 사이즈
        private String dataType;    //데이터 유형
        private int scale;  //값 크기
        private String type;    //레지스터 유형
        private String name;    //항목 명
    }

    @Data
    public static class ResponseItem {
        private String address; //아이템 주소 (HEX 코드)
        private int register;   //레지스터 주소
        private int size;   //데이터 사이즈
        private String dataType;    //데이터 유형
        private int scale;  //값 크기
        private String type;    //레지스터 유형
        private String name;    //항목 명
        private List<InputRegister> inputRegisters; //수신 레지스터 값 목록
    }
}
