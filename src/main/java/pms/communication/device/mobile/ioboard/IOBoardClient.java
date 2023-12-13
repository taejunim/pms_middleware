package pms.communication.device.mobile.ioboard;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.quartz.SchedulerException;
import pms.common.util.DateTimeUtil;
import pms.database.query.ControlQuery;
import pms.database.query.DeviceErrorQuery;
import pms.database.query.DeviceQuery;
import pms.scheduler.device.mobile.IOBoardScheduler;
import pms.system.PMSCode;
import pms.system.backup.BackupFile;
import pms.vo.device.AirConditionerVO;
import pms.vo.device.SensorVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.history.ControlHistoryVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.util.*;

import static pms.system.PMSManager.applicationProperties;

public class IOBoardClient {
    private final IOBoardScheduler ioBoardScheduler = new IOBoardScheduler();
    private static SerialPort serialPort;   //시리얼정보
    private static ControlRequestVO controlRequest = null;
    public static HashMap<Integer, String> inputDeviceCodeMap = new HashMap<>();   // IndexNum : DeviceCode (Key : Value)
    public static HashMap<Integer, String> outputDeviceCodeMap = new HashMap<>();  // IndexNum : DeviceCode (Key : Value)
    private static final Map<String, DeviceVO> sensorDeviceVosMap = new HashMap<>();
    private static final Map<String, DeviceVO> airConditionerDeviceVosMap = new HashMap<>();
    private static Map<String, SensorVO> sensorDataMap = new HashMap<>();
    private static Map<String, AirConditionerVO> airConditionerDataMap = new HashMap<>();
    private static Map<String, DeviceErrorVO> sensorErrorsMap = new HashMap<>();
    private static Map<String, DeviceErrorVO> airConditionerErrorsMap = new HashMap<>();
    private static int previousDBInsertRegDate = 0;
    private static int previousRegDate = 0;
    private static boolean fanPower = false;        // 흡배기 팬 전원 On 상태 여부

    private static Map<String, DeviceErrorVO> previousSensorErrorsMap = new HashMap<>();
    private static Map<String, DeviceErrorVO> previousAirConditionerErrorsMap = new HashMap<>();
    private static Map<String, SensorVO> previousSensorDataMap = new HashMap<>();
    private static Map<String, AirConditionerVO> previousAirConditionerDataMap = new HashMap<>();

    /**
     * IOBoard Execute
     */
    public void execute() {
        setInfo();

        try {
            connect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executeScheduler();
        }
    }


    /**
     * Execute IOBoard scheduler
     */
    private void executeScheduler() {
        try {
            ioBoardScheduler.execute();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connect IOBoard port
     *
     * @throws Exception
     */
    public void connect() throws Exception {
        serialPort = new SerialPort(applicationProperties.getProperty("io-board.port"));
        serialPort.openPort();
        serialPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
    }


    /**
     * Disconnect IOBoard port
     */
    public void disconnect() {
        try {
            if (serialPort.isOpened()) {
                serialPort.closePort();
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check IOBoard Connection
     *
     * @return true - Open
     * false - Close
     */
    public boolean isConnected() {
        return serialPort.isOpened();
    }

    /**
     * Read IOBoard  - 전체 디바이스 Raw data
     */
    public void read() {
        IOBoardReader ioBoardReader = new IOBoardReader();
        ioBoardReader.setConnection(serialPort);
        int regDate = ioBoardReader.request();

        sensorDataMap = ioBoardReader.getSensorDataMap();
        airConditionerDataMap = ioBoardReader.getAirConditionerDataMap();
        setErrorsDataMaps(ioBoardReader.getInputDeviceErrorsData(), ioBoardReader.getOutputDeviceErrorsData());

        processData(regDate);
    }

    /**
     * 전체 장비 Raw 데이터 처리
     * 1. 60초 간격으로 장비 상태정보 DB 등록
     * 2. 이전 이력 비교/처리 및 DB Insert 실행단
     *
     * @param regDate - 실행일자(UNIX TIME)
     */
    private void processData(int regDate) {
        int currentRegDate = regDate;
        boolean dbInsertFlag = false;
        boolean insertFaultFlag = false;

        try {
            if (!containsRegDate(currentRegDate)) {
                if ((previousDBInsertRegDate + 60) > currentRegDate) {      //DB 입력 간격 - 60초
                    for (Map.Entry<String, SensorVO> entry : sensorDataMap.entrySet()) {
                        if (!previousSensorDataMap.get(entry.getKey()).getSensorStatus().equals(entry.getValue().getSensorStatus())) {
                            dbInsertFlag = true;
                            break;
                        } else if (previousSensorDataMap.get(entry.getKey()).getMeasure1() != null) {
                            if (!previousSensorDataMap.get(entry.getKey()).getMeasure1().equals(entry.getValue().getMeasure1()))
                                dbInsertFlag = true;
                            break;
                        }
                    }
                    if (!dbInsertFlag) {
                        for (Map.Entry<String, AirConditionerVO> entry : airConditionerDataMap.entrySet()) {
                            if (!previousAirConditionerDataMap.get(entry.getKey()).getOperationStatus().equals(entry.getValue().getOperationStatus())) {
                                dbInsertFlag = true;
                                break;
                            }
                        }
                    }
                    if (dbInsertFlag) {
                        if (insertSensorsDataData(sensorDataMap) && insertAirConditionersData(airConditionerDataMap)) {
                            insertFaultFlag = true;
                        }
                    }
                } else {
                    if (insertSensorsDataData(sensorDataMap) && insertAirConditionersData(airConditionerDataMap)) {
                        insertFaultFlag = true;
                    }
                }

                if (insertFaultFlag) {
                    previousDBInsertRegDate = currentRegDate;
                    previousSensorDataMap = sensorDataMap;
                    previousAirConditionerDataMap = airConditionerDataMap;

                    if (sensorErrorsMap.size() > 0) {
                        if (!containErrors(sensorErrorsMap, previousSensorErrorsMap)) {
                            if (insertErrorData(sensorErrorsMap)) {
                                previousSensorErrorsMap = sensorErrorsMap;
                            }
                        }
                    } else {
                        previousSensorErrorsMap = new HashMap<>();
                    }

                    if (airConditionerErrorsMap.size() > 0) {
                        if (!containErrors(airConditionerErrorsMap, previousAirConditionerErrorsMap)) {
                            if (insertErrorData(airConditionerErrorsMap)) {
                                previousAirConditionerErrorsMap = airConditionerErrorsMap;
                            }
                        }
                    } else {
                        previousAirConditionerErrorsMap = new HashMap<>();
                    }
                }
                previousRegDate = currentRegDate;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 현재 장비별 오류 이력과 이전 장비별 오류 포함 여부
     * Map(String, DeviceErrorVO) -> Map(장비코드, 장비별 ErrorVO)
     *
     * @param errorVOMap         - 현재 ErrorVO
     * @param previousErrorVOMap - 이전 ErrorVO
     * @return Boolean
     */
    private boolean containErrors(Map<String, DeviceErrorVO> errorVOMap, Map<String, DeviceErrorVO> previousErrorVOMap) {
        boolean result = true;
        if (errorVOMap.size() != previousErrorVOMap.size()) {
            result = false;
        } else {
            for (Map.Entry<String, DeviceErrorVO> entry : errorVOMap.entrySet()) {
                if (!entry.getValue().getErrorCode().equals(previousErrorVOMap.get(entry.getKey()).getErrorCode())) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 전체 장비 오류 데이터 DB 등록
     * Map(String, DeviceErrorVO) -> Map(장비코드, 장비별 ErrorVO)
     *
     * @param errorDataMap - 전체 장비 ErrorDataMap
     * @return Boolean
     */
    private boolean insertErrorData(Map<String, DeviceErrorVO> errorDataMap) {
        List<DeviceErrorVO> errorVOList = new ArrayList<>();

        for (Map.Entry<String, DeviceErrorVO> entry : errorDataMap.entrySet()) {
            errorVOList.add(entry.getValue());
        }

        DeviceErrorQuery deviceErrorQuery = new DeviceErrorQuery();
        int result = deviceErrorQuery.insertDeviceErrors(errorVOList);

        //!!! 파일 백업
        if (result > 0) {
            for (Map.Entry<String, DeviceErrorVO> entry : errorDataMap.entrySet()) {
                new BackupFile().backupData("device-error", entry.getValue().getDeviceCode(), entry.getValue());
            }
        }
        return result > 0;
    }

    /**
     * 센서 상태정보 DB 등록
     *
     * @param sensorDataMap - Map(장비(센서)코드, 센서데이터 VO)
     * @return 쿼리문 실행 성공 카운트
     */
    private boolean insertSensorsDataData(Map<String, SensorVO> sensorDataMap) {
        List<SensorVO> sensorVOList = new ArrayList<>();
        for (Map.Entry<String, SensorVO> entry : sensorDataMap.entrySet()) {
            sensorVOList.add(entry.getValue());
        }

        DeviceQuery deviceQuery = new DeviceQuery();
        int result = deviceQuery.insertSensorsData(sensorVOList);

        //!!! 파일 백업
        if (result > 0) {
            for (Map.Entry<String, SensorVO> entry : sensorDataMap.entrySet()) {
                new BackupFile().backupData("device", entry.getValue().getSensorCode(), entry.getValue());
            }
        }
        return result > 0;
    }

    /**
     * 공조장치 상태정보 DB 등록
     *
     * @param airConditionerDataMap - Map(장비(공조장치)코드, 공조장치 데이터 VO)
     * @return 쿼리문 실행 성공 카운트
     */
    private boolean insertAirConditionersData(Map<String, AirConditionerVO> airConditionerDataMap) {
        List<AirConditionerVO> airConditionerVOList = new ArrayList<>();
        for (Map.Entry<String, AirConditionerVO> entry : airConditionerDataMap.entrySet()) {
            airConditionerVOList.add(entry.getValue());
        }

        DeviceQuery deviceQuery = new DeviceQuery();
        int result = deviceQuery.insertAirConditionersData(airConditionerVOList);

        //!!! 파일 백업
        if (result > 0) {
            for (Map.Entry<String, AirConditionerVO> entry : airConditionerDataMap.entrySet()) {
                new BackupFile().backupData("device", entry.getValue().getAirConditionerCode(), entry.getValue());
            }
        }
        return result > 0;
    }

    /**
     * 이전 실행 RegDate 비교
     *
     * @param currentRegDate - 현재 일시(UNIX TIME)
     * @return Boolean
     */
    private boolean containsRegDate(int currentRegDate) {
        return previousRegDate == currentRegDate;
    }

    /**
     * SetErrorsDataMaps
     *
     * @param inputErrorsData
     * @param outputErrorsData
     */
    private void setErrorsDataMaps(List<DeviceErrorVO> inputErrorsData, List<DeviceErrorVO> outputErrorsData) {
        sensorErrorsMap = setSensorErrorsData(inputErrorsData, outputErrorsData);
        airConditionerErrorsMap = setAirConditionerErrorsData(inputErrorsData, outputErrorsData);
    }

    /**
     * Get Sensor Data Map
     *
     * @return - Map(장비(센서)코드, 센서데이터 VO)
     */
    public Map<String, SensorVO> getSensorDataMap() {
        return sensorDataMap;
    }

    /**
     * Get AirConditioner Data Map
     *
     * @return - Map(장비(공조장치)코드, 공조장치 데이터 VO)
     */
    public Map<String, AirConditionerVO> getAirConditionerDataMap() {
        return airConditionerDataMap;
    }

    /**
     * Get Sensor Error Data Map
     *
     * @return - Map(장비(센서)코드, 센서 오류데이터 VO)
     */
    public Map<String, DeviceErrorVO> getSensorErrorDataMap() {
        return sensorErrorsMap;
    }

    /**
     * Get AirConditionerError Data Map
     *
     * @return - Map(장비(공조장치)코드, 공조장치 오류데이터 VO)
     */
    public Map<String, DeviceErrorVO> getAirConditionerErrorDataMap() {
        return airConditionerErrorsMap;
    }

    /**
     * Set Sensor Error Data Map
     * IOBoard Input Data와 Output Data에서 센서 오류 데이터만 추출하여 Set
     *
     * @param inputErrorsData  - Input 포트 에러 데이터
     * @param outputErrorsData - Output 포트 에러 데이터
     * @return - Map(장비(센서)코드, 센서 오류데이터 VO)
     */
    private Map<String, DeviceErrorVO> setSensorErrorsData(List<DeviceErrorVO> inputErrorsData, List<DeviceErrorVO> outputErrorsData) {
        Map<String, DeviceErrorVO> sensorErrorsMap = new HashMap<>();
        for (DeviceErrorVO vo : inputErrorsData) {
            if (vo.getDeviceCode().startsWith("04")) {
                sensorErrorsMap.put(vo.getDeviceCode(), vo);
            }
        }

        for (DeviceErrorVO vo : outputErrorsData) {
            if (vo.getDeviceCode().startsWith("04")) {
                sensorErrorsMap.put(vo.getDeviceCode(), vo);
            }
        }
        return sensorErrorsMap;
    }

    /**
     * Set AirConditioner Error Data Map
     * IOBoard Input Data와 Output Data에서 공조장치 오류 데이터만 추출하여 Set
     *
     * @param inputErrorsData  - Input 포트 에러 데이터
     * @param outputErrorsData - Output 포트 에러 데이터
     * @return - Map(장비(공조장치)코드, 공조장치 오류데이터 VO)
     */
    private Map<String, DeviceErrorVO> setAirConditionerErrorsData(List<DeviceErrorVO> inputErrorsData, List<DeviceErrorVO> outputErrorsData) {
        Map<String, DeviceErrorVO> airConditionerErrorsMap = new HashMap<>();
        for (DeviceErrorVO vo : inputErrorsData) {
            if (vo.getDeviceCode().startsWith("80")) {
                airConditionerErrorsMap.put(vo.getDeviceCode(), vo);
            }
        }
        for (DeviceErrorVO vo : outputErrorsData) {
            if (vo.getDeviceCode().startsWith("80")) {
                airConditionerErrorsMap.put(vo.getDeviceCode(), vo);
            }
        }
        return airConditionerErrorsMap;
    }

    /**
     * 제어 처리
     * 1. 제어 요청
     * 2. 제어 응답값 처리
     * 3. 제어 이력 DB 등록
     *
     * @return - 제어 응답 VO
     */
    public ControlResponseVO control() {
        IOBoardWriter ioBoardWriter = new IOBoardWriter();
        ioBoardWriter.setConnection(serialPort);
        ioBoardWriter.request(controlRequest);
        ControlResponseVO responseVO = ioBoardWriter.getResponseVO();

        try {
            ControlHistoryVO controlHistoryVO = responseVO.getHistoryVO();
            ControlQuery controlQuery = new ControlQuery();
            controlQuery.insertControlHistory(controlHistoryVO);

            processWriteErrorData(controlHistoryVO);    // 제어 성공 여부에 따른 에러처리

            //!!!IO보드 제어 백업 - 디바이스 코드 맞게 들어가는지 확인 필요
            if (controlQuery.insertControlHistory(controlHistoryVO) > 0) {
                new BackupFile().backupData("control", responseVO.getRequestVO().getDeviceCode(), controlHistoryVO);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        controlRequest = null;

        return responseVO;
    }

    /**
     * 제어 에러 처리
     *
     * @param controlHistoryVO
     */
    private void processWriteErrorData(ControlHistoryVO controlHistoryVO) {
        if (controlHistoryVO.getControlCompleteFlag().equals("N")) {
            DeviceErrorVO errorVO = new DeviceErrorVO();
            errorVO.setDeviceCode(controlRequest.getDeviceCode());
            errorVO.setErrorDate(controlHistoryVO.getDeviceResponseDate());
            errorVO.setErrorCode(PMSCode.getCommonErrorCode("01009"));  // 01009: 송신오류

            Map<String, DeviceErrorVO> deviceErrorVOMap = new HashMap<>();
            deviceErrorVOMap.put(controlRequest.getDeviceCode(), errorVO);

            insertErrorData(deviceErrorVOMap);
        }
    }


    /**
     * 제어 요청 존재 유무 확인
     *
     * @return Boolean
     */
    public boolean isControlRequest() {
        return controlRequest != null;
    }

    /**
     * 요청 제어 값 전역 변수에 Set
     *
     * @param requestVO
     */
    public void setControlRequest(ControlRequestVO requestVO) {
        controlRequest = requestVO;
    }

    /**
     * 상태 오류 발생시 Read 처리
     */
    public void readByError() {
        IOBoardReader ioBoardReader = new IOBoardReader();
        int regDate = DateTimeUtil.getUnixTimestamp();
        ioBoardReader.setInputReadDataByError(PMSCode.getCommonCode("DEVICE_STATUS_09"), "01007", regDate);
        ioBoardReader.setOutputReadDataByError(PMSCode.getCommonCode("DEVICE_STATUS_09"), "01007", regDate);

        sensorDataMap = ioBoardReader.getSensorDataMap();
        airConditionerDataMap = ioBoardReader.getAirConditionerDataMap();
        setErrorsDataMaps(ioBoardReader.getInputDeviceErrorsData(), ioBoardReader.getOutputDeviceErrorsData());

        processData(regDate);
    }

    /**
     * 초기 IOBoard 연결된 장비 정보 설정.
     * 장비 주소 변경시 수정 필요함.
     */
    private void setInfo() {
        sensorDeviceVosMap.put("040401", PmsVO.sensors.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_0404")).get(0));
        sensorDeviceVosMap.put("040402", PmsVO.sensors.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_0404")).get(1));
//        sensorDeviceVosMap.put("040403", PmsVO.sensors.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_0404")).get(2));    //0404-도어-외부
        airConditionerDeviceVosMap.put("800201", PmsVO.airConditioners.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_8002")).get(0));
        airConditionerDeviceVosMap.put("800202", PmsVO.airConditioners.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_8002")).get(1));
        airConditionerDeviceVosMap.put("801101", PmsVO.airConditioners.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_8011")).get(0));
        airConditionerDeviceVosMap.put("801102", PmsVO.airConditioners.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_8011")).get(1));
        airConditionerDeviceVosMap.put("801201", PmsVO.airConditioners.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_8012")).get(0));
        airConditionerDeviceVosMap.put("801202", PmsVO.airConditioners.get(PMSCode.getCommonCode("DEVICE_CATEGORY_SUB_8012")).get(1));

        inputDeviceCodeMap.put(0, sensorDeviceVosMap.get("040401").getDeviceCode());    //0404-도어-배터리
        inputDeviceCodeMap.put(1, sensorDeviceVosMap.get("040402").getDeviceCode());    //0404-도어-PCS
//        inputDeviceCodeMap.put(2, sensorDeviceVosMap.get("040403").getDeviceCode());    //0404-도어-외부
        outputDeviceCodeMap.put(0, airConditionerDeviceVosMap.get("800201").getDeviceCode());    //800201-에어컨-배터리
        outputDeviceCodeMap.put(1, airConditionerDeviceVosMap.get("800202").getDeviceCode());    //800202-에어컨-PCS
        outputDeviceCodeMap.put(2, airConditionerDeviceVosMap.get("801101").getDeviceCode());    //801101-흡기-배터리
        outputDeviceCodeMap.put(3, airConditionerDeviceVosMap.get("801201").getDeviceCode());    //801201-배기-배터리
        outputDeviceCodeMap.put(4, airConditionerDeviceVosMap.get("801202").getDeviceCode());    //801202-배기-PCS
        outputDeviceCodeMap.put(5, airConditionerDeviceVosMap.get("801102").getDeviceCode());    //801102-흡기-PCS

        for (Map.Entry<String, DeviceVO> entry : sensorDeviceVosMap.entrySet()) {
            previousSensorDataMap.put(entry.getKey(), new SensorVO());
        }
        for (Map.Entry<String, DeviceVO> entry : airConditionerDeviceVosMap.entrySet()) {
            previousAirConditionerDataMap.put(entry.getKey(), new AirConditionerVO());
        }
    }

    /**
     * Get Sensor Device info map
     * 센서 정보
     *
     * @return - Map(장비(센서)코드, 센서 데이터 VO)
     */
    public Map<String, DeviceVO> getSensorDeviceVosMap() {
        return sensorDeviceVosMap;
    }

    /**
     * Get air-conditioner device info map
     * 공조장치 정보
     *
     * @return - Map(장비(공조장치)코드, 공조장치 데이터 VO)
     */
    public Map<String, DeviceVO> getAirConditionerDeviceVosMap() {
        return airConditionerDeviceVosMap;
    }

    /**
     * 전체 흡배기 팬 전원 ON.
     * 흡배기 맵핑 변경시 request 명령어(하드코딩) 수정 필요함.
     */
    public void powerOnFans() {
        IOBoardWriter ioBoardWriter = new IOBoardWriter();
        ioBoardWriter.setConnection(serialPort);
        ioBoardWriter.request("H0011110000");   // 명령어 프로토콜 참조

        fanPower = ioBoardWriter.getResult() == 1;

        //!!! IO보드 - 최초 시작시 흡/배기팬 전원 제어 백업 파일 부분 구현 필요
        /*ControlResponseVO responseVO = ioBoardWriter.getResponseVO();
        try {
            ControlHistoryVO controlHistoryVO = responseVO.getHistoryVO();
            ControlQuery controlQuery = new ControlQuery();
            controlQuery.insertControlHistory(controlHistoryVO);

            processWriteErrorData(controlHistoryVO);    // 제어 성공 여부에 따른 에러처리
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        controlRequest = null;
    }

    /**
     * getFanPowerState
     *
     * @return
     */
    public boolean getFanPowerState() {
        return fanPower;
    }

    /**
     * !!! Test Connect IOBoard - 테스트용
     * <p>
     * 수동 포트 입력 후 연결
     */
    private void testConnect() {
        String[] portNameList = SerialPortList.getPortNames();
        System.out.println("+++++++++++PORT LIST+++++++++++");
        for (String s : portNameList) {
            System.out.println(s);
        }
        System.out.println("+++++++++++++++++++++++++++++++");

        System.out.println("연결 포트명 입력: ");
        Scanner scanner = new Scanner(System.in);
        String portName = scanner.nextLine();

        serialPort = new SerialPort(portName);
        try {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    /**
     * IO 보드 테스트용 코드 실행부분
     */
    public void testControlExecute() {
        try {
            Boolean isOpened = false;
            while (!isOpened) {
                Thread.sleep(1000);
                testConnect();
                isOpened = serialPort.isOpened();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            while (serialPort.isOpened()) {
                ControlRequestVO requestVO = new ControlRequestVO();
                Scanner scanner;
                requestVO.setAddress(99);
                while (requestVO.getAddress() == 99) {
                    System.out.println("제어 기기 번호 입력: ");
                    scanner = new Scanner(System.in);
                    int address = scanner.nextInt();
                    if (0 < address && address < 11) {
                        requestVO.setAddress(address);
                    } else {
                        System.err.println("없는 장비!");
                    }
                }

                Boolean isOrderRetry = true;
                while (isOrderRetry) {
                    scanner = new Scanner(System.in);
                    System.out.println("ON/OFF? : ");
                    String text = scanner.nextLine();
                    if (text.equals("on") || text.equals("ON")) {
                        requestVO.setControlValue(1);
                        isOrderRetry = false;
                    } else if (text.equals("off") || text.equals("OFF")) {
                        requestVO.setControlValue(0);
                        isOrderRetry = false;
                    } else {
                        System.err.println("제어 명령어 오기입! 다시!");
                    }
                }
                System.out.println("--- 제어 시작: " + requestVO);
                IOBoardWriter ioBoardWriter = new IOBoardWriter();
                ioBoardWriter.setConnection(serialPort);
                ioBoardWriter.request(requestVO);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
