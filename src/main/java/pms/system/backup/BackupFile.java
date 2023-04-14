package pms.system.backup;

import pms.common.util.ConvertUtil;
import pms.common.util.DateTimeUtil;
import pms.common.util.FieldUtil;
import pms.common.util.FileUtil;
import pms.vo.system.PmsVO;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static pms.system.PMSManager.applicationProperties;

public class BackupFile {
    private final String SEPARATOR = File.separator; //파일 경로 구분자 - OS 구분없이 사용 가능
    private final String homeDirectory = applicationProperties.getProperty("file.path") + SEPARATOR + "PMS" + PmsVO.ess.getEssCode();
    //private final String homeDirectory = System.getProperty("user.home") + SEPARATOR + "PMS" + SEPARATOR + PmsVO.ess.getEssCode(); //사용자 홈 디렉토리
    private final String backupDirectory = applicationProperties.getProperty("file.path.backup");
    private final String deviceDirectory = applicationProperties.getProperty("file.path.backup.device");
    private final String errorDirectory = applicationProperties.getProperty("file.path.backup.device.error");
    private final String energyDirectory = applicationProperties.getProperty("file.path,backup.energy");
    private final String controlDirectory = applicationProperties.getProperty("file.path,backup.control");

    public void backupData(String category, String deviceCode, Object object) {
        Path filePath = getBackupFilePath(category, deviceCode);
        Path parentPath = filePath.getParent();  //상위 경로

        //상위 경로 존재 여부 확인 - 파일 경로 존재
        if (Files.isDirectory(parentPath)) {
            //파일 존재 여부 확인
            if (Files.isRegularFile(filePath)) {
                //파일 작성
                FileUtil.writeFile(filePath, setRows(object));
            } else {
                //파일 생성
                FileUtil.createNewFile(filePath, setColumnHeader(object));

                if (Files.isRegularFile(filePath)) {
                    FileUtil.writeFile(filePath, setRows(object));
                }
            }
        } else {
            boolean isCreate = FileUtil.createDirectory(parentPath);
            //경로 생성 후 파일 생성

            if (isCreate) {
                FileUtil.writeFile(filePath, setRows(object));
            }
        }
    }

    private Path getBackupFilePath(String category, String deviceCode) {
        String creationMonth = DateTimeUtil.getDateFormat("yyyyMM");
        String creationDate = DateTimeUtil.getDateFormat("yyyyMMdd");

        String backupPath = homeDirectory + backupDirectory;
        StringBuilder categoryPath = new StringBuilder();
        StringBuilder subCategoryDirectory = new StringBuilder();
        StringBuilder fileName = new StringBuilder();

        switch (category) {
            case "device":  //장비 데이터
                subCategoryDirectory.append(deviceDirectory).append("-").append(deviceCode);
                categoryPath
                        .append(deviceDirectory)
                        .append(subCategoryDirectory)
                        .append(subCategoryDirectory).append("-").append(creationMonth);

                fileName.append(subCategoryDirectory.toString().toLowerCase()).append("-raw-data-").append(creationDate);
                break;
            case "component":   //장비 구성요소 데이터
                subCategoryDirectory.append(deviceDirectory).append("-").append(deviceCode);
                categoryPath
                        .append(deviceDirectory)
                        .append(subCategoryDirectory)
                        .append(subCategoryDirectory).append("-").append(creationMonth);

                fileName.append(subCategoryDirectory.toString().toLowerCase()).append("-component-raw-data-").append(creationDate);
                break;
            case "device-error":    //장비 오류 데이터
                subCategoryDirectory.append(errorDirectory).append("-").append(deviceCode);
                categoryPath
                        .append(deviceDirectory)
                        .append(errorDirectory)
                        .append(subCategoryDirectory)
                        .append(subCategoryDirectory).append("-").append(creationMonth);

                fileName.append(deviceDirectory).append("-").append(deviceCode).append("-error-data-").append(creationDate);
                break;
            case "component-error": //장비 구성요소 오류 데이터
                subCategoryDirectory.append(errorDirectory).append("-").append(deviceCode);
                categoryPath
                        .append(deviceDirectory)
                        .append(errorDirectory)
                        .append(subCategoryDirectory)
                        .append(subCategoryDirectory).append("-").append(creationMonth);

                fileName.append(deviceDirectory).append("-").append(deviceCode).append("-component-error-data-").append(creationDate);
                break;
            case "energy":  //전력량 이력 데이터
                categoryPath
                        .append(energyDirectory)
                        .append(energyDirectory).append("-").append(creationMonth);

                fileName.append(energyDirectory.toLowerCase()).append("-history-").append(creationDate);
                break;
            case "energy-detail":   //전력량 상세 이력 데이터
                categoryPath
                        .append(energyDirectory)
                        .append(energyDirectory).append("-").append(creationMonth);

                fileName.append(energyDirectory.toLowerCase()).append("-detail-history-").append(creationDate);
                break;
            case "control": //제어 이력 데이터
                categoryPath
                        .append(controlDirectory)
                        .append(controlDirectory).append("-").append(creationMonth);

                fileName.append(controlDirectory.toLowerCase()).append("-history-").append(creationDate);
                break;
        }

        return Paths.get(backupPath, String.valueOf(categoryPath), fileName + ".csv");
    }

    private String setColumnHeader(Object object) {
        StringBuilder columns = new StringBuilder();
        List<Field> fields;

        if (object.getClass().equals(ArrayList.class)) {
            List<?> objectList = (ArrayList<?>) object;

            if (!objectList.isEmpty()) {
                fields = FieldUtil.getFields(objectList.get(0).getClass());
            } else {
                fields = null;
            }
        } else {
            fields = FieldUtil.getFields(object.getClass());
        }

        if (fields != null) {
            for (Field field : fields) {
                String fieldName = field.getName();
                columns.append(ConvertUtil.toUpperSnakeCase(fieldName)).append(", ");
            }

            columns.delete(columns.length() - 2, columns.length());
        }

        return columns.toString();
    }

    private String setRows(Object object) {
        String rows = null;

        if (object.getClass().equals(ArrayList.class)) {
            List<?> objectList = (ArrayList<?>) object;

            if (!objectList.isEmpty()) {
                rows = setValuesList(objectList);
            }
        } else {
            rows = setValues(object);
        }

        return rows;
    }

    private String setValuesList(List<?> objectList) {
        StringBuilder values = new StringBuilder(); //등록 필드 값

        for (Object object : objectList) {
            values.append(setValues(object));
            values.append(", ");    //값 행 뒤에 콤마 추가
        }

        values.delete(values.length() - 2, values.length());    //마지막 값 끝의 공백 및 콤마 제거

        return values.toString();
    }

    private String setValues(Object object) {
        StringBuilder values = new StringBuilder(); //등록 필드 값
        ArrayList<Field> fields = FieldUtil.getFields(object.getClass());   //필드 목록

        try {
            for (Field field : fields) {
                Object fieldValue = field.get(object);

                if (fieldValue == null) {
                    fieldValue = "";
                }

                values.append(fieldValue).append(", ");
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        values.delete(values.length() - 2, values.length());    //마지막 값 끝의 공백 및 콤마 제거

        return values.toString();
    }
}
