package pms.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Resource Utility
 * <p>
 * - 리소스 관련 함수 유틸리티
 */
public class ResourceUtil {
    /**
     * Resource 호출
     *
     * @param resourceName Resource 파일 명
     * @return Resource InputStream
     */
    private static InputStream getResource(String resourceName) {
        InputStream inputStream = null;
        URL url = DateTimeUtil.class.getClassLoader().getResource(resourceName);

        if (url != null) {
            inputStream = DateTimeUtil.class.getClassLoader().getResourceAsStream(resourceName);
        } else {
            url = DateTimeUtil.class.getResource("/resources" + resourceName);

            if (url != null) {
                inputStream = DateTimeUtil.class.getResourceAsStream("/resources/" + resourceName);
            }
        }

        return inputStream;
    }

    /**
     * Properties 파일 호출
     *
     * @param propertiesName Properties 파일 명
     * @return Properties
     */
    public static Properties loadProperties(String propertiesName) {
        Properties properties = new Properties();

        try {
            properties.load(getResource("config/" + propertiesName + ".properties"));   //config 경로의 Properties 파일 로드
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }
}
