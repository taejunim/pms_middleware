package pms.vo.device.control;

import lombok.Data;
import pms.vo.history.ControlHistoryVO;

@Data
public class ControlResponseVO {
    private int result;
    private ControlRequestVO requestVO;
    private ControlHistoryVO historyVO;
}
