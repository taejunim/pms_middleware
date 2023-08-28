package pms.scheduler.device.bms;

import org.quartz.*;
import pms.communication.device.bms.BMSClient;
import pms.communication.web.WebSender;
import pms.system.ess.ESSManager;
import pms.vo.device.BmsVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.sql.SQLException;
import java.util.List;

/**
 * BMS Job
 * <p>
 * - BMS 통신 스케줄러 작업 실행
 */
@DisallowConcurrentExecution
public class BMSJob implements Job {
    private final BMSClient bmsClient = new BMSClient();
    private final WebSender webSender = new WebSender();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String rackCode = (String) jobDataMap.get("rackCode");

        if (bmsClient.isConnected(rackCode)) {
            try {
                if (!bmsClient.isControlRequest(rackCode)) {
                    executeRead(rackCode);
                } else {
                    executeControl(rackCode);
                }
            } finally {
                bmsClient.disconnect(rackCode);
            }
        } else {
            try {
                bmsClient.connect(rackCode);
            } catch (Exception e) {
                //System.out.println("Connected Error.");
                executeConnectionError(rackCode);
                bmsClient.disconnect(rackCode);

                for (DeviceVO rack : PmsVO.racks) {
                    if (rack.getDeviceCode().equals(rackCode)) {
                        bmsClient.setConnection(rackCode, rack);
                    }
                }

                e.printStackTrace();
                //e.getLocalizedMessage();
            }
        }
    }

    /**
     * 수신 실행
     *
     * @param rackCode Rack 코드
     */
    private void executeRead(String rackCode) {
        BmsVO bmsVO = bmsClient.read(rackCode);
        sendReadData(rackCode, bmsVO);

        BmsVO.RackVO rackVO = bmsVO.getRack();

        ESSManager essManager = new ESSManager();
        essManager.saveRackStatus(rackVO);
        essManager.saveSoC(rackVO);
        essManager.saveLimitPower(rackVO);
    }

    /**
     * 제어 실행
     *
     * @param rackCode Rack 코드
     */
    private void executeControl(String rackCode) {
        ControlResponseVO responseVO = bmsClient.control(rackCode);
        String requestType = responseVO.getRequestVO().getType();

        if (requestType.equals("02")) {
            sendControlResponse(rackCode, responseVO);
        }
    }

    /**
     * 연결 오류 시, 실행
     *
     * @param rackCode Rack 코드
     */
    private void executeConnectionError(String rackCode) {
        try {
            BmsVO bmsVO = bmsClient.readByError(rackCode);
            sendReadData(rackCode, bmsVO);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 수신 데이터 전송
     *
     * @param rackCode Rack 코드
     * @param bmsVO    BMS 정보
     */
    private void sendReadData(String rackCode, BmsVO bmsVO) {
        DeviceVO rackInfo = bmsClient.getRackInfo(rackCode);
        List<String> errorCodes = bmsClient.getErrorCodes(rackCode);

        webSender.sendData(rackInfo, bmsVO, errorCodes);
    }

    /**
     * 제어 응답 전송
     *
     * @param rackCode   Rack 코드
     * @param responseVO 제어 응답 정보
     */
    private void sendControlResponse(String rackCode, ControlResponseVO responseVO) {
        int result = responseVO.getResult();
        String remoteId = responseVO.getRequestVO().getRemoteId();
        String controlCode = responseVO.getRequestVO().getControlCode();

        webSender.sendResponse(remoteId, rackCode, controlCode, result, "");  //제어응답전송수정
    }
}
