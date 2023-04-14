package pms.system.backup;

import com.jcraft.jsch.*;

import static pms.system.PMSManager.applicationProperties;

public class BackupClient {
    private final String SFTP_HOST = applicationProperties.getProperty("sftp.host");
    private final int SFTP_PORT = Integer.parseInt(applicationProperties.getProperty("sftp.port"));
    private final String SFTP_USER = applicationProperties.getProperty("sftp.user");
    private final String SFTP_PASSWORD = applicationProperties.getProperty("sftp.password");
    private final String SFTP_ROOT_PATH = applicationProperties.getProperty("sftp.root.path");
    private Session session;
    private Channel channel;
    private ChannelSftp channelSftp;
    private ChannelExec channelExec;
    private boolean isValid;

    public void connectSession() {
        try {
            JSch jSch = new JSch();
            session = jSch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
    }

    public void connectChannel(String type) {
        try {
            if (session.isConnected()) {
                channel = session.openChannel(type);

                switch (type) {
                    case "sftp":
                        channel.connect();
                        break;
                    case "exec":
                        channelExec = (ChannelExec) channel;
                        channelExec.connect();
                        break;
                }
            }
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
    }

    private void disconnect() {
        channel.disconnect();
        session.disconnect();
    }
}
