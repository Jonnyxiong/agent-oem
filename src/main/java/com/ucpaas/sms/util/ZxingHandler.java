package com.ucpaas.sms.util;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * 条形码和二维码编码解码
 * 
 * 
 * @version 2014-02-28
 */
public class ZxingHandler {

	/**
	 * 条形码编码
	 * 
	 * @param contents
	 * @param width
	 * @param height
	 * @param imgPath
	 */
	public static void encode(String contents, int width, int height, String imgPath) {
		int codeWidth = 3 + // start guard
				(7 * 6) + // left bars
				5 + // middle guard
				(7 * 6) + // right bars
				3; // end guard
		codeWidth = Math.max(codeWidth, width);
		try {
			BitMatrix bitMatrix = new MultiFormatWriter().encode(contents, BarcodeFormat.EAN_13, codeWidth, height,
					null);

			MatrixToImageWriter.writeToFile(bitMatrix, "png", new File(imgPath));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void encode_CODE128(String contents, int width, int height, String imgPath) {
		int codeWidth = 3 + // start guard
				(7 * 6) + // left bars
				5 + // middle guard
				(7 * 6) + // right bars
				3; // end guard
		codeWidth = Math.max(codeWidth, width);
		try {
			BitMatrix bitMatrix = new MultiFormatWriter().encode(contents, BarcodeFormat.CODE_128, codeWidth, height,
					null);

			MatrixToImageWriter.writeToFile(bitMatrix, "png", new File(imgPath));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 条形码解码
	 * 
	 * @param imgPath
	 * @return String
	 */
	public static String decode(String imgPath) {
		BufferedImage image = null;
		Result result = null;
		try {
			image = ImageIO.read(new File(imgPath));
			if (image == null) {
				System.out.println("the decode image may be not exit.");
			}
			LuminanceSource source = new BufferedImageLuminanceSource(image);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

			result = new MultiFormatReader().decode(bitmap, null);
			return result.getText();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 二维码编码
	 * 
	 * @param contents
	 * @param width
	 * @param height
	 * @param imgPath
	 */
	public static void encode2(String contents, int width, int height, String imgPath) {
		Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
		// 指定纠错等级
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		// 指定编码格式
		hints.put(EncodeHintType.CHARACTER_SET, "GBK");

		hints.put(EncodeHintType.MARGIN, 1);

		try {
			BitMatrix bitMatrix = new MultiFormatWriter().encode(contents, BarcodeFormat.QR_CODE, width, height, hints);

			MatrixToImageWriter.writeToFile(bitMatrix, "png", new File(imgPath));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 二维码解码
	 * 
	 * @param imgPath
	 * @return String
	 */
	public static String decode2(String imgPath) {
		BufferedImage image = null;
		Result result = null;
		try {
			image = ImageIO.read(new File(imgPath));
			if (image == null) {
				System.out.println("the decode image may be not exit.");
			}
			LuminanceSource source = new BufferedImageLuminanceSource(image);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

			Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
			hints.put(DecodeHintType.CHARACTER_SET, "GBK");

			result = new MultiFormatReader().decode(bitmap, hints);
			return result.getText();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void statPaas(){
		OutputStreamWriter outputStream = null;
		try {
			outputStream = new FileWriter("D://stat.sql");
			outputStream.write("select\n");
			outputStream.write("    CASE e.operatorstype\n");
			outputStream.write("        WHEN 0 THEN '全网'\n");
			outputStream.write("        WHEN 1 THEN '移动'\n");
			outputStream.write("        WHEN 2 THEN '联通'\n");
			outputStream.write("        WHEN 3 THEN '电信'\n");
			outputStream.write("        WHEN 4 THEN '国际'\n");
			outputStream.write("        WHEN 5 THEN '虚拟移动'\n");
			outputStream.write("        WHEN 6 THEN '虚拟联通'\n");
			outputStream.write("        WHEN 7 THEN '虚拟电信'\n");
			outputStream.write("    END AS \"运营商\",\n");
			outputStream.write("		CASE e.templatetype\n");
			outputStream.write("        WHEN 0 THEN '通知短信'\n");
			outputStream.write("        WHEN 4 THEN '验证码短信'\n");
			outputStream.write("        WHEN 5 THEN '营销短信'\n");
			outputStream.write("        WHEN 6 THEN '其它'\n");
			outputStream.write("    END AS \"短信类型\",\n");
			outputStream.write("		SUM(e.sms_num) as \"数量\"\n");
			outputStream.write("from (\n");

			int count = 0;
			String countStr = null;
			for (int i = 1; i < 28; i++) {
				if (i!=1){
					outputStream.write("  union all\n");
				}
				count=i;
				if (i<10)
				{
					countStr ="0"+i;
				} else{
					countStr = i+"";
				}
				outputStream.write("\n");
				outputStream.write("  select operatorstype, templatetype, count(0) as sms_num from t_sms_record_201706"+countStr+" where state in(1,2,3) GROUP BY operatorstype, templatetype\n");
			}
			outputStream.write(") e GROUP BY e.operatorstype, e.templatetype");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {

		}
	}

	public static void statSCXT(){
		OutputStreamWriter outputStream = null;
		try {
			outputStream = new FileWriter("D://stat.sql");
			outputStream.write("select\n");
			outputStream.write("    CASE e.operatorstype\n");
			outputStream.write("        WHEN 1 THEN '移动'\n");
			outputStream.write("        WHEN 2 THEN '联通'\n");
			outputStream.write("        WHEN 3 THEN '电信'\n");
			outputStream.write("    END AS \"运行商\",\n");
			outputStream.write("		CASE e.smstype\n");
			outputStream.write("        WHEN 0 THEN '通知短信'\n");
			outputStream.write("        WHEN 4 THEN '验证码短信'\n");
			outputStream.write("        WHEN 5 THEN '营销短信'\n");
			outputStream.write("    END AS \"短信类型\",\n");
			outputStream.write("		SUM(e.sms_num) as \"数量\"\n");
			outputStream.write("from (\n");

			int count = 0;
			String countStr = null;
			for (int i = 1; i < 28; i++) {
				if (i!=1){
					outputStream.write("  union all\n");
				}
				count=i;
				if (i<10)
				{
					countStr ="0"+i;
				} else{
					countStr = i+"";
				}
				outputStream.write("\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_0_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");
				outputStream.write("  union all\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_1_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");
				outputStream.write("  union all\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_2_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");
				outputStream.write("  union all\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_3_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");
				outputStream.write("  union all\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_4_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");
				outputStream.write("  union all\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_5_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");
				outputStream.write("  union all\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_6_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");
				outputStream.write("  union all\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_7_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");
				outputStream.write("  union all\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_8_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");
				outputStream.write("  union all\n");
				outputStream.write("  select operatorstype, smstype, count(0) as sms_num from t_sms_access_9_201706"+countStr+" where state in(1,3) and smstype not in(6,7,8) and clientid not in('a00166','a00131','a00118','a00116','a00115','a00114','a00113') GROUP BY operatorstype, smstype\n");

				System.out.println(i);

			}
			outputStream.write(") e GROUP BY e.operatorstype, e.smstype");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// // 条形码
		// String imgPath = "target\\zxing_EAN13.png";
		// String contents = "6923450657713";
		// int width = 105, height = 50;
		//
		// ZxingHandler.encode(contents, width, height, imgPath);
		// System.out.println("finished zxing EAN-13 encode.");
		//
		// String decodeContent = ZxingHandler.decode(imgPath);
		// System.out.println("解码内容如下：" + decodeContent);
		// System.out.println("finished zxing EAN-13 decode.");
		//
		// 条形码
//		String imgPath3 = "target\\A110B120.png";
//		String contents3 = "A110B120";
//		int width3 = 105, height3 = 50;
//
//		ZxingHandler.encode_CODE128(contents3, width3, height3, imgPath3);
//		System.out.println("finished zxing CODE_128 encode.");
//
//		String decodeContent3 = ZxingHandler.decode(imgPath3);
//		System.out.println("解码内容如下：" + decodeContent3);
//		System.out.println("finished zxing CODE_128 decode.");
//
//		// // 二维码
//		String imgPath2 = "target\\zxing.png";
//		String contents2 = "A334B334";
//		int width2 = 80, height2 = 80;
//		ZxingHandler.encode2(contents2, width2, height2, imgPath2);
//		System.out.println("finished zxing encode.");
		//
		// String decodeContent2 = ZxingHandler.decode2(imgPath2);
		// System.out.println("解码内容如下：" + decodeContent2);
		// System.out.println("finished zxing decode.");
	}

}