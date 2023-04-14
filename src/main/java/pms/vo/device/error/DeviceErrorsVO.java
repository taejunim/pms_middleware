package pms.vo.device.error;

import lombok.Data;

import java.util.List;

@Data
public class DeviceErrorsVO {
    private List<DeviceErrorVO> deviceErrors;
    private List<ComponentErrorVO> componentErrors;
}
