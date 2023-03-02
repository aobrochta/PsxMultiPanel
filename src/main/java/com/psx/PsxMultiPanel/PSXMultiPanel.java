package com.psx.PsxMultiPanel;

import org.hid4java.HidDevice;
import org.hid4java.HidException;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesListener;
import org.hid4java.HidServicesSpecification;
import org.hid4java.event.HidServicesEvent;
import java.util.ArrayList;

public class PSXMultiPanel implements HidServicesListener {
    private static final Integer VENDOR_ID = 0x6a3;
    private static final Integer PRODUCT_ID = 0xd06;
    private static final int PACKET_IN_LENGTH = 3;
    public static final String SERIAL_NUMBER = null;
    private static final int NB_SWITCH = 20;
    private static byte[] data = new byte[] {0x0A, 0x00, 0x00, 0x00, 0x05, 0x0A, 0x0A, 0x0A, 0x0A, 0x00, 0x00, 0x00};
    private static boolean[] currentSwitchPosition = new boolean[NB_SWITCH];
    private static long[] currentSwitchTimeout = new long[NB_SWITCH];
    private static int MODE_SELECTED = 0;
    private static final int DOUBLE_CLICK_TIMEOUT = 0;

    public static ArrayList<String> buffer = new ArrayList<>();

    private static HidDevice hidDevice;

    public static class MultiPanelLed {
        public static final int AP = 0;
        public static final int HDG = 1;
        public static final int NAV = 2;
        public static final int IAS = 3;
        public static final int ALT = 4;
        public static final int VS = 5;
        public static final int APR = 6;
        public static final int REV = 7;
    }

    public static class MultiPanelSwitch {
        public static final int ALT = 0;
        public static final int VS = 1;
        public static final int IAS = 2;
        public static final int HDG = 3;
        public static final int CRS = 4;
        public static final int ROTATE_UP = 5;
        public static final int ROTATE_DOWN = 6;
        public static final int AP_BTN = 7;
        public static final int HDG_BTN = 8;
        public static final int NAV_BTN = 9;
        public static final int IAS_BTN = 10;
        public static final int ALT_BTN = 11;
        public static final int VS_BTN = 12;
        public static final int APR_BTN = 13;
        public static final int REV_BTN = 14;
        public static final int ARM_THROTTLE = 15;
        public static final int FLAPS_UP = 16;
        public static final int FLAPS_DOWN = 17;
        public static final int VS_DOWN = 18;
        public static final int VS_UP = 19;
    }

    public static class MultiPanelValues {
        public static int altitude = 0;
        public static int verticalSpeed = 0;
        public static int ias = 100;
        public static int hdg = 200;
        public static int crs = 300;
    }
    public void execute() throws HidException, InterruptedException {
        // Configure to use custom specification
        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();

        // Use manual start feature to get immediate attach events
        hidServicesSpecification.setAutoStart(true);
        hidServicesSpecification.setAutoShutdown(true);

        // Get HID services
        HidServices hidServices = HidManager.getHidServices(hidServicesSpecification);
        hidServices.addHidServicesListener(this);

        // Manually start the services to get attachment event
        hidServices.start();

        // Open the device by Vendor ID and Product ID with wildcard serial number
        hidDevice = hidServices.getHidDevice(VENDOR_ID, PRODUCT_ID, SERIAL_NUMBER);
        if (hidDevice != null) {
            System.out.println("Device was found");

            // changeUpperValue(0, data, 5);
            // changeLowerValue(0, data, 4);
            updateDisplay();
            readMessage(hidDevice);
        }

        // Shut down and rely on auto-shutdown hook to clear HidApi resources
        hidServices.shutdown();

    }

    @Override
    public void hidDeviceAttached(HidServicesEvent event) {

    }

    @Override
    public void hidDeviceDetached(HidServicesEvent event) {

    }

    @Override
    public void hidFailure(HidServicesEvent event) {

    }

    private void readMessage(HidDevice hidDevice) throws InterruptedException {
        if (!hidDevice.isOpen()) {
            hidDevice.open();
        }

        // Prepare to read a single data packet
        int val = 0;
        while (val != -1) {
            byte readData[] = new byte[PACKET_IN_LENGTH];
            // This method will be stuck for 500ms or until data is read
            val = hidDevice.read(readData, 500);
            switch (val) {
                case -1:
                    System.err.println("Error: can't read device");
                    break;
                case 0:
                    break;
                default:
                    getMultiPanelEvent(readData);
                    break;
            }
        }
    }

    public static int writeToDevice(byte[] buffer, HidDevice hidDevice) {
        int bytesSent;
        bytesSent = hidDevice.write(buffer, 64, (byte) 0x00);
        if (bytesSent < 0) {
            System.err.println("Data was not sent to device");
        }
        return bytesSent;
    }

    /* MANAGE MULTI PANEL */

    public static void changeUpperValue(int number, byte[] source, int zeroNumber) {
        if (number > 99999 || number < -99999) {
            number = 99999 * getIntSign(number);
        }

        String str = String.format("%0" + zeroNumber + "d", number);
        str = str.replace('-', '0');
        changeByteValue(str, source, 1, 5);
    }

    public static void changeLowerValue(int number, byte[] source, int zeroNumber) {
        String str = "";
        if (zeroNumber > 0) {
            zeroNumber = ((number < 0) ? (zeroNumber + 1) : zeroNumber);

            if (number > 9999 || number < -9999) {
                number = 9999 * getIntSign(number);
            }

            str = String.format("%0" + zeroNumber + "d", number);
        }
        changeByteValue(str, source, 6, 10);
    }

    private static void changeByteValue(int number, byte[] source, int position, boolean isTrue) {
        if (isTrue) {
            source[position] = (byte) (source[position] | (1 << number));
        } else {
            source[position] = (byte) (source[position] & ~(1 << number));
        }
    }

    private static void changeByteValue(String number, byte[] source, int begin, int end) {
        int currentPosition = end - 1;
        int lastPosition = begin - 1;
        char[] numberArray = number.toCharArray();
        int index = numberArray.length - 1;

        while (currentPosition >= lastPosition) {
            char c = '_';
            if (index >= 0) {
                c = numberArray[index];
                index -= 1;
            }
            source[currentPosition] = convertCharToByte(c);
            currentPosition -= 1;
        }
    }

    private static byte convertCharToByte(char character) {
        byte result = 0x0A;
        int numberInt = Character.getNumericValue(character);
        if (numberInt >= 0 && numberInt < 10) {
            result = (byte) numberInt;
        } else if (character == '-') {
            result = (byte) 0x0E;
        }
        return result;
    }

    /* Read data methods */

    private static void getMultiPanelEvent(byte[] readData) {
        if (readData.length >= 3) {
            for(int i = 0; i < NB_SWITCH; i++) {
                boolean switchState = isBitTrue(readData, i);
                boolean currentSwitchPositionState = currentSwitchPosition[i];
                if (switchState != currentSwitchPositionState) {
                    triggerButtonEvent(i, switchState);
                }
            }
        }
    }

    private static boolean isBitTrue(byte[] arr, int bit) {
        int index = bit / 8;  // Get the index of the array for the byte with this bit
        int bitPosition = bit % 8;  // Position of this bit in a byte
        return (arr[index] >> bitPosition & 1) == 1;
    }

    private static void triggerButtonEvent(int buttonId, boolean state) {
        currentSwitchPosition[buttonId] = state;
        if (currentSwitchTimeout[buttonId] <= (System.currentTimeMillis())) {
            currentSwitchTimeout[buttonId] = System.currentTimeMillis() + DOUBLE_CLICK_TIMEOUT;

            switch (buttonId) {
                case MultiPanelSwitch.ALT:
                    altPressed(state);
                    break;
                case MultiPanelSwitch.VS:
                    vsPressed(state);
                    break;
                case MultiPanelSwitch.IAS:
                    iasPressed(state);
                    break;
                case MultiPanelSwitch.HDG:
                    hdgPressed(state);
                    break;
                case MultiPanelSwitch.CRS:
                    crsPressed(state);
                    break;
                case MultiPanelSwitch.ROTATE_UP:
                    rotateUpPressed(state);
                    break;
                case MultiPanelSwitch.ROTATE_DOWN:
                    rotateDownPressed(state);
                    break;
                case MultiPanelSwitch.AP_BTN:
                    apButtonPressed(state);
                    break;
                case MultiPanelSwitch.HDG_BTN:
                    hdgButtonPressed(state);
                    break;
                case MultiPanelSwitch.NAV_BTN:
                    navButtonPressed(state);
                    break;
                case MultiPanelSwitch.IAS_BTN:
                    iasButtonPressed(state);
                    break;
                case MultiPanelSwitch.ALT_BTN:
                    altButtonPressed(state);
                    break;
                case MultiPanelSwitch.VS_BTN:
                    vsButtonPressed(state);
                    break;
                case MultiPanelSwitch.APR_BTN:
                    aprButtonPressed(state);
                    break;
                case MultiPanelSwitch.REV_BTN:
                    revButtonPressed(state);
                    break;
                case MultiPanelSwitch.ARM_THROTTLE:
                    armThrottlePressed(state);
                    break;
                case MultiPanelSwitch.FLAPS_UP:
                    flapsUpPressed(state);
                    break;
                case MultiPanelSwitch.FLAPS_DOWN:
                    flapsDownPressed(state);
                    break;
                case MultiPanelSwitch.VS_UP:
                    vsUpPressed(state);
                    break;
                case MultiPanelSwitch.VS_DOWN:
                    vsDownPressed(state);
                    break;
            }
        }
    }

    private static int getIntSign(int number) {
        int result = 1;
        if (number < 0) { result = -1; }
        return result;
    }

    private static void updateDisplay() {
        writeToDevice(data, hidDevice);
    }

    private static void updateDisplay(int mode) {
        if (    MODE_SELECTED == mode
                || (MODE_SELECTED == MultiPanelSwitch.ALT && mode == MultiPanelSwitch.VS)
                || (mode == MultiPanelSwitch.ALT || MODE_SELECTED == MultiPanelSwitch.VS))
        {
            switch (MODE_SELECTED) {
                case MultiPanelSwitch.IAS:
                    changeUpperValue(MultiPanelValues.ias, data, 3);
                    changeLowerValue(0, data, 0);
                    break;
                case MultiPanelSwitch.HDG:
                    changeUpperValue(MultiPanelValues.hdg, data, 3);
                    changeLowerValue(0, data, 0);
                    break;
                case MultiPanelSwitch.CRS:
                    changeUpperValue(MultiPanelValues.crs, data, 3);
                    changeLowerValue(0, data, 0);
                    break;
                default: // ALT or VS selected
                    changeUpperValue(MultiPanelValues.altitude, data, 5);
                    changeLowerValue(MultiPanelValues.verticalSpeed, data, 4);
                    break;
            }
            writeToDevice(data, hidDevice);
        }
    }

    public static void changeLedValue(int number, byte[] source, boolean isLedOn) {
        changeByteValue(number, source, 10, isLedOn);
        updateDisplay();
    }

    private static void addToBuffer(String message) {
        synchronized (buffer) {
            buffer.add(message);
        }
    }

    private static void addToBuffer(String id, int value) {
        addToBuffer(id + value);
    }

    /**********************************************************************

     ******************* HERE IS THE COMMUNICATION METHODS ****************

     **********************************************************************/

    public boolean isConnected() {
        return hidDevice != null;
    }

    public void setAltitudeMcp(int number) {
        MultiPanelValues.altitude = number;
        updateDisplay(MultiPanelSwitch.ALT);
    }

    public void setVerticalSpeedMcp(int number) {
        MultiPanelValues.verticalSpeed = number;
        updateDisplay(MultiPanelSwitch.VS);
    }

    public void setIasMcp(int number) {
        MultiPanelValues.ias = number;
        updateDisplay(MultiPanelSwitch.IAS);
    }

    public void setHdgMcp(int number) {
        MultiPanelValues.hdg = number;
        updateDisplay(MultiPanelSwitch.HDG);
    }

    public void setCrsMcp(int number) {
        MultiPanelValues.crs = number;
        updateDisplay(MultiPanelSwitch.CRS);
    }

    public static void setLedValue(int id, boolean state) {
        changeLedValue(id, data, state);
    }

    private static boolean isLedOn(int id) {
        return isBitTrue(data, 80 + id);
    }

    /**********************************************************************

     ********************** HERE IS THE MODIFY ZONE ************************

     **********************************************************************/

    private static void altPressed(boolean state) {
        if (state) {
            MODE_SELECTED = MultiPanelSwitch.ALT;
            updateDisplay(MultiPanelSwitch.ALT);
        }
    }

    private static void vsPressed(boolean state) {
        if (state) {
            MODE_SELECTED = MultiPanelSwitch.VS;
            updateDisplay(MultiPanelSwitch.VS);
        }
    }

    private static void iasPressed(boolean state) {
        if (state) {
            MODE_SELECTED = MultiPanelSwitch.IAS;
            updateDisplay(MultiPanelSwitch.IAS);
        }
    }

    private static void hdgPressed(boolean state) {
        if (state) {
            MODE_SELECTED = MultiPanelSwitch.HDG;
            updateDisplay(MultiPanelSwitch.HDG);
        }
    }

    private static void crsPressed(boolean state) {
        if (state) {
            MODE_SELECTED = MultiPanelSwitch.CRS;
            updateDisplay(MultiPanelSwitch.CRS);
        }
    }

    private static void rotateUpPressed(boolean state) {
        if (state) {
            switch (MODE_SELECTED) {
                case MultiPanelSwitch.IAS:
                    addToBuffer(PSXClient.PSXVariableEvent.MCP_IAS_SEL, 1);
                    break;
                case MultiPanelSwitch.HDG:
                    addToBuffer(PSXClient.PSXVariableEvent.MCP_HDG_SEL, 1);
                    break;
                default:
                    addToBuffer(PSXClient.PSXVariableEvent.MCP_ALT_SEL, 1);
                    break;
            }
        }
    }

    private static void rotateDownPressed(boolean state) {
        if (state) {
            switch (MODE_SELECTED) {
                case MultiPanelSwitch.IAS:
                    addToBuffer(PSXClient.PSXVariableEvent.MCP_IAS_SEL, -1);
                    break;
                case MultiPanelSwitch.HDG:
                    addToBuffer(PSXClient.PSXVariableEvent.MCP_HDG_SEL, -1);
                    break;
                default:
                    addToBuffer(PSXClient.PSXVariableEvent.MCP_ALT_SEL, -1);
                    break;
            }
        }
    }

    private static void apButtonPressed(boolean state) {
        if (state) {
            if (PSXClient.CMD_L > 0 || PSXClient.CMD_C > 0 || PSXClient.CMD_R > 0) {
                addToBuffer(PSXClient.PSXVariableEvent.MCP_AP_DISC_SEL, 1);
            } else {
                addToBuffer(PSXClient.PSXVariableEvent.MCP_AP_SEL, 1);
            }
        }
    }

    private static void hdgButtonPressed(boolean state) {
        addToBuffer(PSXClient.PSXVariableEvent.MCP_HDG_BTN, 1);
    }

    private static void navButtonPressed(boolean state) {
        if (state) {
            int value = 1;
            if (isLedOn(MultiPanelLed.NAV)) {
                value = 9;
            }
            addToBuffer(PSXClient.PSXVariableEvent.MCP_LNAV_SEL, value);
        }
    }

    private static void iasButtonPressed(boolean state) {
        if (state) {
            int value = 1;
            if (isLedOn(MultiPanelLed.IAS)) {
                value = 9;
            }
            addToBuffer(PSXClient.PSXVariableEvent.MCP_SPEED_LIGHT, value);
        }
    }

    private static void altButtonPressed(boolean state) {
        if (state) {
            int value = 1;
            if (isLedOn(MultiPanelLed.ALT)) {
                value = 9;
            }
            addToBuffer(PSXClient.PSXVariableEvent.MCP_FLCH_LIGHT, value);
        }
    }

    private static void vsButtonPressed(boolean state) {
        if (state) {
            int value = 1;
            if (isLedOn(MultiPanelLed.VS)) {
                value = 9;
            }
            addToBuffer(PSXClient.PSXVariableEvent.MCP_VS_LIGHT, value);
        }
    }

    private static void aprButtonPressed(boolean state) {
        if (state) {
            int value = 1;
            if (isLedOn(MultiPanelLed.APR)) {
                value = 9;
            }
            addToBuffer(PSXClient.PSXVariableEvent.MCP_APPR_LIGHT, value);
        }
    }

    private static void revButtonPressed(boolean state) {
        if (state) {
            int value = 1;
            if (isLedOn(MultiPanelLed.REV)) {
                value = 9;
            }
            addToBuffer(PSXClient.PSXVariableEvent.MCP_VNAV_LIGHT, value);
        }
    }

    private static void armThrottlePressed(boolean state) {
        int value = 0;
        if (state) {
            value = 1;
        }
        addToBuffer(PSXClient.PSXVariableEvent.MCP_AT_ARM_BTN, value);
    }

    private static void flapsUpPressed(boolean state) {
        if (state) {
            if (PSXClient.FLAPS > 0) {
                PSXClient.FLAPS -= 1;
                addToBuffer(PSXClient.PSXVariableEvent.FLAPS, PSXClient.FLAPS);
            }
        }
    }

    private static void flapsDownPressed(boolean state) {
        if (state) {
            if (PSXClient.FLAPS < 6) {
                PSXClient.FLAPS += 1;
                addToBuffer(PSXClient.PSXVariableEvent.FLAPS, PSXClient.FLAPS);

            }
        }
    }

    private static void vsUpPressed(boolean state) {
        if (state) {
            addToBuffer(PSXClient.PSXVariableEvent.VS_TURN, 1);
        }
    }

    private static void vsDownPressed(boolean state) {
        if (state) {
            addToBuffer(PSXClient.PSXVariableEvent.VS_TURN, -1);
        }
    }
}