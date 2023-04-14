package pms.communication.device.bms;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import pms.common.util.DateTimeUtil;
import pms.system.PMSCode;
import pms.vo.device.BmsVO;
import pms.vo.device.error.ComponentErrorVO;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.device.error.DeviceErrorsVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.lang.reflect.Field;
import java.util.*;

/**
 * BMS Reader
 * <p>
 * - BMS 데이터 수신
 */
public class BMSReader {
    private TCPMasterConnection connection;   //통신 연결 정보
    private final String rackCode;  //Rack 코드
    private final int rackNo;   //Rack 번호
    private final List<DeviceVO.ComponentVO> modules;   //Module 정보
    private Map<String, List<BmsVO.RequestItem>> requestItemsMap; //수신 요청 항목 Map
    private final Map<String, List<BmsVO.ResponseItem>> responseItemsMap = new HashMap<>();   //수신 응답 항목 Map
    private final BmsVO.RackVO rackVO = new BmsVO.RackVO();    //Rack 데이터
    private final Map<Integer, BmsVO.ModuleVO> moduleDataMap = new HashMap<>();   //Module 데이터 Map - 임시 Module 정보 목록
    private final List<DeviceErrorVO> rackErrors = new ArrayList<>();
    private final List<ComponentErrorVO> moduleErrors = new ArrayList<>();

    public BMSReader(DeviceVO rackInfo) {
        this.rackCode = rackInfo.getDeviceCode();
        this.rackNo = rackInfo.getDeviceNo();
        this.modules = PmsVO.modules.get(rackCode);
    }

    public void setRequest(TCPMasterConnection connection, Map<String, List<BmsVO.RequestItem>> requestItemsMap) {
        this.connection = connection;
        this.requestItemsMap = requestItemsMap;
    }

    /**
     * BMS 수신 요청
     * <p>
     * - 수신 요청 및 수신 데이터 처리 실행
     */
    public void request() {
        //수신할 데이터 정보를 그룹별로 수신 요청
        for (String group : requestItemsMap.keySet()) {
            List<BmsVO.RequestItem> requestItems = requestItemsMap.get(group); //그룹별 수신 레지스터 호출

            ReadInputRegistersRequest readRequest = setReadRequest(requestItems);  //수신 요청 정보 생성
            ReadInputRegistersResponse readResponse = getReadResponse(readRequest); //수신 데이터 호출

            if (readResponse != null) {
                setResponseItems(group, requestItems, readResponse);
            }
        }

        if (!responseItemsMap.isEmpty()) {
            setReadData();
        } else {
            setReadDataByError(PMSCode.getDeviceStatus("09"), "01008");  //수신 오류
        }
    }

    public BmsVO getReadData() {
        BmsVO bmsVO = new BmsVO();
        bmsVO.setRack(rackVO);
        bmsVO.setModules(new ArrayList<>(moduleDataMap.values()));  //Module 데이터 Map -> ArrayList 변

        if (rackVO.getFaultFlag().equals("Y") || rackVO.getWarningFlag().equals("Y")) {
            bmsVO.setError(true);
        }

        return bmsVO;
    }

    public DeviceErrorsVO getErrorData() {
        DeviceErrorsVO deviceErrorsVO = new DeviceErrorsVO();

        deviceErrorsVO.setDeviceErrors(rackErrors);
        deviceErrorsVO.setComponentErrors(moduleErrors);

        return deviceErrorsVO;
    }

    private void setReadData() {
        rackVO.setRackCode(rackCode);
        rackVO.setRegDate(DateTimeUtil.getUnixTimestamp());

        for (DeviceVO.ComponentVO module : modules) {
            int moduleNo = module.getComponentNo();

            BmsVO.ModuleVO moduleVO = new BmsVO.ModuleVO();
            moduleVO.setRackCode(rackCode);
            moduleVO.setModuleNo(moduleNo);
            moduleVO.setRegDate(rackVO.getRegDate());

            moduleDataMap.put(moduleNo, moduleVO);
        }

        for (String group : responseItemsMap.keySet()) {
            List<BmsVO.ResponseItem> responseItems = responseItemsMap.get(group);

            for (BmsVO.ResponseItem responseItem : responseItems) {
                switch (group) {
                    case "rack-items-1":
                    case "rack-items-2":
                    case "rack-status-items":
                        setRackData(responseItem);
                        break;
                    case "module-volt-items":
                    case "module-balancing-items":
                    case "module-fault-items":
                    case "cell-volt-items-1":
                    case "cell-volt-items-2":
                    case "cell-volt-items-3":
                    case "cell-temp-items":
                        setModuleData(responseItem);
                        break;
                }
            }
        }
    }

    public void setReadDataByError(String deviceStatusCode, String errorCodeKey) {
        rackVO.setRackCode(rackCode);
        rackVO.setRegDate(DateTimeUtil.getUnixTimestamp());
        rackVO.setOperationStatus(deviceStatusCode);
        rackVO.setOperationModeStatus("0");
        rackVO.setWarningFlag("Y");
        rackVO.setFaultFlag("N");

        String errorCode = PMSCode.getCommonErrorCode(errorCodeKey);
        setRackErrors(errorCode);
    }

    /**
     * 수신 요청 정보 설정
     *
     * @param requestItems 수신 항목 정보 목록
     * @return 수신 요청 정보
     */
    private ReadInputRegistersRequest setReadRequest(List<BmsVO.RequestItem> requestItems) {
        int reference = requestItems.get(0).getRegister();
        int wordCount = 0;

        for (BmsVO.RequestItem requestItem : requestItems) {
            int register = requestItem.getRegister();
            int size = requestItem.getSize();

            if (reference > register) {
                reference = register;
            }

            //Word 개수가 최대 125 이상인 경우 Request 생성 중지 - BMSReadRegister.java 참고
            if (wordCount >= 125) {
                if (wordCount > 125) {
                    System.out.println("[Warning]Read items count exceeded 125. Excess items are excluded.");
                }
                break;
            }

            wordCount += size;
        }

        ReadInputRegistersRequest request = new ReadInputRegistersRequest();
        request.setReference(reference);
        request.setWordCount(wordCount);
        request.setUnitID(rackNo);

        return request;
    }

    /**
     * 수신 데이터 호출
     *
     * @param request 수신 요청 정보
     * @return 수신 데이터
     */
    private ReadInputRegistersResponse getReadResponse(ReadInputRegistersRequest request) {
        try {
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            transaction.setRequest(request);
            transaction.execute();

            return (ReadInputRegistersResponse) transaction.getResponse();
        } catch (ModbusException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 수신 응답 항목 목록 설정
     *
     * @param group        항목 그룹
     * @param requestItems 요청 항목 목록
     * @param response     수신 응답 데이터
     */
    private void setResponseItems(String group, List<BmsVO.RequestItem> requestItems, ReadInputRegistersResponse response) {
        int wordCount = 0;
        List<BmsVO.ResponseItem> responseItems = new ArrayList<>();

        for (BmsVO.RequestItem requestItem : requestItems) {
            int size = requestItem.getSize();
            List<InputRegister> inputRegisters = new ArrayList<>();

            for (int i = wordCount; i < (wordCount + size); i++) {
                InputRegister inputRegister = BMSReadItem.toLittleEndian(response.getRegister(i));
                inputRegisters.add(inputRegister);
            }

            wordCount += size;

            BmsVO.ResponseItem responseItem = setResponseItem(requestItem, inputRegisters);
            responseItems.add(responseItem);
        }

        responseItemsMap.put(group, responseItems);
    }

    /**
     * 수신 응답 항목 설정
     *
     * @param requestItem    요청 항목 목록
     * @param inputRegisters 수신 데이터 목록
     * @return 수신 응답 항목
     */
    private BmsVO.ResponseItem setResponseItem(BmsVO.RequestItem requestItem, List<InputRegister> inputRegisters) {
        BmsVO.ResponseItem responseItem = new BmsVO.ResponseItem();

        responseItem.setAddress(requestItem.getAddress());
        responseItem.setRegister(requestItem.getRegister());
        responseItem.setSize(requestItem.getSize());
        responseItem.setDataType(requestItem.getDataType());
        responseItem.setScale(requestItem.getScale());
        responseItem.setType(requestItem.getType());
        responseItem.setNo(requestItem.getNo());
        responseItem.setName(requestItem.getName());
        responseItem.setInputRegisters(inputRegisters);

        return responseItem;
    }

    private void setRackData(BmsVO.ResponseItem responseItem) {
        try {
            String itemType = responseItem.getType();
            String itemName = responseItem.getName();

            if (itemType.equals("value")) {
                for (Field field : rackVO.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName();

                    if (itemName.equals(fieldName)) {
                        Object fieldValue = BMSReadItem.getFieldValue(responseItem, field);
                        field.set(rackVO, fieldValue);
                    }
                }
            } else if (itemType.equals("status")) {
                List<InputRegister> inputRegisters = responseItem.getInputRegisters();
                int registerValue = inputRegisters.get(0).toUnsignedShort();
                String[] bits = BMSReadItem.toBits(registerValue);

                switch (itemName) {
                    case "systemStatus":
                        setRelayStatus(bits);
                        setOperationMode(bits);
                        setOperationStatus(bits);
                        break;
                    case "rackFault1":
                    case "rackFault2":
                        //레지스터 값이 0이상인 경우 오류 발생 상태
                        if (registerValue > 0) {
                            setRackFault(itemName, bits);
                            rackVO.setFaultFlag(PMSCode.getCommonCode("OCCUR_FLAG_Y"));
                        } else {
                            if (rackVO.getFaultFlag() == null) {
                                rackVO.setFaultFlag(PMSCode.getCommonCode("OCCUR_FLAG_N"));
                            }
                        }
                        break;
                    case "errorLevel1":
                    case "errorLevel2":
                        //레지스터 값이 0이상인 경우 오류 발생 상태
                        if (registerValue > 0) {
                            setErrorLevel(itemName, bits);
                        } else {
                            if (rackVO.getWarningFlag() == null) {
                                rackVO.setWarningFlag(PMSCode.getCommonCode("OCCUR_FLAG_N"));
                            }
                        }
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Relay 동작 및 접점 상태 (bit 0 ~ 6)
     * <p>
     * - Little Endian Bit 번호는 뒤에서 시작하여, Bits String 배열의 15번부터 Bit 추출
     * <p>
     * Ex) bit0=String[15], bit15=String[0]
     *
     * @param bits String 배열 형식의 Bits
     */
    private void setRelayStatus(String[] bits) {
        //릴레이 동작 및 접점 상태 Bit 추출 후 코드 값 변환하여 데이터 설정
        for (int i = 15; i > 8; i--) {
            String bit = bits[i];
            String statusCode = null;

            if (bit.equals("0")) {
                statusCode = PMSCode.getDeviceStatus("03");  //OFF
            } else if (bit.equals("1")) {
                statusCode = PMSCode.getDeviceStatus("04");  //ON
            }

            switch (i) {
                case 15:    //Bit 0 - (+)극 메인 릴레이 동작
                    rackVO.setPositiveMainRelayAction(statusCode);
                    break;
                case 14:    //Bit 1 - (+)극 메인 릴레이 접점
                    rackVO.setPositiveMainRelayContact(statusCode);
                    break;
                case 13:    //Bit 2 - (-)극 메인 릴레이 동작
                    rackVO.setNegativeMainRelayAction(statusCode);
                    break;
                case 12:    //Bit 3 - (-)극 메인 릴레이 접점
                    rackVO.setNegativeMainRelayContact(statusCode);
                    break;
                case 11:    //Bit 4 - 비상정지 릴레이 동작
                    rackVO.setEmergencyRelayAction(statusCode);
                    break;
                case 10:    //Bit 5 - 비상정지 릴레이 접점
                    rackVO.setEmergencyRelayContact(statusCode);
                    break;
                case 9: //Bit 6 - 사전충전 릴레이 동작
                    rackVO.setPrechargeRelayAction(statusCode);
                    break;
            }
        }
    }

    private void setOperationMode(String[] bits) {
        String modeBit = bits[15 - 9].concat(bits[15 - 8]);
        int modeValue = Integer.parseInt(modeBit, 2);
        String modeCode = PMSCode.getOperationMode(String.valueOf(modeValue));

        rackVO.setOperationModeStatus(modeCode);
    }

    private void setOperationStatus(String[] bits) {
        String statusBit = bits[15 - 11].concat(bits[15 - 10]);
        int statusValue = Integer.parseInt(statusBit, 2);
        String statusCode = null;

        switch (statusValue) {
            case 0:
                statusCode = "06";
                break;
            case 1:
                statusCode = "08";
                break;
            case 2:
                statusCode = "99";
                break;
        }

        rackVO.setOperationStatus(PMSCode.getDeviceStatus(statusCode));
    }

    private void setRackFault(String itemName, String[] bits) {
        int endBitIndex = 0;
        String faultType = null;

        if (itemName.equals("rackFault1")) {
            faultType = "F1B";
        } else if (itemName.equals("rackFault2")) {
            endBitIndex = 13;
            faultType = "F2B";
        }

        for (int i = 15; i >= endBitIndex; i--) {
            String errorBit = bits[i];
            int errorValue = Integer.parseInt(errorBit, 2);

            if (errorValue == 1) {
                String faultCode = faultType + String.format("%02d", 15 - i);
                String errorCode = PMSCode.getBMSErrorCode(faultCode);

                setRackErrors(errorCode);
            }
        }
    }

    private void setErrorLevel(String itemName, String[] bits) {
        int endBitIndex = 0;
        String levelType = null;
        String warningFlag = "N";

        if (rackVO.getWarningFlag() != null) {
            warningFlag = rackVO.getWarningFlag();
        }

        if (itemName.equals("errorLevel1")) {
            levelType = "LV1B";
        } else if (itemName.equals("errorLevel2")) {
            endBitIndex = 8;
            levelType = "LV2B";
        }

        for (int i = 15; i >= endBitIndex; i = i - 2) {
            String levelBit = bits[i - 1].concat(bits[i]);
            int levelValue = Integer.parseInt(levelBit, 2);

            if (levelValue > 0) {
                if (levelValue == 1 || levelValue == 2) {
                    warningFlag = "Y";
                }

                String levelCode = levelType + String.format("%02d", 15 - i) + levelValue;
                String errorCode = PMSCode.getBMSErrorCode(levelCode);

                setRackErrors(errorCode);
            }
        }

        rackVO.setWarningFlag(PMSCode.getCommonCode("OCCUR_FLAG_" + warningFlag));
    }

    private void setModuleData(BmsVO.ResponseItem responseItem) {
        String itemType = responseItem.getType();
        String itemName = responseItem.getName();
        int itemNo = responseItem.getNo();

        try {
            for (BmsVO.ModuleVO moduleVO : moduleDataMap.values()) {
                int moduleNo = moduleVO.getModuleNo();

                if (itemNo == moduleNo) {
                    if (itemType.equals("value")) {
                        Field field = moduleVO.getClass().getDeclaredField(itemName);
                        field.setAccessible(true);

                        Object fieldValue = BMSReadItem.getFieldValue(responseItem, field);
                        field.set(moduleVO, fieldValue);
                    } else if (itemType.equals("status")) {
                        List<InputRegister> inputRegisters = responseItem.getInputRegisters();
                        int registerValue = inputRegisters.get(0).toUnsignedShort();
                        String[] bits = BMSReadItem.toBits(registerValue);

                        switch (itemName) {
                            case "balancingStatus":
                                setBalancingFlag(bits, moduleVO);
                                break;
                            case "moduleFault":
                                setModuleFault(moduleNo, bits);
                                break;
                        }
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void setBalancingFlag(String[] bits, BmsVO.ModuleVO moduleVO) {
        try {
            String moduleBalancingFlag = "Y";

            for (int i = 1; i <= 16; i++) {
                String bit = bits[16 - i];
                String fieldName = "cell" + i + "BalancingFlag";
                String cellBalancingFlag = "N";

                if (bit.equals("1")) {
                    cellBalancingFlag = "Y";
                } else {
                    moduleBalancingFlag = "N";
                }

                Field field = moduleVO.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                field.set(moduleVO, PMSCode.getCommonCode("COMPLETE_FLAG_" + cellBalancingFlag));
            }

            moduleVO.setCellBalancingFlag(PMSCode.getCommonCode("COMPLETE_FLAG_" + moduleBalancingFlag));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void setModuleFault(int moduleNo, String[] bits) {
        String faultFlag = "N";

        if (rackVO.getFaultFlag() != null) {
            faultFlag = rackVO.getFaultFlag();
        }

        for (int i = 15; i >= 9; i--) {
            String faultBit = bits[i];
            int faultValue = Integer.parseInt(faultBit, 2);

            if (faultValue == 1) {
                String faultCode = "F3B" + String.format("%02d", 15 - i);
                setModuleErrors(moduleNo, faultCode);

                faultFlag = "Y";
            }
        }

        rackVO.setFaultFlag(PMSCode.getCommonCode("OCCUR_FLAG_" + faultFlag));
    }

    private void setRackErrors(String errorCode) {
        DeviceErrorVO deviceErrorVO = new DeviceErrorVO();
        deviceErrorVO.setErrorDate(rackVO.getRegDate());
        deviceErrorVO.setDeviceCode(rackVO.getRackCode());
        deviceErrorVO.setErrorCode(errorCode);

        rackErrors.add(deviceErrorVO);
    }

    private void setModuleErrors(int componentNo, String referenceCode) {
        String errorCode = PMSCode.getBMSErrorCode(referenceCode);

        ComponentErrorVO componentErrorVO = new ComponentErrorVO();
        componentErrorVO.setErrorDate(rackVO.getRegDate());
        componentErrorVO.setDeviceCode(rackVO.getRackCode());
        componentErrorVO.setComponentNo(componentNo);
        componentErrorVO.setErrorCode(errorCode);

        moduleErrors.add(componentErrorVO);
    }
}
