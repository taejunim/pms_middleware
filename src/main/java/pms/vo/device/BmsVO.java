package pms.vo.device;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import lombok.Data;

import java.util.List;

@Data
public class BmsVO {
    private RackVO rack;
    private List<ModuleVO> modules;
    private boolean isError;

    @Data
    public static class RackVO {
        private String rackCode;    //Rack 코드
        private int regDate;    //등록 일시
        private String operationStatus; //운영 상태
        private String operationModeStatus; //운전 모드 상태
        private float userSoc;  //사용자 SoC
        private float realSoc;  //실제 SoC
        private float soh;  //SoH
        private float voltage;  //전압
        private float currentSensor1;   //전류 센서 1
        private float currentSensor2;   //전류 센서 2
        private int chargeCurrentLimit; //충전 전류 제한
        private int chargePowerLimit;   //충전 전력 제한
        private int dischargeCurrentLimit;  //방전 전류 제한
        private int dischargePowerLimit;    //방전 전력 제한
        private float positiveVoltageResistance;    //(+)극 전압 절연저항
        private float negativeVoltageResistance;    //(-)극 전압 절연저항
        private String positiveMainRelayAction; //(+)극 메인 릴레이 동작
        private String positiveMainRelayContact;    //(+)극 메인 릴레이 접점
        private String negativeMainRelayAction; //(-)극 메인 릴레이 동작
        private String negativeMainRelayContact;    //(-)극 메인 릴레이 접점
        private String emergencyRelayAction;    //비상정지 릴레이 동작
        private String emergencyRelayContact;   //비상정지 릴레이 접점
        private String prechargeRelayAction;    //사전충전 릴레이 동작
        private String warningFlag; //경고 여부
        private String faultFlag;   //결함 여부
    }

    @Data
    public static class ModuleVO {
        private String rackCode;    //Rack 코드
        private int moduleNo;   //Module 번호
        private int regDate;    //등록 일시
        private float moduleVoltage;    //Module 전압
        private float cell1Voltage; //Cell 1. 전압
        private float cell2Voltage; //Cell 2. 전압
        private float cell3Voltage; //Cell 3. 전압
        private float cell4Voltage; //Cell 4. 전압
        private float cell5Voltage; //Cell 5. 전압
        private float cell6Voltage; //Cell 6. 전압
        private float cell7Voltage; //Cell 7. 전압
        private float cell8Voltage; //Cell 8. 전압
        private float cell9Voltage; //Cell 9. 전압
        private float cell10Voltage;    //Cell 10. 전압
        private float cell11Voltage;    //Cell 11. 전압
        private float cell12Voltage;    //Cell 12. 전압
        private float cell13Voltage;    //Cell 13. 전압
        private float cell14Voltage;    //Cell 14. 전압
        private float cell15Voltage;    //Cell 15. 전압
        private float cell16Voltage;    //Cell 16. 전압
        private String cellBalancingFlag;   //전체 Cell 밸런싱 여부
        private String cell1BalancingFlag;  //Cell 1. 밸런싱 여부
        private String cell2BalancingFlag;  //Cell 2. 밸런싱 여부
        private String cell3BalancingFlag;  //Cell 3. 밸런싱 여부
        private String cell4BalancingFlag;  //Cell 4. 밸런싱 여부
        private String cell5BalancingFlag;  //Cell 5. 밸런싱 여부
        private String cell6BalancingFlag;  //Cell 6. 밸런싱 여부
        private String cell7BalancingFlag;  //Cell 7. 밸런싱 여부
        private String cell8BalancingFlag;  //Cell 8. 밸런싱 여부
        private String cell9BalancingFlag;  //Cell 9. 밸런싱 여부
        private String cell10BalancingFlag; //Cell 10. 밸런싱 여부
        private String cell11BalancingFlag; //Cell 11. 밸런싱 여부
        private String cell12BalancingFlag; //Cell 12. 밸런싱 여부
        private String cell13BalancingFlag; //Cell 13. 밸런싱 여부
        private String cell14BalancingFlag; //Cell 14. 밸런싱 여부
        private String cell15BalancingFlag; //Cell 15. 밸런싱 여부
        private String cell16BalancingFlag; //Cell 16. 밸런싱 여부
        private float cellTemperature1; //Cell 온도 1
        private float cellTemperature2; //Cell 온도 2
        private float cellTemperature3; //Cell 온도 3
        private float cellTemperature4; //Cell 온도 4
        private float cellTemperature5; //Cell 온도 5
        private float cellTemperature6; //Cell 온도 6
        private float cellTemperature7; //Cell 온도 7
        private float cellTemperature8; //Cell 온도 8
    }

    @Data
    public static class RequestItem {
        private String address; //레지스터 주소 (HEX 코드)
        private int register;   //레지스터 주소
        private int size;   //데이터 사이즈
        private String dataType;    //데이터 유형
        private int scale;  //값 크기
        private String type;    //레지스터 유형
        private int no; //항목 번호
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
        private int no; //항목 번호
        private String name;    //항목 명
        private List<InputRegister> inputRegisters; //수신 레지스터 값 목록
    }
}
