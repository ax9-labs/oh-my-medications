package com.personal.medtracker;

/** Stable identifiers keep saved choices independent of their display labels. */
public final class MedAppearance {
    public static final String[] TYPES={"capsule","tablet","syrup","package","blister","drops"};
    private static final int[] TYPE_LABELS={R.string.icon_capsule,R.string.icon_tablet,R.string.icon_syrup,R.string.icon_package,R.string.icon_blister,R.string.icon_drops};
    public static final String[] COLORS={"default","rose","coral","amber","mint","blue","purple","slate","teal"};
    private static final int[] COLOR_LABELS={R.string.color_default,R.string.color_rose,R.string.color_coral,R.string.color_amber,R.string.color_mint,R.string.color_blue,R.string.color_purple,R.string.color_slate,R.string.color_teal};
    private static final int[] DEFAULTS={0xFF32988A,0xFFD882A3,0xFFD69C3B,0xFF678DD3,0xFF9382C9,0xFF58A7BC};
    private static final int[] PALETTE={0,0xFFD882A3,0xFFE18870,0xFFD69C3B,0xFF69B592,0xFF678DD3,0xFF9382C9,0xFF7D8F9D,0xFF32988A};
    private MedAppearance(){}
    private static int index(String[] choices,String value){for(int i=0;i<choices.length;i++)if(choices[i].equals(value))return i;return 0;}
    public static String type(String value){return TYPES[index(TYPES,value)];}
    public static String color(String value){return COLORS[index(COLORS,value)];}
    public static String typeLabel(android.content.Context c,String value){return AppLocale.text(c,TYPE_LABELS[index(TYPES,value)]);}
    public static String colorLabel(android.content.Context c,String value){return AppLocale.text(c,COLOR_LABELS[index(COLORS,value)]);}
    public static int tint(String type,String color){int i=index(COLORS,color);return i==0?DEFAULTS[index(TYPES,type)]:PALETTE[i];}
}
