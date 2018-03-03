package com.ucpaas.sms.util.file;

import com.ucpaas.sms.util.Encodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * 文件工具类
 *
 * @author xiejiaan
 */
public class FileUtils {
	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

	/**
	 * 查看文件
	 *
	 * @param path
	 *            文件路径
	 */
	public static void view(String path, OutputStream output) {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(path);
			out = new BufferedOutputStream(output);
			byte[] buffer = new byte[16 * 1024];
			int len = 0;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			out.flush();

		} catch (FileNotFoundException e) {
			logger.debug("查看文件【文件不存在】：path=" + path);
		} catch (IOException e) {
			logger.error("查看文件【失败】：path=" + path, e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				logger.error("关闭文件【失败】：path=" + path, e);
			}
		}
	}

	/**
	 * 下载文件
	 *
	 * @param path
	 *            文件路径
	 */
	public static void download(String path, HttpServletResponse response) {
		String fileName = path.substring(path.lastIndexOf("/") + 1);
		download(fileName, path, response);
	}
	/**
	 * 下载文件
	 *
	 * @param path
	 *            文件路径
	 */
	public static void download(String fileName,String path, HttpServletResponse response) {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(path); // 文件流
			// 设置response的Header
//			response.reset();
//			response.setCharacterEncoding("UTF-8");
//			response.setHeader("Content-Disposition",
//					"attachment;filename=" + new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));
//			response.setContentType(FileContentTypes.getContentType(fileName));

			response.reset();
			response.setContentType("application/octet-stream; charset=utf-8");
			response.setHeader("Content-Disposition", "attachment; filename=" + Encodes.urlEncode(fileName));

			out = new BufferedOutputStream(response.getOutputStream());
			byte[] buffer = new byte[16 * 1024];
			int len = 0;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			out.flush();
			logger.debug("下载文件【成功】：path=" + path);

		} catch (FileNotFoundException e) {
			logger.debug("下载文件【文件不存在】：path=" + path);
		} catch (Throwable e) {
			logger.error("下载文件【失败】：path=" + path, e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				logger.error("关闭文件【失败】：path=" + path, e);
			}
		}
	}

	public static void upload(String path, String fileName, File uploadFile) {
		OutputStream out = null;
		InputStream in = null;
		File saveFile = new File(path, fileName);
		String saveAbsPath = path + "\\" + fileName;

		try {
			out = new FileOutputStream(saveFile);
			in = new FileInputStream(uploadFile);

			byte[] buffer = new byte[1024];
			int len = 0;

			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}

			logger.debug("Excel上传【成功】：path=" + saveAbsPath);

		} catch (IOException e) {
			logger.error("Excel上传【失败】：path=" + saveAbsPath, e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				logger.error("关闭文件【失败】：path=" + saveAbsPath, e);
			}

		}
	}
	public static void upload2(String path, String fileName, CommonsMultipartFile file){
		if (!file.isEmpty()) {
			// 取文件格式后缀名
			String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().indexOf("."));
			fileName = fileName + "." + fileType;

			File destFile = new File(path);

			try {
				// 复制临时文件到指定目录下
				org.apache.commons.io.FileUtils.copyInputStreamToFile(file.getInputStream(), destFile);
			} catch (IOException e) {
				logger.error("【上次文件时异常】", e);
			}

		} else {
			logger.error("【导入智能模板失败】Excel内容为空");
		}
	}
	/**
	 * 删除文件
	 *
	 * @param path
	 *            文件路径
	 */
	public static void delete(String path) {
		new File(path).delete();
	}
	public static boolean delete2(String path) {
		boolean flag = false;
		File file = new File(path);
		// 判断目录或文件是否存在
		if (!file.exists()) {  // 不存在返回 false
			return flag;
		} else {
			// 判断是否为文件
			if (file.isFile()) {  // 为文件时调用删除文件方法
				return deleteFile(path);
			} else {  // 为目录时调用删除目录方法
				return deleteDirectory(path);
			}
		}
	}
	/**
	 * 删除单个文件
	 * @param   sPath    被删除文件的文件名
	 * @return 单个文件删除成功返回true，否则返回false
	 */
	public static boolean deleteFile(String sPath) {
		boolean flag = false;
		flag = false;
		File file = new File(sPath);
		// 路径为文件且不为空则进行删除
		if (file.isFile() && file.exists()) {
			file.delete();
			flag = true;
		}
		return flag;
	}
	/**
	 * 删除目录（文件夹）以及目录下的文件
	 * @param   sPath 被删除目录的文件路径
	 * @return  目录删除成功返回true，否则返回false
	 */
	public static boolean deleteDirectory(String sPath) {
		boolean flag = false;
		//如果sPath不以文件分隔符结尾，自动添加文件分隔符
		if (!sPath.endsWith(File.separator)) {
			sPath = sPath + File.separator;
		}
		File dirFile = new File(sPath);
		//如果dir对应的文件不存在，或者不是一个目录，则退出
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return false;
		}
		flag = true;
		//删除文件夹下的所有文件(包括子目录)
		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			//删除子文件
			if (files[i].isFile()) {
				flag = deleteFile(files[i].getAbsolutePath());
				if (!flag) break;
			} //删除子目录
			else {
				flag = deleteDirectory(files[i].getAbsolutePath());
				if (!flag) break;
			}
		}
		if (!flag) return false;
		//删除当前目录
		if (dirFile.delete()) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * 创建文件夹目录
	 *
	 * @param path
	 */
	public static void makeDir(String path) {
		int last = path.lastIndexOf("/");
		if (last > 0) {
			File file = new File(path.substring(0, last));
			if (!file.isDirectory()) {
				file.mkdirs();
			}
		}
	}

	public static String getSysPath() {
		String path = Thread.currentThread().getContextClassLoader().getResource("").toString();
		String temp = path.replaceFirst("file:/", "").replaceFirst("WEB-INF/classes/", "");
		String separator = System.getProperty("file.separator");
		String resultPath = temp.replaceAll("/", separator + separator);
		return resultPath;
	}

	public static String getClassPath() {
		String path = Thread.currentThread().getContextClassLoader().getResource("").toString();
		String temp = path.replaceFirst("file:/", "");
		String separator = System.getProperty("file.separator");
		//String resultPath = temp.replaceAll("/", separator + separator);
		return separator+temp;
	}

}
