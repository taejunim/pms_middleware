package pms.communication.external.switchboard;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.quartz.SchedulerException;
import pms.common.util.ResourceUtil;
import pms.database.query.DeviceErrorQuery;
import pms.database.query.DeviceQuery;
import pms.scheduler.external.switchboard.PowerMeterScheduler;
import pms.system.PMSCode;
import pms.system.backup.BackupFile;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;
import pms.vo.system.PowerMeterVO;

import java.util.*;

/**
 * packageName    : pms.communication.external.switchboard
 * fileName       : PowerMeterReader
 * author         : youyeong
 * date           : 2023/07/28
 * description    : EV충전기 전력 계측기 통신 Client
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/07/28        youyeong       최초 생성
 */
public class PowerMeterClient {
    private final PowerMeterScheduler powerRelayScheduler = new PowerMeterScheduler();
    public static final Properties deviceProperties = ResourceUtil.loadProperties("device");
    private static ModbusSerialMaster connection;
    private final DeviceVO powerMeterInfo = PmsVO.meters.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_0502")).get(0);
    private static Map<String, List<PowerMeterVO.RequestItem>> requestItemsMap = new HashMap<>();   //수신 요청 아이템 Map
    private static List<String> previousErrorCodes = new ArrayList<>();
    private static int previousRegDate = 0;
    public ModbusSerialMaster getConnection() {
        return connection;
    }

    public void execute() {
        System.out.println("execute");
        setConnection();

        try {
            connect();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            requestItemsMap = new PowerMeterReadItem().getRequestItems();

            executeScheduler();
        }
    }

    private void setConnection() {
        String port = deviceProperties.getProperty("meter.relay.port");
        SerialParameters parameters = new SerialParameters();
        parameters.setPortName(port);                                   //통신 포트
        parameters.setBaudRate(19200);                                  //통신 속도
        parameters.setDatabits(8);                                      //8 Data Bits
        parameters.setParity(AbstractSerialConnection.EVEN_PARITY);     //Parity: NONE
        parameters.setStopbits(AbstractSerialConnection.ONE_STOP_BIT);  //1 Stop Bit
        parameters.setEncoding(Modbus.SERIAL_ENCODING_RTU); //RTU

        connection = new ModbusSerialMaster(parameters);
        connection.setRetries(0);
        connection.setTimeout(3000);
    }

    public void connect() throws Exception {
        System.out.println("connect");
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

    public PowerMeterVO read() {
        System.out.println("read");
        PowerMeterReader powerMeterReader = new PowerMeterReader(powerMeterInfo);
        powerMeterReader.setRequest(connection, requestItemsMap);
        powerMeterReader.request();

        PowerMeterVO powerMeterVO = powerMeterReader.getReadData();
        System.out.println(powerMeterVO);
        List<DeviceErrorVO> powerMeterErrors = powerMeterReader.getPowerMeterErrors();

        processData(powerMeterVO, powerMeterErrors);

        return powerMeterVO;
    }

    public PowerMeterVO readByError() {
        PowerMeterReader powerMeterReader = new PowerMeterReader(powerMeterInfo);
        powerMeterReader.setReadDataByError(PMSCode.getDeviceStatus("09"), "01007");

        PowerMeterVO powerMeterVO = powerMeterReader.getReadData();
        List<DeviceErrorVO> powerMeterErrors = powerMeterReader.getPowerMeterErrors();

        processData(powerMeterVO, powerMeterErrors);

        return powerMeterVO;
    }

    private void processData(PowerMeterVO powerMeterVO, List<DeviceErrorVO> powerRelayErrors) {
        int currentRegDate = powerMeterVO.getRegDate();

        if (!containsRegDate(currentRegDate)) {
            boolean isInsertData = insertData(powerMeterVO);

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

    private boolean insertData(PowerMeterVO powerMeterVO) {

        DeviceQuery deviceQuery = new DeviceQuery();
  /*      int result = deviceQuery.insertPowerRelayData(powerMeterVO);

        if (result > 0) {
            new BackupFile().backupData("device", powerMeterVO.getRelayCode(), powerMeterVO);
        }

        return result > 0;*/

        return true;
    }

    private boolean insertErrorData(List<DeviceErrorVO> errors) {
        DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();
        int result = deviceErrorQuery.insertDeviceError(errors);

        if (result > 0) {
            new BackupFile().backupData("device-error", powerMeterInfo.getDeviceCode(), errors);
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
