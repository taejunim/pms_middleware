package pms.system.ess;

import pms.common.util.DateTimeUtil;
import pms.vo.system.ScheduleVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESSScheduleManager {
    public static Map<String, ScheduleVO> schedules = new HashMap<>();
    public static Map<String, List<ScheduleVO.ScheduleDetailVO>> schedulesMap = new HashMap<>();

    public void checkSchedule() {
        String currentDate = DateTimeUtil.getDateFormat("yyyyMMdd");

        System.out.println(schedules);
        System.out.println(schedulesMap);
    }
}
