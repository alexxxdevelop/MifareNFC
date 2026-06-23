package com.mifare.mifare;

import java.util.ArrayList;
import java.util.List;

public class MifareData {
    public List<Sector> sectors = new ArrayList<>();
}

class Sector {
    public List<Block> blocks = new ArrayList<>();
}

class Block {
    public byte[] bytes;
}
