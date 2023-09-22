package pms.communication.device.airconditioner;

import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import pms.common.util.DateTimeUtil;
import pms.system.PMSCode;
import pms.vo.device.AirConditionerVO;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AirConditionerReader {
    private ModbusSerialMaster connection;
    private final int unitId;
    private final String airConditionerCode;
    private Map<String, List<AirConditionerVO.RequestItem>> requestItemsMap; //수신 요청 항목 Map
    private final AirConditionerVO airConditionerVO = new AirConditionerVO();
    private final List<DeviceErrorVO> airConditionerErrors = new ArrayList<>();
    private final Map<String, List<AirConditionerVO.ResponseItem>> responseItemsMap = new HashMap<>();   //수신 응답 항목 Map


    public AirConditionerReader(DeviceVO airConditionerInfo) {
        this.unitId = 1;     //!!! 고정형 에어컨 유닛아이디 확인
        this.airConditionerCode = airConditionerInfo.getDeviceCode();
    }

    public void setRequest(ModbusSerialMaster connection, Map<String, List<AirConditionerVO.RequestItem>> requestItemsMap) {
        this.connection = connection;
        this.requestItemsMap = requestItemsMap;
    }

    public void request() {
        String reqTime = String.valueOf(DateTimeUtil.getUnixTimestamp());
        boolean isError = false;
        for (String group : requestItemsMap.keySet()) {
            List<AirConditionerVO.RequestItem> requestItems = requestItemsMap.get(group);

            ModbusRequest request = setReadRequest(group, requestItems);
            ModbusResponse response = getReadResponse(request);

            if (response != null) {
                setResponseItems(group, requestItems, response);
            } else {
                isError = true;
            }

            try {
                Thread.sleep(5);    //!!!고정형 에어컨 통신 대기 시간
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (!responseItemsMap.isEmpty() && !isError) {
            setReadData();
        } else {
            setReadDataByError("96", "01008");
        }

    }

    public void setReadDataByError(String statusCode, String errorCodeKey) {
        airConditionerVO.setAirConditionerCode(airConditionerCode);
        airConditionerVO.setRegDate(DateTimeUtil.getUnixTimestamp());
        airConditionerVO.setOperationStatus(PMSCode.getCommonCode("DEVICE_STATUS_" + statusCode));
        airConditionerVO.setFaultFlag("Y");
        airConditionerVO.setWarningFlag("N");

        String errorCode = PMSCode.getCommonErrorCode(errorCodeKey);
        setAirConditionerErrors(errorCode, null);
    }

    private void setAirConditionerErrors(String errorCode, String remarks) {
        DeviceErrorVO deviceErrorVO = new DeviceErrorVO();
        deviceErrorVO.setErrorDate(airConditionerVO.getRegDate());
        deviceErrorVO.setDeviceCode(airConditionerVO.getAirConditionerCode());

        if (errorCode != null) {    //에어컨 에러코드가 레퍼런스에 없을 경우 Remarks에 저장
            deviceErrorVO.setErrorCode(errorCode);
        } else {
            deviceErrorVO.setRemarks(remarks);
        }

        airConditionerErrors.add(deviceErrorVO);
    }

    private void setReadData() {
        airConditionerVO.setAirConditionerCode(airConditionerCode);
        airConditionerVO.setRegDate(DateTimeUtil.getUnixTimestamp());

        for (String group : responseItemsMap.keySet()) {
            List<AirConditionerVO.ResponseItem> responseItems = responseItemsMap.get(group);
            for (AirConditionerVO.ResponseItem responseItem : responseItems) {
                setResponseData(responseItem);
            }
        }
    }

    private void setResponseData(AirConditionerVO.ResponseItem responseItem) {
        String itemType = responseItem.getType();
        String itemName = responseItem.getName();

        if (itemType.equals("value")) {
            for (Field field : airConditionerVO.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();

                if (itemName.equals(fieldName)) {
                    Object fieldValue = AirConditionerReadItem.getFieldValue(responseItem, field);
                    try {
                        field.set(airConditionerVO, fieldValue);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (itemType.equals("status")) {
            switch (itemName) {
                case "operationStatus":
                    if (responseItem.getCoilStatus()) {
                        airConditionerVO.setOperationStatus(PMSCode.getCommonCode("DEVICE_STATUS_04")); //운전
                    } else {
                        airConditionerVO.setOperationStatus(PMSCode.getCommonCode("DEVICE_STATUS_03")); //정지
                    }
                    break;
                case "operationModeStatus":
                    int mode = responseItem.getMultipleRegister().toUnsignedShort();
                    if (mode == 0) {    //냉방
                        airConditionerVO.setOperationModeStatus(PMSCode.getCommonCode("AIR_CONDITIONER_MODE_01"));
                    } else if (mode == 2) { //송풍
                        airConditionerVO.setOperationModeStatus(PMSCode.getCommonCode("AIR_CONDITIONER_MODE_02"));
                    } else if (mode == 3) { //자동
                        airConditionerVO.setOperationModeStatus(PMSCode.getCommonCode("AIR_CONDITIONER_MODE_03"));
                    } else if (mode == 4) { //난방
                        airConditionerVO.setOperationModeStatus(PMSCode.getCommonCode("AIR_CONDITIONER_MODE_04"));
                    }
                    break;
                case "errorCode":
                    if (responseItem.getInputRegister().toUnsignedShort() == 0) {
                        airConditionerVO.setFaultFlag("N");
                    } else {
                        airConditionerVO.setFaultFlag("Y");

                        String errorCode = String.valueOf(responseItem.getInputRegister().toUnsignedShort());
                        setAirConditionerErrors(PMSCode.getAirConditionerCode(errorCode), errorCode);
                    }
            }
        }
    }

    private void setResponseItems(String group, List<AirConditionerVO.RequestItem> requestItems, ModbusResponse response) {
        List<AirConditionerVO.ResponseItem> responseItems = new ArrayList<>();
        int startRegister = requestItems.get(0).getRegister();

        for (AirConditionerVO.RequestItem requestItem : requestItems) {
            AirConditionerVO.ResponseItem responseItem = setResponseItem(requestItem, response, startRegister);
            responseItems.add(responseItem);
        }
        responseItemsMap.put(group, responseItems);
    }

    private AirConditionerVO.ResponseItem setResponseItem(AirConditionerVO.RequestItem requestItem, ModbusResponse response, int startRegister) {
        AirConditionerVO.ResponseItem responseItem = new AirConditionerVO.ResponseItem();

        responseItem.setFunctionType(requestItem.getFunctionType());
        responseItem.setAddress(requestItem.getAddress());
        responseItem.setRegister(requestItem.getRegister());
        responseItem.setDataType(requestItem.getDataType());
        responseItem.setScale(requestItem.getScale());
        responseItem.setType(requestItem.getType());
        responseItem.setName(requestItem.getName());

        switch (responseItem.getFunctionType()) {
            case "coils":
                responseItem.setCoilStatus(((ReadCoilsResponse) response).getCoilStatus(requestItem.getRegister() - startRegister));
                break;
            case "holdingRegisters":
                responseItem.setMultipleRegister(((ReadMultipleRegistersResponse) response).getRegister(requestItem.getRegister() - startRegister));
                break;
            case "inputRegisters":
                responseItem.setInputRegister(((ReadInputRegistersResponse) response).getRegister(requestItem.getRegister() - startRegister));
                break;
        }

        return responseItem;
    }

    private ModbusResponse getReadResponse(ModbusRequest request) {
        ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection.getConnection());
        ModbusResponse result;

        try {
            transaction.setRequest(request);
            transaction.execute();
            result = transaction.getResponse();
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }

        return result;
    }

    private ModbusRequest setReadRequest(String readType, List<AirConditionerVO.RequestItem> requestItems) {
        int reference = requestItems.get(0).getRegister();
        int wordCount = 1;

        if (requestItems.size() > 1) {
            int maxRegister = 0;

            for (AirConditionerVO.RequestItem requestItem : requestItems) {
                int register = requestItem.getRegister();

                if (maxRegister < register) {
                    maxRegister = register;
                }

                if (reference > register) {
                    reference = register;
                }
            }
            wordCount = maxRegister - reference + 1;    //!!!고정형 에어컨 주소 - 데이터 시작이 0부터인 경우

            if (wordCount >= 125) {
                if (wordCount > 125) {
                    System.out.println("[Warning]AirConditioner Read items count exceeded 125. Excess items are excluded.");
                }
            }
        }

        ModbusRequest modbusRequest = getModbusRequest(readType, reference, wordCount);
        return modbusRequest;
    }

    private ModbusRequest getModbusRequest(String readType, int reference, int wordCount) {
        ModbusRequest modbusRequest = null;
        if (readType.equals("airConditioner-items-coils")) {
            ReadCoilsRequest request = new ReadCoilsRequest();
            request.setReference(reference);
            request.setBitCount(wordCount);
            request.setUnitID(unitId);
            modbusRequest = request;
        } else if (readType.equals("airConditioner-items-holdingRegisters")) {
            ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest();
            request.setReference(reference);
            request.setWordCount(wordCount);
            request.setUnitID(unitId);
            modbusRequest = request;
        } else if (readType.equals("airConditioner-items-inputRegisters")) {
            ReadInputRegistersRequest request = new ReadInputRegistersRequest();
            request.setReference(reference);
            request.setWordCount(wordCount);
            request.setUnitID(unitId);
            modbusRequest = request;
        }
        return modbusRequest;
    }


    public AirConditionerVO getReadData() {
        return airConditionerVO;
    }

    public List<DeviceErrorVO> getErrorData() {
        return airConditionerErrors;
    }
}
