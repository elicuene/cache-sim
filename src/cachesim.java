import java.util.*;
import java.io.*;


public class cachesim {

    private int size;
    private int assoc;
    private int block_Size;
    private String config;
    private File tracefile;
    private Block[][] cacheArray;
    private byte[] mainMem;
    int sets;
    String address;

    public static void main(String[] args) throws FileNotFoundException {

        File filename = new File(args[0]);
        Scanner scan = new Scanner(filename);
        int cacheSize = Integer.parseInt(args[1]) * 1024;
        int associativity = Integer.parseInt(args[2]);
        String cacheConfig = args[3];
        int blockSize = Integer.parseInt(args[4]);

        cachesim simulator = new cachesim(filename, cacheSize, associativity, cacheConfig, blockSize);


        while (scan.hasNextLine()) {
            boolean isLoad = false;
            boolean hit = false;
            String input = scan.nextLine();
            String[] splitIn = input.split(" ");

            String insn;
            String access;
            int accessSize;
            String valueToWrite;

            insn = splitIn[0];
            access = splitIn[1];
            accessSize = Integer.parseInt(splitIn[2]);

            if (splitIn.length == 3) {
                valueToWrite = null;
                isLoad = true;
            }
            else {
                valueToWrite = splitIn[3];
            }
            hit = simulator.isHit(access);

            int decimalAddress = simulator.getDecimalAddress(access);


            int accessTag = simulator.getAccessTag(decimalAddress);
            int accessOffset = simulator.getAccessOffset(decimalAddress);
            int accessIndex = simulator.getAccessIndex(decimalAddress);

            if (isLoad && hit) {
                simulator.loadHit(decimalAddress, accessTag, accessIndex, accessOffset, accessSize, access);
            }
            else if (isLoad && !hit) {
                simulator.loadMiss(decimalAddress, accessSize, blockSize, accessOffset, access);
            }
            else if (!isLoad && hit) {
                byte[] writeData;
                writeData = simulator.getByteArray(accessSize, valueToWrite);
                simulator.writeDataHit(decimalAddress, writeData, accessTag, accessIndex, accessOffset, access);
            }
            else {
                byte[] writeData;
                writeData = simulator.getByteArray(accessSize, valueToWrite);
                simulator.writeMiss(decimalAddress, writeData, accessTag, accessIndex, accessOffset, access);
            }
        }
    }

    public cachesim(File file, int sizes, int assocs, String configs, int blockSize) {
        size = sizes;
        assoc = assocs;
        block_Size = blockSize;
        config = configs;
        tracefile = file;
        mainMem = new byte[(int)Math.pow(2, 16)];

        int frameCount = size / blockSize;
        sets = frameCount / assoc;

        cacheArray = new Block[sets][assoc];

        for (int i = 0; i < sets; i++) {
            for (int j = 0; j < assoc; j++) {
                cacheArray[i][j] = new Block();
            }
        }



    }

    public void loadMiss(int decimalAddress, int readSize, int block_Size, int offset, String originalAddy) {
        byte[] bytesFromMem = new byte[block_Size];
        for (int i = 0; i < block_Size; i++) {
            bytesFromMem[i] = mainMem[decimalAddress+i];
        }
        Block newBlock = new Block(block_Size, sets, decimalAddress, bytesFromMem);
        int addIndex = newBlock.index;
        newBlock.dirty = 0;
        loadMissEdit(newBlock, addIndex);

        String toPrint = "";
        for (int i = 0; i < readSize; i++) {
            String toAdd = Integer.toHexString(newBlock.data[i]);
            if (toAdd.length() == 8) toAdd = toAdd.substring(6);
            if (newBlock.data[i] == 0) toAdd = "00";
            if (newBlock.data[i] < 16 && newBlock.data[i] > 0) toAdd = "0" + toAdd;

            toPrint = toPrint + toAdd;
        }

        System.out.println("load "+originalAddy+" miss "+toPrint);
    }

    public void loadHit(int decimalAddress, int tag, int index, int offset, int readSize, String originalAddy) {
        String toPrint = "";
        for (int i = 0; i < cacheArray[index].length; i++) {
            if (cacheArray[index][i].tag == tag && cacheArray[index][i].valid) {
                for (int j = 0; j < readSize; j++) {
                    String toAdd = Integer.toHexString(cacheArray[index][i].data[offset+j]);
                    if (toAdd.length()==8) toAdd = toAdd.substring(6);
                    if (cacheArray[index][i].data[j+offset] == 0) toAdd = "00";
                    if (cacheArray[index][i].data[offset+j] < 16 && cacheArray[index][i].data[offset+j] > 0) toAdd = "0" + toAdd;

                    toPrint = toPrint + toAdd;

                }
                loadHitEdit(index, i);
                break;
            }
        }
        System.out.println("load "+originalAddy+" hit "+toPrint);
    }

    public void loadHitEdit(int index, int way) {
        // find block that triggered the hit, store the block and its index
        // move everything to the right one to the left
        Block move = cacheArray[index][way];
        for (int i = way; i < cacheArray[index].length-1; i++) {
            cacheArray[index][i] = cacheArray[index][i+1];
        }
        cacheArray[index][cacheArray[index].length-1] = move;

    }

    public void loadMissEdit(Block newBlock, int index) {
        // if space exists, add it to the leftmost invalid way
        // if theres no space, check the evict block, if the block is dirty, write back
        // then remove LRU, move everything left, place it in null
        for (int i = 0; i < cacheArray[index].length; i++) {
            if (!cacheArray[index][i].valid) {
                cacheArray[index][i] = newBlock;
                cacheArray[index][i].dirty = 0;
                break;
            }
        }
        if (cacheArray[index][0].dirty == 1) {
            for (int i = 0; i < block_Size; i++) {
                mainMem[cacheArray[index][0].address + i] = cacheArray[index][0].data[i];
            }
        }

        for (int i = 0; i < cacheArray[index].length-1; i++) {
            cacheArray[index][i] = cacheArray[index][i+1];
        }
        cacheArray[index][cacheArray[index].length - 1] = newBlock;


    }

    public boolean isHit(String accessAddress) {
        int decAddress = getDecimalAddress(accessAddress);
        int accessIndex = (decAddress/block_Size) % sets;
        int accessTag = decAddress / (sets * block_Size);
        for (int i = 0; i < cacheArray[accessIndex].length; i++) {
            if (cacheArray[accessIndex][i].tag == accessTag && cacheArray[accessIndex][i].valid) {
                return true;
            }
        }
        return false;
    }

    public void writeMiss(int decimalAddress, byte[] data, int tag, int index, int offset, String originalAddy) {
        if (config.equals("wt")) {
            for (int i = 0; i < data.length; i++) {
                mainMem[decimalAddress+i] = data[i];
            }
        }

        else {
            byte[] dataFromMem = new byte[block_Size];
            int ind = 0;

            for (int i = decimalAddress - offset; i < decimalAddress - offset + block_Size; i++) {
                dataFromMem[ind++] = mainMem[i];
            }
            for (int i = 0; i < data.length; i++) {
                dataFromMem[offset+i] = data[i];
            }
            Block newBlock = new Block(block_Size, sets, decimalAddress, dataFromMem);

            boolean space = false;
            for (int i = 0; i < cacheArray[index].length; i++) {
                if (!cacheArray[index][i].valid) {
                    cacheArray[index][i] = newBlock;
                    space = true;
                }
            }

            if (space == false) {
                if (cacheArray[index][0].dirty == 1) {
                    for (int i = 0; i < cacheArray[index][0].data.length; i++) {
                        mainMem[cacheArray[index][0].address+i] = cacheArray[index][0].data[i];
                    }
                }
                for (int i = 0; i < cacheArray[index].length - 1; i++) {
                    cacheArray[index][i] = cacheArray[index][i+1];
                }
                cacheArray[index][cacheArray[index].length - 1] = newBlock;
            }

        }
        System.out.println("store "+originalAddy+" miss");
    }

    public void writeDataHit(int decimalAddress, byte[] data, int tag, int index, int offset, String originalAddy) {
        // if write-through, if hit, overwrite in cache and write to memory too, if miss only write to memory


        // if write-back, if hit, write overwrite cache but not memory, make it dirty
        // if miss, add proper block to cache, in the block, overwrite what was in memory

        // for copying the memory into the block, int i = address - offset; i < address-offset+blockSize

        if (config.equals("wt")) {
            // write to cache
            for (int i = 0; i < cacheArray[index].length; i++) {
                if (tag == cacheArray[index][i].tag && cacheArray[index][i].valid) {
                    for (int j = 0; j < data.length; j++) {
                        cacheArray[index][i].data[offset+j] = data[j];
                    }
                    break;
                }
            }
            // write to memory
            for (int i = 0; i < data.length; i++) {
                mainMem[decimalAddress+i] = data[i];
            }
        }

        else if (config.equals("wb")) {

            for (int i = 0; i < data.length; i++) {
                if (tag == cacheArray[index][i].tag && cacheArray[index][i].valid) {
                    for (int j = 0; j < data.length; j++) {
                        cacheArray[index][i].data[j+offset] = data[j];
                    }
                    cacheArray[index][i].dirty = 1;
                    break;
                }
            }


//            byte[] newData = new byte[block_Size];
//
//            for (int i = 0; i < data.length; i++) {
//                newData[i] = data[i];
//            }
//
//            Block toPlace = new Block(block_Size, assoc, decimalAddress, newData);
//
//            for (int i = 0; i < cacheArray[index].length; i++) {
//                if (tag == cacheArray[index][i].tag) {
//                    cacheArray[index][i] = toPlace;
//                }
//            }
        }

        System.out.println("store "+originalAddy+" hit");



    }

    public byte[] getByteArray(int dataSize, String data) {
        byte[] byteArray = new byte[dataSize];

        for (int i = 0; i < data.length(); i += 2) {
            byteArray[i/2] = (byte) ((Character.digit(data.charAt(i), 16) << 4) + Character.digit(data.charAt(i+1), 16));
        }
        return byteArray;
    }

    public int getDecimalAddress(String hexAddress) {
        int decimal = Integer.parseInt(hexAddress, 16);
        return decimal;
    }

    public int getAccessTag(int decimalAddress) {
        int accessTag = decimalAddress / (sets * block_Size);
        return accessTag;
    }

    public int getAccessOffset(int decimalAddress) {
        int accessOffset = decimalAddress % block_Size;
        return accessOffset;
    }

    public int getAccessIndex(int decimalAddress) {
        int accessIndex = (decimalAddress / block_Size) % sets;
        return accessIndex;
    }
}

class Block {
    int address;
    int size;
    int setsCount;
    int index;
    int offset;
    int tag;
    byte[] data;
    int dirty;
    boolean valid;

    public Block() {
        tag = 0;
        size = 0;
        setsCount = 0;
        dirty = 0;
        valid = false;
    }

    public Block(int block_size, int setCount, int decimalAddress, byte[] dataIn) {
        address = decimalAddress;
        size = block_size;
        setsCount = setCount;
        dirty = 1;
        valid = true;
        data = dataIn;

        index = (address/size) % setsCount;
        offset = address % size;
        tag = address / (setsCount * size);

    }
}
