package dev.draxon.gui;
import java.util.Map;
/** Alpha masks (14x14, hex digit = 0..15 alpha) generated from the supplied icon sheet. */
final class IconData {
    static final int SIZE = 14;
    static final Map<String, String[]> MASKS = Map.of(
        "COMBAT", new String[]{"00000000000000","05840000003760","06ff900007ff80","01fff9007fff30","003eff74fff500","0002dff8be3000","00000aff810000","004289aff62400","005d7f58e7d600","002fe5005ef200","03fb780087bf30","0fc00000000cf1","04200000000140","00000000000000"},
        "MOVEMENT", new String[]{"000000001b9000","000000009ff600","000023325ff400","0005ffffa53000","004fb4dff80000","008c03ffe96340","00001eff78fff2","00006ffc006420","0000dfff900000","0018f74dfb0000","19efd002fc0000","2fe81006f60000","0210000cf10000","00000009900000"},
        "RENDER", new String[]{"00000000000000","000027bc840000","0009ffd9ffb100","00aeaf40cfed10","05f40dd9f50e90","0bfc8ffff83ed0","0eeaffffffffc0","0f709fffffff60","0cd8efffb89400","05fffffd000000","00bffffe200000","001bffff900000","00005acc300000","00000000000000"},
        "PLAYER", new String[]{"00000000000000","000004aa400000","00004ffff40000","0000affffa0000","0000affffa0000","00003ffff30000","00000288200000","00000366410000","0005dffffe6000","006ffffffff600","01effffffffe00","03ffffffffff30","00566666666500","00000000000000"},
        "MISC", new String[]{"00000133000000","000007ff500000","00300bffa01300","03fddffffcdf20","0dfffd77dfffc0","09ffc0001dff80","00af500007f900","00af500007f900","09ffc0001dff80","0dfffc67dfffc0","03fdcffffcdf20","00300bffa01300","000008ff600000","00000144100000"}
    );
    private IconData() {}
}
