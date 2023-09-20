package pms.communication.device.bms;

import com.ghgande.j2mod.modbus.io.ModbusRTUTCPTransport;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import org.quartz.SchedulerException;
import pms.database.query.ControlQuery;
import pms.database.query.DeviceErrorQuery;
import pms.database.query.DeviceQuery;
import pms.scheduler.device.bms.BMSScheduler;
import pms.system.PMSCode;
import pms.system.backup.BackupFile;
import pms.vo.device.BmsVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.device.error.ComponentErrorVO;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.device.error.DeviceErrorsVO;
import pms.vo.history.ControlHistoryVO;
import pms.vo.system.DeviceVO;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.*;

import static pms.communication.CommunicationManager.deviceProperties;

/**
 * BMS Client
 * <p>
 * - BMS 통신 클라이언트
 */
public class BMSClient {
    private final BMSScheduler bmsScheduler = new BMSScheduler();   //BMS 스케줄러
    private static final Map<String, TCPMasterConnection> connections = new HashMap<>();   //BMS 통신 연결 정보 목록
    private static final Map<String, DeviceVO> rackInfoMap = new HashMap<>();   //Rack 장비 정보
    private static Map<String, List<BmsVO.RequestItem>> requestItemsMap = new HashMap<>();   //수신 요청 아이템 Map
    private static final Map<String, List<String>> previousErrorCodesMap = new HashMap<>(); //Rack 별 이전 오류 코드 목록 Map
    private static final Map<String, List<String>> previousCommonErrorCodesMap = new HashMap<>();
    private static final Map<String, Integer> previousRegDateMap = new HashMap<>(); //Rack 별 데이터 등록 일시 Map
    private static final Map<String, ControlRequestVO> controlRequestMap = new HashMap<>(); //Rack 별 제어 요청 Map

    /**
     * BMS 통신 설정 및 실행
     *
     * @param rackCode Rack 코드
     */
    public void execute(String rackCode, DeviceVO rackInfo) {
        setConnection(rackCode, rackInfo);

        try {
            connect(rackCode);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            requestItemsMap = new BMSReadItem(rackCode).getRequestItems();

            executeScheduler(rackCode);
        }
    }

    /**
     * Rack 장비 정보 호출
     *
     * @param rackCode Rack 코드
     * @return 장비 정보
     */
    public DeviceVO getRackInfo(String rackCode) {
        return rackInfoMap.get(rackCode);
    }

    /**
     * Rack 통신 연결 여부
     *
     * @param rackCode Rack 코드
     * @return Rack 통신 연결 여부
     */
    public boolean isConnected(String rackCode) {
        return connections.get(rackCode).isConnected();
    }

    /**
     * Rack 통신 연결
     *
     * @param rackCode Rack Code
     * @throws Exception Exception
     */
    public void connect(String rackCode) throws Exception {
        //setConnection(rackCode, getRackNo(rackCode));
        connections.get(rackCode).connect();
    }

    /**
     * Rack 통신 연결 해제
     *
     * @param rackCode Rack 코드
     */
    public void disconnect(String rackCode) {
        connections.get(rackCode).close();
    }

    /**
     * Rack 통신 연결 정보 호출
     *
     * @param rackCode Rack 코드
     * @return TCPMasterConnection
     */
    private TCPMasterConnection getConnection(String rackCode) {
        return connections.get(rackCode);
    }

    /**
     * Rack 통신 연결 정보 설정
     *
     * @param rackCode Rack 코드
     */
    public void setConnection(String rackCode, DeviceVO rackVO) {
        rackInfoMap.put(rackCode, rackVO);

        String rackKey = "rack-" + getRackInfo(rackCode).getDeviceNo();
        String host = deviceProperties.getProperty(rackKey + ".host");
        String port = deviceProperties.getProperty(rackKey + ".port");

        try {
            TCPMasterConnection connection = new TCPMasterConnection(InetAddress.getByName(host));
            connection.setPort(Integer.parseInt(port));

            ModbusRTUTCPTransport transport = new ModbusRTUTCPTransport();
            connection.setModbusTransport(transport);
            connection.setTimeout(1000);

            connections.put(rackCode, connection);    //연결 정보 추가
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Rack 통신 스케줄러 실행
     *
     * @param rackCode Rack 코드
     */
    private void executeScheduler(String rackCode) {
        try {
            bmsScheduler.execute(rackCode);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Rack 별 수신 데이터 설정 및 처리
     *
     * @param rackCode Rack 코드
     * @return Rack, Module 수신 데이터
     */
    public BmsVO read(String rackCode) {
        BMSReader bmsReader = new BMSReader(getRackInfo(rackCode));
        bmsReader.setRequest(getConnection(rackCode), requestItemsMap);
        bmsReader.request();    //데이터 수신 요청

        BmsVO bmsVO = bmsReader.getReadData();  //Rack, Module 수신 데이터 호출
        DeviceErrorsVO errorsVO = bmsReader.getErrorData(); //오류 데이터 호출

        processData(rackCode, bmsVO, errorsVO); //호출한 데이터 처리

        return bmsVO;
    }

    /**
     * 통신 오류 발생 시, 데이터 설정 및 처리
     *
     * @param rackCode Rack 코드
     * @return 통신 오류 발생 시, 설정한 데이터
     * @throws SQLException SQLException
     */
    public BmsVO readByError(String rackCode) throws SQLException {
        BMSReader bmsReader = new BMSReader(getRackInfo(rackCode));
        bmsReader.setReadDataByError(PMSCode.getDeviceStatus("09"), "01007");    //연결 오류 처리

        BmsVO bmsVO = bmsReader.getReadData();
        DeviceErrorsVO errorsVO = bmsReader.getErrorData();

        processData(rackCode, bmsVO, errorsVO);

        return bmsVO;
    }

    /**
     * 오류 코드 목록 호출
     *
     * @param rackCode Rack 코드
     * @return 오류 코드 목록
     */
    public List<String> getErrorCodes(String rackCode) {
        return previousErrorCodesMap.get(rackCode);
    }

    /**
     * 수신 데이터 처리
     *
     * @param rackCode Rack 코드
     * @param bmsVO    BMS 데이터 정보
     * @param errorsVO 오류 데이터 정보
     */
    private void processData(String rackCode, BmsVO bmsVO, DeviceErrorsVO errorsVO) {
        int currentRegDate = bmsVO.getRack().getRegDate();

        //이전 등록 일시와 현재 등록 일시 확인 - 중복 키 오류 방지
        if (!containsRegDate(rackCode, currentRegDate)) {
            boolean isInsertData = insertData(bmsVO);   //Rack 정보 등록

            if (isInsertData) {
                //장비 오류 발생 시 실행
                if (bmsVO.isError()) {
                    List<String> currentErrorCodes = setCurrentErrorCodes(errorsVO);    //현재 오류 코드 목록 설정

                    //이전 오류 코드와 현재 오류 코드 비교 후 데이터 처리
                    if (!containsErrors(rackCode, currentErrorCodes)) {
                        boolean isInsertError = insertErrorData(errorsVO);

                        if (isInsertError) {
                            previousErrorCodesMap.replace(rackCode, currentErrorCodes); //이전 오류 코드 목록 갱신
                        }
                    }
                } else {
                    previousErrorCodesMap.remove(rackCode); //이전 오류 코드 목록 제거
                }

                previousRegDateMap.replace(rackCode, currentRegDate);   //이전 등록 일시 갱신
            }
        }
    }

    /**
     * 수신 데이터 등록
     * <p>
     * - Rack 데이터 등록 후, Module 데이터 등록
     *
     * @param bmsVO 수신 BMS 데이터 정보
     */
    private boolean insertData(BmsVO bmsVO) {
        boolean complete = false;
        int rackResult;
        int moduleResult;

        BmsVO.RackVO rackVO = bmsVO.getRack();
        List<BmsVO.ModuleVO> modules = bmsVO.getModules();

        DeviceQuery deviceQuery = new DeviceQuery();
        rackResult = deviceQuery.insertRackData(rackVO);

        if (rackResult > 0) {
            new BackupFile().backupData("device", rackVO.getRackCode(), rackVO);

            if (!modules.isEmpty()) {
                moduleResult = deviceQuery.insertModuleData(modules);

                if (moduleResult > 0) {
                    complete = true;
                    new BackupFile().backupData("component", rackVO.getRackCode(), modules);
                }
            } else {
                complete = true;
            }
        }

        return complete;
    }

    /**
     * 오류 데이터 등록
     * <p>
     * Rack 오류 등록 후, Module 오류 등록
     *
     * @param errors 오류 정보 목록
     * @return 등록 결과
     */
    private boolean insertErrorData(DeviceErrorsVO errors) {
        boolean complete = false;
        int rackResult;
        int moduleResult;

        List<DeviceErrorVO> deviceErrors = errors.getDeviceErrors();
        List<ComponentErrorVO> componentErrors = errors.getComponentErrors();

        DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();
        rackResult = deviceErrorQuery.insertDeviceErrors(deviceErrors);

        if (rackResult > 0) {
            new BackupFile().backupData("device-error", deviceErrors.get(0).getDeviceCode(), deviceErrors);

            if (!componentErrors.isEmpty()) {
                moduleResult = deviceErrorQuery.insertComponentErrors(componentErrors);

                if (moduleResult > 0) {
                    new BackupFile().backupData("component-error", componentErrors.get(0).getDeviceCode(), componentErrors);
                    complete = true;
                }
            } else {
                complete = true;
            }
        }

        return complete;
    }

    /**
     * 현재 오류 정보를 오류 코드 목록으로 생성
     *
     * @param currentErrors 현재 오류 정보
     * @return 현재 오류 코드 목록
     */
    private List<String> setCurrentErrorCodes(DeviceErrorsVO currentErrors) {
        List<DeviceErrorVO> currentRackErrors = currentErrors.getDeviceErrors();    //Rack 오류 정보 목록
        List<ComponentErrorVO> currentModuleErrors = currentErrors.getComponentErrors();    //Module 오류 정보 목록

        List<String> currentErrorCodes = new ArrayList<>();

        for (DeviceErrorVO rackError : currentRackErrors) {
            String errorCode = rackError.getErrorCode();
            currentErrorCodes.add(errorCode);
        }

        for (ComponentErrorVO moduleError : currentModuleErrors) {
            String errorCode = moduleError.getErrorCode();
            currentErrorCodes.add(errorCode);
        }

        return currentErrorCodes;
    }

    /**
     * 등록 일시 동일 여부 확인
     * <p>
     * - 이전 등록 일시와 현재 등록 일시가 동일한지 확인
     *
     * @param rackCode       Rack 코드
     * @param currentRegDate 현재 등록 일시
     * @return 일시 동일 여부
     */
    private boolean containsRegDate(String rackCode, int currentRegDate) {
        if (previousRegDateMap.containsKey(rackCode)) {
            return previousRegDateMap.get(rackCode) == currentRegDate;
        } else {
            previousRegDateMap.put(rackCode, currentRegDate);
            return false;
        }
    }

    /**
     * 오류 코드 동일 여부 확인
     * <p>
     * - 이전 오류와 현재 오류가 동일한지 확인
     *
     * @param rackCode          Rack 코드
     * @param currentErrorCodes 현재 오류 코드 목록
     * @return 오류 동일 여부
     */
    private boolean containsErrors(String rackCode, List<String> currentErrorCodes) {
        if (previousErrorCodesMap.containsKey(rackCode)) {
            System.out.println("Rack-" + rackCode + " 이전 오류 코드 : " + previousErrorCodesMap.get(rackCode) + " / 현재 오류 코드 : " + currentErrorCodes);
            return new HashSet<>(previousErrorCodesMap.get(rackCode)).containsAll(currentErrorCodes);
        } else {
            previousErrorCodesMap.put(rackCode, currentErrorCodes);
            return false;
        }
    }

    /**
     * 제어 실행
     *
     * @param rackCode Rack 코드
     * @return ControlResponseVO - 제어 응답 정보
     */
    public ControlResponseVO control(String rackCode) {
        int rackNo = getRackInfo(rackCode).getDeviceNo();   //Rack 번호 - 통신 Unit ID
        //ControlRequestVO requestVO = getControlRequest(rackCode);   //제어 요청 정보
        //RequestVO requestVONew = ControlUtil.setControlRequestVO()
        ControlRequestVO requestVO = controlRequestMap.get(rackCode);

        BMSWriter bmsWriter = new BMSWriter();
        bmsWriter.setRequest(rackCode, rackNo, getConnection(rackCode), requestVO);
        bmsWriter.request();    //제어 요청

        ControlResponseVO responseVO = bmsWriter.getResponse(); //제어 응답 정보

        //수정 필요
        ControlHistoryVO controlHistoryVO = responseVO.getHistoryVO();
        insertControlHistory(controlHistoryVO);

        controlRequestMap.remove(rackCode); //제어 완료 후, 제어 요청 정보 제거

        return responseVO;
    }

    private void insertControlHistory(ControlHistoryVO controlHistoryVO) {
        ControlQuery controlQuery = new ControlQuery();
        int result = controlQuery.insertControlHistory(controlHistoryVO);

        if (result > 0) {
            new BackupFile().backupData("control", null, controlHistoryVO);
        }
    }

    /**
     * 제어 요청 여부 확인
     *
     * @param rackCode Rack 코드
     * @return 제어 요청 여부
     */
    public boolean isControlRequest(String rackCode) {
        return controlRequestMap.containsKey(rackCode);
    }

    public void setControlRequestMap(String rackCode, ControlRequestVO requestVO) {
        controlRequestMap.put(rackCode, requestVO);
    }

    /**
     * 제어 요청 정보 호출
     *
     * @param rackCode Rack 코드
     * @return 제어 요청 정보
     */
    /*private ControlRequestVO getControlRequest(String rackCode) {
        return controlRequestMap.get(rackCode);
    }*/

    /**
     * 제어 요청 정보 설정
     *
     * @param rackCode    Rack 코드
     * @param requestType 요청 구분
     * @param remoteId    원격 ID - 원격 제어 요청 시 사용
     * @param controlVO   제어 정보
     */
    /*public void setControlRequest(String rackCode, String requestType, String remoteId, DeviceVO.ControlVO controlVO) {
        String requestTypeCode = PMSCode.getCommonCode("CONTROL_REQUEST_TYPE_" + requestType);

        ControlRequestVO requestVO = new ControlRequestVO();
        requestVO.setRequestType(requestTypeCode);
        requestVO.setRemoteId(remoteId);
        requestVO.setRequestDate(DateTimeUtil.getUnixTimestamp());
        requestVO.setControlVO(controlVO);

        controlRequestMap.put(rackCode, requestVO);
    }*/
}
