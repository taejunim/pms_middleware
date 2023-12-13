package pms.communication.external.switchboard.meter;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.quartz.SchedulerException;
import pms.common.util.ResourceUtil;
import pms.database.query.DeviceErrorQuery;
import pms.database.query.DeviceQuery;
import pms.scheduler.external.switchboard.meter.PowerMeterScheduler;
import pms.system.PMSCode;
import pms.system.backup.BackupFile;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;
import pms.vo.device.external.PowerMeterVO;

import java.util.*;

import static pms.system.PMSManager.applicationProperties;

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
    private final PowerMeterScheduler powerMeterScheduler = new PowerMeterScheduler();
    private static final Map<String, ModbusSerialMaster> connections = new HashMap<>();   //미터기별 connection 정보 갖고 있는 map
    private static final Map<String, DeviceVO> powerMeterInfoMap = new HashMap<>(); //Rack 장비 정보
    private static Map<String, List<PowerMeterVO.RequestItem>> requestItemsMap = new HashMap<>();   //수신 요청 아이템 Map
    private static final Map<String, List<String>> previousErrorCodesMap = new HashMap<>();
    private static final Map<String, Integer> previousRegDateMap = new HashMap<>();

    public ModbusSerialMaster getConnection(String meterCode) {
        return connections.get(meterCode);
    }

    public void execute(DeviceVO deviceVO) {
        setConnection(deviceVO);
        try {
            connect(deviceVO.getDeviceCode());
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            requestItemsMap = new PowerMeterReadItem().getRequestItems();
            executeScheduler(deviceVO.getDeviceCode());
        }
    }

    public DeviceVO getMeterInfo(String meterCode) {
        return powerMeterInfoMap.get(meterCode);
    }

    public List<String> getErrorCodes(String meterCode) {
        return previousErrorCodesMap.get(meterCode);
    }

    private void setConnection(DeviceVO deviceVO) {
        powerMeterInfoMap.put(deviceVO.getDeviceCode(), deviceVO);
        String port = applicationProperties.getProperty("meter.power-" + deviceVO.getDeviceNo() + ".port");      //포트 정보 있는 properties ex) meter.power-1 , meter.power-2
        SerialParameters parameters = new SerialParameters();

        parameters.setPortName(port);                                               //통신 포트
        parameters.setBaudRate(19200);                                              //통신 속도
        parameters.setDatabits(8);                                                  //8 Data Bits
        parameters.setParity(AbstractSerialConnection.EVEN_PARITY);                 //Parity: EVEN
        parameters.setStopbits(AbstractSerialConnection.ONE_STOP_BIT);              //1 Stop Bit
        parameters.setEncoding(Modbus.SERIAL_ENCODING_RTU);                         //RTU

        ModbusSerialMaster connection = new ModbusSerialMaster(parameters);
        connection.setRetries(0);
        connection.setTimeout(3000);

        connections.put(deviceVO.getDeviceCode(), connection);                      //connections map에는 deviceCode가 key
    }

    public void connect(String meterCode) throws Exception {
        connections.get(meterCode).connect();
    }

    public boolean isConnected(String meterCode) {
        return connections.get(meterCode).isConnected();
    }

    public void disconnect(String meterCode) {
        connections.get(meterCode).disconnect();
    }

    private void executeScheduler(String meterCode) {
        try {
            powerMeterScheduler.execute(meterCode);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public PowerMeterVO read(String meterCode) {
        PowerMeterReader powerMeterReader = new PowerMeterReader(powerMeterInfoMap.get(meterCode));
        powerMeterReader.setRequest(connections.get(meterCode), requestItemsMap);
        powerMeterReader.request();

        PowerMeterVO powerMeterVO = powerMeterReader.getReadData();
        List<DeviceErrorVO> powerMeterErrors = powerMeterReader.getPowerMeterErrors();

        processData(powerMeterVO, powerMeterErrors);

        return powerMeterVO;
    }

    public PowerMeterVO readByError(String meterCode) {
        PowerMeterReader powerMeterReader = new PowerMeterReader(powerMeterInfoMap.get(meterCode));
        powerMeterReader.setReadDataByError(PMSCode.getDeviceStatus("09"), "01007");

        PowerMeterVO powerMeterVO = powerMeterReader.getReadData();
        List<DeviceErrorVO> powerMeterErrors = powerMeterReader.getPowerMeterErrors();

        processData(powerMeterVO, powerMeterErrors);

        return powerMeterVO;
    }

    private void processData(PowerMeterVO powerMeterVO, List<DeviceErrorVO> powerMeterErrors) {
        int currentRegDate = powerMeterVO.getRegDate();

        if (!containsRegDate(powerMeterVO.getMeterCode(), currentRegDate)) {
            boolean isInsertData = insertData(powerMeterVO);

            if (isInsertData) {
                List<String> currentErrorCodes = setCurrentErrorCodes(powerMeterErrors);

                if (!containsErrors(powerMeterVO.getMeterCode(), currentErrorCodes)) {
                    boolean isInsertError = insertErrorData(powerMeterErrors, powerMeterVO.getMeterCode());

                    if (isInsertError) {
                        previousErrorCodesMap.replace(powerMeterVO.getMeterCode(), currentErrorCodes); //이전 오류 코드 목록 갱신
                    }
                }

                previousRegDateMap.replace(powerMeterVO.getMeterCode(), currentRegDate);   //이전 등록 일시 갱신
            }
        }
    }

    private boolean insertData(PowerMeterVO powerMeterVO) {

        DeviceQuery deviceQuery = new DeviceQuery();
        int result = deviceQuery.insertPowerMeterData(powerMeterVO);

        if (result > 0) {
            new BackupFile().backupData("device", powerMeterVO.getMeterCode(), powerMeterVO);
        }

        return result > 0;
    }

    private boolean insertErrorData(List<DeviceErrorVO> errors, String deviceCode) {
        DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();
        int result = deviceErrorQuery.insertDeviceErrors(errors);

        if (result > 0) {
            new BackupFile().backupData("device-error", powerMeterInfoMap.get(deviceCode).getDeviceCode(), errors);
        }

        return result > 0;
    }

    private boolean containsRegDate(String meterCode, int currentRegDate) {
        if (previousRegDateMap.containsKey(meterCode)) {
            return previousRegDateMap.get(meterCode) == currentRegDate;
        } else {
            previousRegDateMap.put(meterCode, currentRegDate);
            return false;
        }
    }

    private boolean containsErrors(String meterCode, List<String> currentErrorCodes) {
        if (previousErrorCodesMap.containsKey(meterCode)) {
            return new HashSet<>(previousErrorCodesMap.get(meterCode)).containsAll(currentErrorCodes);
        } else {
            previousErrorCodesMap.put(meterCode, currentErrorCodes);
            return false;
        }
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
