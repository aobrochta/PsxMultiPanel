package com.psx.PsxMultiPanel;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import static java.lang.Integer.parseInt;

public class PSXClient {

    public static final String HOST = "192.168.1.60";
    public static final int PORT = 10747;

    private static BufferedWriter os;
    private static PSXMultiPanel psxMultiPanel = new PSXMultiPanel();

    public static int CMD_L;
    public static int CMD_C;
    public static int CMD_R;

    public static int FLAPS;

    private static class PSXVariable {
        public static final String ALTITUDE_VALUE = "Qi35";
        public static final String VERTICAL_SPEED_VALUE = "Qi34";
        public static final String IAS_VALUE = "Qi32";
        public static final String HDG_VALUE = "Qi33";
        public static final String CRS_VALUE = "Qs448";
        public static final String CMD_L_BTN = "Qh73";
        public static final String CMD_C_BTN = "Qh74";
        public static final String CMD_R_BTN = "Qh75";
        public static final String LNAV_BTN = "Qh65";
        public static final String VS_BTN = "Qh69";

        public static final String SPEED_BTN = "Qh64";

        public static final String FLCH_BTN = "Qh67";
        public static final String APPR_BTN = "Qh72";

        public static final String VNAV_BTN = "Qh66";

        public static final String LNAV_LIGHT = "Qh65";

        public static final String SPEED_LIGHT = "Qi64";

        public static final String FLAPS = "Qh389";
    }

    public static class PSXVariableEvent {
        public static final String MCP_ALT_SEL = "Qh80=";
        public static final String MCP_VS_SEL = "Qh79=";
        public static final String MCP_IAS_SEL = "Qh77=";
        public static final String MCP_HDG_SEL = "Qh78=";
        public static final String MCP_AP_SEL = "Qh73=";
        public static final String MCP_AP_DISC_SEL = "Qh400=";
        public static final String MCP_LNAV_SEL = "Qh65=";
        public static final String MCP_SPEED_LIGHT = "Qh64=";
        public static final String MCP_FLCH_LIGHT = "Qh67=";
        public static final String MCP_VS_LIGHT = "Qh69=";
        public static final String MCP_APPR_LIGHT = "Qh72=";
        public static final String MCP_VNAV_LIGHT = "Qh66=";
        public static final String MCP_HDG_BTN = "Qh61=";
        public static final String MCP_AT_ARM_BTN = "Qh58=";
        public static final String FLAPS = "Qh389=";
        public static final String VS_TURN = "Qh79=";
    }

    private static boolean find(String cmp, String[] array) {
        boolean result = false;
        for (String s : array) {
            if (s.equals(cmp)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        Socket socketOfClient = null;
        os = null;
        BufferedReader is = null;

        new Thread(() -> {
            try {
                psxMultiPanel.execute();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        try {

            // Send a request to connect to the server is listening
            // on machine 'localhost' port 9999.
            socketOfClient = new Socket(HOST, PORT);

            // Create output stream at the client (to send data to the server)
            os = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));


            // Input stream at Client (Receive data from the server).
            is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + HOST);
            return;
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + HOST);
            return;
        }

        while (!psxMultiPanel.isConnected()) {
            Thread.sleep(100);
        }

        try {

            // Read buffer provided by PsxMultiPanel to send to PSX
            new Thread(() -> {
                while (psxMultiPanel.isConnected()) {
                    synchronized (psxMultiPanel.buffer) {
                        for (String command : psxMultiPanel.buffer) {
                            try {
                                sendMessageToPsx(command);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if (psxMultiPanel.buffer.size() > 0) {
                            psxMultiPanel.buffer = new ArrayList<>();
                        }
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();


            // Read data sent from the server.
            // By reading the input stream of the Client Socket.
            String responseLine;
            while ((responseLine = is.readLine()) != null) {
                String[] splited = responseLine.split("=");
                if (splited.length > 1) {
                    getPSXDataAndSendToDisplay(splited[0], splited[1], psxMultiPanel);
                }
            }

            os.close();
            is.close();
            socketOfClient.close();
        } catch (UnknownHostException e) {
            System.err.println("Trying to connect to unknown host: " + e);
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }

    private static void sendMessageToPsx(String message) throws IOException {
        // Write data to the output stream of the Client Socket.
        os.write(message);
        // End of line
        os.newLine();
        // Flush data.
        os.flush();
    }

    private static void setBtnLed(int id, int value) {
        boolean state = false;
        if (value > 1) {
            state = true;
        }
        psxMultiPanel.setLedValue(id, state);
    }

    private static void setCmd(String cmd, int value) {
        switch (cmd) {
            case PSXVariable.CMD_L_BTN:
                CMD_L = value;
                break;
            case PSXVariable.CMD_C_BTN:
                CMD_C = value;
                break;
            case PSXVariable.CMD_R_BTN:
                CMD_R = value;
                break;
        }

        boolean state = false;
        if (CMD_L > 1 || CMD_C > 1 || CMD_R > 1) {
            state = true;
        }
        psxMultiPanel.setLedValue(PSXMultiPanel.MultiPanelLed.AP, state);
    }

    private static void getPSXDataAndSendToDisplay(String id, String value, PSXMultiPanel psxMultiPanel) {

        // Data for display
        if (PSXVariable.ALTITUDE_VALUE.equals(id)) {
            psxMultiPanel.setAltitudeMcp(parseInt(value) * 100);
        } else if (PSXVariable.VERTICAL_SPEED_VALUE.equals(id)) {
            psxMultiPanel.setVerticalSpeedMcp(parseInt(value) * 100);
        } else if (PSXVariable.IAS_VALUE.equals(id)) {
            psxMultiPanel.setIasMcp(parseInt(value));
        } else if (PSXVariable.HDG_VALUE.equals(id)) {
            psxMultiPanel.setHdgMcp(parseInt(value));
        } else if (PSXVariable.CRS_VALUE.equals(id)) {
            String qnh = getSocketValue(value, 3, ";");
            psxMultiPanel.setCrsMcp(parseInt(qnh) / 100);
        }

        else if (PSXVariable.CMD_L_BTN.equals(id) || PSXVariable.CMD_C_BTN.equals(id) || PSXVariable.CMD_R_BTN.equals(id)) {
            setCmd(id, parseInt(value));
        }

        // Data for leds
        else if (PSXVariable.LNAV_LIGHT.equals(id)) {
            setBtnLed(PSXMultiPanel.MultiPanelLed.NAV, parseInt(value));
        } else if (PSXVariable.SPEED_BTN.equals(id)) {
            setBtnLed(PSXMultiPanel.MultiPanelLed.IAS, parseInt(value));
        } else if (PSXVariable.FLCH_BTN.equals(id)) {
            setBtnLed(PSXMultiPanel.MultiPanelLed.ALT, parseInt(value));
        } else if (PSXVariable.VS_BTN.equals(id)) {
            setBtnLed(PSXMultiPanel.MultiPanelLed.VS, parseInt(value));
        } else if (PSXVariable.APPR_BTN.equals(id)) {
            setBtnLed(PSXMultiPanel.MultiPanelLed.APR, parseInt(value));
        } else if (PSXVariable.VNAV_BTN.equals(id)) {
            setBtnLed(PSXMultiPanel.MultiPanelLed.REV, parseInt(value));
        }

        // Initialize flaps variable
        else if (PSXVariable.FLAPS.equals(id)) {
            FLAPS = parseInt(value);
        }
    }

    private static String getSocketValue(String s, int index, String separateur) {
        String result = "0";
        String[] split = s.split(separateur);
        if (index < split.length) {
            result = split[index];
        }
        return result;
    }
}
