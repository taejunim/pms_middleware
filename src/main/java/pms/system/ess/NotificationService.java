package pms.system.ess;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Balance;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import pms.vo.device.error.DeviceErrorVO;
import pms.vo.system.DeviceVO;
import pms.vo.system.PmsVO;

import java.util.List;

import static pms.system.PMSManager.applicationProperties;

public class NotificationService {
    private final DefaultMessageService messageService;

    public NotificationService() {
        String domain = applicationProperties.getProperty("message.api.domain");
        String apiKey = applicationProperties.getProperty("message.api.key");
        String apiSecretKey = applicationProperties.getProperty("message.api.secret-key");

        messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecretKey, domain);
    }

    /**
     * 문자 메시지 잔여 포인트 호출
     *
     * @return 포인트 정보
     */
    private float getPoint() {
        Balance balance = messageService.getBalance();
        Float point = balance.getPoint();

        return point != null ? point : 0;
    }

    /**
     * 오류 메시지 내용 생성
     *
     * @param deviceVO 장비 정보
     * @param errors 오류 정보 목록
     * @return 생성된 오류 메시지
     */
    public String setErrorMessageContent(DeviceVO deviceVO, List<DeviceErrorVO> errors) {
        int errorCount = errors.size();
        String deviceName = deviceVO.getDeviceName();
        DeviceErrorVO deviceErrorVO = errors.get(0);
        String errorCode = deviceErrorVO.getErrorCode();
        String errorCodeName = PmsVO.errorCodes.get(errorCode).getErrorCodeName();

        StringBuilder builder = new StringBuilder();
        builder.append("[").append(PmsVO.ess.getEssName()).append("]");
        builder.append("장비: ").append(deviceName).append("/");
        builder.append("오류: ").append(errorCodeName).append("(오류 코드: ").append(errorCode).append(")");

        if (errorCount > 1) {
            builder.append(" 외 ").append(errorCount-1).append("건의 오류가 발생하였습니다.");
        } else {
            builder.append(" 오류가 발생하였습니다.");
        }

        return builder.toString();
    }

    /**
     * 문자 메시지 전송
     *
     * @param content 문자 메시지 내용
     */
    public void sendMessage(String content) {

        //포인트 확인 후 문자 메시지 전송 실행
        if (getPoint() >= 13.0) {
            Message message = new Message();
            message.setFrom("01049344442"); //발신번호
            message.setTo("01071320829");   //수신번호
            message.setText(content);    //SMS 메시지 - 한글 45자, 영자 90자 이하

            SingleMessageSendingRequest messageSendingRequest = new SingleMessageSendingRequest(message);
            SingleMessageSentResponse messageSentResponse = messageService.sendOne(messageSendingRequest);

            if (messageSentResponse != null) {
                String statusCode = messageSentResponse.getStatusCode();

                if (statusCode.equals("2000")) {
                    System.out.println("[메시지 알림] 문자 메시지 전송 완료");
                } else {
                    System.out.println("[메시지 알림] 문자 메시지 전송 실패 - 상태 코드: " + statusCode);
                }
            } else {
                System.out.println("[메시지 알림] 문자 메시지 전송 실패 - 전송 응답 없음.");
            }
        } else {
            System.out.println("[메시지 알림] 문자 메시지 전송 실패 - 잔액 부족.");
        }
    }

    /**
     * E-mail 전송
     */
    public void sendEmail() {

    }
}
