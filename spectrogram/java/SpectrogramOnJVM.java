

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;


public class SpectrogramOnJVM {

	public static void main(String[] args) throws Exception {

		int fftlen = 512;
		int hopSize = 160;
		int height = 128;
		
		String wavDir = "E:/DeepLearning/data/china-celebrity-speech/0406/png/wav/";
		String spectrogramDir = "E:/DeepLearning/data/china-celebrity-speech/0406/png/java_png";
		produceSpectrogramForEachWav(wavDir, spectrogramDir, fftlen, hopSize,height);

	}

	/**
	 * 为每个wav文件生成一张语谱图，并保存至spectrogramDir目录。
	 * 
	 * @param wavDir
	 *            类别所在的父目录，每个类别下面有多个wav文件。
	 * @param spectrogramDir
	 *            语谱图的存放目录
	 * @param fftlen
	 *            FFT的点数
	 * @param hopsize
	 *            帧移
	 * @param height
	 *            语谱图的高度
	 * @throws Exception
	 */
	public static void produceSpectrogramForEachWav(String wavDir,
			String spectrogramDir, int fftlen, int hopsize, int height)
			throws Exception {
		File file = new File(wavDir);
		if (file.exists()) {
			File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				for (File file2 : files) {
					if (file2.isDirectory()) {

						System.out.println(file2.getAbsolutePath());
						// 目录名，或者类别名
						String labelName = file2.getName();
						// 新的路径名
						String newSpectrogramDir = spectrogramDir
								+ File.separator + labelName;
						// 创建新的路径
						File dir = new File(newSpectrogramDir);
						if (!dir.exists()) {
							dir.mkdirs();
							System.out.println("created newSpectrogramDir:"
									+ newSpectrogramDir);
						}

						produceSpectrogramForEachWav(wavDir + File.separator
								+ labelName, newSpectrogramDir, fftlen,
								hopsize, height);
					}
					if (file2.isFile()) {
						String wavFilePath = file2.getAbsolutePath();
						if (wavFilePath.endsWith(".wav")) {
							// xyz.wav
							String filenameWithSuffix = file2.getName();
							// xyz
							String justfilename = filenameWithSuffix.substring(
									0, filenameWithSuffix.lastIndexOf('.'));

							String imagePath = spectrogramDir + File.separator
									+ justfilename + ".png";

							System.out.println("wavFilePath:" + wavFilePath);
							System.out.println("imagePath:" + imagePath);

							produceSpectrogram(fftlen, hopsize, height,
									wavFilePath, imagePath);
						}
					}
				}
			}
		}
	}

	/**
	 * 给指定的一个wav文件生成一张语谱图。
	 * 
	 * @param fftlen
	 *            FFT的点数
	 * @param hopsize
	 *            帧移
	 * @param height
	 *            语谱图的高度
	 * @param wavFilePath
	 *            wav文件的路径
	 * @param imagePath
	 *            语谱图的保存路径
	 * @throws Exception
	 */
	private static void produceSpectrogram(int fftlen, int hopsize, int height,
			String wavFilePath, String imagePath) throws Exception {
		List<float[]> pixelList = SpectrogramUtil.getPixelList(
				new File(wavFilePath), fftlen, hopsize);
		pixelList = SpectrogramUtil.sliceHeight(pixelList, height);
		saveImage(pixelList, imagePath, false, true);
	}

	/**
	 * 保存语谱图。
	 * 
	 * http://blog.csdn.net/icamera0/article/details/50647465
	 * http://www.2cto.com/kf/201603/492898.html
	 * 
	 * @param pixelList
	 *            语谱图从左至右的每一列组成的list。每一列是一个float[]，存放了该列的像素值。
	 * @param imagePath
	 *            语谱图的保存路径。
	 * @param argb
	 *            每个像素点的值是否是argb值。
	 * @param rgb
	 *            每个像素点的值是否是rgb值。
	 * @throws IOException
	 */
	public static void saveImage(List<float[]> pixelList, String imagePath,
			boolean argb, boolean rgb) throws IOException {
		int rows = pixelList.get(0).length;
		int cols = pixelList.size();

		if (argb) {
			BufferedImage argbImage = new BufferedImage(cols, rows,
					BufferedImage.TYPE_INT_ARGB);

			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < cols; j++) {
					int d = (int) pixelList.get(j)[i]; // pixel[i][j];
					// （24-31 位表示 alpha，16-23 位表示红色，8-15 位表示绿色，0-7 位表示蓝色）。
					int argbval = getARGB(d, d, d, 255); // 或者new
															// java.awt.Color(d,
															// d,
															// d,255).getRGB();
					argbImage.setRGB(j, i, argbval);
				}
			}
			ImageIO.write(argbImage, "png", new File(imagePath));
			return;
		} else if (rgb) {

			BufferedImage rgbImage = new BufferedImage(cols, rows,
					BufferedImage.TYPE_INT_RGB);

			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < cols; j++) {
					int d = (int) pixelList.get(j)[i]; // pixel[i][j];
					// （24-31 位表示 alpha，16-23 位表示红色，8-15 位表示绿色，0-7 位表示蓝色）。
					int rgbval = getRGB(d, d, d);
					rgbImage.setRGB(j, i, rgbval);
				}
			}
			ImageIO.write(rgbImage, "png", new File(imagePath));
		}
	}

	/**
	 * 获取r,g,b,a分量对应的ARGB值。
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 * @return
	 */
	private static int getARGB(int r, int g, int b, int a) {
		int value = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8)
				| ((b & 0xFF) << 0);
		return value;
	}

	/**
	 * 获取r,g,b分量对应的RGB值。
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @return
	 */
	private static int getRGB(int r, int g, int b) {
		int value = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
		return value;
	}

}
