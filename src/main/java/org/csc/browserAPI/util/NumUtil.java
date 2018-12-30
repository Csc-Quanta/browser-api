package org.csc.browserAPI.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumUtil {
    /**
     * 去零
     *
     * @param amount
     * @param scale
     * @return
     */
    public static String dealZero(String amount, int scale) {
        BigDecimal bigDecimal = new BigDecimal(amount);
        amount = bigDecimal.divide(new BigDecimal(10).pow(18)).setScale(scale, RoundingMode.HALF_DOWN)
                .stripTrailingZeros().toPlainString();
        return amount;
    }
}
