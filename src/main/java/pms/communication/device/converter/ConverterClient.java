package pms.communication.device.converter;

import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.quartz.SchedulerException;
import pms.database.query.DeviceQuery;
import pms.scheduler.device.converter.ConverterScheduler;
import pms.system.backup.BackupFile;
import pms.vo.device.ConverterVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.history.ControlHistoryVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pms.system.PMSManager.applicationProperties;

/**
 * Converter Client
 * <p>
 * - 컨버터(이동형 ESS의 PCS) 통신 클라이언트
 * <p>
 * - AC/DC Converter & DC/DC Converter
 */
public class ConverterClient {
    private final ConverterScheduler converterScheduler = new ConverterScheduler();
    private final String connectionURL = applicationProperties.getProperty("converter.url");
    private static PlcConnection connection;
    private static List<ConverterVO.RequestItem> requestItems = new ArrayList<>();
    //private static Map<String, List<ConverterVO.RequestItem>> requestItemsMap = new HashMap<>();
    private static final int previousRegDate = 0;
    private static final List<String> previousErrorCodes = new ArrayList<>();   //이전 오류 코드 목록
    private static final List<String> previousCommonErrorCodes = new ArrayList<>(); //이전 공통 오류 코드 목록
    private static final ControlRequestVO controlRequest = null;
    private static final Map<String, ControlRequestVO> controlRequestMap = new HashMap<>();
    private static final List<ControlRequestVO> controlRequests = new ArrayList<>();
    private final DeviceQuery deviceQuery = new DeviceQuery();
    private final BackupFile backupFile = new BackupFile();

    /**
     * 통신 클라이언트 실행
     */
    public void execute() {
        try {
            connect();
        } catch (PlcConnectionException e) {
            e.printStackTrace();
        } finally {
            requestItems = new ConverterReadItem().getRequestItems();
            //requestItemsMap = new ConverterReadItem().getRequestItemsMap();
            executeScheduler();
        }
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
     * 통신 연결 실행
     *
     * @throws PlcConnectionException PlcConnectionException
     */
    public void connect() throws PlcConnectionException {
        PlcDriverManager plcDriverManager = new PlcDriverManager();
        connection = plcDriverManager.getConnection(connectionURL);
        //connection.connect();
    }

    /**
     * 통신 연결 해제 실행
     */
    public void disconnect() {
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 스케줄러 실행
     */
    private void executeScheduler() {
        try {
            converterScheduler.execute();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 데이터 수신 실행
     *
     * @return 수신 데이터
     */
    public ConverterVO read() {
        ConverterReader converterReader = new ConverterReader();
        converterReader.setRequest(connection, requestItems);
        converterReader.request();

        return converterReader.getConverterVO();
    }

    /**
     * 수신 데이터 등록
     *
     * @param converterVO 컨버터 수신 정보
     * @return 데이터 등록 결과
     */
    private boolean insertData(ConverterVO converterVO) {
        boolean acConverterResult = insertACConverterData(converterVO);
        boolean dcConverterResult = insertDCConverterData(converterVO);

        return acConverterResult && dcConverterResult;
    }

    private boolean insertACConverterData(ConverterVO converterVO) {
        boolean complete = false;

        ConverterVO.ACConverterVO acConverterVO = converterVO.getAcConverter();
        List<ConverterVO.ACInverterVO> acInverters = converterVO.getAcInverters();

        String acConverterCode = acConverterVO.getConverterCode();
        int acConverterResult = deviceQuery.insertACConverterData(acConverterVO);

        if (acConverterResult > 0) {
            backupFile.backupData("device", acConverterCode, acConverterVO);

            if (!acInverters.isEmpty()) {
                int acInverterResult = deviceQuery.insertACInverterData(acInverters);

                if (acInverterResult > 0) {
                    complete = true;
                    new BackupFile().backupData("component", acConverterCode, acInverters);
                }
            }
        }

        return complete;
    }

    private boolean insertDCConverterData(ConverterVO converterVO) {
        boolean complete = false;

        ConverterVO.DCConverterVO dcConverterVO = converterVO.getDcConverter();
        List<ConverterVO.DCInverterVO> dcInverters = converterVO.getDcInverters();

        String dcConverterCode = dcConverterVO.getConverterCode();
        int dcConverterResult = deviceQuery.insertDCConverterData(dcConverterVO);

        if (dcConverterResult > 0) {
            backupFile.backupData("device", dcConverterCode, dcConverterVO);

            if (!dcInverters.isEmpty()) {
                int dcInverterResult = deviceQuery.insertDCInverterData(dcInverters);

                if (dcInverterResult > 0) {
                    complete = true;
                    new BackupFile().backupData("component", dcConverterCode, dcInverters);
                }
            }
        }

        return complete;
    }

    /**
     * 제어 요청 여부 확인
     *
     * @return 제어 요청 여부
     */
    public boolean isControlRequest() {
        return controlRequests.size() > 0;
    }

    /**
     * 제어 요청 정보 생성
     *
     * @param requestVO 제어 요청 정보
     */
    public void setControlRequest(ControlRequestVO requestVO) {
        //controlRequest = requestVO;
        //controlRequestMap.put(requestVO.getControlCode(), requestVO);
        controlRequests.add(requestVO);
    }

    /**
     * 제어 실행
     *
     * @return 제어 응답 정보
     */
    public ControlResponseVO control() {
        System.out.println("RequestVO : " + controlRequests);
        System.out.println("RequestVO 1 : " + controlRequests.get(0));

        ConverterWriter converterWriter = new ConverterWriter();
        converterWriter.setRequest(connection, controlRequests.get(0));
        converterWriter.request();

        ControlResponseVO responseVO = converterWriter.getResponse();
        ControlHistoryVO controlHistoryVO = responseVO.getHistoryVO();
        System.out.println("Response Request : " + responseVO.getRequestVO());
        controlRequests.remove(responseVO.getRequestVO());

        System.out.println("RequestVO 1 : " + controlRequests.get(0));
        System.out.println("RequestVO 2 : " + controlRequests.get(1));
        System.out.println("Control request : " + controlRequests);

        return responseVO;
    }
}
