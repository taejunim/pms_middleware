package pms.communication.device.mobile.converter;

import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.quartz.SchedulerException;
import pms.scheduler.device.converter.ConverterScheduler;
import pms.vo.device.ConverterVO;
import pms.vo.device.control.ControlRequestVO;
import pms.vo.device.control.ControlResponseVO;
import pms.vo.history.ControlHistoryVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pms.communication.CommunicationManager.deviceProperties;

/**
 * Converter Client
 * <p>
 * - 컨버터(이동형 ESS의 PCS) 통신 클라이언트
 * <p>
 * - AC/DC Converter & DC/DC Converter
 */
public class ConverterClient {
    private final ConverterScheduler converterScheduler = new ConverterScheduler();
    private final String connectionURL = deviceProperties.getProperty("converter.url");
    private static PlcConnection connection;
    private static List<ConverterVO.RequestItem> requestItems = new ArrayList<>();
    //private static Map<String, List<ConverterVO.RequestItem>> requestItemsMap = new HashMap<>();
    private static final int previousRegDate = 0;
    private static final List<String> previousErrorCodes = new ArrayList<>();
    private static final List<String> previousCommonErrorCodes = new ArrayList<>();
    private static final ControlRequestVO controlRequest = null;
    private static final Map<String, ControlRequestVO> controlRequestMap = new HashMap<>();
    private static final List<ControlRequestVO> controlRequests = new ArrayList<>();

    public void execute() {
        try {
            connect();
        } catch (PlcConnectionException e) {
            e.printStackTrace();
        } finally {
            requestItems = new ConverterReadItem().getRequestItems();
            //requestItemsMap = new ConverterReadItem().getRequestItemsMap();
            executeScheduler();
        }
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public void connect() throws PlcConnectionException {
        PlcDriverManager plcDriverManager = new PlcDriverManager();
        connection = plcDriverManager.getConnection(connectionURL);
        //connection.connect();
    }

    public void disconnect() {
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeScheduler() {
        try {
            converterScheduler.execute();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public ConverterVO read() {
        ConverterReader converterReader = new ConverterReader();
        converterReader.setRequest(connection, requestItems);
        converterReader.request();

        return converterReader.getConverterVO();
    }

    public boolean isControlRequest() {
        return controlRequests.size() > 0;
    }

    public void setControlRequest(ControlRequestVO requestVO) {
        //controlRequest = requestVO;
        //controlRequestMap.put(requestVO.getControlCode(), requestVO);
        controlRequests.add(requestVO);
    }

    public ControlResponseVO control() {
        System.out.println("RequestVO : " + controlRequests);

        System.out.println("RequestVO 1 : " + controlRequests.get(0));

        ConverterWriter converterWriter = new ConverterWriter();
        converterWriter.setRequest(connection, controlRequests.get(0));
        converterWriter.request();

        ControlResponseVO responseVO = converterWriter.getResponse();
        ControlHistoryVO controlHistoryVO = responseVO.getHistoryVO();
        System.out.println("Response Request : " + responseVO.getRequestVO());
        controlRequests.remove(responseVO.getRequestVO());


        System.out.println("RequestVO 1 : " + controlRequests.get(0));
        System.out.println("RequestVO 2 : " + controlRequests.get(1));
        System.out.println("Control request : " + controlRequests);

        return responseVO;
    }

    private boolean insertData(ConverterVO converterVO) {
        boolean complete = false;

        return complete;
    }
}
