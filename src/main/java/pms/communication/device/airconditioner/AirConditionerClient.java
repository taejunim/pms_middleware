package pms.communication.device.airconditioner;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import pms.database.query.ControlQuery;
import pms.database.query.DeviceErrorQuery;
import pms.database.query.DeviceQuery;
import pms.scheduler.device.airconditioner.AirConditionerScheduler;
import pms.system.PMSCode;
import pms.system.backup.BackupFile;
import pms.vo.device.AirConditionerVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.device.error.DeviceErrorsVO;
import pms.vo.history.ControlHistoryVO;
import pms.vo.system.DeviceVO;

import java.util.*;

import static pms.communication.CommunicationManager.deviceProperties;

public class AirConditionerClient {
    private static final Map<String, ModbusSerialMaster> connections = new HashMap<>();
    private final AirConditionerScheduler airConditionerScheduler = new AirConditionerScheduler();
    private static final Map<String, ControlRequestVO> controlRequestMap = new HashMap<>(); //에어컨 별 제어 요청 Map
    private static Map<String, List<AirConditionerVO.RequestItem>> requestItemsMap = new HashMap<>();   //수신 요청 아이템 List
    private static final Map<String, DeviceVO> airConditionerInfoMap = new HashMap<>();
    private static int previousRegDate = 0;
    private static List<String> previousErrorCodes = new ArrayList<>();


    public void execute(String airConditionerCode, DeviceVO airConditionerVO) {
        setConnection(airConditionerCode, airConditionerVO);

        try {
            connect(airConditionerCode);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            requestItemsMap = new AirConditionerReadItem().getRequestItemsMap();

            executeScheduler(airConditionerCode);
        }
    }

    private void executeScheduler(String airConditionerCode) {
        try {
            airConditionerScheduler.execute(airConditionerCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect(String airConditionerCode) throws Exception {
        connections.get(airConditionerCode).connect();
    }

    public void disconnect(String airConditionerCode) {
        connections.get(airConditionerCode).disconnect();
    }

    private void setConnection(String airConditionerDeviceCode, DeviceVO airConditionerVO) {
        airConditionerInfoMap.put(airConditionerDeviceCode, airConditionerVO);

        String airConditionerKey = "airConditioner-" + getAirConditionerInfo(airConditionerDeviceCode).getDeviceNo();
        String port = deviceProperties.getProperty(airConditionerKey + ".port");

        SerialParameters parameters = new SerialParameters();
        parameters.setPortName(port);
        parameters.setBaudRate(9600);
        parameters.setDatabits(8);
        parameters.setParity(AbstractSerialConnection.NO_PARITY);   //Parity: NONE
        parameters.setStopbits(AbstractSerialConnection.ONE_STOP_BIT);  //1 Stop Bit
        parameters.setEncoding(Modbus.SERIAL_ENCODING_RTU); //RTU
        parameters.setEcho(false);
        parameters.setRs485Mode(true);
//        parameters.setOpenDelay(5);
//        parameters.setRs485DelayAfterTxMicroseconds(2000);
//        parameters.setRs485DelayBeforeTxMicroseconds(2000);


        ModbusSerialMaster connection = new ModbusSerialMaster(parameters);
        connection.setRetries(0);
        connection.setTimeout(2000);

        connections.put(airConditionerDeviceCode, connection);
    }

    public boolean isConnected(String airConditionerCode) {
        return connections.get(airConditionerCode).isConnected();
    }

    public boolean isControlRequest(String airConditionerCode) {
        return controlRequestMap.containsKey(airConditionerCode);
    }

    public void setControlRequestMap(String airConditionerCode, ControlRequestVO requestVO) {
        controlRequestMap.put(airConditionerCode, requestVO);
    }

    public AirConditionerVO read(String airConditionerCode) {
        AirConditionerReader airConditionerReader = new AirConditionerReader(getAirConditionerInfo(airConditionerCode));
        airConditionerReader.setRequest(getConnection(airConditionerCode), requestItemsMap);
        airConditionerReader.request();

        AirConditionerVO airConditionerVO = airConditionerReader.getReadData();
        List<DeviceErrorVO> airConditionerErrors = airConditionerReader.getErrorData();

        if (airConditionerVO.getWarningFlag() == null) {
            airConditionerVO.setWarningFlag("N");
        }

        processData(airConditionerVO, airConditionerErrors);

        return airConditionerVO;
    }

    private void processData(AirConditionerVO airConditionerVO, List<DeviceErrorVO> airConditionerErrors) {
        int currentRegDate = airConditionerVO.getRegDate();

        if (!containsRegDate(currentRegDate)) {
            boolean isInsertData = insertData(airConditionerVO);

            if (isInsertData) {
                if (airConditionerVO.getWarningFlag().equals("Y") || airConditionerVO.getFaultFlag().equals("Y")) {
                    List<String> currentErrorCodes = setCurrentErrorCodes(airConditionerErrors);

                    if (!containsErrors(currentErrorCodes)) {
                        boolean isInsertError = insertErrorData(airConditionerErrors);

                        if (isInsertError) {
                            previousErrorCodes.clear();
                            previousErrorCodes = currentErrorCodes;
                        }
                    }
                } else if (airConditionerVO.getWarningFlag().equals("N") && airConditionerVO.getFaultFlag().equals("N")) {
                    previousErrorCodes.clear();
                }
                previousRegDate = currentRegDate;
            }
        }
    }

    private boolean insertErrorData(List<DeviceErrorVO> airConditionerErrors) {
        DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();
        int result = deviceErrorQuery.insertDeviceErrors(airConditionerErrors);

        if (result > 0) {
            new BackupFile().backupData("device-error", airConditionerErrors.get(0).getDeviceCode(), airConditionerErrors); //!!! 고정형 에어컨 파일 백업
        }

        return result > 0;
    }

    private boolean containsErrors(List<String> currentErrorCodes) {
        return new HashSet<>(previousErrorCodes).containsAll(currentErrorCodes);
    }

    private List<String> setCurrentErrorCodes(List<DeviceErrorVO> airConditionerErrors) {
        List<String> currentErrorCodes = new ArrayList<>();

        for (DeviceErrorVO errorVO : airConditionerErrors) {
            String errorCode = errorVO.getErrorCode();
            currentErrorCodes.add(errorCode);
        }

        return currentErrorCodes;
    }

    private boolean insertData(AirConditionerVO airConditionerVO) {
        if (airConditionerVO.getWarningFlag() == null) {
            airConditionerVO.setWarningFlag("N");
        }

        DeviceQuery deviceQuery = new DeviceQuery();
        int result = deviceQuery.insertAirConditionerData(airConditionerVO);

        if (result > 0) {
            new BackupFile().backupData("device", airConditionerVO.getAirConditionerCode(), airConditionerVO); //!!! 고정형 에어컨 파일 백업
        }
        return result > 0;
    }

    private boolean containsRegDate(int currentRegDate) {
        return previousRegDate == currentRegDate;
    }

    private ModbusSerialMaster getConnection(String airConditionerCode) {
        return connections.get(airConditionerCode);
    }

    public DeviceVO getAirConditionerInfo(String airConditionerCode) {
        return airConditionerInfoMap.get(airConditionerCode);
    }

    public List<String> getErrorCodes() {
        return previousErrorCodes;
    }

    public AirConditionerVO readByError(String airConditionerCode) {
        AirConditionerReader airConditionerReader = new AirConditionerReader(getAirConditionerInfo(airConditionerCode));
        airConditionerReader.setReadDataByError(PMSCode.getDeviceStatus("09"), "01007");

        AirConditionerVO airConditionerVO = airConditionerReader.getReadData();
        List<DeviceErrorVO> airConditionerErrors = airConditionerReader.getErrorData();

        processData(airConditionerVO, airConditionerErrors);

        return airConditionerVO;
    }

    public ControlResponseVO control(String airConditionerCode) {
        ControlRequestVO requestVO = controlRequestMap.get(airConditionerCode);

        AirConditionerWriter airConditionerWriter = new AirConditionerWriter();
        airConditionerWriter.setRequest(airConditionerCode, getConnection(airConditionerCode), requestVO);
        airConditionerWriter.request();

        ControlResponseVO responseVO = airConditionerWriter.getResponse();

        ControlHistoryVO controlHistoryVO = responseVO.getHistoryVO();
        insertControlHistory(controlHistoryVO);

        controlRequestMap.remove(airConditionerCode);

        return responseVO;
    }

    private void insertControlHistory(ControlHistoryVO controlHistoryVO) {
        ControlQuery controlQuery = new ControlQuery();
        int result = 0;
        try {
            result = controlQuery.insertControlHistory(controlHistoryVO);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (result > 0) {
            new BackupFile().backupData("control", null, controlHistoryVO); //!!! 고정형 에어컨 파일 백업
        }
    }
}
