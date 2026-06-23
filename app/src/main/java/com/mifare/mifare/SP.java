package com.mifare.mifare;

import android.nfc.tech.MifareClassic;
import android.os.Handler;
import android.util.Log;

import com.innohi.YNHAPI;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import android_serialport_api.ComBean;
import android_serialport_api.MyFunc;
import android_serialport_api.SerialHelper;
import android_serialport_api.SerialPortFinder;

public class SP {
    static String path = "/dev/ttyS7";
    static SerialControl serialCom;
    static DispQueueThread DispQueue;
    static byte[] recvdata = new byte[1500];
    static int recvlen=0;
    static byte[] gcardnum = new byte[4];

    static Timer timer;
    static YNHAPI mAPI;
    static boolean checkGpio = false;
    static YNHAPI.GpioState oldState;
    static boolean isInit = false;

    public static void init() {
        try {
            if (isInit) return;
            SerialPortFinder mSerialPortFinder= new SerialPortFinder();
            String[] entryValues = mSerialPortFinder.getAllDevicesPath();
            //Helper.log(entryValues.length);
            //for (int i = 0; i < entryValues.length; i++) Helper.log(entryValues[i]);

            DispQueue = new DispQueueThread();
            DispQueue.start();

            serialCom = new SerialControl();
            serialCom.setPort(path);
            serialCom.setBaudRate(Helper.set.portSpeed);
            OpenComPort();

            timer = new Timer();
            mAPI = YNHAPI.getInstance();
            SP.StartTimer();
            SP.StartTimer1();

            Helper.toast("Порт открыт: " + path + " YNHAPI: " + mAPI.getApiVersion());
            isInit = true;
        }
        catch (Exception e) { Helper.toast(e.getMessage()); }
    }

    private static void DispRecData(ComBean ComRecData) {
        byte[] temp = new byte[20];
        try {
            if(solveRecv(ComRecData.bRec, temp)==0) {
                Helper.showVar("uid", "");
                Helper.showVar("block", "");
                if (Helper.set.algo == 1) {
                    byte[] uid = getUid();
                    Helper.showVar("uid", NFC.bin2hex(uid));
                }

                boolean isValue = false;
                isValue = NFC.handleFields(null);
                Helper.showVar("isError", Helper.isError);
                Helper.showVar("isValue", isValue);
                if (!Helper.isError && !isValue) {
                    mAPI.setGpioMode(YNHAPI.GPIO_5, YNHAPI.GpioMode.INPUT);
                    checkGpio = true;
                    Helper.showVar("checkGpio", checkGpio);

                    byte[] _uid = gcardnum;
                    if (Helper.set.wiegandOrder == 1) _uid = reverseBytes(gcardnum);
                    int code = bytesToInt(_uid);
                    Helper.showVar("wiegand", NFC.bin2hex(_uid));
                    Helper.showVar("wiegand1", code);
                    if (Helper.set.wiegandMode == 0) mAPI.writeWiegand(YNHAPI.WiegandFormat.FORMAT_34, code);
                    else if (Helper.set.wiegandMode == 1) mAPI.writeWiegand(YNHAPI.WiegandFormat.FORMAT_26, code);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (checkGpio) {
                                checkGpio = false;
                                Helper.showVar("checkGpio", checkGpio);
                                String html = ("<div class=\"cont-text\">" + Helper.set.cont_text5 + "</div>");
                                String html_cont = ("<div class=\"red item-number\">" + Helper.set.cont_text1 + "</div>");
                                Helper.showNfc(html, html_cont, Helper.set.cont_text_secs);
                            }
                        }
                    }, Helper.set.cont_wait_gpio * 1000);
                }
                else {
                    checkGpio = false;
                    Helper.showVar("checkGpio", checkGpio);
                }
            }

        }
        catch (Exception e) {
            /*StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append("Файл: " + element.getFileName() +
                        ", Класс: " + element.getClassName() +
                        ", Метод: " + element.getMethodName() +
                        ", Строка: " + element.getLineNumber() + "\n");
            }
            Helper.toast(sb.toString());*/
            Helper.toast("Ошибка чтения! Приложите и удерживайте вашу карту или браслет");
        }
    }

    public static MifareData getMifare(Field[] fields) {
        MifareData result = new MifareData();
        for (int i = 0; i < 40; i++) {
            Sector sector = new Sector();
            int _i = i;
            boolean readSector = Arrays.stream(fields).anyMatch(x -> x.sector == _i);
            if (readSector) {
                //Authenticate a sector with key A.
                Field f = Arrays.stream(fields).filter(x -> x.sector == _i).findFirst().orElse(null);
                for (int j = 0; j < 4; j++) {
                    Block block = new Block();
                    int _j = j;
                    boolean readBlock = false;
                    if (Helper.set.algo == 0) readBlock = Arrays.stream(fields).anyMatch(x -> x.block == _j);
                    else if (Helper.set.algo == 1) readBlock = _j == 2;
                    else if (Helper.set.algo == 2) readBlock = _j == 0;
                    if (readBlock) {
                        byte[] data = null;
                        if (Helper.set.algo == 0 || Helper.set.algo == 2) data = getBlock(NFC.hexToBytes(f.key), i, j);
                        else if (Helper.set.algo == 1) data = getBlock(NFC.getXorKey(gcardnum), i, j);
                        block.bytes = data;
                        Helper.showVar("block", NFC.bin2hex(data));
                    }
                    sector.blocks.add(block);
                }
            }
            result.sectors.add(sector);
        }
        return result;
    }

    static byte[] getUid() {
        b1();
        b2();
        return gcardnum;
    }

    static byte[] getBlock(byte[] key, int sector, int block) {
        b1();
        b2();
        b3();
        b4(key, sector, block);
        return b5(sector, block);
    }

    //region standard
    private static class SerialControl extends SerialHelper {
        public SerialControl() {
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData) {
            DispQueue.AddQueue(ComRecData);// 线程定时刷新显示(推荐)
        }
    }

    private static class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {

                    MainActivity.activity.runOnUiThread(new Runnable() {
                        public void run() {
                            DispRecData(ComData);
                        }
                    });

                    try {
                        Thread.sleep(10);// 显示性能高的话，可以把此数值调小。
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        public synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
        }
    }

    private static int solveRecv(byte[] bRec, byte[] retRec) {
        int sta = -1;

        if((byte)bRec[0] == 0x02){

            byte len = bRec[1];
            if(len <= bRec.length){

                byte result = MyFunc.bccCalc(bRec, 1, len-3);
                if((byte)bRec[len-2] == (byte)result){
                    retRec[0] = (byte) (len-5);
                    java.lang.System.arraycopy( bRec,3,retRec, 1,len-5);
                    sta = 0;
                }
            }
        }

        return sta;
    }

    private static void OpenComPort() {
        try {
            serialCom.open();
        } catch (SecurityException e) {
            Helper.toast("No_read_or_write_permissions");
        } catch (IOException e) {
            Helper.toast("Unknown_error");
        } catch (InvalidParameterException e) {
            Helper.toast("Parameter_error");
        }
    }

    public static void CloseComPort() {
        try {
            if (serialCom != null) {
                serialCom.stopSend();
                serialCom.close();
            }
        }
        catch (Exception e) { Helper.toast(e.getMessage()); }
    }

    private static void b1() {
        recvdata[0] = (byte)0x52;
        recvlen = serialCom.sendSocket((byte)0x00, (byte)0x40, (short) 1, recvdata, recvdata, 100);
        if(recvlen>0) {
            StringBuilder sMsg = new StringBuilder();
            try {
                //sMsg.append(MyFunc.ByteArrToHex(recvdata, 0, recvlen));
                //Helper.log(sMsg.toString());

            } catch (Exception ex) {
                Helper.toast(ex.getMessage());
            }
        }
    }

    private static void b2() {
        recvdata[0] = (byte)0x93;
        recvlen = serialCom.sendSocket((byte)0x00, (byte)0x41, (short) 1, recvdata, recvdata, 100);
        if(recvlen>0) {
            StringBuilder sMsg = new StringBuilder();
            try {
                //sMsg.append(MyFunc.ByteArrToHex(recvdata, 0, recvlen));
                java.lang.System.arraycopy( recvdata, 0 , gcardnum, 0, 4);
                Helper.showVar("uid", NFC.bin2hex(gcardnum));

                //Helper.log(sMsg.toString());
            } catch (Exception ex) {
                Helper.toast(ex.getMessage());
            }
        }
    }

    private static void b3() {
        recvdata[0] = (byte)0x93;
        java.lang.System.arraycopy( gcardnum,0 , recvdata, 1, 4);
        recvlen = serialCom.sendSocket((byte)0x00, (byte)0x42, (short) 5, recvdata, recvdata, 100);
        if(recvlen>0) {
            StringBuilder sMsg = new StringBuilder();
            try {
                //sMsg.append(MyFunc.ByteArrToHex(recvdata, 0, recvlen));
                //Helper.log(sMsg.toString());
            } catch (Exception ex) {
                Helper.toast(ex.getMessage());
            }
        }
    }

    private static void b4(byte[] key, int sector, int block) {
        recvdata[0] = 0x60;
        recvdata[1] = (byte)sector;
        recvdata[1] *= 4;
        recvdata[1] += (byte)block;

        java.lang.System.arraycopy( key,0 , recvdata, 2, 6);
        java.lang.System.arraycopy( gcardnum,0 , recvdata, 8, 4);

        recvlen = serialCom.sendSocket((byte)0x00, (byte)0x50, (short) 12, recvdata, recvdata, 300);
        if(recvlen>0) {
            StringBuilder sMsg = new StringBuilder();
            try {
                //sMsg.append(MyFunc.ByteArrToHex(recvdata, 0, recvlen));
                //Helper.log(sMsg.toString());
            } catch (Exception ex) {
                Helper.toast(ex.getMessage());
            }
        }
    }

    private static byte[] b5(int sector, int block) {
        byte[] result = new byte[0];
        recvdata[0] = (byte)sector;
        recvdata[0] *= 4;
        recvdata[0] += (byte)block;
        recvlen = serialCom.sendSocket((byte)0x00, (byte)0x51, (short) 1, recvdata, recvdata, 100);
        if(recvlen>0) {
            StringBuilder sMsg = new StringBuilder();
            try {
                //sMsg.append(MyFunc.ByteArrToHex(recvdata, 0, recvlen));
                //Helper.log(sMsg.toString());
                result = new byte[recvlen];
                java.lang.System.arraycopy( recvdata, 0 , result, 0, recvlen);
            } catch (Exception ex) {
                Helper.toast(ex.getMessage());
            }
        }
        return result;
    }

    private static int bytesToInt(byte[] byteArray) {
        int intValue = 0;
        for (int i = 0; i < byteArray.length; i++) {
            intValue = (intValue << 8) | (byteArray[i] & 0xFF);
        }
        return intValue;
    }

    private static byte[] reverseBytes(byte[] array) {
        byte[] reversedArray = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            reversedArray[i] = array[array.length - 1 - i];
        }
        return reversedArray;
    }
    //endregion

    //region Timer
    static TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            try {
                // Выполняем действия в UI-потоке
                YNHAPI.GpioState state = mAPI.getGpioState(YNHAPI.GPIO_5);
                if (state != oldState) {
                    Helper.showVar("state", state);
                    oldState = state;
                    if (checkGpio && state == YNHAPI.GpioState.LOW) {
                        checkGpio = false;
                        Helper.showVar("checkGpio", checkGpio);
                        mAPI.setGpioMode(YNHAPI.RELAY, YNHAPI.GpioMode.INPUT);
                        mAPI.setGpioState(YNHAPI.RELAY, YNHAPI.GpioState.LOW);
                        mAPI.setGpioState(YNHAPI.RELAY, YNHAPI.GpioState.HIGH);
                        String html_cont = ("<div class=\"green item-number\">" + Helper.set.cont_text4 + "</div>");
                        Helper.showNfc("arrow_up.svg", html_cont, 0);
                    }
                    else if (state == YNHAPI.GpioState.HIGH) {
                        mAPI.setGpioState(YNHAPI.RELAY, YNHAPI.GpioState.LOW);
                        Helper.closeDialog();
                    }
                }
            }
            catch (Exception e) { Helper.toast(e.getMessage()); }
        }
    };

    public static void StartTimer() {
        timer.scheduleAtFixedRate(timerTask, 0, 100);
    }

    static TimerTask timerTask1 = new TimerTask() {
        @Override
        public void run() {
            try {
                CloseComPort();
                OpenComPort();
            }
            catch (Exception e) { Helper.toast(e.getMessage()); }
        }
    };

    public static void StartTimer1() {
        timer.scheduleAtFixedRate(timerTask1, 0, 300000);
    }
    //endregion
}
