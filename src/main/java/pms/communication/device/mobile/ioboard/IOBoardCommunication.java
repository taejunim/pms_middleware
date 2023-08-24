package pms.communication.device.mobile.ioboard;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * IOBoard 데이터 통신(Request - Response) 사용되는 공통 기능
 */
public class IOBoardCommunication {
    private SerialPort serialPort;  //통신 정보
    private boolean isReading;  //읽기 진행 상태
    private int[] responseData; //통신 응답값
    private List<Integer> responseDataList = new ArrayList<>();

    /**
     * Set Connection
     *
     * @param serialPort
     */
    public void setConnection(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    /**
     * Send Request
     *
     * @param reqString
     */
    public void sendRequest(String reqString) {
        boolean foundStx = false;
        List<Integer> responseList = new ArrayList<>();
        Timer timer = startTimer(); //제한 시간 타이머

        try {
            serialPort.writeString(reqString);

            while (isReading) { //ETX 또는 제한 시간 이전까지 응답값 반복 읽기
                byte[] read = serialPort.readBytes();

                if (read != null && read.length > 0) {
                    for (byte rByte : read) {
                        char ascii = (char) rByte;
                        switch (ascii) {
                            case '!':       //STX: !(Data Write)
                            case '?':       //STX: ?(Data Read)
                            case '*':       //STX: *(Write Response)
                            case '>':       //STX: >(Read Response)
                            case '&':       //STX: &(Error)
                                foundStx = true;
                        }

                        if (foundStx) {
                            responseList.add((int) rByte & 0xff);     //-128~127 범위 표현을 0~255로 변환

                            if (foundStx && ascii == '\n') {    //ETX: \n\r
                                if (((char) responseList.get(responseList.size() - 2).intValue()) == '\r') {
                                    isReading = false;     //STX 와 ETX 찾음. 읽기 종료.
                                    closeTimer(timer);     //타이머 종료
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SerialPortException e) {
            isReading = false;
            closeTimer(timer);  //타이머 종료
            responseList = null;
            e.printStackTrace();
        }

        int[] responseData = toIntList(responseList);    //ArrayList -> int[]

        System.out.println("!!! 제어 응답값: ");
        for (int i : responseData) {
            System.out.print((char) i);     //!!! 제어 응답값
        }

        if (!checkCrc(responseData)) {        //CRC 확인
            responseData = null;
        } else if ((char) responseData[0] == '&') {   //응답 데이터 STX=='&' -> 에러
            responseData = null;
        }
        this.responseData = responseData;
    }

    /**
     * Get ResponseData
     *
     * @return
     */
    public int[] getResponseData() {
        return responseData;
    }

    /**
     * Response Data CRC 확인
     *
     * @return Boolean -isNormal
     */
    public boolean checkCrc(int[] dataList) {
        boolean isNormal = false;
        String crc = "";
        String crcAnswer = "";

        if (dataList.length > 5) {
            int sumData = 0;
            for (int i = 0; i < dataList.length - 4; i++) {
                sumData = sumData + dataList[i];
            }

            crc = Integer.toHexString(sumData);     //0 ~ CRC 전까지 데이터 합하여 16진수 변환
            if (crc.length() > 1) {
                crc = crc.substring(crc.length() - 2).toUpperCase();    //16진수값 중 뒤에 두자리만 검증용으로 사용
            } else {
                crc = "0" + crc.toUpperCase();
            }

            //응답 데이터 CRC 를 아스키코드 2자리 문자로 변환
            crcAnswer = Character.toString((char) dataList[dataList.length - 4]) + (char) dataList[dataList.length - 3];
        }

        if (!crc.equals("") && crc.equals(crcAnswer)) {     //CRC 비교
            isNormal = true;
        }
        return isNormal;
    }

    /**
     * Start Timer
     * Request 시 Response 대기 시간 제한
     *
     * @return
     */
    private Timer startTimer() {
        this.isReading = true;
        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (isReading) {
                    isReading = false;
                    System.err.println("[IO 보드] 응답 대기 시간 초과");
                }
            }
        };
        timer.schedule(task, 400); //읽기 제한 시간
        return timer;
    }

    /**
     * Close Timer
     *
     * @param timer
     */
    private void closeTimer(Timer timer) {
        timer.cancel();
    }

    /**
     * List -> IntList
     *
     * @param dataList
     * @return
     */
    private int[] toIntList(List<Integer> dataList) {
        int[] intList = null;

        if (dataList != null) {
            intList = new int[dataList.size()];
            for (int i = 0; i < intList.length; i++) {
                intList[i] = dataList.get(i);
            }
        }
        return intList;
    }
}
