package pms.system.backup;

import com.jcraft.jsch.*;
import org.quartz.SchedulerException;
import pms.database.query.DeviceQuery;
import pms.scheduler.backup.BackupScheduler;
import pms.vo.system.PmsVO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pms.common.util.DateTimeUtil.getCalculatedDate;
import static pms.system.PMSManager.applicationProperties;

/**
 * packageName    : pms.system.backup
 * fileName       : BackupClient
 * author         : tjlim
 * date           : 2023/08/02
 * description    : 서버 로그 파일들을 개발 서버에 백업
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/08/02        tjlim       최초 생성
 */
public class BackupClient {
    private final BackupScheduler backupScheduler = new BackupScheduler();
    public final String SEPARATOR = File.separator; //파일 경로 구분자 - OS 구분없이 사용 가능
    private final String SFTP_HOST = applicationProperties.getProperty("sftp.host");
    private final int SFTP_PORT = Integer.parseInt(applicationProperties.getProperty("sftp.port"));
    private final String SFTP_USER = applicationProperties.getProperty("sftp.user");
    private final String SFTP_PASSWORD = applicationProperties.getProperty("sftp.password");
    private final String SFTP_ROOT_PATH = applicationProperties.getProperty("sftp.root.path") + SEPARATOR + PmsVO.ess.getEssCode();
    public final String FILE_PATH = applicationProperties.getProperty("file.path");
    private boolean isValid = false;

    public void execute() {
        executeScheduler();
    }

    private void executeScheduler() {
        try {
            backupScheduler.execute("0 10 0 ? * SUN");
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public void backupFile(Path path) {
        String uploadingFile = String.valueOf(path);

        File file = new File(uploadingFile);

        String filePath = SFTP_ROOT_PATH + SEPARATOR + uploadingFile.replace(FILE_PATH + SEPARATOR, "").replace(file.getName(), "");

        makeDirectory(filePath);

        Session session = connectSession();
        Channel channel = connectChannel(session, "sftp");
        ChannelSftp channelSftp = (ChannelSftp) channel;

        if (!isValid()) {
            System.out.println("File (" + uploadingFile + ") Upload fail.");
        } else {
            boolean isUploaded = upload(channelSftp, filePath, file); //101.101.208.212 서버의 /{pmsCode} 폴더에 업로드

            disconnect(session, channel, channelSftp);

            //업로드한 파일 로컬에서 삭제
            if (isUploaded) {
                System.out.println("File (" + path + ") Upload Completed.");
                try {
                    // 파일 삭제
                    boolean isFileDeleted = Files.deleteIfExists(path);

                    if (isFileDeleted) {
                        System.out.println("File (" + path + ") deleted.");

                        String deviceCode = file.getName().split("-")[1];
                        String tempFileDate = file.getName().replace(".csv","");
                        String startDate = tempFileDate.substring(tempFileDate.length()-8) + "000000";
                        String endDate = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(getCalculatedDate(startDate, Calendar.DATE, 1));

                        DeviceQuery deviceQuery = new DeviceQuery();
                        int result = deviceQuery.deleteRawData(deviceCode, startDate, endDate);

                        if (result > 0) {
                            System.out.println("[ deviceCode : "+deviceCode+" / "+startDate+" ~ "+endDate+" deleted ]");
                        } else {
                            System.out.println("Table raw not deleted");
                        }

                    } else {
                        System.out.println("File (" + path + ") not deleted.");
                    }

                } catch (DirectoryNotEmptyException e) {
                    System.out.println("디렉토리가 비어있지 않습니다");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("IOException");
                }
            } else {
                System.out.println("파일 업로드 실패");
            }

        }
    }

    //백업 서버 디렉토리 만들기
    public void makeDirectory(String path) {
        Session session = connectSession();
        Channel channel = connectChannel(session, "exec");

        try {
            ChannelExec channelExec = (ChannelExec) channel;
            channelExec.setCommand("mkdir -p " + path); //전체 경로 한번에 만들기

            //실행
            channelExec.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    public List<Path> getFileList(String rootPath, String today) {

        Path dirPath = Paths.get(rootPath);

        List<Path> result;
        Stream<Path> walk = null;
        try {
            walk = Files.walk(dirPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        result = walk.filter(Files::isRegularFile)
                .filter(Files -> !Files.getFileName().toString().contains(today)) //당일 제외한 파일 가져오기
                .filter(Files -> !Files.getFileName().toString().endsWith(".DS_Store"))
                .collect(Collectors.toList());

        return result;
    }

    public Session connectSession() {
        try {
            JSch jSch = new JSch();
            Session session = jSch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            return session;
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
    }

    public Channel connectChannel(Session session, String type) {
        try {
            Channel channel = null;

            if (session.isConnected()) {
                channel = session.openChannel(type);

                if (type.equals("sftp")) {
                    channel.connect();
                    isValid = true;
                }
            }

            return channel;
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param path : serverVO.path 를 통해 scp로 들어간 서버로 접속하여 upload한다.
     * @param file : File file을 객체를 받음
     */
    public boolean upload(ChannelSftp channelSftp, String path, File file) {
        boolean isUploaded = false;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            channelSftp.cd(path);
            channelSftp.put(in, file.getName());

            // 업로드 성공 확인
            if (this.isExist(channelSftp, path + SEPARATOR + file.getName())) {
                isUploaded = true;
            }
        } catch (SftpException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return isUploaded;
    }

    /**
     * 디렉토리( or 파일) 존재 여부
     * @param path 디렉토리 (or 파일)
     * @return
     */
    public boolean isExist(ChannelSftp channelSftp, String path) {
        Vector res = null;
        try {
            res = channelSftp.ls(path);
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            }
        }
        return res != null && !res.isEmpty();
    }

    private void disconnect(Session session, Channel channel, ChannelSftp channelSftp) {
        channel.disconnect();
        session.disconnect();
        channelSftp.quit();
        channel.disconnect();
        session.disconnect();
        setValid(false);
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }
}