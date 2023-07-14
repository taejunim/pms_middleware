package pms.communication.device.mobile.ioboard;

import jssc.SerialPort;
import pms.common.util.DateTimeUtil;
import pms.system.PMSCode;
import pms.vo.device.AirConditionerVO;
import pms.vo.device.SensorVO;
import pms.vo.device.error.DeviceErrorVO;

import java.util.*;

/**
 * IOBoardReader Class
 */
public class IOBoardReader {
    private IOBoardCommunication ioBoardCommunication = new IOBoardCommunication();
    private final Map<String, SensorVO> sensorDataMap = new HashMap<>(); //전체 센서 데이터
    private final Map<String, AirConditionerVO> airConditionerDataMap = new HashMap<>(); //공조 장치 데이터
    private final List<DeviceErrorVO> inputDeviceErrorsData = new ArrayList<>();  //에러 데이터
    private final List<DeviceErrorVO> outputDeviceErrorsData = new ArrayList<>();  //에러 데이터


    /**
     * Set Connection
     *
     * @param serialPort
     */
    public void setConnection(SerialPort serialPort) {
        ioBoardCommunication.setConnection(serialPort);
    }

    /**
     * Request
     *
     * @return regDate - 요청 시간 (UnixTime)
     */
    public int request() {
        int regDate = DateTimeUtil.getUnixTimestamp();

        int[] inputData = getInputReadData();    //보드 Input 포트 정보 호출
        int[] outputData = getOutputReadData();   //보드 Output 포트 정보 호출

        //!!! TEST Log
//        System.err.print("inputData: ");
//        for (int i : inputData) {
//            System.err.print((char) i);
//        }
//        System.out.println("");
//        System.err.print("outputData: ");
//        for (int j : outputData) {
//            System.err.print((char) j);
//        }


        if (inputData != null) {
            for (Map.Entry<Integer, String> entry : IOBoardClient.inputDeviceCodeMap.entrySet()) {
                if (entry.getValue().substring(0, 2).equals("04")) {    //04 - 센서
                    setSensorsData(entry.getValue(), PMSCode.getCommonCode("DEVICE_STATUS_10"), inputData[entry.getKey()], regDate);
                } else if (entry.getValue().substring(0, 2).equals("80")) { //80 - 공조
                    setAirConditionersData(entry.getValue(), getAirConditionerStatusCode(inputData[entry.getKey()]), regDate);
                }
            }
        } else {
            setInputReadDataByError(PMSCode.getCommonCode("DEVICE_STATUS_96"), "01008", regDate);
        }

        if (outputData != null) {
            for (Map.Entry<Integer, String> entry : IOBoardClient.outputDeviceCodeMap.entrySet()) {
                if (entry.getValue().substring(0, 2).equals("04")) {    //04 - 센서.getValue().charAt(0))]);
                    setSensorsData(entry.getValue(), PMSCode.getCommonCode("DEVICE_STATUS_10"), outputData[entry.getKey()], regDate);
                } else if (entry.getValue().substring(0, 2).equals("80")) { //80 - 공조
                    setAirConditionersData(entry.getValue(), getAirConditionerStatusCode(outputData[entry.getKey()]), regDate);
                }
            }
        } else {
            setOutputReadDataByError(PMSCode.getCommonCode("DEVICE_STATUS_96"), "01008", regDate);
        }

//        System.out.println(sensorDataMap);  //!!!
//        System.out.println(airConditionerDataMap);  //!!!


        return regDate;
    }

    /**
     * Get Sensor Data Map
     *
     * @return - Map<Key=String(DeviceCode), Value=SensorVO>
     */
    public Map<String, SensorVO> getSensorDataMap() {
        return sensorDataMap;
    }

    /**
     * Get AirConditioner Data Map
     *
     * @return - Map<Key=String(DeviceCode), Value=AirConditionerVO>
     */
    public Map<String, AirConditionerVO> getAirConditionerDataMap() {
        return airConditionerDataMap;
    }

    /**
     * Get InputDevice Errors Data
     * IOBoard Input 단자에서 발생하는 에러 데이터
     *
     * @return - List
     */
    public List<DeviceErrorVO> getInputDeviceErrorsData() {
        return inputDeviceErrorsData;
    }

    /**
     * Get OutputDevice Errors Data
     * IOBoard Output 단자에서 발생하는 에러 데이터
     *
     * @return - List
     */
    public List<DeviceErrorVO> getOutputDeviceErrorsData() {
        return outputDeviceErrorsData;
    }

    /**
     * Get Input Read Data
     * Input 단자 상태 값 Set Request String -> Get response
     *
     * @return - int[] responseData
     */
    private int[] getInputReadData() {
        ioBoardCommunication.sendRequest("?I88\r\n");
        int[] responseData = ioBoardCommunication.getResponseData();

        if (responseData != null) {
            responseData = Arrays.copyOfRange(responseData, 2, responseData.length - 2);
        }
        return responseData;
    }


    /**
     * Get Output Read Data
     * Output 단자 상태 값 Set Request String -> Get response
     *
     * @return - int[] responseData
     */
    private int[] getOutputReadData() {
        ioBoardCommunication.sendRequest("?O8E\r\n");
        int[] responseData = ioBoardCommunication.getResponseData();
        if (responseData != null) {
            responseData = Arrays.copyOfRange(responseData, 2, responseData.length - 2);
        }
        return responseData;
    }

    /**
     * Set Input Read Data By Error
     * 오류 발생시 Input 장비 상태 데이터 셋
     *
     * @param statusCode - 상태 코드
     * @param errCodeKey - 오류 코드 키
     * @param regDate    - UNIX Time
     */
    public void setInputReadDataByError(String statusCode, String errCodeKey, int regDate) {
        for (Map.Entry<Integer, String> entry : IOBoardClient.inputDeviceCodeMap.entrySet()) {
            if (entry.getValue().substring(0, 2).equals("04")) {    //04 - 센서
                setSensorsData(entry.getValue(), statusCode, null, regDate);
            } else if (entry.getValue().substring(0, 2).equals("80")) { //80 - 공조
                setAirConditionersData(entry.getValue(), statusCode, regDate);
            }
            setInputDeviceErrors(entry.getValue(), errCodeKey, regDate);
        }
    }

    /**
     * Set Output Read Data By Error
     * 오류 발생시 Output 장비 상태 데이터 셋
     *
     * @param statusCode - 상태 코드
     * @param errCodeKey - 오류 코드 키
     * @param regDate    - Unix Time
     */
    public void setOutputReadDataByError(String statusCode, String errCodeKey, int regDate) {
        for (Map.Entry<Integer, String> entry : IOBoardClient.outputDeviceCodeMap.entrySet()) {
            if (entry.getValue().substring(0, 2).equals("04")) {    //04 - 센서
                setSensorsData(entry.getValue(), statusCode, null, regDate);
            } else if (entry.getValue().substring(0, 2).equals("80")) { //80 - 공조
                setAirConditionersData(entry.getValue(), statusCode, regDate);
            }
            setOutputDeviceErrors(entry.getValue(), errCodeKey, regDate);
        }
    }

    /**
     * Set Sensors Data
     * 센서VO 장비코드별 Map 처리
     *
     * @param sensorCode - 센서 장비 코드
     * @param statusCode - 상태 코드
     * @param data       - 데이터
     * @param regDate    - Unix Time
     */
    private void setSensorsData(String sensorCode, String statusCode, Integer data, int regDate) {
        SensorVO sensorVO = new SensorVO();
        sensorVO.setRegDate(regDate);
        sensorVO.setSensorCode(sensorCode);
        sensorVO.setSensorStatus(statusCode);

        if (data != null) {
            sensorVO.setMeasure1(String.valueOf((char) data.intValue()));
            sensorVO.setWarningFlag(PMSCode.getCommonCode("OCCUR_FLAG_N"));
            sensorVO.setFaultFlag(PMSCode.getCommonCode("OCCUR_FLAG_N"));
        } else {
            sensorVO.setWarningFlag(PMSCode.getCommonCode("OCCUR_FLAG_Y"));
            sensorVO.setFaultFlag(PMSCode.getCommonCode("OCCUR_FLAG_Y"));
        }

        sensorDataMap.put(sensorCode, sensorVO);
    }

    /**
     * Set AirConditioners Data
     * 공조장치VO 장비코드별 Map처리
     *
     * @param airConditionersCode
     * @param statusCode
     * @param regDate
     */
    private void setAirConditionersData(String airConditionersCode, String statusCode, int regDate) {
        AirConditionerVO airConditionerVO = new AirConditionerVO();
        airConditionerVO.setRegDate(regDate);
        airConditionerVO.setAirConditionerCode(airConditionersCode);
        airConditionerVO.setOperationStatus(statusCode);

        if (statusCode.equals(PMSCode.getCommonCode("DEVICE_STATUS_03")) || statusCode.equals(PMSCode.getCommonCode("DEVICE_STATUS_04"))) {
            airConditionerVO.setWarningFlag(PMSCode.getCommonCode("OCCUR_FLAG_N"));
            airConditionerVO.setFaultFlag(PMSCode.getCommonCode("OCCUR_FLAG_N"));
        } else {
            airConditionerVO.setWarningFlag(PMSCode.getCommonCode("OCCUR_FLAG_Y"));
            airConditionerVO.setFaultFlag(PMSCode.getCommonCode("OCCUR_FLAG_Y"));
        }
        airConditionerDataMap.put(airConditionersCode, airConditionerVO);
    }

    /**
     * Set Input Device Errors
     * Input 장비별 DeviceErrorVO 생성 후 List 처리
     *
     * @param deviceCode - 장비 코드
     * @param errCodeKey - 오류 키
     * @param regDate    - Unix Time
     */
    private void setInputDeviceErrors(String deviceCode, String errCodeKey, int regDate) {
        DeviceErrorVO deviceErrorVO = new DeviceErrorVO();
        deviceErrorVO.setErrorDate(regDate);
        deviceErrorVO.setDeviceCode(deviceCode);
        deviceErrorVO.setErrorCode(PMSCode.getCommonErrorCode(errCodeKey));
        inputDeviceErrorsData.add(deviceErrorVO);
    }

    /**
     * Set Output Device Errors
     * Output 장비별 DeviceErrorVO 생성 후 List 처리
     *
     * @param deviceCode - 장비 코드
     * @param errCodeKey - 오류 키
     * @param regDate    - Unix Time
     */
    private void setOutputDeviceErrors(String deviceCode, String errCodeKey, int regDate) {
        DeviceErrorVO deviceErrorVO = new DeviceErrorVO();
        deviceErrorVO.setErrorDate(regDate);
        deviceErrorVO.setDeviceCode(deviceCode);
        deviceErrorVO.setErrorCode(PMSCode.getCommonErrorCode(errCodeKey));
        outputDeviceErrorsData.add(deviceErrorVO);
    }

    /**
     * Get AirConditioner Status Code
     * 공조장치 Response 값에 따른 공조장치 상태코드 값 반환
     *
     * @param data - Response 데이터 값
     * @return 공통코드(DEVICE_STATUS_03 or 04)
     */
    private String getAirConditionerStatusCode(Integer data) {
        String operationStatus;
        if ((char) data.intValue() == '0') {
            operationStatus = PMSCode.getCommonCode("DEVICE_STATUS_03"); //off
        } else {
            operationStatus = PMSCode.getCommonCode("DEVICE_STATUS_04"); //on
        }
        return operationStatus;
    }
}
