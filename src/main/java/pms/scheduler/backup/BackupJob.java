package pms.scheduler.backup;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import pms.system.backup.BackupClient;

import java.nio.file.Path;
import java.util.List;

/**
 * packageName    : pms.scheduler.backup
 * fileName       : BackupJob
 * author         : tjlim
 * date           : 2023/08/02
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2023/08/02        tjlim       최초 생성
 */
public class BackupJob implements Job {
    private final BackupClient backupClient = new BackupClient();

    /**
     * 스케줄러 Job 실행
     *
     * @param jobExecutionContext Job 실행 정보
     * @throws JobExecutionException Job Execution Exception
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        String rootPath = backupClient.FILE_PATH + backupClient.SLASH;

        List<Path> fileList = backupClient.getFileList(rootPath, new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date())); // 백업 실행하는 날이 매주 일요일 0시라서 당일 파일 제외해서 백업

        for (Path path : fileList) {
            backupClient.backupFile(path);
        }
    }
}
