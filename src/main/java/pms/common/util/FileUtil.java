package pms.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileUtil {
    private static final String separator = File.separator; //파일 경로 구분자 - OS 구분없이 사용 가능

    public static void createFile(String directory, String fileName, String content) {
        Path parentPath = Paths.get(directory); //상위 경로
        Path filePath = Paths.get(parentPath + separator + fileName);   //파일 경로

        //상위 경로 존재 여부 확인 - 파일 경로 존재
        if (Files.isDirectory(parentPath)) {
            //파일 존재 여부 확인 - 파일 존재
            if (Files.isRegularFile(filePath)) {
                writeFile(filePath, "\n" + content);    //파일이 존재 하는 경우, 내용 추가
            } else {
                createNewFile(filePath, content);  //신규 파일 생성 실행
            }
        }
        //상위 경로 존재 여부 확인 - 파일 경로 미존재
        else {
            Boolean isCreateDirectory = createDirectory(parentPath);    //파일 경로 생성 후 생성 여부

            //파일 경로 생성 여부 - 성공
            if (isCreateDirectory) {
                createNewFile(filePath, content);  //파일 경로 생성 후, 파일 생성 실행
            }
        }
    }

    public static Boolean createDirectory(Path parentPath) {
        try {
            Files.createDirectories(parentPath);    //파일 디렉토리 생성 실행
            return true;
        } catch (IOException e) {
            e.getLocalizedMessage();
            return false;
        }
    }

    public static void createNewFile(Path filePath, String content) {
        try {
            Files.createFile(filePath); //파일 생성 실행

            //파일 작성 가능 여부 - 작성 가능
            if (Files.isWritable(filePath)) {
                writeFile(filePath, content);   //파일 작성 실행
            }
        } catch (IOException e) {
            e.getLocalizedMessage();
        }
    }

    public static void writeFile(Path filePath, String content) {
        byte[] contentBytes = (content + "\n").getBytes();   //파일 내용 Byte 변환

        try {
            /*
             * 파일 내용 작성
             * filePath - 파일 경로
             * contentBytes - 변환된 파일 내용
             * StandardOpenOption.APPEND 기존 파일 내용에 추가
             */
            Files.write(filePath, contentBytes, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.getLocalizedMessage();
        }
    }
}
