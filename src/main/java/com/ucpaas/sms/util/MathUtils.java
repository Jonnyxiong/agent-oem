package com.ucpaas.sms.util;

import java.math.BigDecimal;

public class MathUtils
{
    public static BigDecimal doubleToDecimal(double d)
    {
        return new BigDecimal(Double.toString(d));
    }
    
    public static double add(double d1, double d2, int len)
    {
        // 进行加法运算
        return round(add(d1, d2), len);
    }
    
    public static double add(double d1, double d2)
    {
        // 进行加法运算
        BigDecimal b1 = new BigDecimal(Double.toString(d1));
        BigDecimal b2 = new BigDecimal(Double.toString(d2));
        return b1.add(b2).doubleValue();
    }
    
    public static String addAsStr(double d1, double d2, int len)
    {
        return roundAsString(add(d1, d2), len);
    }
    
    public static double sub(double d1, double d2, int len)
    {
        // 进行减法运算
        return round(sub(d1, d2), len);
    }
    
    public static double sub(double d1, double d2)
    { // 进行减法运算
        BigDecimal b1 = new BigDecimal(Double.toString(d1));
        BigDecimal b2 = new BigDecimal(Double.toString(d2));
        return b1.subtract(b2).doubleValue();
    }
    
    public static String subAsStr(double d1, double d2, int len)
    {
        return roundAsString(sub(d1, d2), len);
    }
    
    public static double mul(double d1, double d2, int len)
    {
        // 进行乘法运算
        return round(mul(d1, d2), len);
    }
    
    public static double mul(double d1, double d2)
    { // 进行乘法运算
        BigDecimal b1 = new BigDecimal(Double.toString(d1));
        BigDecimal b2 = new BigDecimal(Double.toString(d2));
        return b1.multiply(b2).doubleValue();
    }
    
    public static String mulAsStr(double d1, double d2, int len)
    {
        return roundAsString(mul(d1, d2), len);
    }
    
    public static double div(double d1, double d2, int len)
    {// 进行除法运算
        BigDecimal b1 = new BigDecimal(Double.toString(d1));
        BigDecimal b2 = new BigDecimal(Double.toString(d2));
        return b1.divide(b2, len, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    public static String divAsStr(double d1, double d2, int len)
    {
        return Double.toString(div(d1, d2, len));
    }
    
    public static double round(double d, int len)
    {
        // 进行四舍五入操作
        BigDecimal b1 = new BigDecimal(Double.toString(d));
        BigDecimal b2 = new BigDecimal(1);
        // 任何一个数字除以1都是原数字
        // ROUND_HALF_UP是BigDecimal的一个常量，表示进行四舍五入的操作
        return b1.divide(b2, len, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    public static String roundAsString(double d, int len)
    {
        return Double.toString(round(d, len));
    }
    

}
