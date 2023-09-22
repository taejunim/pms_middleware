package pms.vo.device;

import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import lombok.Data;

@Data
public class AirConditionerVO {
    private String airConditionerCode; //에어컨코드
    private int regDate;               //등록일시
    private String operationStatus;    //운전 상태 = "03: OFF, 04: ON, 09: 연결 해제, 96: 수신 오류, 97: 송신 오류, 98:경고, 99: 결함"
    private String operationModeStatus;//운전 모드 상태 = "01: 냉방, 02: 송풍, 03: 자동, 04: 난방"
    private float indoorTemperature;  //실내 온도
    private float setTemperature;     //설정 온도(희망 온도)
//    private int errorCode;          //에러 코드(0: 에러없음, 1~999: 에러코드)
    private String warningFlag;        //경고 여부
    private String faultFlag;          //결함 여부

    @Data
    public static class RequestItem {
        private String functionType;//Modbus Function
        private String address;     //레지스터 주소 (HEX 코드)
        private int register;       //레지스터 주소
//        private int size;             //데이터 사이즈
        private String dataType;    //데이터 유형
        private int scale;          //값 크기
        private String type;        //레지스터 유형
        private String name;        //항목 명
    }

    @Data
    public static class ResponseItem {
        private String functionType;//Modbus Function
        private String address;     //아이템 주소 (HEX 코드)
        private int register;       //레지스터 주소
//        private int size;             //데이터 사이즈
        private String dataType;    //데이터 유형
        private int scale;          //값 크기
        private String type;        //레지스터 유형
        private int no;             //항목 번호
        private String name;        //항목 명
        private Boolean coilStatus;             //functionType=coils 수신 값
        private Register multipleRegister;      //functionType=holdingRegisters 수신 값
        private InputRegister inputRegister;    //functionType=inputRegisters 수신 값
    }
}
