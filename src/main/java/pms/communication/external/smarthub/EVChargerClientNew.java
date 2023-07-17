package pms.communication.external.smarthub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import pms.common.util.DateTimeUtil;
import pms.database.query.EVChargerQuery;
import pms.vo.device.external.EVChargerVO;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static pms.communication.CommunicationManager.deviceProperties;

public class EVChargerClientNew {
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final String host = deviceProperties.getProperty("charger.api.host");   //EV 충전기 API Host
    private final String path = deviceProperties.getProperty("charger.api.path");   //EV 충전기 API Path
    private final String stationId = deviceProperties.getProperty("charger.station.id");    //스테이션 ID(스마트 허브)
    private final int totalChargerCount = Integer.parseInt(deviceProperties.getProperty("charger.count"));   //총 EV 충전기 개수
    private String response;    //EV 충전기 API 응답 데이터
    private static String searchDateTime = null;    //EV 충전기 상태 조회 일시
    private static String requestDateTime = null;
    private List<EVChargerVO> evChargers = new ArrayList<>();   //EV 충전기 정보 목록
    private static String controlRequest = null;    //EV 충전기의 제어 요청

    /**
     * 현재 스테이션(스마트 허브)의 총 EV 충전기 개수
     *
     * @return
     */
    public int getTotalChargerCount() {
        return totalChargerCount;
    }

    /**
     * EV 충전기 상태 데이터 요청
     */
    public void request() {
        HttpPost requestPost = setRequestPost();
        HttpEntity requestEntity = setRequestEntity();

        requestPost.setEntity(requestEntity);

        try {
            CloseableHttpResponse httpResponse = httpClient.execute(requestPost);
            HttpEntity responseEntity = httpResponse.getEntity();
            response = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            searchDateTime = DateTimeUtil.getCurrentTimestamp();

            JsonObject responseJson = getResponse();
            evChargers = setEVChargers("0", responseJson);
        }
    }

    /**
     * 요청 Http Post 생성
     *
     * @return HttpPost
     */
    private HttpPost setRequestPost() {
        String uri = host + path;
        HttpPost requestPost = new HttpPost(uri);
        requestPost.setHeader("Content-Type", "application/json");

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(3 * 1000)
                .setConnectTimeout(3 * 1000)
                .setConnectionRequestTimeout(3 * 1000)
                .build();

        requestPost.setConfig(requestConfig);

        return requestPost;
    }

    /**
     * 요청 Http Entity 생성
     *
     * @return HttpEntity
     */
    private HttpEntity setRequestEntity() {
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("id", stationId);
        requestJson.addProperty("eventType", "req");
        requestJson.addProperty("deviceType", "charger");
        requestJson.addProperty("dataType", "status");

        JsonObject dataJson = new JsonObject();
        dataJson.addProperty("requestTime", DateTimeUtil.getCurrentTimestamp());
        JsonArray chargersJson = setChargersJson();
        dataJson.add("chargerList", chargersJson);

        requestJson.add("data", dataJson);

        try {
            return new StringEntity(requestJson.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 데이터 요청할 EV 충전기 Json 배열 생성
     *
     * @return JsonArray
     */
    private JsonArray setChargersJson() {
        JsonArray chargers = new JsonArray();

        for (int i = 1; i <= totalChargerCount; i++) {
            String chargerId = stationId + String.format("%02d", i);

            JsonObject chargerObject = new JsonObject();
            chargerObject.addProperty("chargerId", chargerId);

            chargers.add(chargerObject);
        }

        return chargers;
    }

    /**
     * 응답 데이터 Json Object 변환
     *
     * @return JsonObject
     */
    private JsonObject getResponse() {
        JsonElement jsonElement = JsonParser.parseString(response);

        return jsonElement.getAsJsonObject();
    }

    /**
     * 응답 받은 Json 객체에서 EV 충전기 정보 설정
     *
     * @param requestType   EV 충전기 요청 구분
     * @param requestDate   요청 일시
     * @param jsonObject    응답 받은 Json 객체
     * @param chargerObject Json 객체에서 추출한 EV 충전기 정보 객체
     * @return EV 충전기 정보
     */
    private EVChargerVO setEVCharger(String requestType, int requestDate, JsonObject jsonObject, JsonObject chargerObject) {
        String chargerId = chargerObject.get("chargerId").getAsString();
        String status = chargerObject.get("chargerStatus").getAsString();
        float voltage = chargerObject.get("voltage").getAsFloat();
        float current = chargerObject.get("current").getAsFloat();
        String readyDate = chargerObject.get("readyTime").getAsString();
        String startDate = chargerObject.get("startTime").getAsString();
        String endDate = chargerObject.get("endTime").getAsString();
        String cancelDate = chargerObject.get("cancelTime").getAsString();

        if (readyDate.equals("") || status.equals("0") || status.equals("5")) readyDate = null;
        if (startDate.equals("")) startDate = null;
        if (endDate.equals("")) endDate = null;
        if (cancelDate.equals("")) cancelDate = null;

        EVChargerVO evChargerVO = new EVChargerVO();
        evChargerVO.setRequestDate(requestDate);
        evChargerVO.setRequestType(requestType);
        evChargerVO.setChargerId(chargerId);
        evChargerVO.setStatus(status);
        evChargerVO.setVoltage(voltage);
        evChargerVO.setCurrent(current);
        evChargerVO.setReadyDate(readyDate);
        evChargerVO.setStartDate(startDate);
        evChargerVO.setEndDate(endDate);
        evChargerVO.setCancelDate(cancelDate);
        evChargerVO.setOriginalJson(jsonObject.toString());

        return evChargerVO;
    }

    /**
     * 응답 받은 Json 객체에서 EV 충전기 정보 목록 설정
     *
     * @param requestType EV 충전기 요청 구분
     * @param jsonObject  응답 받은 Json 객체
     * @return EV 충전기 정보 목록
     */
    private List<EVChargerVO> setEVChargers(String requestType, JsonObject jsonObject) {
        System.out.println("충전기 정보 생성 : " + requestType);
        JsonObject dataObject = jsonObject.get("data").getAsJsonObject();
        JsonArray chargerArray = dataObject.get("chargerList").getAsJsonArray();

        List<EVChargerVO> evChargers = new ArrayList<>();
        int requestDate = DateTimeUtil.getUnixTimestamp();

        for (JsonElement chargerElement : chargerArray) {
            JsonObject chargerObject = chargerElement.getAsJsonObject();
            System.out.println("충전기 객체 : " + chargerObject);
            EVChargerVO evChargerVO = setEVCharger(requestType, requestDate, jsonObject, chargerObject);
            evChargers.add(evChargerVO);
        }

        //System.out.println("EVChargerVO List : " + evChargers);

        new EVChargerQuery().insertEVChargerHistory(evChargers);  //EV 충전기 API 이력 등록

        return evChargers;
    }

    /**
     * EV 충전기 상태 별 EV 충전기 추출 후, 해당 목록 호출
     *
     * @param checkType EV 충전기 상태 확인 유형
     * @return 상태 확인 유형 별 EV 충전기 목록
     */
    public List<EVChargerVO> getEVChargers(String checkType) {
        List<EVChargerVO> chargers = evChargers;

        /* 충전기 상태(chargerStatus) - 0: 대기, 1: 시작, 2: 진행, 3: 종료, 4: 진행 오류, 5: 사용 불가, 6: 준비, 7: 취소 */
        if (checkType != null) {
            switch (checkType) {
                case "ess-charge":  //ESS 충전
                case "charger-cancel":  //EV 충전기 충전 취소
                    chargers.removeIf(
                            evChargerVO -> !(evChargerVO.getStatus().equals("0")
                                    || evChargerVO.getStatus().equals("3")
                                    || evChargerVO.getStatus().equals("5")
                                    || evChargerVO.getStatus().equals("7")
                            )
                    );  //0: 대기, 3: 종료, 5: 사용 불가, 7: 취소
                    break;
                case "ess-discharge":   //ESS 방전
                    chargers.removeIf(
                            evChargerVO -> !(evChargerVO.getStatus().equals("2")
                                    || evChargerVO.getStatus().equals("4")
                            )
                    );  //2: 진행, 4: 진행 오류
                    break;
                case "charger-ready":   //EV 충전기 충전 준비
                    chargers.removeIf(
                            evChargerVO -> !(evChargerVO.getStatus().equals("0")
                                    //|| evChargerVO.getStatus().equals("2")    //2: 충전 진행 - 임시 해제
                                    || evChargerVO.getStatus().equals("3")
                                    || evChargerVO.getStatus().equals("5")
                                    || evChargerVO.getStatus().equals("6")
                                    || evChargerVO.getStatus().equals("7")
                            )
                    );  //0: 대기, 3: 종료, 5: 사용 불가, 6: 준비, 7: 취소
                    break;
                case "charger-start":   //EV 충전기 충전 시작
                    chargers.removeIf(
                            evChargerVO -> !(evChargerVO.getStatus().equals("1")
                                    || evChargerVO.getStatus().equals("2")
                                    || evChargerVO.getStatus().equals("4")
                            )
                    );  //1: 시작, 2: 진행, 4: 진행 오류
                    break;
                case "charger-end": //EV 충전기 충전 종료
                case "charger-error":   //EV 충전기 오류
                    chargers.removeIf(
                            evChargerVO -> !(evChargerVO.getStatus().equals("0")
                                    || evChargerVO.getStatus().equals("3")
                                    || evChargerVO.getStatus().equals("4")
                                    || evChargerVO.getStatus().equals("5")
                                    || evChargerVO.getStatus().equals("7")
                            )
                    );  //0: 대기, 3: 종료, 4: 진행 오류, 5: 사용 불가, 7: 취소
                    break;
            }
        }

        return chargers;
    }

    /**
     * EV 충전기 제어 요청 호출
     *
     * @return EV 충전기 제어 요청
     */
    public String getControlRequest() {
        return controlRequest;
    }

    /**
     * EV 충전기 제어 요청 초기화
     */
    public void resetControlRequest() {
        controlRequest = null;
    }

    /**
     * 제어 유형 별 EV 충전기 제어 요청 설정
     *
     * @param controlType 제어 유형
     * @param jsonObject  Json 객체
     */
    public void setControlRequest(String controlType, JsonObject jsonObject) {
        requestDateTime = DateTimeUtil.getCurrentTimestamp();
        List<EVChargerVO> requestChargers;
        int chargerCount;
        
        System.out.println("[EV 충전기]제어 요청 구분: " + controlType);

        switch (controlType) {
            case "chargingReady":   //준비
                evChargers = setEVChargers("3", jsonObject);    //요청 구분(3: 충전기 충전 준비)
                requestChargers = getEVChargers("charger-ready");   //EV 충전기 충전 준비
                chargerCount = requestChargers.size();

                if (chargerCount == totalChargerCount) {
                    controlRequest = "ready";
                }
                break;
            case "chargingStart":   //시작
                evChargers = setEVChargers("1", jsonObject);    //요청 구분(1: 충전기 충전 시작)
                requestChargers = getEVChargers("charger-start"); //EV 충전기 충전 시작
                chargerCount = requestChargers.size();

                if (chargerCount == totalChargerCount) {
                    controlRequest = "all-charging";
                } else if (chargerCount < totalChargerCount) {
                    controlRequest = "charging";
                }
                break;
            case "chargingEnd": //종료
                evChargers = setEVChargers("2", jsonObject);    //요청 구분(2: 충전기 충전 종료)
                requestChargers = getEVChargers("charger-end");   //EV 충전기 충전 종료
                chargerCount = requestChargers.size();

                if (chargerCount == totalChargerCount) {
                    controlRequest = "end";
                } else if (chargerCount < totalChargerCount) {
                    controlRequest = "charging";
                }
                break;
            case "chargingCancel":  //취소
                evChargers = setEVChargers("4", jsonObject);    //요청 구분(4: 충전기 충전 취소)
                requestChargers = getEVChargers("charger-cancel");  //EV 충전기 충전 취소
                chargerCount = requestChargers.size();

                if (chargerCount == totalChargerCount) {
                    controlRequest = "cancel";
                }
                break;
            case "chargingError":   //오류
                evChargers = setEVChargers("5", jsonObject);    //요청 구분(5: 충전기 오류)
                requestChargers = getEVChargers("charger-error");   //EV 충전기 오류
                chargerCount = requestChargers.size();

                if (chargerCount == totalChargerCount) {
                    controlRequest = "error";
                }
                break;
        }

        System.out.println("제어 요청 : " + controlRequest);
    }
}
