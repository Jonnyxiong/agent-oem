/**
 * lpjLiu 2017-05-25
 */
package com.ucpaas.sms.util;

import com.ucpaas.sms.util.security.Cryptos;
import com.ucpaas.sms.util.security.Des3Utils;
import com.ucpaas.sms.util.security.Digests;

public class SecurityUtils {

	public static final int HASH_INTERATIONS = 1024;

	public static final int SALT_SIZE = 8;

	// 系统生成sid秘钥
	private static final String KEY = "8989621";
	private static final String SID_BASE_STRING = "123321";

	/**
	 * des3加密
	 *
	 * @param str
	 *            需要加密的字符串
	 * @return
	 */
	public static String encodeDes3(String str) {
		return Des3Utils.encodeDes3(str);
	}

	/**
	 * des3解密
	 *
	 * @param str
	 *            需要解密的字符串
	 * @return
	 */
	public static String decodeDes3(String str) {
		return Des3Utils.decodeDes3(str);
	}

	public static String generateSid() {
		return getSignature(SID_BASE_STRING + System.currentTimeMillis(), KEY);
	}

	public static String getSignature(String dataStr, String keyStr) {
		byte[] data = dataStr.getBytes();
		byte[] key = keyStr.getBytes();
		return Encodes.encodeHex(Digests.md5(Cryptos.hmacSha1(data, key)));
	}

	/**
	 * 生成安全的密码，生成随机的16位salt并经过1024次 sha-1 hash
	 */
	public static String encryptSHA(String plainPassword) {
		byte[] salt = Digests.generateSalt(SALT_SIZE);
		byte[] hashPassword = Digests.sha1(plainPassword.getBytes(), salt, HASH_INTERATIONS);
		return Encodes.encodeHex(salt) + Encodes.encodeHex(hashPassword);
	}

	public static String encryptMD5(String plainPassword) {
		return Encodes.encodeHex(Digests.md5(plainPassword));
	}

	/**
	 * 验证密码
	 *
	 * @param plainPassword
	 *            明文密码
	 * @param password
	 *            密文密码
	 * @return 验证成功返回true
	 */
	public static boolean validatePassword(String plainPassword, String password) {
		byte[] salt = Encodes.decodeHex(password.substring(0, 16));
		byte[] hashPassword = Digests.sha1(plainPassword.getBytes(), salt, HASH_INTERATIONS);
		return password.equals(Encodes.encodeHex(salt) + Encodes.encodeHex(hashPassword));
	}

	public static void main(String[] args) {

		System.out.println(generateSid());
		/*String str = Encodes.encodeHex(Digests.md5(Encodes.encodeHex(Digests.md5("111111"))));
		System.out.println(str);

		String pwd = "111111";
		String ePwd = "0f84a1174aa131de35909e440f0cd485f3951c2f992427fa4997050b";
		String ePwd1 = "694f2f473b56a66231ec0f73185435662ba3ed1cd1cc0ca450c0c496";
		System.out.println(encryptSHA(pwd));
		System.out.println(encryptSHA(pwd));

		System.out.println(validatePassword(pwd, ePwd));
		System.out.println(validatePassword(pwd, ePwd1));*/

		/*
		 * String email =
		 * "bGl1bGlwZW5nanVAdWNwYWFzLmNvbSZNVFE1TlRjNE16azJNVEU1T0E9PSY1NzllNzJkZjJjNGI0 ZGM5OTYzODM3MDk0NDk0ZTE0Mw=="
		 * ; String str = Encodes.decodeBase64String(email);
		 * System.out.println(str);
		 */

		/*
		 * String email =
		 * "bGl1bGlwZW5nanVAdWNwYWFzLmNvbSZNVFE1TlRjNE16azJNVEU1T0E9PSY1NzllNzJkZjJjNGI0ZGM5OTYzODM3MDk0NDk0ZTE0Mw==";
		 * System.out.println("老的"); String encrypt =
		 * EncryptUtils.decodeBase64(email); System.out.println("Data：" +
		 * encrypt); String[] data = encrypt.split("&"); String oldTime =
		 * EncryptUtils.decodeBase64(data[1]); System.out.println("Data1：" +
		 * data[1]); System.out.println("Data2：" + data[2]);
		 * System.out.println("重新加密："+EncryptUtils.encodeMd5(data[0] +
		 * oldTime));
		 *
		 * System.out.println("新的"); encrypt =
		 * Encodes.decodeBase64String(email); System.out.println("Data：" +
		 * encrypt); String[] data1 = encrypt.split("&"); oldTime =
		 * Encodes.decodeBase64String(data1[1]); System.out.println("Data1：" +
		 * data1[1]); System.out.println("Data2：" + data1[2]);
		 *
		 * System.out.println("重新加密："+ Encodes.encodeHex(Digests.md5(data1[0] +
		 * oldTime)));
		 */
	}
}
