package pms.system.ess;

import pms.common.util.DateTimeUtil;
import pms.database.query.EnergyQuery;
import pms.database.query.SystemQuery;
import pms.system.PMSCode;
import pms.system.backup.BackupFile;
import pms.vo.device.BmsVO;
import pms.vo.device.PcsVO;
import pms.vo.history.EnergyDetailHistoryVO;
import pms.vo.history.EnergyHistoryVONew;
import pms.vo.system.PmsVO;

import java.util.HashMap;
import java.util.Map;

/**
 * ESS Manger
 * <p>
 * ESS 운영 관리
 */
public class ESSManager {
    private final EnergyQuery energyQuery = new EnergyQuery();
    private final String ESS_TYPE = PmsVO.ess.getEssType();  //ESS 유형
    public final float CONTRACT_POWER = PmsVO.ess.getContractPower();   //계약 전력
    private static final Map<String, String> rackStatusMap = new HashMap<>();   //Rack 별 상태 Map
    private static final Map<String, Float> socMap = new HashMap<>();   //Rack 별 SoC Map
    private static final Map<String, Float> limitPowerMap = new HashMap<>();    //Rack 별 제한 전력 Map
    private static String operationType = null; //운영 구분
    private static String operationMode = "0";  //운전 모드
    private static String energyNo = null;  //전력량 이력 번호
    private static float accumulatedEnergy = 0; //누적 전력량
    private static float referenceEnergy = 0;   //조정 기준 전력량 - 현재 PCS 누적 전력량이 초기화되지 않아 임시 방안
    private String totalCharge = PmsVO.ess.getTotalCharge();    //ESS 총 충전 누적 전력량
    private String totalDischarge = PmsVO.ess.getTotalDischarge();  //ESS 총 방전 누적 전력량
    private static final Map<String, Float> evChargerPowerMap = new HashMap<>();

    /**
     * Rack 별 운영 상태 저장
     *
     * @param rackVO Rack 정보
     */
    public void saveRackStatus(BmsVO.RackVO rackVO) {
        String rackCode = rackVO.getRackCode(); //Rack 코드
        String operationStatus = rackVO.getOperationStatus();   //운영 상태

        rackStatusMap.put(rackCode, operationStatus);
    }

    /**
     * Rack 운영 여부 확인
     * <p>
     * - Rack 운영 상태에 따른 ESS 운영 가능 여부 판별
     *
     * @return 운영 여부
     */
    public boolean isRackOperation() {
        boolean isOperate = true;

        if (rackStatusMap.size() > 0) {
            for (String operationStatus : rackStatusMap.values()) {
                //'08: 준비' 상태가 아닌 경우 처리
                if (!operationStatus.equals("08")) {
                    isOperate = false;
                    break;
                }
            }
        } else {
            isOperate = false;
        }

        return isOperate;
    }

    /**
     * Rack 별 SoC 저장
     *
     * @param rackVO Rack 정보
     */
    public void saveSoC(BmsVO.RackVO rackVO) {
        String rackCode = rackVO.getRackCode();

        if (rackVO.getOperationStatus().equals("08")) {
            float userSoC = rackVO.getUserSoc();
            socMap.put(rackCode, userSoC);
        } else {
            socMap.remove(rackCode);
        }
    }

    /**
     * 평균 SoC 계산
     *
     * @return 평균 SoC
     */
    public float averageSoC() {
        float sumSoC = 0;   //SoC 합계
        int rackCount = socMap.size();  //Rack 개수(Rack 운영 상태가 '08: 준비'가 아닌 경우 제외)

        for (float userSoC : socMap.values()) {
            sumSoC += userSoC;
        }

        return (float) (Math.round((sumSoC / rackCount) * 10) / 10.0);
    }

    /**
     * 배터리 전력량(kWh) 변환
     * <p>
     * - 배터리 평균 SoC를 배터리 전력량(kWh)으로 변환
     *
     * @return 배터리 전력량(kWh)
     */
    public float convertToBatteryEnergy() {
        float totalBatteryEnergy = PmsVO.ess.getTotalBatteryEnergy();   //총 배터리 전력량
        float averageSoC = averageSoC();    //평균 SoC(%)
        float batteryEnergy = (totalBatteryEnergy * averageSoC / 100);  //총 배터리 전력량 * 평균 SoC / 100

        return (float) (Math.round(batteryEnergy * 10) / 10.0);
    }

    /**
     * 제한 전력 저장
     *
     * @param rackVO Rack 정보
     */
    public void saveLimitPower(BmsVO.RackVO rackVO) {
        //Rack 운영 상태가 '08: 준비' 중인 경우에만 제한 전력 저장
        if (rackVO.getOperationStatus().equals("08")) {
            String rackCode = rackVO.getRackCode();
            float voltage = rackVO.getVoltage();    //전압(500 ~ 1000)
            int limitCurrent = rackVO.getChargeCurrentLimit();  //충전 전류 제한(0 ~ 150)

            //전압이 1000 이하, 충전 전류 제한이 150 이하인 경우만 처리
            if (voltage < 1000 && limitCurrent < 150) {
                float limitPower = voltage * limitCurrent;
                limitPowerMap.put(rackCode, limitPower);
            }
        }
    }

    /**
     * 제한 전력 계산
     *
     * @return 제한 전력 값
     */
    public int calculateLimitPower() {
        float sumLimitPower = 0;
        int rackCount = limitPowerMap.size();

        for (float limitPower : limitPowerMap.values()) {
            System.out.println("limitPower : " + limitPower);
            sumLimitPower += limitPower;
        }

        System.out.println("sumLimitPower : " + sumLimitPower);

        int limitPower = (int) (sumLimitPower / (1000 * rackCount));    //제한 전력 계산 값
        int setLimitPower = setLimitPower();

        System.out.println("limitPower : " + limitPower);
        System.out.println("setLimitPower : " + setLimitPower);
        System.out.println("Math.min : " + Math.min(limitPower, setLimitPower));

        return Math.min(limitPower, setLimitPower); //설정된 제한 전력 값과 비교하여 낮은 전력 값을 전달
        //return (int) (sumLimitPower / (1000 * rackCount));
    }

    /**
     * 제한 전력 설정
     * <p>
     * - PMS 환경 설정에서 설정된 충전 및 방전 제한 전력 값으로 설정
     *
     * @return 제한 전력 값
     */
    private int setLimitPower() {
        int setLimitPower = 0;

        if (ESS_TYPE.equals("01")) {
            setLimitPower = PMSCode.getControlVO("0200010205").getControlValue();   //설정된 충전 제한 전력 값
            //호출 방식 변경해야함
            /*if (operationMode.equals("1")) {
                setLimitPower = PMSCode.getControlVO("0200010205").getControlValue();   //설정된 제한 전력 값
            } else if (operationMode.equals("2")) {
                setLimitPower = PMSCode.getControlVO("0200010206").getControlValue();   //설정된 제한 전력 값
            }*/
        } else if (ESS_TYPE.equals("02")) {
            if (operationMode.equals("1")) {
                int setCurrent = PMSCode.getControlVO("0301010305").getControlValue();
            }
        }

        return setLimitPower;
    }

    /**
     * EV 충전기 전력 저장
     * <p>
     * - EV 충전기 측에 설치된 전력 계측기를 통해 측정되는 사용 전력 저장
     *
     * @param meterCode          계측기 코드
     * @param totalPower 총 피상 전력
     */
    public void saveEVChargerPower(String meterCode, float totalPower) {
        float power = 0;

        if (totalPower >= 0.1) {
            power = (float) (Math.ceil(totalPower * 10) / 10.0);

            if (power >= 60.0) {
                power = 60;
            }
        }

        System.out.println("[전력 계측기] 총 전력 = " + totalPower);
        System.out.println("[전력 계측기] 충전기 소비 전력 = " + power);

        evChargerPowerMap.put(meterCode, power);
    }

    /**
     * EV 충전기 사용 여부 확인
     * <p>
     * - 저장한 EV 충전기 전력의 총 전력 합으로 사용 여부 확인
     *
     * @return 사용 여부
     */
    public boolean isUsingEVCharger() {
        return getTotalEVChargerPower() > 0.2;  //충전기 기준 전력과 비교 - 임시 설정
    }

    /**
     * 총 EV 충전기 전력 호출
     *
     * @return 총 EV 충전기 전력
     */
    public float getTotalEVChargerPower() {
        float totalPower = 0;

        for (float power : evChargerPowerMap.values()) {
            totalPower += power;
        }

        return totalPower;
    }

    /**
     * 참조 전력량(0점 조정 기준 전력량) 호출
     *
     * @return 참조 전력량
     */
    public float getReferenceEnergy() {
        return referenceEnergy;
    }

    /**
     * 총 누적 충전 전력량 호출
     *
     * @return 총 누적 충전 전력량
     */
    public String getTotalCharge() {
        return totalCharge;
    }

    /**
     * 총 누적 방전 전력량 호출
     *
     * @return 총 누적 방전 전력량
     */
    public String getTotalDischarge() {
        return totalDischarge;
    }

    /**
     * ESS 총 누적 전력량 갱신
     *
     * @param operationMode            운전 모드 상태
     * @param currentAccumulatedEnergy 현재 누적 전력량
     */
    private void updateTotalEnergy(String operationMode, float currentAccumulatedEnergy) {
        float energy = currentAccumulatedEnergy - accumulatedEnergy;

        if (operationMode.equals("1")) {
            totalCharge = String.format("%.1f", Float.parseFloat(totalCharge) + energy);
        } else if (operationMode.equals("2")) {
            totalDischarge = String.format("%.1f", Float.parseFloat(totalDischarge) + energy);
        }

        SystemQuery systemQuery = new SystemQuery();
        int result = systemQuery.updateTotalEnergy(totalCharge, totalDischarge);

        if (result > 0) {
            PmsVO.ess = systemQuery.getESS();
        }
    }

    /**
     * 운영 구분 설정
     *
     * @param controlType 제어 구분
     */
    public void setOperationType(String controlType) {
        operationType = controlType;
    }

    /**
     * 전력량 데이터 처리
     * <p>
     * - PCS 운전 상태에 따라 전력량 이력 등록 및 갱신
     *
     * @param pcsVO PcsVO
     */
    public void processEnergyData(PcsVO pcsVO) {
        String currentOperation = pcsVO.getOperationStatus();
        String currentOperationMode = pcsVO.getOperationModeStatus();

        if (currentOperation.equals("12")) {
            //운전 모드 상태가 충전(1) 또는 방전(2) 상태인 경우
            if (!currentOperationMode.equals("0")) {
                //신규 전력량 이력 및 상세 이력 등록 - 이전 운전 모드 상태가 대기(0) 상태인 경우
                if (operationMode.equals("0")) {
                    beginOperation(pcsVO, currentOperationMode);
                } else {
                    //이전 운전의 전력량 이력 갱신 후, 신규 전력량 이력 및 상세 이력 등록 - 이전 운전 모드 상태가 현재 운전 모드 상태와 다른 경우
                    if (!operationMode.equals(currentOperationMode)) {
                        endOperation(pcsVO, "1");   //운전 종료
                        beginOperation(pcsVO, currentOperationMode);
                    } else {    //전력량 상세 이력 등록 - 이전 운전 모드 상태와 현재 운전 모드 상태가 같은 경우
                        float currentAccumulatedEnergy = 0;

                        //현재 운전 모드에 따라 현재 누적 전력량 계산
                        if (currentOperationMode.equals("1")) {
                            currentAccumulatedEnergy = pcsVO.getAccumulatedChargeEnergy() - referenceEnergy;
                        } else if (currentOperationMode.equals("2")) {
                            currentAccumulatedEnergy = pcsVO.getAccumulatedDischargeEnergy() - referenceEnergy;
                        }

                        //현재 누적 전력량이 기존 누적 전력량보다 큰 경우에만 실행
                        if (accumulatedEnergy < currentAccumulatedEnergy) {
                            float roundAccumulatedEnergy = (float) (Math.round(currentAccumulatedEnergy * 10) / 10.0);
                            int result = insertEnergyDetailHistory(roundAccumulatedEnergy);

                            if (result > 0) {
                                updateTotalEnergy(currentOperationMode, currentAccumulatedEnergy);
                                accumulatedEnergy = roundAccumulatedEnergy;
                            }
                        }
                    }
                }
            } else {
                endOperation(pcsVO, "1");   //운전 종료
            }
        } else {
            if (currentOperationMode.equals("0")) {
                switch (currentOperation) {
                    case "11":
                        endOperation(pcsVO, "1");   //운전 종료
                        break;
                    case "13":
                        endOperation(pcsVO, "4");   //비상정지 종료
                        break;
                    case "99":
                        endOperation(pcsVO, "2");   //결함 종료
                        break;
                }
            }
        }
    }

    /**
     * ESS 운전 시작
     *
     * @param pcsVO                PCS 정보
     * @param currentOperationMode 현재 운전 모드
     */
    private void beginOperation(PcsVO pcsVO, String currentOperationMode) {
        if (currentOperationMode.equals("1") || currentOperationMode.equals("2")) {
            EnergyHistoryVONew energyHistoryVONew = setEnergyHistoryVONew("0", pcsVO.getPcsCode(), pcsVO.getOperationModeStatus());
            int result = insertEnergyHistoryNew(energyHistoryVONew);

            if (result > 0) {
                operationMode = currentOperationMode;
                accumulatedEnergy = 0;
                referenceEnergy = 0;

                if (currentOperationMode.equals("1")) {
                    referenceEnergy = pcsVO.getAccumulatedChargeEnergy();
                } else {
                    referenceEnergy = pcsVO.getAccumulatedDischargeEnergy();
                }

                insertEnergyDetailHistory(accumulatedEnergy);
            }
        }
    }

    /**
     * ESS 운전 종료
     *
     * @param pcsVO   PCS 정보
     * @param endType 종료 구분
     */
    private void endOperation(PcsVO pcsVO, String endType) {
        if (!operationMode.equals("0")) {
            EnergyHistoryVONew energyHistoryVONew = setEnergyHistoryVONew(endType, pcsVO.getPcsCode(), operationMode);
            int result = insertEnergyHistoryNew(energyHistoryVONew);

            if (result > 0) {
                energyNo = null;
                operationType = null;
                operationMode = pcsVO.getOperationModeStatus();
                accumulatedEnergy = 0;
                referenceEnergy = 0;
            }
        }
    }

    /**
     * 전력량 이력 정보 생성
     *
     * @param historyType   이력 상태 구분
     * @param pcsCode       PCS 코드
     * @param operationMode 운전 모드
     * @return 전력량 이력 정보
     */
    private EnergyHistoryVONew setEnergyHistoryVONew(String historyType, String pcsCode, String operationMode) {
        int historyDate = DateTimeUtil.getUnixTimestamp();

        if (historyType.equals("0")) {
            energyNo = historyDate + pcsCode + operationMode;  //전력량 이력 번호 생성
        }

        EnergyHistoryVONew energyHistoryVO = new EnergyHistoryVONew();
        energyHistoryVO.setEnergyNo(energyNo);
        energyHistoryVO.setOperationHistoryType(historyType);
        energyHistoryVO.setOperationHistoryDate(historyDate);
        energyHistoryVO.setPcsCode(pcsCode);
        energyHistoryVO.setOperationMode(operationMode);
        energyHistoryVO.setOperationType(operationType);

        if (operationType.equals("01")) {
            energyHistoryVO.setScheduleNo("");
        }

        return energyHistoryVO;
    }

    /**
     * 전력량 이력 등록
     *
     * @param energyHistoryVO 전력량 이력 정보
     * @return 등록 결과
     */
    private int insertEnergyHistoryNew(EnergyHistoryVONew energyHistoryVO) {

        int result = energyQuery.insertEnergyHistoryNew(energyHistoryVO);

        if (result > 0) {
            new BackupFile().backupData("energy", null, energyHistoryVO);
        }

        return result;
    }

    /**
     * 전력량 상세 이력 등록
     *
     * @param accumulatedEnergy 누적 전력량
     * @return 등록 결과
     */
    private int insertEnergyDetailHistory(float accumulatedEnergy) {
        EnergyDetailHistoryVO energyDetailHistoryVO = new EnergyDetailHistoryVO();
        energyDetailHistoryVO.setEnergyNo(energyNo);
        energyDetailHistoryVO.setAccumulatedEnergy(accumulatedEnergy);

        int result = energyQuery.insertEnergyDetailHistory(energyDetailHistoryVO);

        if (result > 0) {
            new BackupFile().backupData("energy-detail", null, energyDetailHistoryVO);
        }

        return result;
    }

    public void sendNotificationMessage() {

    }
}
