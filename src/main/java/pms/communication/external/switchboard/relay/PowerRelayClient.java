package pms.communication.external.switchboard.relay;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.quartz.SchedulerException;
import pms.database.query.DeviceErrorQuery;
import pms.database.query.DeviceQuery;
import pms.scheduler.external.switchboard.relay.PowerRelayScheduler;
import pms.system.PMSCode;
import pms.system.backup.BackupFile;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;
import pms.vo.device.external.PowerRelayVO;

import java.util.*;

/**
 * Power Relay Client
 * <p>
 * - 전력 계전기 통신 클라이언트
 * <p>
 * - 계통(Grid) 측의 전력 계전
 */
public class PowerRelayClient {
    private final PowerRelayScheduler powerRelayScheduler = new PowerRelayScheduler();
    private static ModbusSerialMaster connection;
    private final DeviceVO powerRelayInfo = PmsVO.meters.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_0501")).get(0);
    private static Map<String, List<PowerRelayVO.RequestItem>> requestItemsMap = new HashMap<>();   //수신 요청 아이템 Map
    private static int heartbeatInterval = 3;
    private static List<String> previousErrorCodes = new ArrayList<>();
    private static int previousRegDate = 0;
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
            requestItemsMap = new PowerRelayReadItem().getRequestItems();

            executeScheduler();
        }
    }

    private void setConnection() {
        //String port = deviceProperties.getProperty("pcs.port");
        String port = "/dev/tty.usbserial-A9PZ4U3D";

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

    public void connect() throws Exception {
        connection.connect();
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public void disconnect() {
        connection.disconnect();
    }

    private void executeScheduler() {
        try {
            powerRelayScheduler.execute();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public boolean checkHeartbeatInterval() {

        if (heartbeatInterval > 0) {
            heartbeatInterval = heartbeatInterval - 1;
            return false;
        }

        return heartbeatInterval == 0;
    }

    public PowerRelayVO read() {
        PowerRelayReader powerRelayReader = new PowerRelayReader(powerRelayInfo);
        powerRelayReader.setRequest(connection, requestItemsMap);
        powerRelayReader.request();

        PowerRelayVO powerRelayVO = powerRelayReader.getReadData();
        List<DeviceErrorVO> powerRelayErrors = powerRelayReader.getPowerRelayErrors();

        processData(powerRelayVO, powerRelayErrors);

        return powerRelayVO;
    }

    public PowerRelayVO readByError() {
        PowerRelayReader powerRelayReader = new PowerRelayReader(powerRelayInfo);
        powerRelayReader.setReadDataByError(PMSCode.getDeviceStatus("09"), "01007");

        PowerRelayVO powerRelayVO = powerRelayReader.getReadData();
        List<DeviceErrorVO> pcsErrors = powerRelayReader.getPowerRelayErrors();

        processData(powerRelayVO, pcsErrors);

        return powerRelayVO;
    }

    private void processData(PowerRelayVO powerRelayVO, List<DeviceErrorVO> powerRelayErrors) {
        int currentRegDate = powerRelayVO.getRegDate();

        if (!containsRegDate(currentRegDate)) {
            boolean isInsertData = insertData(powerRelayVO);

            if (isInsertData) {
                List<String> currentErrorCodes = setCurrentErrorCodes(powerRelayErrors);

                if (!containsErrors(currentErrorCodes)) {
                    boolean isInsertError = insertErrorData(powerRelayErrors);

                    if (isInsertError) {
                        previousErrorCodes.clear();
                        previousErrorCodes = currentErrorCodes;
                    }
                }

                previousRegDate = currentRegDate;
            }
        }
    }

    private boolean insertData(PowerRelayVO powerRelayVO) {

        DeviceQuery deviceQuery = new DeviceQuery();
        int result = deviceQuery.insertPowerRelayData(powerRelayVO);

        if (result > 0) {
            new BackupFile().backupData("device", powerRelayVO.getRelayCode(), powerRelayVO);
        }

        return result > 0;
    }

    private boolean insertErrorData(List<DeviceErrorVO> errors) {
        DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();
        int result = deviceErrorQuery.insertDeviceError(errors);

        if (result > 0) {
            new BackupFile().backupData("device-error", powerRelayInfo.getDeviceCode(), errors);
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
}
