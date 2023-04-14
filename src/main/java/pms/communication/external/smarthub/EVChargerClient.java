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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pms.communication.CommunicationManager.deviceProperties;

public class EVChargerClient {
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final String host = deviceProperties.getProperty("charger.api.host");
    private final String path = deviceProperties.getProperty("charger.api.path");
    private final String stationId = deviceProperties.getProperty("charger.station.id");
    private final int chargerCount = Integer.parseInt(deviceProperties.getProperty("charger.count"));
    private String response;
    private static String searchDateTime = null;
    private List<EVChargerVO> evChargers = new ArrayList<>();
    private static String evChargerRequest = null;

    private static final Map<String, List<EVChargerVO>> evChargerRequestMap = new HashMap<>();

    public void removeEVChargerRequest() {
        evChargerRequest = null;
    }

    public String getEVChargerRequest() {
        return evChargerRequest;
    }

    public void setEvChargerRequest(String controlType, JsonObject jsonObject) {
        if (controlType.equals("chargingStart")) {
            //List<EVChargerVO> requestChargers = setChargers(chargers, "Y"); //충전 시작 EV 충전기
            evChargers = setEVChargers("1", jsonObject);
            List<EVChargerVO> requestChargers = getEVChargers("charger-start"); //충전 시작 EV 충전기
            int requestCount = requestChargers.size();

            if (requestCount == chargerCount) {
                evChargerRequest = "allCharging";
            } else if (requestCount < chargerCount) {
                evChargerRequest = "charging";
            }
        } else if (controlType.equals("chargingEnd")) {
            //List<EVChargerVO> requestChargers = setChargers(chargers, "N"); //충전 종료 EV 충전기
            evChargers = setEVChargers("2", jsonObject);
            List<EVChargerVO> requestChargers = getEVChargers("charger-end");   //충전 종료 EV 충전기
            int requestCount = requestChargers.size();

            if (requestCount == chargerCount) {
                evChargerRequest = "standby";
            } else if (requestCount < chargerCount) {
                evChargerRequest = "charging";
            }
        }
    }

    public int getChargerCount() {
        return chargerCount;
    }

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

    private List<EVChargerVO> setEVChargers(String requestType, JsonObject jsonObject) {
        JsonObject dataObject = jsonObject.get("data").getAsJsonObject();
        JsonArray chargerArray = dataObject.get("chargerList").getAsJsonArray();

        List<EVChargerVO> evChargers = new ArrayList<>();
        int requestDate = DateTimeUtil.getUnixTimestamp();

        for (JsonElement chargerElement : chargerArray) {
            JsonObject chargerObject = chargerElement.getAsJsonObject();

            String chargerId = chargerObject.get("chargerId").getAsString();
            String status = chargerObject.get("chargerStatus").getAsString();
            float voltage = chargerObject.get("voltage").getAsFloat();
            float current = chargerObject.get("current").getAsFloat();
            String startDate = chargerObject.get("startTime").getAsString();
            String endDate = chargerObject.get("endTime").getAsString();

            if (startDate.equals("")) {
                startDate = null;
            }
            if (endDate.equals("")) {
                endDate = null;
            }

            EVChargerVO evChargerVO = new EVChargerVO();
            evChargerVO.setRequestDate(requestDate);
            evChargerVO.setRequestType(requestType);
            evChargerVO.setChargerId(chargerId);
            evChargerVO.setStatus(status);
            evChargerVO.setVoltage(voltage);
            evChargerVO.setCurrent(current);
            evChargerVO.setStartDate(startDate);
            evChargerVO.setEndDate(endDate);
            evChargerVO.setOriginalJson(jsonObject.toString());

            evChargers.add(evChargerVO);
        }

        new EVChargerQuery().insertEVChargerHistory(evChargers);

        return evChargers;
    }

    public List<EVChargerVO> getEVChargers(String checkType) {
        List<EVChargerVO> chargers = evChargers;

        if (checkType != null) {
            switch (checkType) {
                case "ess-charge":  //ESS 충전
                    chargers.removeIf(
                            evChargerVO -> !(evChargerVO.getStatus().equals("0") || evChargerVO.getStatus().equals("3") || evChargerVO.getStatus().equals("5"))
                    );  //0: 대기, 3: 종료, 5: 사용 불가
                    break;
                case "ess-discharge":   //ESS 방전
                    chargers.removeIf(
                            evChargerVO -> !(evChargerVO.getStatus().equals("2") || evChargerVO.getStatus().equals("4"))
                    );  //2: 진행, 4: 진행 오류
                    break;
                case "charger-start":   //EV 충전기 충전 시작
                    chargers.removeIf(
                            evChargerVO -> !(evChargerVO.getStatus().equals("1") || evChargerVO.getStatus().equals("2"))
                    );  //1: 시작, 2: 진행
                    break;
                case "charger-end": //EV 충전기 충전 종료
                    chargers.removeIf(
                            //evChargerVO -> (evChargerVO.getStatus().equals("1") || evChargerVO.getStatus().equals("2") || evChargerVO.getStatus().equals("4"))
                            evChargerVO -> !(evChargerVO.getStatus().equals("0") || evChargerVO.getStatus().equals("3") || evChargerVO.getStatus().equals("4") || evChargerVO.getStatus().equals("5"))
                    );  //0: 대기, 3: 종료, 4: 진행 오류, 5: 사용 불가
                    break;
            }
        }

        return chargers;
    }

    private JsonObject getResponse() {
        JsonElement jsonElement = JsonParser.parseString(response);

        return jsonElement.getAsJsonObject();
    }

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

    private JsonArray setChargersJson() {
        JsonArray chargers = new JsonArray();

        for (int i = 1; i <= chargerCount; i++) {
            String chargerId = stationId + String.format("%02d", i);

            JsonObject chargerObject = new JsonObject();
            chargerObject.addProperty("chargerId", chargerId);

            chargers.add(chargerObject);
        }

        return chargers;
    }
}
