package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Arrays;

/**
 * 串口辅助工具类
 */
public abstract class SerialHelper {
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private SendThread mSendThread;
    private String sPort = "/dev/";
    private int iBaudRate = 9600;
    private boolean _isOpen = false;
    private byte[] _bLoopData = new byte[]{0x30};
    private int iDelay = 500;
    private byte[] recvdata = new byte[1500];
    private int recvlen=0;
    private boolean readyFlag=false;

    //----------------------------------------------------
    public SerialHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public SerialHelper() {
        this("/dev/", 9600);
    }

    public SerialHelper(String sPort) {
        this(sPort, 9600);
    }

    public SerialHelper(String sPort, String sBaudRate) {
        this(sPort, Integer.parseInt(sBaudRate));
    }

    //----------------------------------------------------
    public void open() throws SecurityException, IOException, InvalidParameterException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();
        mReadThread = new ReadThread();
        mReadThread.start();
        mSendThread = new SendThread();
        mSendThread.setSuspendFlag();
        mSendThread.start();
        _isOpen = true;
    }

    //----------------------------------------------------
    public void close() {
        if (mReadThread != null)
            mReadThread.interrupt();
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
        _isOpen = false;
    }

    //----------------------------------------------------
    public int send(byte[] bOutArray) {
        int iResult = 0;
        try {
            if (mOutputStream == null)
                return 0;
            if (!_isOpen)
                return 0;

            mOutputStream.write(bOutArray);
            iResult = bOutArray.length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return iResult;
    }

    //----------------------------------------------------
    public void sendHex(String sHex) {
        byte[] bOutArray = MyFunc.HexToByteArr(sHex);
        send(bOutArray);
    }

    //----------------------------------------------------
    public void sendTxt(String sTxt) {
        byte[] bOutArray = sTxt.getBytes();
        send(bOutArray);
    }

    //发送接收数据通道
    public int sendSocket(byte addr, byte cmd, short datalen, byte[] sdata, byte[] rdata, int timeout)
    {
        byte[] senddata = new byte[datalen+8];
        short sendlen=0;
        byte bccresult;
        int rdatalen;
        int timecount = timeout;

        recvlen = 0;
        readyFlag = false;

        senddata[sendlen] = (byte) 0xAB;    sendlen++;
        senddata[sendlen] = (byte) 0x00;    sendlen++;
        senddata[sendlen] = (byte) addr;    sendlen++;
        senddata[sendlen] = (byte) ((datalen+1)>>8);    sendlen++;
        senddata[sendlen] = (byte) (datalen+1);       sendlen++;
        senddata[sendlen] = (byte) cmd;     sendlen++;

        if((sdata != null) && (datalen>0)){
            System.arraycopy( sdata,0,senddata, sendlen,datalen);   sendlen += datalen;
        }

        senddata[sendlen] = MyFunc.bccCalc(senddata, 1, sendlen -1); sendlen++;

        senddata[sendlen] = (byte) 0xBA;   sendlen++;

        //发送数据
        send(senddata);

        //等待返回结果
        while(recvlen<=0){
//            MyFunc.delayMs(1);
            try{

                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            timecount--;
            if(timecount==0){
                break;
            }
        }
//        Log.d("recv2",""+recvlen+"time:"+timecount);
//        Log.d("recv2",MyFunc.ByteArrToHex(recvdata, 0, recvlen));
        if(recvlen>0){

            if(((byte)recvdata[0] == (byte)0xAB) && ((byte)recvdata[recvlen-1] == (byte)0xBA)){

                rdatalen = (short) ((recvdata[3]<<8) +recvdata[4]) - 1;

                bccresult = MyFunc.bccCalc(recvdata, 1, rdatalen+5);

                if((byte)bccresult == (byte)recvdata[recvlen-2]) {

                    if(rdatalen>0){
                        System.arraycopy( recvdata,6, rdata, 0, rdatalen);
                    }

                    return rdatalen;
                }
            }
            recvlen = 0;
        }

        readyFlag = false;

        if(timeout==0) {
            return -2;
        }
        return -1;
    }

    //----------------------------------------------------
    private class ReadThread extends Thread {
        short UART_LEN=0;
        byte[] USART_BUF = new byte[1100];
        int timeout=0;

        @Override
        public void run() {
            super.run();

            while (!isInterrupted()) {
                try {
                    if (mInputStream == null) return;

                    if (mInputStream.available() <= 0) {

                        if (timeout > 0) {

                            try {
                                Thread.sleep(1);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }

                            timeout--;
                            if (timeout == 0) { //超时接收，可以接收不同长度协议
//                                if (!readyFlag)
                                {
                                    readyFlag = true;
                                    recvlen = UART_LEN;
                                    System.arraycopy(USART_BUF, 0, recvdata, 0, UART_LEN);
//                                    Log.d("recv","UART_LEN："+UART_LEN);
//                                    Log.d("recv int:", MyFunc.ByteArrToHex(recvdata, 0, recvlen));
                                }
                                ComBean ComRecData = new ComBean(sPort, USART_BUF, UART_LEN);
                                onDataReceived(ComRecData);
                                UART_LEN = 0;
                            }
                        }

                        continue;
                    }

                    byte[] buffer = new byte[1100];
                    int size = mInputStream.read(buffer);
                    if (size > 0) {

                        timeout = 3;

                        System.arraycopy(buffer,0, USART_BUF, UART_LEN, size);

                        UART_LEN += size;
                        if(UART_LEN > 1100){
                            UART_LEN = 0;
                        }
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    //----------------------------------------------------
    private class SendThread extends Thread {
        public boolean suspendFlag = true;// 控制线程的执行

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                synchronized (this) {
                    while (suspendFlag) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                send(getbLoopData());
                try {
                    Thread.sleep(iDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //线程暂停
        public void setSuspendFlag() {
            this.suspendFlag = true;
        }

        //唤醒线程
        public synchronized void setResume() {
            this.suspendFlag = false;
            notify();
        }
    }

    //----------------------------------------------------
    public int getBaudRate() {
        return iBaudRate;
    }

    public boolean setBaudRate(int iBaud) {
        if (_isOpen) {
            return false;
        } else {
            iBaudRate = iBaud;
            return true;
        }
    }

    public boolean setBaudRate(String sBaud) {
        int iBaud = Integer.parseInt(sBaud);
        return setBaudRate(iBaud);
    }

    //----------------------------------------------------
    public String getPort() {
        return sPort;
    }

    public boolean setPort(String sPort) {
        if (_isOpen) {
            return false;
        } else {
            this.sPort = sPort;
            return true;
        }
    }

    //----------------------------------------------------
    public boolean isOpen() {
        return _isOpen;
    }

    //----------------------------------------------------
    public byte[] getbLoopData() {
        return _bLoopData;
    }

    //----------------------------------------------------
    public void setbLoopData(byte[] bLoopData) {
        this._bLoopData = bLoopData;
    }

    //----------------------------------------------------
    public void setTxtLoopData(String sTxt) {
        this._bLoopData = sTxt.getBytes();
    }

    //----------------------------------------------------
    public void setHexLoopData(String sHex) {
        this._bLoopData = MyFunc.HexToByteArr(sHex);
    }

    //----------------------------------------------------
    public int getiDelay() {
        return iDelay;
    }

    //----------------------------------------------------
    public void setiDelay(int iDelay) {
        this.iDelay = iDelay;
    }

    //----------------------------------------------------
    public void startSend() {
        if (mSendThread != null) {
            mSendThread.setResume();
        }
    }

    //----------------------------------------------------
    public void stopSend() {
        if (mSendThread != null) {
            mSendThread.setSuspendFlag();
        }
    }

    //----------------------------------------------------
    protected abstract void onDataReceived(ComBean ComRecData);
}