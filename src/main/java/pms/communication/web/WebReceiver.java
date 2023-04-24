package pms.communication.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pms.communication.device.bms.BMSClient;
import pms.communication.device.pcs.PCSClient;
import pms.communication.external.smarthub.EVChargerClient;
import pms.system.ess.ControlUtil;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.util.List;

import static pms.communication.CommunicationManager.deviceProperties;

public class WebReceiver extends WebClient {
    private final String ESS_TYPE = PmsVO.ess.getEssType();

    public void receive(String json) {
        JsonElement jsonElement = JsonParser.parseString(json);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (jsonObject != null) {
            String id = jsonObject.get("id").getAsString();
            String eventType = jsonObject.get("eventType").getAsString();
            //System.out.println(jsonObject);
            if (!id.equals(deviceProperties.getProperty("charger.station.id"))) {
                if (eventType.equals("req")) {
                    String dataType = jsonObject.get("dataType").getAsString();

                    if (dataType.equals("control")) {
                        requestControl(jsonObject);
                    }
                }
            } else {
                //EV 충전기 메세지
                System.out.println(jsonObject);
                System.out.println("[EV 충전기] 제어 요청");
                requestEVCharger(jsonObject);
            }
        }
    }

    private boolean checkHeader(JsonObject jsonObject) {
        boolean isCheck = false;

        String deviceCategorySub = jsonObject.get("deviceCategorySub").getAsString();
        String deviceCode = jsonObject.get("deviceCode").getAsString();

        switch (deviceCategorySub) {
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
            /*case "0301":
                break;
            case "0302":
                break;
            case "8001":
                break;*/
            case "8002":
                List<DeviceVO> airConditioners = PmsVO.airConditioners.get(deviceCategorySub);

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
        boolean isCheck = false;

        String targetId = dataObject.get("targetId").getAsString();
        String controlCode = dataObject.get("controlCode").getAsString();

        if (targetId.equals(MIDDLEWARE_ID)) {
            if (ESS_TYPE.equals("01")) {
                switch (controlCode) {
                    case "0101010100":
                    case "0101010101":
                    case "0101010102":
                    case "0101020100":
                    case "0101020101":
                    case "0101020102":
                    case "0200010200":
                    case "0200010201":
                    case "0200010202":
                    case "0200010203":
                    case "0200010204":
                    case "0200010205":
                    case "0200010206":
                    case "9001019000":
                    case "9001019001":
                    case "9001019002":
                    case "9001019098":
                    case "9001019099":
                        isCheck = true;
                        break;
                }
            } else if (ESS_TYPE.equals("02")) {
                switch (controlCode) {
                    case "0101010100":
                    case "0101010101":
                    case "0101010102":
                    case "0101020100":
                    case "0101020101":
                    case "0101020102":
                    case "8002018098":
                    case "8002018099":
                    case "8002028098":
                    case "8002028099":
                    case "9001019000":
                    case "9001019001":
                    case "9001019002":
                    case "9001019098":
                    case "9001019099":
                        isCheck = true;
                        break;
                }
            }
        }

        return isCheck;
    }

    private void requestControl(JsonObject jsonObject) {
        String remoteId = jsonObject.get("id").getAsString();

        if (checkHeader(jsonObject)) {
            JsonObject dataObject = jsonObject.get("data").getAsJsonObject();

            if (checkControlData(dataObject)) {
                String deviceCategory = jsonObject.get("deviceCategory").getAsString();
                String deviceCategorySub = jsonObject.get("deviceCategorySub").getAsString();
                String deviceCode = jsonObject.get("deviceCode").getAsString();
                String controlCode = dataObject.get("controlCode").getAsString();

                ControlRequestVO requestVO = ControlUtil.setRemoteControlRequestVO(remoteId, "02", deviceCategorySub, controlCode);

                if (requestVO != null) {
                    switch (deviceCategorySub) {
                        case "0101":
                            requestRack(deviceCode, requestVO);
                            break;
                        case "0200":
                            requestPCS(requestVO);
                            break;
                        /*case "0301":
                            break;
                        case "0302":
                            break;
                        case "8001":
                            break;*/
                        case "8002":
                            requestAirConditioner(requestVO);
                            break;
                        case "9001":
                            break;
                    }
                } else {
                    new WebSender().sendResponse(remoteId, "control", 0);   //제어 불가 - 추후 변경 예정
                }
            } else {
                new WebSender().sendResponse(remoteId, "control", 2);   //데이터 오류
            }
        } else {
            new WebSender().sendResponse(remoteId, "control", 3);   //필수 값 누락
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

    private void requestAirConditioner(ControlRequestVO requestVO) {
        //ESS 유형 별 공조장치 제어(01: 이동형, 02: 고정형)
        if (ESS_TYPE.equals("01")) {

        } else if (ESS_TYPE.equals("02")) {

        }
    }

    private void requestMiddleware(ControlRequestVO requestVO) {

    }

    private void requestEVCharger(JsonObject jsonObject) {
        JsonObject dataObject = jsonObject.get("data").getAsJsonObject();
        String controlType = dataObject.get("controlType").getAsString();

        EVChargerClient evChargerClient = new EVChargerClient();
        evChargerClient.setEvChargerRequest(controlType, jsonObject);

        System.out.println("[EV 충전기] 제어 요청 생성");
    }
}
