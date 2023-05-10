package pms.communication.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pms.system.ess.ESSManager;
import pms.vo.device.AirConditionerVO;
import pms.vo.device.BmsVO;
import pms.vo.device.PcsVO;
import pms.vo.device.SensorVO;
import pms.vo.system.DeviceVO;

import java.util.List;

public class WebSender extends WebClient {

    public void sendConnect() {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("id", getMiddlewareId());
        jsonObject.addProperty("eventType", "req");
        jsonObject.addProperty("deviceType", "M/W");
        jsonObject.addProperty("dataType", "connect");

        String json = gson.toJson(jsonObject);
        System.out.println("PMS Websocket connected. - " + json);

        webSocketClient.send(json);
    }

    public void sendData(DeviceVO deviceVO, Object deviceData, List<String> errors) {
        JsonObject jsonObject = setHeaderJson(deviceVO);
        JsonObject bodyJson = new JsonObject();
        String category = deviceVO.getDeviceCategory();

        switch (category) {
            case "01":
                bodyJson = setRackDataJson(deviceData, errors);
                break;
            case "02":
                bodyJson = setPCSDataJson(deviceData, errors);
                break;
            case "04":
                bodyJson = setSensorDataJson(deviceVO.getDeviceRoom(), deviceData, errors);
                break;
            case "80":
                bodyJson = setAirConditionerDataJson(deviceVO.getDeviceRoom(), deviceData, errors);
                break;
        }
        jsonObject.add("data", bodyJson);
        String json = getJson(jsonObject);

        webSocketClient.send(json);
    }

    public void sendResponse(String id, String responseType, int responseResult) {
        String result = null;  //제어 결과 (0, 2, 3: fail, 1: success)
        String message = "";

        switch (responseResult) {
            case 0:
                result = "fail";
                message = "기타 오류";
                break;
            case 1:
                result = "success";
                break;
            case 2:
                result = "fail";
                message = "데이터 오류";
                break;
            case 3:
                result = "fail";
                message = "필수 값 누락";
                break;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("eventType", "res");
        jsonObject.addProperty("dataType", responseType);
        jsonObject.addProperty("result", result);
        jsonObject.addProperty("message", message);

        String json = getJson(jsonObject);
        webSocketClient.send(json);
    }

    private String getJson(JsonObject jsonObject) {
        Gson gson = new Gson();

        return gson.toJson(jsonObject);
    }

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

    private JsonArray setErrorJsonArray(List<String> errors) {
        JsonArray errorArray = new JsonArray();

        for (String errorCode : errors) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorCode", errorCode);

            errorArray.add(errorObject);
        }

        return errorArray;
    }

    private String setErrorFlag(String warningFlag, String faultFlag) {
        String errorFlag = "N";

        if (warningFlag.equals("Y") || faultFlag.equals("Y")) {
            errorFlag = "Y";
        }

        return errorFlag;
    }

    private JsonObject addErrorJson(JsonObject bodyJson, String warningFlag, String faultFlag, String errorFlag, List<String> errors) {
        bodyJson.addProperty("warningFlag", warningFlag);
        bodyJson.addProperty("faultFlag", faultFlag);
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

    private JsonObject setRackDataJson(Object deviceData, List<String> errors) {
        BmsVO.RackVO rackVO = (BmsVO.RackVO) deviceData;

        float sumCurrent = rackVO.getCurrentSensor1() + rackVO.getCurrentSensor2();
        float averageCurrent = (float) (Math.round((sumCurrent / 2) * 10) / 10.0);

        String errorFlag = setErrorFlag(rackVO.getWarningFlag(), rackVO.getFaultFlag());

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("rackCode", rackVO.getRackCode());
        bodyJson.addProperty("operationStatus", rackVO.getOperationStatus());
        bodyJson.addProperty("operationModeStatus", rackVO.getOperationModeStatus());
        bodyJson.addProperty("soc", rackVO.getUserSoc());
        bodyJson.addProperty("voltage", String.format("%.2f", rackVO.getVoltage()));
        bodyJson.addProperty("currentSensor1", rackVO.getCurrentSensor1());
        bodyJson.addProperty("currentSensor2", rackVO.getCurrentSensor2());
        bodyJson.addProperty("averageCurrent", String.format("%.1f", averageCurrent));
        bodyJson.addProperty("chargeCurrentLimit", rackVO.getChargeCurrentLimit());
        bodyJson.addProperty("chargePowerLimit", rackVO.getChargePowerLimit());
        bodyJson.addProperty("dischargeCurrentLimit", rackVO.getDischargeCurrentLimit());
        bodyJson.addProperty("dischargePowerLimit", rackVO.getDischargePowerLimit());
        bodyJson.addProperty("positiveVoltageResistance", rackVO.getPositiveVoltageResistance());
        bodyJson.addProperty("negativeVoltageResistance", rackVO.getNegativeVoltageResistance());
        bodyJson.addProperty("positiveMainRelayAction", rackVO.getPositiveMainRelayAction());
        bodyJson.addProperty("positiveMainRelayContact", rackVO.getPositiveMainRelayContact());
        bodyJson.addProperty("negativeMainRelayAction", rackVO.getNegativeMainRelayAction());
        bodyJson.addProperty("negativeMainRelayContact", rackVO.getNegativeMainRelayContact());
        bodyJson.addProperty("emergencyRelayAction", rackVO.getEmergencyRelayAction());
        bodyJson.addProperty("emergencyRelayContact", rackVO.getEmergencyRelayContact());
        bodyJson.addProperty("prechargeRelayAction", rackVO.getPrechargeRelayAction());

        return addErrorJson(bodyJson, rackVO.getWarningFlag(), rackVO.getFaultFlag(), errorFlag, errors);
    }

    private JsonObject setPCSDataJson(Object deviceData, List<String> errors) {
        PcsVO pcsVO = (PcsVO) deviceData;

        //평균 선간 전압
        float sumLineVoltage = pcsVO.getRsLineVoltage() + pcsVO.getStLineVoltage() + pcsVO.getTrLineVoltage();
        float averageLineVoltage = (float) (Math.round((sumLineVoltage / 3) * 10) / 10.0);

        //평균 상 전압
        float sumPhaseCurrent = pcsVO.getRPhaseCurrent() + pcsVO.getSPhaseCurrent() + pcsVO.getTPhaseCurrent();
        float averagePhaseCurrent = (float) (Math.round((sumPhaseCurrent / 3) * 10) / 10.0);

        //총 누적 전력량 및 실시간 누적 젼력량
        ESSManager essManager = new ESSManager();
        String operationMode = pcsVO.getOperationModeStatus();
        float referenceEnergy = essManager.getReferenceEnergy();
        float currentAccumulatedEnergy = 0;

        switch (operationMode) {
            case "0":
                currentAccumulatedEnergy = 0;
                break;
            case "1":
                currentAccumulatedEnergy = pcsVO.getAccumulatedChargeEnergy() - referenceEnergy;
                break;
            case "2":
                currentAccumulatedEnergy = pcsVO.getAccumulatedDischargeEnergy() - referenceEnergy;
                break;
        }

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("operationStatus", pcsVO.getOperationStatus());
        bodyJson.addProperty("operationModeStatus", pcsVO.getOperationModeStatus());
        bodyJson.addProperty("outputPower", String.format("%.1f", pcsVO.getOutputPower()));
        bodyJson.addProperty("rsLineVoltage", String.format("%.1f", pcsVO.getRsLineVoltage()));
        bodyJson.addProperty("stLineVoltage", String.format("%.1f", pcsVO.getStLineVoltage()));
        bodyJson.addProperty("trLineVoltage", String.format("%.1f", pcsVO.getTrLineVoltage()));
        bodyJson.addProperty("averageLineVoltage", String.format("%.1f", averageLineVoltage));
        bodyJson.addProperty("rPhaseCurrent", String.format("%.1f", pcsVO.getRPhaseCurrent()));
        bodyJson.addProperty("sPhaseCurrent", String.format("%.1f", pcsVO.getSPhaseCurrent()));
        bodyJson.addProperty("tPhaseCurrent", String.format("%.1f", pcsVO.getTPhaseCurrent()));
        bodyJson.addProperty("averagePhaseCurrent", String.format("%.1f", averagePhaseCurrent));
        bodyJson.addProperty("frequency", String.format("%.1f", pcsVO.getFrequency()));
        bodyJson.addProperty("dcLinkVoltage", String.format("%.1f", pcsVO.getDcLinkVoltage()));
        bodyJson.addProperty("batteryVoltage", String.format("%.1f", pcsVO.getBatteryVoltage()));
        bodyJson.addProperty("batteryCurrent", String.format("%.1f", pcsVO.getBatteryCurrent()));
        bodyJson.addProperty("igbtTemperature1", pcsVO.getIgbtTemperature1());
        bodyJson.addProperty("igbtTemperature2", pcsVO.getIgbtTemperature2());
        bodyJson.addProperty("igbtTemperature3", pcsVO.getIgbtTemperature3());
        bodyJson.addProperty("acMainMcStatus", pcsVO.getAcMainMcStatus());
        bodyJson.addProperty("dcMainMcStatus", pcsVO.getDcMainMcStatus());
        bodyJson.addProperty("emergencyStopFlag", pcsVO.getEmergencyStopFlag());

        JsonObject totalAccumulatedJson = new JsonObject();
        totalAccumulatedJson.addProperty("charge", essManager.getTotalCharge());   //총 누적 전력량 - 총 누적 충전량
        totalAccumulatedJson.addProperty("discharge", essManager.getTotalDischarge());    //총 누적 전력량 - 총 누적 방전량

        JsonObject energyStatusJson = new JsonObject();
        energyStatusJson.add("totalAccumulated", totalAccumulatedJson); //전력량 현황 - 총 누적 전력량
        energyStatusJson.addProperty("currentAccumulated", String.format("%.1f", currentAccumulatedEnergy));   //전력량 현황 - 현재 누적 전력량

        bodyJson.add("energyStatus", energyStatusJson);   //전력량 현황

        String errorFlag = setErrorFlag(pcsVO.getWarningFlag(), pcsVO.getFaultFlag());

        return addErrorJson(bodyJson, pcsVO.getWarningFlag(), pcsVO.getFaultFlag(), errorFlag, errors);
    }

    private JsonObject setSensorDataJson(String roomNum, Object deviceData, List<String> errors) {
        SensorVO sensorVO = (SensorVO) deviceData;

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("deviceRoom", roomNum);
        bodyJson.addProperty("sensorStatus", sensorVO.getSensorStatus());
        bodyJson.addProperty("measure1", sensorVO.getMeasure1());
        bodyJson.addProperty("measure2", sensorVO.getMeasure2());
        bodyJson.addProperty("measure3", sensorVO.getMeasure3());

        String errorFlag = setErrorFlag(sensorVO.getWarningFlag(), sensorVO.getFaultFlag());

        return addErrorJson(bodyJson, sensorVO.getWarningFlag(), sensorVO.getFaultFlag(), errorFlag, errors);
    }

    private JsonObject setAirConditionerDataJson(String roomNum, Object deviceData, List<String> errors) {
        AirConditionerVO airConditionerVO = (AirConditionerVO) deviceData;

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("deviceRoom", roomNum);
        bodyJson.addProperty("operationStatus", airConditionerVO.getOperationStatus());
//        bodyJson.addProperty("operationModeStatus", airConditionerVO.getOperationModeStatus());
//        bodyJson.addProperty("indoorTemperature", airConditionerVO.getIndoorTemperature());

        String errorFlag = setErrorFlag(airConditionerVO.getWarningFlag(), airConditionerVO.getFaultFlag());


        return addErrorJson(bodyJson, airConditionerVO.getWarningFlag(), airConditionerVO.getFaultFlag(), errorFlag, errors);
    }
}
