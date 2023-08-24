package pms.communication.device.mobile.ioboard;

import jssc.SerialPort;
import pms.system.ess.ControlUtil;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;

/**
 * IOBoardWriter Class
 * IO 보드 제어
 */
public class IOBoardWriter {
    private final IOBoardCommunication ioBoardCommunication = new IOBoardCommunication();
    private ControlResponseVO responseVO = new ControlResponseVO();
    private int result; //제어 결과

    /**
     * Set Connection
     *
     * @param serialPort - SerialPort
     */
    public void setConnection(SerialPort serialPort) {
        this.ioBoardCommunication.setConnection(serialPort);
    }

    /**
     * Request
     * 제어 처리
     *
     * @param requestVO - 제어정보
     */
    public void request(ControlRequestVO requestVO) {
        int requestAddress = requestVO.getAddress();
        int controlValue = requestVO.getControlValue();

        String reqString = getRequestString(requestAddress, controlValue);
        ioBoardCommunication.sendRequest(reqString);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        setResult(controlValue, ioBoardCommunication.getResponseData());

        if (result == 1) {  //!!! 제어 응답값 처리 하는거 확인하고 더 구현해야됨.
            responseVO = ControlUtil.setControlResponseVO(requestAddress, (short) controlValue, requestVO);
        } else {
            responseVO = ControlUtil.setControlResponseVO(0, (short) controlValue, requestVO);
        }
    }

    /**
     * Get Result
     * 제어 결과 반환
     *
     * @return  - 1:성공, 0:실패
     */
    public int getResult() {
        return result;
    }

    /**
     * Get ResponseVO
     * 제어 응답 VO 반환
     *
     * @return  ControlResponseVO
     */
    public ControlResponseVO getResponseVO() {
        return responseVO;
    }

    /**
     * Set Result
     * 제어 응답 데이터에 따른 결과값 Set
     *
     * @param controlValue  - 1:운전, 0:정지
     * @param response      - 응답 데이터
     */
    private void setResult(int controlValue, int[] response) {
        result = 0; //실패
        if (response != null) {
            String checkString = String.valueOf((char) response[1]) + (char) response[2] + (char) response[3];

            if (controlValue == 1) {    //운전 == H
                if (checkString.equals("HOK")) {
                    result = 1; //성공
                }
            } else if (controlValue == 0) {     //정지 == L
                if ((char) response[1] == 'L') {
                    if (checkString.equals("LOK")) {
                        result = 1; //성공
                    }
                }
            }
        }
    }


    private String getRequestString(int requestAddress, int controlValue) {
        String requestString = null;
        String stx = "!";
        StringBuilder data = new StringBuilder();

        for (int i = 1; i < 11; i++) {
            if (i == requestAddress) {
                data.append("1");
            } else {
                data.append("0");
            }
        }

        if (controlValue == 1) {    //운전
            String crc = makeCrc(stx + "H" + data);
            requestString = stx + "H" + data + crc + "\r\n";
        } else if (controlValue == 0) {     //정지
            String crc = makeCrc(stx + "L" + data);
            requestString = stx + "L" + data + crc + "\r\n";
        }

        return requestString;
    }


    /**
     * Make Crc
     * (아스키코드)문자열 1바이트 CRC 생성
     *
     * @param str - 데이터 문자열(아스키코드)
     * @return String   - CRC
     */
    private String makeCrc(String str) {
        int sumData = 0;
        for (int i = 0; i < str.length(); i++) {
            sumData = sumData + (int) str.charAt(i);
        }
        String crc = Integer.toHexString(sumData);
        if (crc.length() > 1) {
            crc = crc.substring(crc.length() - 2).toUpperCase();
        } else {
            crc = "0" + crc.toUpperCase();
        }
        return crc;
    }
}
