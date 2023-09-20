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
    private static String previousCommonErrorCode;
    private static int previousRegDate = 0;
    private static ControlRequestVO controlRequest = null;

    /**
     * 통신 연결 정보 호출
     *
     * @return 통신 연결 정보
     */
    public ModbusSerialMaster getConnection() {
        return connection;
    }

    /**
     * 통신 클라이언트 실행
     */
    public void execute() {
        setConnection();    //통신 연결 정보 설정

        try {
            connect();  //통신 연결
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //통신 연결이 되면 PCS 초기화 제어 실행
            if (isConnected()) {
                controlByStatus("0401", "0200010200", null);    //PCS 초기화 제어
            }

            requestItems = new PCSReadItem().getRequestItems(); //수신 요청 항목 생성
            executeScheduler(); //스케줄러 실행
        }
    }

    /**
     * PCS 장비 정보 호출
     *
     * @return PCS 장비 정보
     */
    public DeviceVO getPcsInfo() {
        return pcsInfo;
    }

    /**
     * 통신 연결 여부 확인
     *
     * @return 통신 연결 여부
     */
    public boolean isConnected() {
        return connection.isConnected();
    }

    /**
     * 통신 연결
     *
     * @throws Exception Exception
     */
    public void connect() throws Exception {
        connection.connect();
    }

    /**
     * 통신 연결 해제
     */
    public void disconnect() {
        connection.disconnect();
    }

    /**
     * 통신 연결 정보 설정
     */
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
        connection.setTimeout(1000);
    }

    /**
     * PCS 통신 스케줄러 실행
     */
    private void executeScheduler() {
        try {
            pcsScheduler.execute();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 데이터 수신 및 처리
     *
     * @return PCS 수신 데이터
     */
    public PcsVO read() {
        PCSReader pcsReader = new PCSReader(pcsInfo);
        pcsReader.setRequest(connection, requestItems); //수신 요청 설정
        pcsReader.request();    //수신 요청

        PcsVO pcsVO = pcsReader.getReadData();  //PCS 수신 데이터 호출
        List<DeviceErrorVO> pcsErrors = pcsReader.getPcsErrors();   //PCS 장비 오류 정보 호출

        processData(pcsVO, pcsErrors, null);    //수신 데이터 처리

        return pcsVO;
    }

    /**
     * 통신 오류 발생 시에 데이터 수신 처리
     *
     * @return PCS 수신 데이터
     */
    public PcsVO readByError() {
        PCSReader pcsReader = new PCSReader(pcsInfo);
        pcsReader.setReadDataByError(PMSCode.getDeviceStatus("09"), "01007");   //연결 오류

        PcsVO pcsVO = pcsReader.getReadData();  //PCS 수신 데이터 호출
        List<DeviceErrorVO> pcsErrors = pcsReader.getPcsErrors();   //PCS 장비 오류 정보 호출
        DeviceErrorVO commonError = pcsReader.getCommonError(); //공통 통신 오류 정보 호출

        processData(pcsVO, pcsErrors, commonError); //수신 데이터 처리

        return pcsVO;
    }

    /**
     * 장비 오류 코드 호출
     *
     * @return 장비 오류 코드 목록
     */
    public List<String> getErrorCodes() {
        return previousErrorCodes;
    }

    /**
     * 공통(통신) 오류 코드 호출
     *
     * @return 공통 오류 코드
     */
    public String getCommonErrorCode() {
        return previousCommonErrorCode;
    }

    /**
     * 수신 데이터 처리
     *
     * @param pcsVO     PCS 정보
     * @param pcsErrors PCS 오류 정보
     */
    private void processData(PcsVO pcsVO, List<DeviceErrorVO> pcsErrors, DeviceErrorVO commonError) {
        int currentRegDate = pcsVO.getRegDate();

        //이전 등록 시간과 현재 시간을 확인하여 Duplicate 오류 방지
        if (!containsRegDate(currentRegDate)) {
            boolean isInsertData = insertData(pcsVO);   //PCS 데이터 등록

            //데이터 등록 완료 시, 오류 데이터 처리
            if (isInsertData) {
                if (pcsVO.getWarningFlag().equals("Y") || pcsVO.getFaultFlag().equals("Y")) {

                    if (commonError == null) {
                        List<String> currentErrorCodes = setCurrentErrorCodes(pcsErrors);

                        //이전 오류와 현재 오류 비교하여
                        if (!containsErrors(currentErrorCodes)) {
                            boolean isInsertError = insertErrorData(pcsErrors);

                            if (isInsertError) {
                                previousErrorCodes.clear();
                                previousErrorCodes = currentErrorCodes;
                            }
                        }
                    } else {
                        String currentCommonErrorCode = commonError.getErrorCode();

                        if (!containsCommonError(currentCommonErrorCode)) {
                            boolean isInsertCommonError = insertCommonErrorData(commonError);

                            if (isInsertCommonError) {
                                previousCommonErrorCode = currentCommonErrorCode;
                            }
                        }
                    }
                } else if (pcsVO.getWarningFlag().equals("N") && pcsVO.getFaultFlag().equals("N")) {
                    previousErrorCodes.clear();
                    previousCommonErrorCode = null;
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

    private boolean insertCommonErrorData(DeviceErrorVO deviceErrorVO) {
        DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();
        int result = deviceErrorQuery.insertCommonError(deviceErrorVO);

        if (result > 0) {
            new BackupFile().backupData("device-error", pcsInfo.getDeviceCode(), deviceErrorVO);
        }

        return result > 0;
    }

    private boolean insertErrorData(List<DeviceErrorVO> errors) {
        DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();
        int result = deviceErrorQuery.insertDeviceErrors(errors);

        if (result > 0) {
            new BackupFile().backupData("device-error", pcsInfo.getDeviceCode(), errors);
        }

        return result > 0;
    }

    private boolean containsRegDate(int currentRegDate) {
        return previousRegDate == currentRegDate;
    }

    /**
     * 공통 오류 코드 동일 확인
     * <p>
     * 이전 공통 오류 코드와 현재 발생한 공통 오류 코드가 동일한지 확인
     *
     * @param currentCommonErrorCode 현재 공통 오류 코드
     * @return 동일 결과
     */
    private boolean containsCommonError(String currentCommonErrorCode) {
        System.out.println("PCS 이전 공통 오류 : " + previousCommonErrorCode + " / 현재 공통 오류 : " + currentCommonErrorCode);
        return previousCommonErrorCode.equals(currentCommonErrorCode);
    }

    /**
     * 오류 코드 동일 확인
     * <p>
     * 이전 오류 코드와 현재 발생한 오류 코드가 동일한지 확인
     *
     * @param currentErrorCodes 현재 오류 코드
     * @return 동일 결과
     */
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
        //responseVO 예외처리
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
