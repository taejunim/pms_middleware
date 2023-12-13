package pms.communication.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pms.system.ess.ESSManager;
import pms.vo.device.*;
import pms.vo.device.external.PowerMeterVO;
import pms.vo.device.external.PowerRelayVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.util.*;

/**
 * Web Sender
 * <p>
 * - 웹소켓 송신
 */
public class WebSender extends WebClient {

    /**
     * 연결 확인 전송
     */
    public void sendConnect() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", getMiddlewareId());
        jsonObject.addProperty("eventType", "req");
        jsonObject.addProperty("deviceType", "M/W");
        jsonObject.addProperty("dataType", "connect");

        Gson gson = new Gson();
        String json = gson.toJson(jsonObject);

        webSocketClient.send(json);
        System.out.println("PMS Websocket connected. - " + json);
    }

    /**
     * 제어 응답 데이터 전송
     *
     * @param remoteId    원격 제어 ID
     * @param deviceCode  장비 코드
     * @param controlCode 제어 코드
     * @param result      제어 결과
     * @param message     제어 메시지
     */
    public void sendResponse(String remoteId, String deviceCode, String controlCode, int result, String message) {
        String controlResult = null;  //제어 결과 (0, 2, 3: fail, 1: success)

        switch (result) {
            case 0:
                controlResult = "fail";
                if (message.equals("")) {
                    message = "기타 오류";
                }
                break;
            case 1:
                controlResult = "success";
                break;
            case 2:
                controlResult = "fail";
                if (message.equals("")) {
                    message = "데이터 오류";
                }
                break;
            case 3:
                controlResult = "fail";
                if (message.equals("")) {
                    message = "필수 값 누락";
                }
                break;
        }

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("deviceCode", deviceCode); //장비 코드
        bodyJson.addProperty("controlCode", controlCode);   //제어 코드
        bodyJson.addProperty("controlResult", controlResult);   //제어 결과
        bodyJson.addProperty("message", message);   //메시지

        JsonObject headerJson = new JsonObject();
        headerJson.addProperty("id", remoteId); //원격 제어 ID
        headerJson.addProperty("eventType", "res"); //이벤트 타입
        headerJson.addProperty("dataType", "control");  //데이터 타입
        headerJson.add("data", bodyJson);

        String json = getJson(headerJson);
        webSocketClient.send(json);
    }

    /**
     * 장비 데이터 전송
     *
     * @param deviceVO   장비 정보
     * @param deviceData 장비 데이터
     * @param errors     오류 정보
     */
    public void sendData(DeviceVO deviceVO, Object deviceData, List<String> errors) {
        JsonObject headerJson = setHeaderJson(deviceVO);
        JsonObject bodyJson = new JsonObject();
        String category = deviceVO.getDeviceCategory(); //장비 분류
        String categorySub = deviceVO.getDeviceCategorySub();   //장비 하위 분류

        switch (category) {
            case "01":  //BMS
                bodyJson = setRackDataJson(deviceData, errors);
                break;
            case "02":  //PCS
                bodyJson = setPCSDataJson(deviceData, errors);
                break;
            case "03":  //컨버터(이동형 PCS)
                switch (categorySub) {
                    case "0301":    //AC/DC 컨버터
                        bodyJson = setACConverterDataJson(deviceData, errors);
                        break;
                    case "0302":    //DC/DC 컨버터
                        bodyJson = setDCConverterDataJson(deviceData, errors);
                        break;
                }
                break;
            case "04":  //센서
                bodyJson = setSensorDataJson(deviceVO.getDeviceRoom(), deviceData, errors);
                break;
            case "05":  //미터기
                switch (categorySub) {
                    case "0501":    //전력 계전기
                        bodyJson = setPowerRelayDataJson(deviceData);
                        break;
                    case "0502":    //전력 계측기
                        bodyJson = setPowerMeterDataJson(deviceVO.getDeviceNo(), deviceData);
                        break;
                }
                break;
            case "80":  //공조 장치
                bodyJson = setAirConditionerDataJson(deviceVO.getDeviceRoom(), deviceData, errors);
                break;
        }

        headerJson.add("data", bodyJson);
        String json = getJson(headerJson);

        webSocketClient.send(json);
    }

    /**
     * Json 객체를 String 형식으로 변환
     *
     * @param jsonObject Json Object
     * @return String 형식의 Json
     */
    private String getJson(JsonObject jsonObject) {
        Gson gson = new Gson();

        return gson.toJson(jsonObject);
    }

    /**
     * Header Json 객체 생성
     *
     * @param deviceVO 장비 정보
     * @return Header Json 객체
     */
    private JsonObject setHeaderJson(DeviceVO deviceVO) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", getMiddlewareId());
        jsonObject.addProperty("eventType", "req");
        jsonObject.addProperty("deviceCategory", deviceVO.getDeviceCategory());
        jsonObject.addProperty("deviceCategorySub", deviceVO.getDeviceCategorySub());
        jsonObject.addProperty("deviceCode", deviceVO.getDeviceCode());
        jsonObject.addProperty("dataType", "status");

        return jsonObject;
    }

    /**
     * 장비 오류 Json 배열 생성
     *
     * @param errors 오류 정보
     * @return 장비 오류 Json 배열
     */
    private JsonArray setErrorJsonArray(List<String> errors) {
        JsonArray errorArray = new JsonArray();

        for (String errorCode : errors) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorCode", errorCode);

            errorArray.add(errorObject);
        }

        return errorArray;
    }

    /**
     * 오류 여부 설정
     *
     * @param warningFlag 경고 여부
     * @param faultFlag   결함 여부
     * @return 오류 여부(Y: 발생, N: 미발생)
     */
    private String setErrorFlag(String warningFlag, String faultFlag) {
        String errorFlag = "N";

        if (warningFlag.equals("Y") || faultFlag.equals("Y")) {
            errorFlag = "Y";
        }

        return errorFlag;
    }

    /**
     * 장비 오류 Json 객체 추가
     *
     * @param bodyJson  장비 데이터 Json
     * @param errorFlag 오류 여부
     * @param errors    오류 코드 목록
     * @return 장비 오류 Json 객체
     */
    private JsonObject addErrorJson(JsonObject bodyJson, String errorFlag, List<String> errors) {
        bodyJson.addProperty("errorFlag", errorFlag);

        JsonArray errorArray = new JsonArray();

        if (errorFlag.equals("Y")) {
            if (errors != null) {
                errorArray = setErrorJsonArray(errors);
            }
        }

        bodyJson.add("errorList", errorArray);

        return bodyJson;
    }

    /**
     * Rack 데이터 Json 객체 생성
     *
     * @param deviceData 장비 데이터
     * @param errors     오류 코드 목록
     * @return Rack 데이터 Json 객체
     */
    private JsonObject setRackDataJson(Object deviceData, List<String> errors) {
        BmsVO bmsVO = (BmsVO) deviceData;
        BmsVO.RackVO rackVO = bmsVO.getRack();
        List<BmsVO.ModuleVO> modules = bmsVO.getModules();
        String errorFlag = setErrorFlag(rackVO.getWarningFlag(), rackVO.getFaultFlag());

        float sumCurrent = rackVO.getCurrentSensor1() + rackVO.getCurrentSensor2(); //전류 센서 합계
        float averageCurrent = (float) (Math.round((sumCurrent / 2) * 10) / 10.0);  //평균 전류 계산

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("rackCode", rackVO.getRackCode()); //Rack 코드
        bodyJson.addProperty("operationStatus", rackVO.getOperationStatus());   //운전 상태
        bodyJson.addProperty("operationModeStatus", rackVO.getOperationModeStatus());   //운전 모드 상태
        bodyJson.addProperty("soc", rackVO.getUserSoc());   //SoC
        bodyJson.addProperty("voltage", String.format("%.2f", rackVO.getVoltage()));    //전압
        bodyJson.addProperty("currentSensor1", rackVO.getCurrentSensor1()); //전류 센서1
        bodyJson.addProperty("currentSensor2", rackVO.getCurrentSensor2()); //전류 센서2
        bodyJson.addProperty("averageCurrent", String.format("%.1f", averageCurrent));  //평균 전류
        bodyJson.addProperty("chargeCurrentLimit", rackVO.getChargeCurrentLimit()); //충전 전류 제한 값
        bodyJson.addProperty("chargePowerLimit", rackVO.getChargePowerLimit()); //충전 전력 제한 값
        bodyJson.addProperty("dischargeCurrentLimit", rackVO.getDischargeCurrentLimit());   //방전 전류 제한 값
        bodyJson.addProperty("dischargePowerLimit", rackVO.getDischargePowerLimit());   //방전 전력 제한 값
        bodyJson.addProperty("positiveVoltageResistance", rackVO.getPositiveVoltageResistance());   //(+)극 전압 절연저항
        bodyJson.addProperty("negativeVoltageResistance", rackVO.getNegativeVoltageResistance());   //(-)극 전압 절연저항
        bodyJson.addProperty("positiveMainRelayAction", rackVO.getPositiveMainRelayAction());   //(+)극 메인 릴레이 동작
        bodyJson.addProperty("positiveMainRelayContact", rackVO.getPositiveMainRelayContact()); //(+)극 메인 릴레이 접점
        bodyJson.addProperty("negativeMainRelayAction", rackVO.getNegativeMainRelayAction());   //(-)극 메인 릴레이 동작
        bodyJson.addProperty("negativeMainRelayContact", rackVO.getNegativeMainRelayContact()); //(-)극 메인 릴레이 접점
        bodyJson.addProperty("emergencyRelayAction", rackVO.getEmergencyRelayAction()); //비상정지 릴레이 동작
        bodyJson.addProperty("emergencyRelayContact", rackVO.getEmergencyRelayContact());   //비상정지 릴레이 접점
        bodyJson.addProperty("prechargeRelayAction", rackVO.getPrechargeRelayAction()); //사전충전 릴레이 동작

        bodyJson.add("moduleSummary", setModuleSummaryJson(modules));

        return addErrorJson(bodyJson, errorFlag, errors);
    }

    /**
     * Module 요약 Json 객체 생성
     *
     * @param modules Module 데이터 목록
     * @return Module 요약 Json 객체
     */
    private JsonObject setModuleSummaryJson(List<BmsVO.ModuleVO> modules) {
        List<Float> cellVoltages = new ArrayList<>();   //모든 모듈의 셀 전압 목록
        List<Float> moduleTemps = new ArrayList<>();    //모듈 별 평균 온도 목록

        for (BmsVO.ModuleVO moduleVO : modules) {
            cellVoltages.add(moduleVO.getCell1Voltage());
            cellVoltages.add(moduleVO.getCell2Voltage());
            cellVoltages.add(moduleVO.getCell3Voltage());
            cellVoltages.add(moduleVO.getCell4Voltage());
            cellVoltages.add(moduleVO.getCell5Voltage());
            cellVoltages.add(moduleVO.getCell6Voltage());
            cellVoltages.add(moduleVO.getCell7Voltage());
            cellVoltages.add(moduleVO.getCell8Voltage());
            cellVoltages.add(moduleVO.getCell9Voltage());
            cellVoltages.add(moduleVO.getCell10Voltage());
            cellVoltages.add(moduleVO.getCell11Voltage());
            cellVoltages.add(moduleVO.getCell12Voltage());
            cellVoltages.add(moduleVO.getCell13Voltage());
            cellVoltages.add(moduleVO.getCell14Voltage());
            cellVoltages.add(moduleVO.getCell15Voltage());
            cellVoltages.add(moduleVO.getCell16Voltage());

            float sumCellTemp = moduleVO.getCellTemperature1() + moduleVO.getCellTemperature2()
                    + moduleVO.getCellTemperature3() + moduleVO.getCellTemperature4()
                    + moduleVO.getCellTemperature5() + moduleVO.getCellTemperature6()
                    + moduleVO.getCellTemperature7() + moduleVO.getCellTemperature8();  //온도 합계
            float averageTemp = (float) (Math.round((sumCellTemp / 8) * 10) / 10.0);    //평균 온도 계산

            moduleTemps.add(averageTemp);
        }

        Float maxCellVoltage = Collections.max(cellVoltages);   //최대 Cell 전압
        Float minCellVoltage = Collections.min(cellVoltages);   //최소 Cell 전압

        DoubleSummaryStatistics moduleTempsStatistics = moduleTemps.stream().mapToDouble(Float::floatValue).summaryStatistics();
        float averageModuleTemp = (float) moduleTempsStatistics.getAverage();   //평균 모듈 온도
        float maxModuleTemp = (float) moduleTempsStatistics.getMax();   //최고 모듈 온도
        float minModuleTemp = (float) moduleTempsStatistics.getMin();   //최소 모듈 온도

        JsonObject summaryJson = new JsonObject();
        summaryJson.addProperty("averageTemp", String.format("%.1f", averageModuleTemp));   //평균 온도
        summaryJson.addProperty("maxModuleTemp", String.format("%.1f", maxModuleTemp)); //최고 모듈 온도
        summaryJson.addProperty("minModuleTemp", String.format("%.1f", minModuleTemp)); //최저 모듈 온도
        summaryJson.addProperty("maxCellVoltage", maxCellVoltage);  //최대 Cell 전압
        summaryJson.addProperty("minCellVoltage", minCellVoltage);  //최소 Cell 전압

        return summaryJson;
    }

    /**
     * 실시간 누적 전력량 설정
     *
     * @param pcsVO           PCS 정보
     * @param referenceEnergy 참조 전력량(조정 기준 전력량)
     * @return 실시간 누적 전력량
     */
    private float setCurrentAccumulatedEnergy(PcsVO pcsVO, float referenceEnergy) {
        String operationMode = pcsVO.getOperationModeStatus();  //운전 모드

        //운전 모드에 따른 실시간 누적 전력량 계산
        switch (operationMode) {
            case "1":   //충전
                return pcsVO.getAccumulatedChargeEnergy() - referenceEnergy;
            case "2":   //방전
                return pcsVO.getAccumulatedDischargeEnergy() - referenceEnergy;
            default:
                return 0;
        }
    }

    /**
     * PCS 데이터 Json 객체 생성
     *
     * @param deviceData 장비 데이터
     * @param errors     오류 코드 목록
     * @return PCS 데이터 Json 객체
     */
    private JsonObject setPCSDataJson(Object deviceData, List<String> errors) {
        PcsVO pcsVO = (PcsVO) deviceData;
        String errorFlag = setErrorFlag(pcsVO.getWarningFlag(), pcsVO.getFaultFlag());

        float rsLineVoltage = pcsVO.getRsLineVoltage(); //R-S 선간 전압
        float stLineVoltage = pcsVO.getStLineVoltage(); //S-T 선간 전압
        float trLineVoltage = pcsVO.getTrLineVoltage(); //T-R 선간 전압
        float sumLineVoltage = rsLineVoltage + stLineVoltage + trLineVoltage;
        float averageLineVoltage = (float) (Math.round((sumLineVoltage / 3) * 10) / 10.0);  //평균 선간 전압 계산

        float rPhaseCurrent = pcsVO.getRPhaseCurrent(); //R상 전류
        float sPhaseCurrent = pcsVO.getSPhaseCurrent(); //S상 전류
        float tPhaseCurrent = pcsVO.getTPhaseCurrent(); //T상 전류
        float sumPhaseCurrent = rPhaseCurrent + sPhaseCurrent + tPhaseCurrent; //상 전류 총 합계
        float averagePhaseCurrent = (float) (Math.round((sumPhaseCurrent / 3) * 10) / 10.0);    //평균 상 전류 게산

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("operationStatus", pcsVO.getOperationStatus());    //운전 상태
        bodyJson.addProperty("operationModeStatus", pcsVO.getOperationModeStatus());    //운전 모드 상태
        bodyJson.addProperty("outputPower", String.format("%.1f", pcsVO.getOutputPower())); //출력 전력
        bodyJson.addProperty("rsLineVoltage", String.format("%.1f", rsLineVoltage));    //계통 R-S 선간전압
        bodyJson.addProperty("stLineVoltage", String.format("%.1f", stLineVoltage));    //계통 S-T 선간전압
        bodyJson.addProperty("trLineVoltage", String.format("%.1f", trLineVoltage));    //계통 T-R 선간전압
        bodyJson.addProperty("averageLineVoltage", String.format("%.1f", averageLineVoltage));  //평균 선간전압
        bodyJson.addProperty("rPhaseCurrent", String.format("%.1f", rPhaseCurrent));    //계통 R상 전류
        bodyJson.addProperty("sPhaseCurrent", String.format("%.1f", sPhaseCurrent));    //계통 S상 전류
        bodyJson.addProperty("tPhaseCurrent", String.format("%.1f", tPhaseCurrent));    //계통 T상 전류
        bodyJson.addProperty("averagePhaseCurrent", String.format("%.1f", averagePhaseCurrent));    //평균 상 전류
        bodyJson.addProperty("frequency", String.format("%.1f", pcsVO.getFrequency())); //계통 주파수
        bodyJson.addProperty("dcLinkVoltage", String.format("%.1f", pcsVO.getDcLinkVoltage())); //DC-Link 전압
        bodyJson.addProperty("batteryVoltage", String.format("%.1f", pcsVO.getBatteryVoltage()));   //배터리 전압
        bodyJson.addProperty("batteryCurrent", String.format("%.1f", pcsVO.getBatteryCurrent()));   //배터리 전류
        bodyJson.addProperty("igbtTemperature1", pcsVO.getIgbtTemperature1());  //IGBT 온도 1
        bodyJson.addProperty("igbtTemperature2", pcsVO.getIgbtTemperature2());  //IGBT 온도 2
        bodyJson.addProperty("igbtTemperature3", pcsVO.getIgbtTemperature3());  //IGBT 온도 3
        bodyJson.addProperty("acMainMcStatus", pcsVO.getAcMainMcStatus());  //AC 메인 전자접촉기
        bodyJson.addProperty("dcMainMcStatus", pcsVO.getDcMainMcStatus());  //DC 메인 전자접촉기
        bodyJson.addProperty("emergencyStopFlag", pcsVO.getEmergencyStopFlag());    //비상정지 발생 여부

        ESSManager essManager = new ESSManager();
        float referenceEnergy = essManager.getReferenceEnergy();    //참조 전력량(조정 기준 전력량)
        float currentAccumulatedEnergy = setCurrentAccumulatedEnergy(pcsVO, referenceEnergy); //실시간 누적 전력량

        //총 누적 전력량 Json
        JsonObject totalAccumulatedJson = new JsonObject();
        totalAccumulatedJson.addProperty("charge", essManager.getTotalCharge());    //총 누적 전력량 - 총 누적 충전량
        totalAccumulatedJson.addProperty("discharge", essManager.getTotalDischarge());  //총 누적 전력량 - 총 누적 방전량

        //전력량 현황 Json
        JsonObject energyStatusJson = new JsonObject();
        energyStatusJson.add("totalAccumulated", totalAccumulatedJson); //전력량 현황 - 총 누적 전력량
        energyStatusJson.addProperty("currentAccumulated", String.format("%.1f", currentAccumulatedEnergy));    //전력량 현황 - 현재 누적 전력량
        energyStatusJson.addProperty("currentBattery", essManager.convertToBatteryEnergy());    //전력량 현황 - 현재 배터리 전력량

        bodyJson.add("energyStatus", energyStatusJson);   //전력량 현황

        return addErrorJson(bodyJson, errorFlag, errors);
    }

    /**
     * AC 컨버터 데이터 Json 객체 생성
     *
     * @param deviceData 장비 데이터
     * @param errors     오류 코드 목록
     * @return AC 컨버터 데이터 Json 객체
     */
    private JsonObject setACConverterDataJson(Object deviceData, List<String> errors) {
        ConverterVO converterVO = (ConverterVO) deviceData;
        ConverterVO.ACConverterVO acConverterVO = converterVO.getAcConverter();
        String errorFlag = setErrorFlag(acConverterVO.getWarningFlag(), acConverterVO.getFaultFlag());

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("operationStatus", acConverterVO.getOperationStatus());
        bodyJson.addProperty("operationModeStatus", acConverterVO.getOperationModeStatus());
        bodyJson.addProperty("setOperationMode", acConverterVO.getSetOperationMode());
        bodyJson.addProperty("setCurrent", acConverterVO.getSetCurrent());
        bodyJson.addProperty("totalActiveCurrent", acConverterVO.getTotalActiveCurrent());
        bodyJson.addProperty("totalVoltage", acConverterVO.getTotalVoltage());
        bodyJson.addProperty("totalPower", acConverterVO.getTotalPower());
        bodyJson.addProperty("internalTemp", acConverterVO.getInternalTemp());
        bodyJson.addProperty("transformerTemp", acConverterVO.getTransformerTemp());

        /*JsonObject leftInverterJson = new JsonObject();
        JsonObject rightInverterJson = new JsonObject();*/

        for (ConverterVO.ACInverterVO inverterVO : converterVO.getAcInverters()) {
            if (inverterVO.getInverterNo() == 1) {
                bodyJson.add("leftInverter", setACInverterDataJson(inverterVO));
            } else if (inverterVO.getInverterNo() == 2) {
                bodyJson.add("rightInverter", setACInverterDataJson(inverterVO));
            }
        }

        /*bodyJson.add("leftInverter", leftInverterJson);
        bodyJson.add("rightInverter", rightInverterJson);*/

        ESSManager essManager = new ESSManager();
        JsonObject totalAccumulatedJson = new JsonObject();
        totalAccumulatedJson.addProperty("charge", essManager.getTotalCharge());    //총 누적 전력량 - 총 누적 충전량
        totalAccumulatedJson.addProperty("discharge", essManager.getTotalDischarge());  //총 누적 전력량 - 총 누적 방전량

        JsonObject energyStatusJson = new JsonObject();
        energyStatusJson.add("totalAccumulated", totalAccumulatedJson); //전력량 현황 - 총 누적 전력량
        /* 수정 필요 */
        energyStatusJson.addProperty("currentAccumulated", String.format("%.1f", (float) 10));  //전력량 현황 - 현재 누적 전력량
        /* 수정 필요 */
        energyStatusJson.addProperty("currentBattery", String.format("%.1f", (float) 100)); //전력량 현황 - 현재 누적 전력량

        bodyJson.add("energyStatus", energyStatusJson); //전력량 현황

        return addErrorJson(bodyJson, errorFlag, errors);
    }

    /**
     * AC 인버터 데이터 Json 객체 생성
     *
     * @param inverterVO 인버터 정보
     * @return AC 인버터 데이터 Json 객체
     */
    private JsonObject setACInverterDataJson(ConverterVO.ACInverterVO inverterVO) {
        JsonObject inverterJson = new JsonObject();
        inverterJson.addProperty("modeStatus", inverterVO.getModeStatus());
        inverterJson.addProperty("inverterStatus", inverterVO.getInverterStatus());
        inverterJson.addProperty("power", inverterVO.getPower());
        inverterJson.addProperty("totalCurrent", inverterVO.getTotalCurrent());
        inverterJson.addProperty("outputVoltage", inverterVO.getOutputVoltage());
        inverterJson.addProperty("outputFrequency", inverterVO.getOutputFrequency());
        inverterJson.addProperty("dcVoltage", inverterVO.getDcVoltage());
        inverterJson.addProperty("dcOffset", inverterVO.getDcOffset());
        inverterJson.addProperty("activeCurrent", inverterVO.getActiveCurrent());
        inverterJson.addProperty("activeCurrentContrast", inverterVO.getActiveCurrentContrast());
        inverterJson.addProperty("reactiveCurrentContrast", inverterVO.getReactiveCurrentContrast());
        inverterJson.addProperty("powerFactor", inverterVO.getPowerFactor());
        inverterJson.addProperty("acCurrent", inverterVO.getAcCurrent());
        inverterJson.addProperty("gridVoltage", inverterVO.getGridVoltage());
        inverterJson.addProperty("gridFrequency", inverterVO.getGridFrequency());
        inverterJson.addProperty("gridPhaseDifference", inverterVO.getGridPhaseDifference());
        inverterJson.addProperty("stackTemp", inverterVO.getStackTemp());
        inverterJson.addProperty("inductor1Temp", inverterVO.getInductor1Temp());
        inverterJson.addProperty("inductor2Temp", inverterVO.getInductor2Temp());
        inverterJson.addProperty("capacitorTemp", inverterVO.getCapacitorTemp());

        return inverterJson;
    }

    /**
     * DC 컨버터 데이터 Json 객체 생성
     *
     * @param deviceData 장비 데이터
     * @param errors     오류 코드 목록
     * @return DC 컨버터 데이터 Json 객체
     */
    private JsonObject setDCConverterDataJson(Object deviceData, List<String> errors) {
        ConverterVO converterVO = (ConverterVO) deviceData;
        ConverterVO.DCConverterVO dcConverterVO = converterVO.getDcConverter();
        String errorFlag = setErrorFlag(dcConverterVO.getWarningFlag(), dcConverterVO.getFaultFlag());

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("operationStatus", dcConverterVO.getOperationStatus());
        bodyJson.addProperty("totalDcPower", dcConverterVO.getTotalDcPower());
        bodyJson.addProperty("totalCurrent", dcConverterVO.getTotalCurrent());
        bodyJson.addProperty("convertDcPower", dcConverterVO.getConvertDcPower());
        bodyJson.addProperty("dcCurrent", dcConverterVO.getDcCurrent());
        bodyJson.addProperty("internalTemp", dcConverterVO.getInternalTemp());

        for (ConverterVO.DCInverterVO inverterVO : converterVO.getDcInverters()) {
            if (inverterVO.getInverterNo() == 1) {
                bodyJson.add("leftInverter", setDCInverterDataJson(inverterVO));
            } else if (inverterVO.getInverterNo() == 2) {
                bodyJson.add("rightInverter", setDCInverterDataJson(inverterVO));
            }
        }

        JsonObject energyStatusJson = new JsonObject();
        energyStatusJson.addProperty("currentAccumulated", String.format("%.1f", (float) 10));   //전력량 현황 - 현재 누적 전력량
        bodyJson.add("energyStatus", energyStatusJson);   //전력량 현황

        return addErrorJson(bodyJson, errorFlag, errors);
    }

    private JsonObject setDCInverterDataJson(ConverterVO.DCInverterVO inverterVO) {
        JsonObject inverterJson = new JsonObject();
        inverterJson.addProperty("modeStatus", inverterVO.getModeStatus());
        inverterJson.addProperty("inverterStatus", inverterVO.getInverterStatus());
        inverterJson.addProperty("power", inverterVO.getPower());
        inverterJson.addProperty("current", inverterVO.getCurrent());
        inverterJson.addProperty("voltage", inverterVO.getVoltage());
        inverterJson.addProperty("dcPower", inverterVO.getDcPower());
        inverterJson.addProperty("dcCurrent", inverterVO.getDcCurrent());
        inverterJson.addProperty("activeCurrentContrast", inverterVO.getActiveCurrentContrast());
        inverterJson.addProperty("refActiveCurrentPercentage", inverterVO.getRefActiveCurrentPercentage());
        inverterJson.addProperty("stackTemp", inverterVO.getStackTemp());
        inverterJson.addProperty("inductorTemp", inverterVO.getInductorTemp());
        inverterJson.addProperty("capacitorTemp", inverterVO.getCapacitorTemp());

        return inverterJson;
    }

    /**
     * 센서 데이터 Json 생성
     *
     * @param roomCode   장비 실 코드
     * @param deviceData 장비 데이터
     * @param errors     오류 코드 목록
     * @return 센서 데이터 Json 객체
     */
    private JsonObject setSensorDataJson(String roomCode, Object deviceData, List<String> errors) {
        SensorVO sensorVO = (SensorVO) deviceData;

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("deviceRoom", roomCode);
        bodyJson.addProperty("sensorStatus", sensorVO.getSensorStatus());
        bodyJson.addProperty("measure1", sensorVO.getMeasure1());
        bodyJson.addProperty("measure2", sensorVO.getMeasure2());
        bodyJson.addProperty("measure3", sensorVO.getMeasure3());

        String errorFlag = setErrorFlag(sensorVO.getWarningFlag(), sensorVO.getFaultFlag());

        return addErrorJson(bodyJson, errorFlag, errors);
    }

    /**
     * 전력 계전기 데이터 Json 생성
     *
     * @param deviceData 장비 데이터
     * @return 전력 계전기 데이터 Json 객체
     */
    private JsonObject setPowerRelayDataJson(Object deviceData) {
        PowerRelayVO relayVO = (PowerRelayVO) deviceData;

        float rsLineVoltage = relayVO.getRsLineVoltage();   //R-S 선간 전압
        float stLineVoltage = relayVO.getRsLineVoltage();   //S-T 선간 전압
        float trLineVoltage = relayVO.getRsLineVoltage();   //T-R 선간 전압
        float sumLineVoltage = rsLineVoltage + stLineVoltage + trLineVoltage;   //선간 전압 합계
        float averageLineVoltage = (float) (Math.round((sumLineVoltage / 3) * 10) / 10.0);  //선간 전압 평균 계산

        float rPhaseCurrent = relayVO.getRPhaseCurrent();   //R상 전류
        float sPhaseCurrent = relayVO.getSPhaseCurrent();   //S상 전류
        float tPhaseCurrent = relayVO.getTPhaseCurrent();   //T상 전류
        float sumPhaseCurrent = rPhaseCurrent + sPhaseCurrent + tPhaseCurrent;  //상 전류 합계
        float averagePhaseCurrent = (float) (Math.round((sumPhaseCurrent / 3) * 10) / 10.0);    //상 전류 평균 계산

        float rPhaseReversePower = relayVO.getRPhaseReversePower(); //R상 역전력
        float sPhaseReversePower = relayVO.getSPhaseReversePower(); //S상 역전력
        float tPhaseReversePower = relayVO.getTPhaseReversePower(); //T상 역전력
        float totalReversePower = rPhaseReversePower + sPhaseReversePower + tPhaseReversePower; //역전력 합계

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("status", relayVO.getStatus());    //통신 상태
        bodyJson.addProperty("rsLineVoltage", rsLineVoltage);   //R-S 선간 전압
        bodyJson.addProperty("stLineVoltage", stLineVoltage);   //S-T 선간 전압
        bodyJson.addProperty("trLineVoltage", trLineVoltage);   //T-R 선간 전압
        bodyJson.addProperty("averageLineVoltage", averageLineVoltage); //평균 선간 전압
        bodyJson.addProperty("rPhaseCurrent", rPhaseCurrent); //R상 전류
        bodyJson.addProperty("sPhaseCurrent", sPhaseCurrent); //S상 전류
        bodyJson.addProperty("tPhaseCurrent", tPhaseCurrent); //T상 전류
        bodyJson.addProperty("averagePhaseCurrent", averagePhaseCurrent);   //평균 상 전류
        bodyJson.addProperty("frequency", relayVO.getFrequency()); //주파수
        bodyJson.addProperty("rPhaseActivePower", relayVO.getRPhaseActivePower()); //R상 유효 전력
        bodyJson.addProperty("sPhaseActivePower", relayVO.getSPhaseActivePower()); //S상 유효 전력
        bodyJson.addProperty("tPhaseActivePower", relayVO.getTPhaseActivePower()); //T상 유효 전력
        bodyJson.addProperty("totalActivePower", relayVO.getTotalActivePower());  //총 유효 전력
        bodyJson.addProperty("rPhaseReversePower", rPhaseReversePower);    //R상 역전력
        bodyJson.addProperty("sPhaseReversePower", sPhaseReversePower);    //S상 역전력
        bodyJson.addProperty("tPhaseReversePower", tPhaseReversePower);    //T상 역전력
        bodyJson.addProperty("totalReversePower", totalReversePower);   //총 역전력
        bodyJson.addProperty("overVoltageRelayAction", relayVO.getStatus());    //과전압 릴레이 동작
        bodyJson.addProperty("underVoltageRelayAction", relayVO.getStatus());   //저전압 릴레이 동작
        bodyJson.addProperty("overFrequencyRelayAction", relayVO.getStatus());  //과주파수 릴레이 동작
        bodyJson.addProperty("underFrequencyRelayAction", relayVO.getStatus()); //저주파수 릴레이 동작
        bodyJson.addProperty("reversePowerRelayAction", relayVO.getStatus());   //역전력 릴레이 동작

        return bodyJson;
    }

    /**
     * 전력 계측기 데이터 Json 생성
     *
     * @param meterNo    전력 계측기 번호
     * @param deviceData 장비 데이터
     * @return 전력 계측기 데이터 Json 객체
     */
    private JsonObject setPowerMeterDataJson(int meterNo, Object deviceData) {
        PowerMeterVO meterVO = (PowerMeterVO) deviceData;

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("meterCode", meterVO.getMeterCode()); //계측기 코드
        bodyJson.addProperty("meterNo", meterNo);   //계전기 번호
        bodyJson.addProperty("status", meterVO.getStatus());    //통신 상태
        bodyJson.addProperty("rsLineVoltage", meterVO.getRsLineVoltage());  //R-S 선간 전압
        bodyJson.addProperty("stLineVoltage", meterVO.getStLineVoltage());  //S-T 선간 전압
        bodyJson.addProperty("trLineVoltage", meterVO.getTrLineVoltage());  //T-R 선간 전압
        bodyJson.addProperty("averageLineVoltage", meterVO.getAverageLineVoltage());    //평균 선간 전압
        bodyJson.addProperty("rPhaseCurrent", meterVO.getRPhaseCurrent());  //R상 전류
        bodyJson.addProperty("sPhaseCurrent", meterVO.getSPhaseCurrent());  //S상 전류
        bodyJson.addProperty("tPhaseCurrent", meterVO.getTPhaseCurrent());  //T상 전류
        bodyJson.addProperty("nPhaseCurrent", meterVO.getNPhaseCurrent());  //N상 전류(중성선)
        bodyJson.addProperty("gPhaseCurrent", meterVO.getGPhaseCurrent());  //G상 전류(접지선)
        bodyJson.addProperty("averagePhaseCurrent", meterVO.getAveragePhaseCurrent());  //평균 상 전류
        bodyJson.addProperty("frequency", meterVO.getFrequency());  //주파수
        bodyJson.addProperty("rPhaseActivePower", meterVO.getRPhaseActivePower());  //R상 유효 전력
        bodyJson.addProperty("sPhaseActivePower", meterVO.getSPhaseActivePower());  //S상 유효 전력
        bodyJson.addProperty("tPhaseActivePower", meterVO.getTPhaseActivePower());  //T상 유효 전력
        bodyJson.addProperty("totalActivePower", meterVO.getTotalActivePower());    //총 유효 전력

        return bodyJson;
    }

    /**
     * 공조 장치 데이터 Json 객체 생성
     *
     * @param roomCode   장비 실 코드
     * @param deviceData 장비 데이터
     * @param errors     오류 코드 목록
     * @return 공조 장치 데이터 Json 객체
     */
    private JsonObject setAirConditionerDataJson(String roomCode, Object deviceData, List<String> errors) {
        AirConditionerVO airConditionerVO = (AirConditionerVO) deviceData;

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("deviceRoom", roomCode);
        bodyJson.addProperty("operationStatus", airConditionerVO.getOperationStatus());

        if (PmsVO.ess.getEssType().equals("01")) {  //고정형
            bodyJson.addProperty("operationModeStatus", airConditionerVO.getOperationModeStatus());
            bodyJson.addProperty("setTemperature", airConditionerVO.getSetTemperature());
        }

        String errorFlag = setErrorFlag(airConditionerVO.getWarningFlag(), airConditionerVO.getFaultFlag());

        return addErrorJson(bodyJson, errorFlag, errors);
    }
}
