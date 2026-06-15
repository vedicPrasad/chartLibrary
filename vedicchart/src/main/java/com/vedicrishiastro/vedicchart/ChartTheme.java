package com.vedicrishiastro.vedicchart;

import android.graphics.Color;

public final class ChartTheme {
    private final int backgroundColor;
    private final int borderColor;
    private final int signTextColor;
    private final int planetTextColor;
    private final int accentColor;
    private final int[] houseColors;
    private final int selectedHouseColor;
    private final float borderWidth;
    private final float signTextSizeSp;
    private final float planetTextSizeSp;
    private final boolean showSignNames;

    private ChartTheme(Builder builder) {
        backgroundColor = builder.backgroundColor;
        borderColor = builder.borderColor;
        signTextColor = builder.signTextColor;
        planetTextColor = builder.planetTextColor;
        accentColor = builder.accentColor;
        houseColors = builder.houseColors.clone();
        selectedHouseColor = builder.selectedHouseColor;
        borderWidth = builder.borderWidth;
        signTextSizeSp = builder.signTextSizeSp;
        planetTextSizeSp = builder.planetTextSizeSp;
        showSignNames = builder.showSignNames;
    }

    public static ChartTheme light() {
        return new Builder()
                .backgroundColor(Color.rgb(255, 253, 248))
                .borderColor(Color.rgb(43, 35, 28))
                .signTextColor(Color.rgb(52, 64, 84))
                .planetTextColor(Color.rgb(154, 32, 45))
                .accentColor(Color.rgb(193, 132, 38))
                .houseColors(defaultHouseColors())
                .selectedHouseColor(Color.rgb(242, 143, 43))
                .build();
    }

    public static ChartTheme dark() {
        return new Builder()
                .backgroundColor(Color.rgb(15, 23, 42))
                .borderColor(Color.rgb(226, 232, 240))
                .signTextColor(Color.rgb(203, 213, 225))
                .planetTextColor(Color.rgb(251, 191, 36))
                .accentColor(Color.rgb(217, 119, 6))
                .houseColor(Color.rgb(238, 214, 179))
                .selectedHouseColor(Color.rgb(217, 119, 6))
                .build();
    }

    public static ChartTheme temple() {
        return new Builder()
                .backgroundColor(Color.rgb(255, 248, 232))
                .borderColor(Color.rgb(92, 46, 20))
                .signTextColor(Color.rgb(87, 69, 56))
                .planetTextColor(Color.rgb(171, 37, 46))
                .accentColor(Color.rgb(203, 125, 24))
                .houseColor(Color.rgb(238, 214, 179))
                .selectedHouseColor(Color.rgb(203, 125, 24))
                .build();
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public int getSignTextColor() {
        return signTextColor;
    }

    public int getPlanetTextColor() {
        return planetTextColor;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public int getHouseColor(int index) {
        if (houseColors.length == 0) {
            return Color.rgb(238, 214, 179);
        }
        int colorIndex = ((index % houseColors.length) + houseColors.length) % houseColors.length;
        return houseColors[colorIndex];
    }

    public int[] getHouseColors() {
        return houseColors.clone();
    }

    public int getSelectedHouseColor() {
        return selectedHouseColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public float getSignTextSizeSp() {
        return signTextSizeSp;
    }

    public float getPlanetTextSizeSp() {
        return planetTextSizeSp;
    }

    public boolean shouldShowSignNames() {
        return showSignNames;
    }

    private static int[] defaultHouseColors() {
        return new int[]{
                Color.rgb(244, 208, 127),
                Color.rgb(136, 198, 181),
                Color.rgb(237, 156, 122),
                Color.rgb(142, 181, 230),
                Color.rgb(210, 168, 219),
                Color.rgb(226, 196, 112),
                Color.rgb(128, 197, 211),
                Color.rgb(231, 145, 153),
                Color.rgb(165, 204, 132),
                Color.rgb(185, 166, 224),
                Color.rgb(224, 172, 105),
                Color.rgb(132, 187, 158),
        };
    }

    public static final class Builder {
        private int backgroundColor = Color.rgb(255, 253, 248);
        private int borderColor = Color.rgb(43, 35, 28);
        private int signTextColor = Color.rgb(52, 64, 84);
        private int planetTextColor = Color.rgb(154, 32, 45);
        private int accentColor = Color.rgb(193, 132, 38);
        private int[] houseColors = defaultHouseColors();
        private int selectedHouseColor = Color.rgb(242, 143, 43);
        private float borderWidth = 2f;
        private float signTextSizeSp = 14f;
        private float planetTextSizeSp = 16f;
        private boolean showSignNames = false;

        public Builder backgroundColor(int value) {
            backgroundColor = value;
            return this;
        }

        public Builder borderColor(int value) {
            borderColor = value;
            return this;
        }

        public Builder signTextColor(int value) {
            signTextColor = value;
            return this;
        }

        public Builder planetTextColor(int value) {
            planetTextColor = value;
            return this;
        }

        public Builder accentColor(int value) {
            accentColor = value;
            return this;
        }

        public Builder houseColor(int value) {
            houseColors = new int[]{value};
            return this;
        }

        public Builder houseColors(int... values) {
            if (values == null || values.length == 0) {
                houseColors = defaultHouseColors();
            } else {
                houseColors = values.clone();
            }
            return this;
        }

        public Builder selectedHouseColor(int value) {
            selectedHouseColor = value;
            return this;
        }

        public Builder borderWidth(float value) {
            borderWidth = value;
            return this;
        }

        public Builder signTextSizeSp(float value) {
            signTextSizeSp = value;
            return this;
        }

        public Builder planetTextSizeSp(float value) {
            planetTextSizeSp = value;
            return this;
        }

        public Builder showSignNames(boolean value) {
            showSignNames = value;
            return this;
        }

        public ChartTheme build() {
            return new ChartTheme(this);
        }
    }
}
