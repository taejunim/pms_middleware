package pms.system.ess;

import pms.common.util.DateTimeUtil;
import pms.database.query.EnergyQuery;
import pms.database.query.SystemQuery;
import pms.system.PMSCode;
import pms.system.backup.BackupFile;
import pms.vo.device.BmsVO;
import pms.vo.device.PcsVO;
import pms.vo.history.EnergyDetailHistoryVO;
import pms.vo.history.EnergyHistoryVO;
import pms.vo.system.PmsVO;

import java.util.HashMap;
import java.util.Map;

public class ESSManager {
    private static final Map<String, String> rackStatusMap = new HashMap<>();
    private static final Map<String, Float> socMap = new HashMap<>();
    private static final Map<String, Float> limitPowerMap = new HashMap<>();
    private static String operationType = null;
    private static String operationMode = "0";  //운전 모드
    private static String energyNo = null;  //전력량 이력 번호
    private static float accumulatedEnergy = 0; //누적 전력량
    private static float referenceEnergy = 0;   //조정 기준 전력량 - 현재 PCS 누적 전력량이 초기화되지 않아 임시 방안
    private String totalCharge = PmsVO.ess.getTotalCharge();
    private String totalDischarge = PmsVO.ess.getTotalDischarge();

    public void saveRackStatus(BmsVO.RackVO rackVO) {
        String rackCode = rackVO.getRackCode();
        String operationStatus = rackVO.getOperationStatus();

        rackStatusMap.put(rackCode, operationStatus);
    }

    public boolean isRackOperation() {
        for (String operationStatus : rackStatusMap.values()) {
            if (!operationStatus.equals("08")) {
                return false;
            }
        }

        return true;
    }

    public void saveSoC(BmsVO.RackVO rackVO) {
        String rackCode = rackVO.getRackCode();

        if (rackVO.getOperationStatus().equals("08")) {
            float userSoC = rackVO.getUserSoc();
            socMap.put(rackCode, userSoC);
        } else {
            socMap.remove(rackCode);
        }
    }

    public float averageSoC() {
        float sumSoC = 0;
        int rackCount = socMap.size();

        for (float userSoC : socMap.values()) {
            sumSoC += userSoC;
        }

        return (float) (Math.round((sumSoC / rackCount) * 10) / 10.0);
    }

    public void saveLimitPower(BmsVO.RackVO rackVO) {
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

    public int calculateLimitPower() {
        float sumLimitPower = 0;
        int rackCount = limitPowerMap.size();

        for (float limitPower : limitPowerMap.values()) {
            sumLimitPower += limitPower;
        }

        int limitPower = (int) (sumLimitPower / (1000 * rackCount));
        int setLimitPower = PMSCode.getControlVO("0200010205").getControlValue();

        return Math.min(limitPower, setLimitPower);
        //return (int) (sumLimitPower / (1000 * rackCount));
    }

    public float getReferenceEnergy() {
        return referenceEnergy;
    }

    public String getTotalCharge() {
        return totalCharge;
    }

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
        int result = systemQuery.updateESS(totalCharge, totalDischarge);

        if (result > 0) {
            PmsVO.ess = systemQuery.getESS();
        }
    }

    public void setOperationType(String controlType) {
        operationType = controlType;
    }

    /**
     * 전력량 데이터 처리
     * <p>
     * PCS 운전 상태에 따라 전력량 이력 등록 및 갱신
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
                    beginEnergyHistory(pcsVO, currentOperationMode);
                } else {
                    //이전 운전의 전력량 이력 갱신 후, 신규 전력량 이력 및 상세 이력 등록 - 이전 운전 모드 상태가 현재 운전 모드 상태와 다른 경우
                    if (!operationMode.equals(currentOperationMode)) {
                        endEnergyHistory(pcsVO, "1");   //정상 종료
                        beginEnergyHistory(pcsVO, currentOperationMode);
                    } else {    //전력량 상세 이력 등록 - 이전 운전 모드 상태와 현재 운전 모드 상태가 같은 경우
                        float currentAccumulatedEnergy = 0;

                        if (currentOperationMode.equals("1")) {
                            currentAccumulatedEnergy = pcsVO.getAccumulatedChargeEnergy() - referenceEnergy;
                        } else if (currentOperationMode.equals("2")) {
                            currentAccumulatedEnergy = pcsVO.getAccumulatedDischargeEnergy() - referenceEnergy;
                        }

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
                endEnergyHistory(pcsVO, "1");   //정상 종료
            }
        } else {
            if (currentOperationMode.equals("0")) {
                switch (currentOperation) {
                    case "11":
                        endEnergyHistory(pcsVO, "1");   //운전 종료
                        break;
                    case "13":
                        endEnergyHistory(pcsVO, "4");   //비상정지 종료
                        break;
                    case "99":
                        endEnergyHistory(pcsVO, "2");   //결함 종료
                        break;
                }
            }
        }
    }

    private void beginEnergyHistory(PcsVO pcsVO, String currentOperationMode) {
        if (currentOperationMode.equals("1") || currentOperationMode.equals("2")) {
            EnergyHistoryVO energyHistoryVO = setEnergyHistoryVO(pcsVO.getPcsCode(), pcsVO.getOperationModeStatus());
            int result = insertEnergyHistory(energyHistoryVO);

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

    private void endEnergyHistory(PcsVO pcsVO, String endType) {
        if (!operationMode.equals("0")) {
            int result = updateEnergyHistory(pcsVO, endType);    //정상 종료

            if (result > 0) {
                energyNo = null;
                operationType = null;
                operationMode = pcsVO.getOperationModeStatus();
                accumulatedEnergy = 0;
                referenceEnergy = 0;
            }
        }
    }

    private EnergyHistoryVO setEnergyHistoryVO(String deviceCode, String operationModeType) {
        int startTime = DateTimeUtil.getUnixTimestamp();
        energyNo = operationModeType + deviceCode + startTime;

        EnergyHistoryVO energyHistoryVO = new EnergyHistoryVO();
        energyHistoryVO.setEnergyNo(energyNo);
        energyHistoryVO.setDeviceCode(deviceCode);
        energyHistoryVO.setOperationModeType(operationModeType);
        energyHistoryVO.setOperationType(operationType);
        energyHistoryVO.setOperationHistoryType("0");
        energyHistoryVO.setSchedulerOperationFlag("N");
        energyHistoryVO.setStartDate(startTime);

        return energyHistoryVO;
    }

    private int insertEnergyHistory(EnergyHistoryVO energyHistoryVO) {
        EnergyQuery energyQuery = new EnergyQuery();
        return energyQuery.insertEnergyHistory(energyHistoryVO);
    }

    private int updateEnergyHistory(PcsVO pcsVO, String operationEndType) {
        int endDate = DateTimeUtil.getUnixTimestamp();
        String updateDate = DateTimeUtil.getCurrentTimestamp();

        EnergyHistoryVO energyHistoryVO = new EnergyHistoryVO();
        energyHistoryVO.setEnergyNo(energyNo);
        energyHistoryVO.setDeviceCode(pcsVO.getPcsCode());
        energyHistoryVO.setOperationHistoryType(operationEndType);
        energyHistoryVO.setFinalAccumulatedEnergy(accumulatedEnergy);
        energyHistoryVO.setEndDate(endDate);
        energyHistoryVO.setUpdatedAt(updateDate);

        EnergyQuery energyQuery = new EnergyQuery();
        return energyQuery.updateEnergyHistory(energyHistoryVO);
    }

    private int insertEnergyDetailHistory(float accumulatedEnergy) {
        EnergyDetailHistoryVO energyDetailHistoryVO = new EnergyDetailHistoryVO();
        energyDetailHistoryVO.setEnergyNo(energyNo);
        energyDetailHistoryVO.setAccumulatedEnergy(accumulatedEnergy);

        EnergyQuery energyQuery = new EnergyQuery();
        int result = energyQuery.insertEnergyDetailHistory(energyDetailHistoryVO);

        if (result > 0) {
            new BackupFile().backupData("energy-detail", null, energyDetailHistoryVO);
        }

        return result;
    }
}
