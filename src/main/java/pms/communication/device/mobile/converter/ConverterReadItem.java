package pms.communication.device.mobile.converter;

import pms.vo.device.ConverterVO;

import java.util.ArrayList;
import java.util.List;

public class ConverterReadItem {
    private final List<ConverterVO.RequestItem> requestItems = new ArrayList<>();

    public ConverterReadItem() {
        setACConverterItems();
        setACLeftInverterItems();
        setACRightInverterItems();

        setDCConverterItems();
        setDCLeftInverterItems();
        setDCRightInverterItems();
    }

    public List<ConverterVO.RequestItem> getRequestItems() {
        return requestItems;
    }

    private ConverterVO.RequestItem setItem(String functionCode, String address, String dataType, int size, int scale, String type, String group, String name) {
        ConverterVO.RequestItem requestItem = new ConverterVO.RequestItem();
        requestItem.setFunctionCode(functionCode);
        requestItem.setAddress(address);
        requestItem.setDataType(dataType);
        requestItem.setSize(size);
        requestItem.setScale(scale);
        requestItem.setType(type);
        requestItem.setGroup(group);
        requestItem.setName(name);

        return requestItem;
    }

    private void setACConverterItems() {
        List<ConverterVO.RequestItem> items = new ArrayList<>();
        items.add(setItem("03", "0214", "UINT", 1, 1, "status", "ac", "setOperationMode"));
        items.add(setItem("03", "0314", "INT", 1, 10, "value", "ac", "setCurrent"));
        items.add(setItem("04", "0067", "UINT", 1, 1, "status", "ac", "operationStatus"));    //운전 상태
        items.add(setItem("04", "0048", "UINT", 1, 1, "status", "ac", "operationModeStatus"));    //운전 모드 상태
        items.add(setItem("04", "0077", "INT", 1, 10, "value", "ac", "totalActiveCurrent"));  //총 계통 유효 전류
        items.add(setItem("04", "0034", "INT", 1, 10, "value", "ac", "internalTemp"));    //함체 내부 온도
        items.add(setItem("04", "0177", "INT", 1, 10, "value", "ac", "transformerTemp")); //교류 변압기 온도

        requestItems.addAll(items);
    }

    private void setACLeftInverterItems() {
        List<ConverterVO.RequestItem> items = new ArrayList<>();
        items.add(setItem("04", "0052", "UINT", 1, 1, "status", "ac-left", "modeStatus"));    //모드 상태
        items.add(setItem("04", "0051", "UINT", 1, 1, "status", "ac-left", "inverterStatus"));    //인버터 상태
        items.add(setItem("04", "0049", "UINT", 1, 1, "status", "ac-left", "faultId"));
        items.add(setItem("04", "0047", "UINT", 1, 1, "status", "ac-left", "warningId"));
        items.add(setItem("04", "0682", "INT", 1, 10, "value", "ac-left", "power"));
        items.add(setItem("04", "0010", "INT", 1, 10, "value", "ac-left", "totalCurrent"));
        items.add(setItem("04", "0011", "INT", 1, 10, "value", "ac-left", "outputVoltage"));
        items.add(setItem("04", "0017", "INT", 1, 100, "value", "ac-left", "outputFrequency"));
        items.add(setItem("04", "0012", "INT", 1, 10, "value", "ac-left", "dcVoltage"));
        items.add(setItem("04", "0013", "INT", 1, 10, "value", "ac-left", "dcOffset"));
        items.add(setItem("04", "0015", "INT", 1, 10, "value", "ac-left", "activeCurrent"));
        items.add(setItem("04", "0014", "INT", 1, 10, "value", "ac-left", "activeCurrentContrast"));
        items.add(setItem("04", "0016", "INT", 1, 10, "value", "ac-left", "reactiveCurrentContrast"));
        items.add(setItem("04", "0021", "INT", 1, 1000, "value", "ac-left", "powerFactor"));
        items.add(setItem("04", "0038", "INT", 1, 10, "value", "ac-left", "acCurrent"));
        items.add(setItem("04", "0022", "INT", 1, 10, "value", "ac-left", "gridVoltage"));
        items.add(setItem("04", "0027", "INT", 1, 100, "value", "ac-left", "gridFrequency"));
        items.add(setItem("04", "0026", "INT", 1, 1, "value", "ac-left", "gridPhaseDifference"));
        items.add(setItem("04", "0031", "INT", 1, 10, "value", "ac-left", "stackTemp"));
        items.add(setItem("04", "0173", "INT", 1, 10, "value", "ac-left", "inductor1Temp"));
        items.add(setItem("04", "0176", "INT", 1, 10, "value", "ac-left", "inductor2Temp"));
        items.add(setItem("04", "0174", "INT", 1, 10, "value", "ac-left", "capacitorTemp"));

        requestItems.addAll(items);
    }

    private void setACRightInverterItems() {
        List<ConverterVO.RequestItem> items = new ArrayList<>();
        items.add(setItem("04", "0069", "UINT", 1, 1, "status", "ac-right", "modeStatus"));    //모드 상태
        items.add(setItem("04", "0070", "UINT", 1, 1, "status", "ac-right", "inverterStatus"));    //인버터 상태
        items.add(setItem("04", "0046", "UINT", 1, 1, "status", "ac-right", "faultId"));
        items.add(setItem("04", "0050", "UINT", 1, 1, "status", "ac-right", "warningId"));
        items.add(setItem("04", "0702", "INT", 1, 10, "value", "ac-right", "power"));
        items.add(setItem("04", "0701", "INT", 1, 10, "value", "ac-right", "totalCurrent"));
        items.add(setItem("04", "0703", "INT", 1, 10, "value", "ac-right", "outputVoltage"));
        items.add(setItem("04", "0704", "INT", 1, 100, "value", "ac-right", "outputFrequency"));
        items.add(setItem("04", "0705", "INT", 1, 10, "value", "ac-right", "dcVoltage"));
        items.add(setItem("04", "0706", "INT", 1, 10, "value", "ac-right", "dcOffset"));
        items.add(setItem("04", "0707", "INT", 1, 10, "value", "ac-right", "activeCurrent"));
        items.add(setItem("04", "0708", "INT", 1, 10, "value", "ac-right", "activeCurrentContrast"));
        items.add(setItem("04", "0709", "INT", 1, 10, "value", "ac-right", "reactiveCurrentContrast"));
        items.add(setItem("04", "0710", "INT", 1, 1000, "value", "ac-right", "powerFactor"));
        items.add(setItem("04", "0711", "INT", 1, 10, "value", "ac-right", "acCurrent"));
        items.add(setItem("04", "0712", "INT", 1, 10, "value", "ac-right", "gridVoltage"));
        items.add(setItem("04", "0713", "INT", 1, 100, "value", "ac-right", "gridFrequency"));
        items.add(setItem("04", "0714", "INT", 1, 1, "value", "ac-right", "gridPhaseDifference"));
        items.add(setItem("04", "0715", "INT", 1, 10, "value", "ac-right", "stackTemp"));
        items.add(setItem("04", "0032", "INT", 1, 10, "value", "ac-right", "inductor1Temp"));
        items.add(setItem("04", "0036", "INT", 1, 10, "value", "ac-right", "inductor2Temp"));
        items.add(setItem("04", "0033", "INT", 1, 10, "value", "ac-right", "capacitorTemp"));

        requestItems.addAll(items);
    }

    private void setDCConverterItems() {
        List<ConverterVO.RequestItem> items = new ArrayList<>();
        items.add(setItem("04", "0087", "UINT", 1, 1, "status", "dc", "operationStatus"));    //운전 상태
        items.add(setItem("04", "0088", "INT", 1, 10, "value", "dc", "totalDcPower"));    //총 직류 전력
        items.add(setItem("04", "0078", "INT", 1, 10, "value", "dc", "totalCurrent"));    //총 전류
        items.add(setItem("04", "0040", "UINT", 1, 10, "value", "dc", "convertDcPower")); //변환 직류 전력
        items.add(setItem("04", "0060", "INT", 1, 10, "value", "dc", "dcCurrent"));   //직류 전류
        items.add(setItem("04", "0724", "INT", 1, 10, "value", "dc", "internalTemp"));    //함체 내부 온도

        requestItems.addAll(items);
    }

    private void setDCLeftInverterItems() {
        List<ConverterVO.RequestItem> items = new ArrayList<>();
        items.add(setItem("04", "0066", "UINT", 1, 1, "status", "dc-left", "modeStatus"));
        items.add(setItem("04", "0059", "UINT", 1, 1, "status", "dc-left", "inverterStatus"));
        items.add(setItem("04", "0761", "UINT", 1, 1, "status", "dc-left", "faultId"));
        items.add(setItem("04", "0762", "UINT", 1, 1, "status", "dc-left", "warningId"));
        items.add(setItem("04", "0752", "UINT", 1, 10, "value", "dc-left", "power"));
        items.add(setItem("04", "0765", "UINT", 1, 1, "value", "dc-left", "current"));
        items.add(setItem("04", "0757", "UINT", 1, 10, "value", "dc-left", "voltage"));
        items.add(setItem("04", "0006", "UINT", 1, 1, "value", "dc-left", "dcPower"));
        items.add(setItem("04", "0758", "UINT", 1, 10, "value", "dc-left", "dcCurrent"));
        items.add(setItem("04", "0751", "UINT", 1, 10, "value", "dc-left", "activeCurrentContrast"));
        items.add(setItem("04", "0753", "UINT", 1, 10, "value", "dc-left", "refActiveCurrentPercentage"));
        items.add(setItem("04", "0303", "INT", 1, 10, "value", "dc-left", "stackTemp"));
        items.add(setItem("04", "0721", "INT", 1, 10, "value", "dc-left", "inductorTemp"));
        items.add(setItem("04", "0722", "INT", 1, 10, "value", "dc-left", "capacitorTemp"));

        requestItems.addAll(items);
    }

    private void setDCRightInverterItems() {
        List<ConverterVO.RequestItem> items = new ArrayList<>();
        items.add(setItem("04", "0080", "UINT", 1, 1, "status", "dc-right", "modeStatus"));
        items.add(setItem("04", "0079", "UINT", 1, 1, "status", "dc-right", "inverterStatus"));
        items.add(setItem("04", "0781", "UINT", 1, 1, "status", "dc-right", "faultId"));
        items.add(setItem("04", "0782", "UINT", 1, 1, "status", "dc-right", "warningId"));
        items.add(setItem("04", "0772", "UINT", 1, 10, "value", "dc-right", "power"));
        items.add(setItem("04", "0785", "UINT", 1, 1, "value", "dc-right", "current"));
        items.add(setItem("04", "0777", "UINT", 1, 10, "value", "dc-right", "voltage"));
        items.add(setItem("04", "0039", "UINT", 1, 1, "value", "dc-right", "dcPower"));
        items.add(setItem("04", "0778", "UINT", 1, 10, "value", "dc-right", "dcCurrent"));
        items.add(setItem("04", "0771", "UINT", 1, 10, "value", "dc-right", "activeCurrentContrast"));
        items.add(setItem("04", "0773", "UINT", 1, 10, "value", "dc-right", "refActiveCurrentPercentage"));
        items.add(setItem("04", "0304", "INT", 1, 10, "value", "dc-right", "stackTemp"));
        items.add(setItem("04", "0723", "INT", 1, 10, "value", "dc-right", "inductorTemp"));
        items.add(setItem("04", "0725", "INT", 1, 10, "value", "dc-right", "capacitorTemp"));

        requestItems.addAll(items);
    }
}
