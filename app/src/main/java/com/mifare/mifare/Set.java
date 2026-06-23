package com.mifare.mifare;

public class Set {
    public String pass = "0000";
    public String logo;
    public String media;
    public String text_center = "ПРИЛОЖИТЕ БРАСЛЕТ ИЛИ КАРТУ К СЧИТЫВАТЕЛЮ";
    public String text_righttop = "Инфотерминал";
    public int text_time = 5;
    public int static_img_secs = 5;
    public int media_mins = 5;
    public String border_color = "#ab6e00";
    public String arrow_color = "#ab6e00";
    public Field[] fields;
    public Field[] fields1;
    public Field[] fields2;
    public int algo = 0;
    public int deviceType = 0;

    public int cont_wait_gpio = 5;
    public String cont_text1 = "Выход запрещен!";
    public String cont_text2 = "Освободите занятый вами шкаф";
    public String cont_text3 = "Обработка информации. Пожалуйста, подождите";
    public String cont_text4 = "Проходите!";
    public String cont_text5 = "Обратитесь к администратору";
    public int cont_text_secs = 5;
    public boolean debugMode = false;
    public int wiegandOrder = 0;
    public int wiegandMode = 0;
    public int portSpeed = 9600;
    public String yandexToken;
    public String yandexFolder;
    public boolean statusBar = false;
    public boolean navigationBar = false;

    public String pass2 = "16052005";
    public Boolean set1;
    public Boolean set2;
    public Boolean set3;
    public Boolean set4;
    public Boolean set5;
    public Boolean set6;
    public Boolean set7;
    public Boolean set8;
    public Boolean set9;
    public Boolean set10;
    public Boolean set11;
    public Boolean set12;
    public Boolean set13;
    public Boolean set14;
    public Boolean set15;
    public Boolean set16;
    public Boolean set20;
    public Boolean set21;
    public Boolean set22;
    public Boolean set23;
    public Boolean set24;
    public Boolean set25;
    public Boolean set26;
    public Boolean set27;
    public Boolean set29;
    public Boolean set30;
}

class Field {
    public String name;
    public String name1;
    public int sector;
    public int block;
    public int byte1;
    public int byte2;
    public String key = "FFFFFFFFFFFF";
    public int conv = 0;
}

