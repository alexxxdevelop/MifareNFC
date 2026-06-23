package com.mifare.mifare;

import static android.app.PendingIntent.FLAG_MUTABLE;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NFC {
    public static NfcAdapter mNfcAdapter;
    public static final String MIME_TEXT_PLAIN = "text/plain";

    public static void nfcInit(AppCompatActivity a) {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(a);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            //Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            //finish();
        }
    }

    public static void onResume(AppCompatActivity a) {
        try {
            if (mNfcAdapter == null) return;

            final Intent intent = new Intent(a.getApplicationContext(), a.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            final PendingIntent pendingIntent = PendingIntent.getActivity(a.getApplicationContext(), 0, intent, FLAG_MUTABLE);

            IntentFilter[] filters = new IntentFilter[1];
            String[][] techList = new String[][]{ new String[] { MifareClassic.class.getName() } };

            // Notice that this is the same filter as in our manifest.
            filters[0] = new IntentFilter();
            filters[0].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
            filters[0].addCategory(Intent.CATEGORY_DEFAULT);
            filters[0].addDataType(MIME_TEXT_PLAIN);

            mNfcAdapter.enableForegroundDispatch(a, pendingIntent, filters, techList);
        }
        catch (Exception e) { }
    }

    public static void onNewIntent(Intent intent, AppCompatActivity a) {
        try {
            Tag tag = intent.getParcelableExtra(mNfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            boolean haveMifareUltralight = false;
            for (String tech : techList) {
                if (tech.indexOf("MifareClassic") >= 0) {
                    haveMifareUltralight = true;
                    break;
                }
            }
            if (!haveMifareUltralight) {
                Toast.makeText(a, "MifareClassic is not supported", Toast.LENGTH_LONG).show();
                return;
            }

            handleFields(tag);
        }
        catch (Exception e) { Helper.toast("Ошибка чтения! Приложите и удерживайте вашу карту или браслет"); }
    }

    static MifareData readTag(Tag tag, Field[] fields) {
        MifareClassic mfc = MifareClassic.get(tag);
        // Read TAG
        try {
            MifareData result = new MifareData();
            //Enable I/O operations to the tag from this TagTechnology object.
            mfc.connect();
            byte[] xorKey = getXorKey(tag.getId());
            int sectorCount = mfc.getSectorCount(); // Get the number of sectors contained in TAG
            for (int i = 0; i < sectorCount; i++) {
                Sector sector = new Sector();
                try {
                    int _i = i;
                    boolean readSector = Arrays.stream(fields).anyMatch(x -> x.sector == _i);
                    if (readSector) {
                        //Authenticate a sector with key A.
                        Field f = Arrays.stream(fields).filter(x -> x.sector == _i).findFirst().orElse(null);
                        byte[] key = new byte[0];
                        switch (Helper.set.algo) {
                            case 0: case 2: key = hexToBytes(f.key); break;
                            case 1: key = xorKey; break;
                        }
                        boolean auth = mfc.authenticateSectorWithKeyA(i, key);
                        int bCount;
                        int bIndex;
                        if (auth) {
                            // Read the block in the sector
                            bCount = mfc.getBlockCountInSector(i);
                            bIndex = mfc.sectorToBlock(i);
                            for (int j = 0; j < bCount; j++) {
                                Block block = new Block();
                                int _j = j;
                                boolean readBlock = false;
                                switch (Helper.set.algo) {
                                    case 0: readBlock = Arrays.stream(fields).anyMatch(x -> x.block == _j); break;
                                    case 1: readBlock = _j == 2; break;
                                    case 2: readBlock = _j == 0; break;
                                }
                                if (readBlock) {
                                    byte[] data = mfc.readBlock(bIndex);
                                    block.bytes = data;
                                }
                                bIndex++;
                                sector.blocks.add(block);
                            }
                        }
                        else Helper.toast("Неверный ключ");
                    }
                }
                catch (Exception e) {  }
                result.sectors.add(sector);
            }
            return result;
        } catch (Exception e) {

        } finally {
            if (mfc != null) {
                try {
                    mfc.close();
                } catch (IOException e) {

                }
            }
        }
        return null;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length * 2) + "X", new BigInteger(1,data));
    }

    public static boolean handleFields(Tag tag) {
        Helper.isError = true;
        boolean isValue = false;
        Field[] fields = new Field[0];
        switch (Helper.set.algo) {
            case 0: fields = Helper.set.fields; break;
            case 1: fields = Helper.set.fields1; break;
            case 2: fields = Helper.set.fields2; break;
        }
        if (fields == null || fields.length == 0) Helper.toast("Отсутствуют поля для считывания");
        else {
            int secs = Helper.set.text_time;
            MifareData md = null;
            if (Helper.set.deviceType == 0) md = readTag(tag, fields);
            else if (Helper.set.deviceType == 1) md = SP.getMifare(fields);
            List<String> items = new ArrayList<>();
            if (md != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < fields.length; i++) {
                    Field f = fields[i];
                    switch (Helper.set.algo) {
                        case 1: f.block = 2; f.byte1 = 4; f.byte2 = 7; break;
                        case 2: f.block = 0; f.byte1 = 6; f.byte2 = 7; break;
                    }
                    byte[] bytes = Arrays.copyOfRange(md.sectors.get(f.sector).blocks.get(f.block).bytes, f.byte1, f.byte2 + 1);

                    int value = 0, value1 = 0;
                    String data = "";
                    switch (Helper.set.algo) {
                        case 0:
                            value = (bytes[0] & 0xFF) << 8 | (bytes[1] & 0xFF);
                            if (f.conv == 0) data = String.valueOf(value); else if (f.conv == 1) data = bytesToHexString(bytes);
                            break;
                        case 1:
                            data = bin2hex(bytes);
                            data = Character.toString(data.charAt(1)) + Character.toString(data.charAt(3)) + Character.toString(data.charAt(5)) + Character.toString(data.charAt(7));
                            value = Integer.parseInt(data);
                            break;
                        case 2:
                            byte flag = md.sectors.get(f.sector).blocks.get(f.block).bytes[2];
                            value1 = (bytes[0] & 0xFF) << 8 | (bytes[1] & 0xFF);
                            if (flag == 21) {
                                value = value1;
                                data = String.valueOf(value);
                            }
                            byte batteryByte = md.sectors.get(f.sector).blocks.get(f.block).bytes[3];
                            String battery = "";
                            if (batteryByte == 1) battery = "Полный заряд батареи";
                            else if (batteryByte == 2) battery = "Средний заряд батареи";
                            else if (batteryByte == 3) battery = "Низкий заряд батареи. Требуется замена";
                            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
                            items.add(sdf.format(new Date()) + ";" + f.sector + ";" + value1 + ";" + battery);
                            break;
                    }

                    if (value > 0) isValue = true;
                    if (Helper.set.deviceType == 1 && value <= 0) secs = Helper.set.text_time + Helper.set.cont_wait_gpio;
                    html(sb, f, value, data);
                }
                if (Helper.set.algo == 2) System.uploadFile(items);

                StringBuilder sb_cont = new StringBuilder();
                html_cont(sb_cont, isValue, sb);

                Helper.showNfc(sb.toString(), sb_cont.toString(), secs);
                Helper.isError = false;
            }
        }
        return isValue;
    }

    static void html(StringBuilder sb, Field f, int value, Object data) {
        sb.append("<div class=\"flex1 h100\">");
        sb.append("<div class=\"h100 flex flex-column flex-stretch flex-mobile\">");
        if (value > 0) {
            sb.append("<h2 class=\"border nfc-panel\" style=\"margin-top: 50px\">" + f.name + "</h2>");
            sb.append("<div class=\"center-items item-number1 border nfc-panel flex1\">" + data + "</div>");
        } else {
            sb.append("<h2 class=\"border nfc-panel\" style=\"margin-top: 50px\">" + f.name + "</h2>");
            sb.append("<div class=\"center-items item-number border nfc-panel flex1\">" + f.name1 + "</div>");
        }
        sb.append("</div>");
        sb.append("</div>");
    }

    static void html_cont(StringBuilder sb_cont, boolean isValue, StringBuilder sb) {
        if (Helper.set.deviceType == 1) {
            if (isValue) {
                sb_cont.append("<div class=\"red item-number\">" + Helper.set.cont_text1 + "</div><br />");
                sb_cont.append("<div class=\"cont-text\">" + Helper.set.cont_text2 + "</div>");
            }
            else {
                sb_cont.append("<div class=\"cont-text\">" + Helper.set.cont_text3 + "</div>");
                sb.setLength(0);
            }
        }
    }

    public static byte[] getXorKey(byte[] uid) {
        byte[] xor = hexToBytes("073A66C854");
        byte xor0 = (byte)(uid[0] ^ xor[0]);
        byte xor1 = (byte)(uid[1] ^ xor[1]);
        byte xor2 = (byte)(uid[2] ^ xor[2]);
        byte xor3 = (byte)(uid[3] ^ xor[3]);
        byte xor4 = (byte)(uid[0] ^ uid[1] ^ uid[2] ^ uid[3] ^ xor[4]);
        byte[] key = { xor0, xor1, xor2, xor3, xor4, -57 };
        return key;
    }
}
