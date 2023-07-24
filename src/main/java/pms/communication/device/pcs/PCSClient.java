package pms.communication.device.pcs;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.quartz.SchedulerException;
import pms.database.query.ControlQuery;
import pms.database.query.DeviceErrorQuery;
import pms.database.query.DeviceQuery;
import pms.scheduler.device.pcs.PCSScheduler;
import pms.system.PMSCode;
import pms.system.backup.BackupFile;
import pms.system.ess.ControlUtil;
import pms.vo.device.PcsVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.history.ControlHistoryVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.util.*;

import static pms.communication.CommunicationManager.deviceProperties;

public class PCSClient {
    private final PCSScheduler pcsScheduler = new PCSScheduler();
    private static ModbusSerialMaster connection;
    private final DeviceVO pcsInfo = PmsVO.pcs;
    private static int heartbeat = 0;
    private static int heartbeatInterval = 3;
    private static List<PcsVO.RequestItem> requestItems = new ArrayList<>();
    private static List<String> previousErrorCodes = new ArrayList<>();
    private static final List<String> previousCommonErrorCodes = new ArrayList<>();
    private static int previousRegDate = 0;
    private static ControlRequestVO controlRequest = null;

    public ModbusSerialMaster getConnection() {
        return connection;
    }

    public void execute() {
        setConnection();

        try {
            connect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isConnected()) {
                controlByStatus("0401", "0200010200", null);    //PCS 초기화
            }

            requestItems = new PCSReadItem().getRequestItems();
            executeScheduler();
        }
    }

    public DeviceVO getPcsInfo() {
        return pcsInfo;
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public void connect() throws Exception {
        connection.connect();
    }

    public void disconnect() {
        connection.disconnect();
    }

    private void setConnection() {
        String port = deviceProperties.getProperty("pcs.port");

        SerialParameters parameters = new SerialParameters();
        parameters.setPortName(port);   //통신 포트
        parameters.setBaudRate(9600);   //통신 속도
        parameters.setDatabits(8);  //8 Data Bits
        parameters.setParity(AbstractSerialConnection.NO_PARITY);   //Parity: NONE
        parameters.setStopbits(AbstractSerialConnection.ONE_STOP_BIT);  //1 Stop Bit
        parameters.setEncoding(Modbus.SERIAL_ENCODING_RTU); //RTU
        parameters.setEcho(false);

        connection = new ModbusSerialMaster(parameters);
        connection.setRetries(0);
        connection.setTimeout(3000);
    }

    private void executeScheduler() {
        try {
            pcsScheduler.execute();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public PcsVO read() {
        PCSReader pcsReader = new PCSReader(pcsInfo);
        pcsReader.setRequest(connection, requestItems);
        pcsReader.request();

        PcsVO pcsVO = pcsReader.getReadData();
        List<DeviceErrorVO> pcsErrors = pcsReader.getPcsErrors();

        if (pcsVO.getWarningFlag() == null) {
            pcsVO.setWarningFlag("N");
        }

        processData(pcsVO, pcsErrors);

        return pcsVO;
    }

    public PcsVO readByError() {
        PCSReader pcsReader = new PCSReader(pcsInfo);
        pcsReader.setReadDataByError(PMSCode.getDeviceStatus("09"), "01007");

        PcsVO pcsVO = pcsReader.getReadData();
        List<DeviceErrorVO> pcsErrors = pcsReader.getPcsErrors();

        processData(pcsVO, pcsErrors);

        return pcsVO;
    }

    public List<String> getErrorCodes() {
        return previousErrorCodes;
    }

    private void processData(PcsVO pcsVO, List<DeviceErrorVO> pcsErrors) {
        int currentRegDate = pcsVO.getRegDate();

        if (!containsRegDate(currentRegDate)) {
            boolean isInsertData = insertData(pcsVO);

            if (isInsertData) {
                if (pcsVO.getWarningFlag().equals("Y") || pcsVO.getFaultFlag().equals("Y")) {
                    List<String> currentErrorCodes = setCurrentErrorCodes(pcsErrors);

                    if (!containsErrors(currentErrorCodes)) {
                        boolean isInsertError = insertErrorData(pcsErrors);

                        if (isInsertError) {
                            previousErrorCodes.clear();
                            previousErrorCodes = currentErrorCodes;
                        }
                    }
                } else if (pcsVO.getWarningFlag().equals("N") && pcsVO.getFaultFlag().equals("N")) {
                    previousErrorCodes.clear();
                }

                previousRegDate = currentRegDate;
            }
        }
    }

    private boolean insertData(PcsVO pcsVO) {
        if (pcsVO.getWarningFlag() == null) {
            pcsVO.setWarningFlag("N");
        }

        DeviceQuery deviceQuery = new DeviceQuery();
        int result = deviceQuery.insertPCSData(pcsVO);

        if (result > 0) {
            new BackupFile().backupData("device", pcsVO.getPcsCode(), pcsVO);
        }

        return result > 0;
    }

    private boolean insertErrorData(List<DeviceErrorVO> errors) {
        DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();
        int result = deviceErrorQuery.insertDeviceError(errors);

        if (result > 0) {
            new BackupFile().backupData("device-error", pcsInfo.getDeviceCode(), errors);
        }

        return result > 0;
    }

    private boolean containsRegDate(int currentRegDate) {
        return previousRegDate == currentRegDate;
    }

    private boolean containsErrors(List<String> currentErrorCodes) {
        System.out.println("PCS 이전 오류 : " + previousErrorCodes + " / 현재 오류 : " + currentErrorCodes);
        return new HashSet<>(previousErrorCodes).containsAll(currentErrorCodes);
    }

    private List<String> setCurrentErrorCodes(List<DeviceErrorVO> currentErrors) {
        List<String> currentErrorCodes = new ArrayList<>();

        for (DeviceErrorVO errorVO : currentErrors) {
            String errorCode = errorVO.getErrorCode();
            currentErrorCodes.add(errorCode);
        }

        return currentErrorCodes;
    }

    public boolean checkHeartbeatInterval() {

        if (heartbeatInterval > 0) {
            heartbeatInterval = heartbeatInterval - 1;
            return false;
        }

        return heartbeatInterval == 0;
    }

    public void resetHeartbeatInterval() {
        heartbeatInterval = 3;
    }

    public int sendHeartbeat() {
        if (heartbeat < 255) {
            heartbeat = heartbeat + 1;
        } else {
            heartbeat = 0;
        }

        PCSWriter pcsWriter = new PCSWriter();
        pcsWriter.setRequest(pcsInfo.getDeviceNo(), connection);
        pcsWriter.request(6, heartbeat);

        return pcsWriter.getResult();
    }

    public void controlByStatus(String detailType, String controlCode, String controlValue) {
        ControlRequestVO requestVO = ControlUtil.setControlRequestVO("04", detailType, pcsInfo.getDeviceCategory(), controlCode, controlValue, null);

        PCSWriter pcsWriter = new PCSWriter();
        pcsWriter.setRequest(pcsInfo.getDeviceNo(), connection);
        pcsWriter.request(requestVO);

        ControlResponseVO responseVO = pcsWriter.getResponse();
        ControlHistoryVO controlHistoryVO = responseVO.getHistoryVO();

        insertControlHistory(controlHistoryVO);
    }

    public ControlResponseVO control() {
        PCSWriter pcsWriter = new PCSWriter();
        pcsWriter.setRequest(pcsInfo.getDeviceNo(), connection);
        pcsWriter.request(controlRequest);

        ControlResponseVO responseVO = pcsWriter.getResponse();

        ControlHistoryVO controlHistoryVO = responseVO.getHistoryVO();
        insertControlHistory(controlHistoryVO);

        controlRequest = null;

        return responseVO;
    }

    private void insertControlHistory(ControlHistoryVO controlHistoryVO) {
        ControlQuery controlQuery = new ControlQuery();
        int result = controlQuery.insertControlHistory(controlHistoryVO);

        if (result > 0) {
            new BackupFile().backupData("control", null, controlHistoryVO);
        }
    }

    public boolean isControlRequest() {
        return controlRequest != null;
    }

    public void setControlRequest(ControlRequestVO requestVO) {
        controlRequest = requestVO;
    }
}
