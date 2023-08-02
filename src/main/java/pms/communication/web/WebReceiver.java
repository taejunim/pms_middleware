package pms.communication.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pms.communication.device.bms.BMSClient;
import pms.communication.device.mobile.converter.ConverterClient;
import pms.communication.device.mobile.ioboard.IOBoardClient;
import pms.communication.device.pcs.PCSClient;
import pms.communication.external.smarthub.EVChargerClientNew;
import pms.system.ess.ControlUtil;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.util.ArrayList;
import java.util.List;

import static pms.communication.CommunicationManager.deviceProperties;

public class WebReceiver extends WebClient {
    private final String ESS_TYPE = PmsVO.ess.getEssType();
    private final String STATION_ID = deviceProperties.getProperty("charger.station.id");

    public void receive(String json) {
        JsonElement jsonElement = JsonParser.parseString(json);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (jsonObject != null) {
            String id = jsonObject.get("id").getAsString();
            String eventType = jsonObject.get("eventType").getAsString();

            //Json 데이터 ID와 충전기 스테이션 ID와 동일하면 충전기 제어 요
            if (id.equals(STATION_ID)) {
                //EV 충전기 메세지
                System.out.println(jsonObject);
                System.out.println("[EV 충전기] 제어 요청");
                requestEVCharger(jsonObject);
            } else {
                if (eventType.equals("req")) {
                    String dataType = jsonObject.get("dataType").getAsString();

                    if (dataType.equals("control")) {
                        requestControl(jsonObject);
                    }
                }
            }
        }
    }

    private boolean checkHeader(JsonObject jsonObject) {
        boolean isCheck = false;

        String categorySub = jsonObject.get("deviceCategorySub").getAsString();
        String deviceCode = jsonObject.get("deviceCode").getAsString();

        switch (categorySub) {
            case "0101":
                for (DeviceVO rack : PmsVO.racks) {
                    if (rack.getDeviceCode().equals(deviceCode)) {
                        isCheck = true;
                        break;
                    }
                }
                break;
            case "0200":
                if (PmsVO.pcs.getDeviceCode().equals(deviceCode)) {
                    isCheck = true;
                }
                break;
            case "0301":
            case "0302":
                DeviceVO deviceVO = PmsVO.converters.get(categorySub);

                if (deviceVO.getDeviceCode().equals(deviceCode)) {
                    isCheck = true;
                }
                break;
            /*case "8001":
                break;*/
            case "8002":
                List<DeviceVO> airConditioners = PmsVO.airConditioners.get(categorySub);

                for (DeviceVO airConditioner : airConditioners) {
                    if (airConditioner.getDeviceCode().equals(deviceCode)) {
                        isCheck = true;
                        break;
                    }
                }
                break;
            case "9001":
                if (PmsVO.middleware.getDeviceCode().equals(deviceCode)) {
                    isCheck = true;
                }
                break;
        }

        return isCheck;
    }

    private boolean checkControlData(JsonObject dataObject) {
        String targetId = dataObject.get("targetId").getAsString();
        String controlCode = dataObject.get("controlCode").getAsString();

        if (targetId.equals(MIDDLEWARE_ID)) {
            for (Object codeKey : PmsVO.controlCodes.keySet()) {
                if (controlCode.equals(codeKey)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void requestControl(JsonObject jsonObject) {
        String remoteId = jsonObject.get("id").getAsString();

        if (checkHeader(jsonObject)) {
            JsonObject dataObject = jsonObject.get("data").getAsJsonObject();
            String deviceCategory = jsonObject.get("deviceCategory").getAsString();
            String deviceCategorySub = jsonObject.get("deviceCategorySub").getAsString();
            String deviceCode = jsonObject.get("deviceCode").getAsString();

            if (checkControlData(dataObject)) {
                System.out.println(jsonObject);
                System.out.println("==============================================");
                String controlCode = dataObject.get("controlCode").getAsString();
                String controlValue = dataObject.get("controlValue").getAsString();
                String controllerId = dataObject.get("controllerId").getAsString();

                ControlRequestVO requestVO = ControlUtil.setRemoteControlRequestVO(remoteId, "02", deviceCategory, controlCode, controlValue, controllerId);

                if (requestVO != null) {
                    switch (deviceCategorySub) {
                        case "0101":
                            requestRack(deviceCode, requestVO);
                            break;
                        case "0200":
                            requestPCS(requestVO);
                            break;
                        case "0301":
                        case "0302":
                            requestConverter(requestVO);
                            break;
                        /*case "8001":
                            break;*/
                        case "8002":
                            requestAirConditioner(requestVO);
                            break;
                        case "9001":
                            break;
                    }
                } else {
                    new WebSender().sendResponse(remoteId, deviceCode, controlCode, 0, ""); //제어 불가 - 추후 변경 예정
                }
            } else {
                new WebSender().sendResponse(remoteId, deviceCode, "", 2, "");  //데이터 오류
            }
        } else {
            new WebSender().sendResponse(remoteId, "", "", 3, "");   //필수 값 누락
        }
    }

    private void requestRack(String rackCode, ControlRequestVO requestVO) {
        BMSClient bmsClient = new BMSClient();
        bmsClient.setControlRequestMap(rackCode, requestVO);
    }

    private void requestPCS(ControlRequestVO requestVO) {
        PCSClient pcsClient = new PCSClient();
        pcsClient.setControlRequest(requestVO);
    }

    /**
     * 수정 필요
     *
     * @param requestVO
     */
    private void requestConverter(ControlRequestVO requestVO) {
        ConverterClient converterClient = new ConverterClient();
        String controlCode = requestVO.getControlCode();

        //초기화 시작(0301010303), 비상정지(0301010306) 제어 시
        if (controlCode.equals("0301010303") || controlCode.equals("0301010306")) {
            List<ControlRequestVO> controlRequests = new ArrayList<>();
            ControlRequestVO addRequestVO;

            if (controlCode.equals("0301010303")) {
                addRequestVO = ControlUtil.setControlRequestVO("04", "0401", "03", "0301010304", null, null);
            } else {
                addRequestVO = ControlUtil.setControlRequestVO("04", "0499", "03", "0301010308", null, null);
            }

            controlRequests.add(requestVO);
            controlRequests.add(addRequestVO);

            for (ControlRequestVO controlRequestVO : controlRequests) {
                converterClient.setControlRequest(controlRequestVO);
            }
        } else {
            converterClient.setControlRequest(requestVO);
        }
    }

    private void requestAirConditioner(ControlRequestVO requestVO) {
        //ESS 유형 별 공조장치 제어(01: 이동형, 02: 고정형)
        if (ESS_TYPE.equals("01")) {

        } else if (ESS_TYPE.equals("02")) {
            IOBoardClient ioBoardClient = new IOBoardClient();
            ioBoardClient.setControlRequest(requestVO);
        }
    }

    private void requestMiddleware(ControlRequestVO requestVO) {

    }

    private void requestEVCharger(JsonObject jsonObject) {
        JsonObject dataObject = jsonObject.get("data").getAsJsonObject();
        String controlType = dataObject.get("controlType").getAsString();

        EVChargerClientNew evChargerClientNew = new EVChargerClientNew();
        evChargerClientNew.setControlRequest(controlType, jsonObject);

        System.out.println("[EV 충전기] 제어 요청 생성");
    }
}