
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by tsiang on 2017/5/17.
 */

public class NetHandler {

	// model文件在客户端的存放目录
	public String modelFileDir = ".";
	// 语音文件的上传目录
	public String fileUploadDir = ".";

	public static final String CHARSET = "utf-8";
	private Socket socket;

	public NetHandler(Socket socket) {
		this.socket = socket;
	}

	public String getModelFileDir() {
		return modelFileDir;
	}

	public void setModelFileDir(String modelFileDir) {
		this.modelFileDir = modelFileDir;
	}

	public String getFileUploadDir() {
		return fileUploadDir;
	}

	public void setFileUploadDir(String fileUploadDir) {
		this.fileUploadDir = fileUploadDir;
	}

	/**
	 * 将SRClientRequest对象write到输出流。调用者应该调用out.close()关闭输出流。
	 * 
	 * @param request
	 * @throws IOException
	 */
	public void sendRequest(SRRequest request) throws IOException {
		OutputStream out = socket.getOutputStream();
		// 写入flag字段
		out.write(request.getMagic().getBytes(CHARSET));
		// 写入requestId字段

		out.write(request.getRequestId().getBytes(CHARSET));
		// 写入type字段
		out.write(request.getRequestType());

		if (request.getRequestType() == SRRequest.REQUEST_TYPE_PING) {
			out.flush();
			return;
		}

		// 写入deviceId信息
		out.write((byte) request.getDeviceId().getBytes(CHARSET).length);
		out.write(request.getDeviceId().getBytes(CHARSET));

		if (request.getRequestType() == SRRequest.REQUEST_TYPE_CLEAR
				|| request.getRequestType() == SRRequest.REQUEST_TYPE_GETMODEL) {
			out.flush();
			return;
		}

		if (request.getRequestType() == SRRequest.REQUEST_TYPE_REGIST
				|| request.getRequestType() == SRRequest.REQUEST_TYPE_VERIFY) {
			// 写入文件名信息
			out.write((byte) request.getAudioFile().getName().getBytes(CHARSET).length);
			out.write(request.getAudioFile().getName().getBytes(CHARSET));

			// 写入文件内容信息
			long audioLenth = request.getAudioFile().length();
			out.write(TypeTransferUtil.longToByteArray(audioLenth));
			FileInputStream fis = new FileInputStream(request.getAudioFile());
			byte[] buffer = new byte[4096];
			int len = 0;
			while ((len = fis.read(buffer, 0, buffer.length)) > 0) {
				out.write(buffer, 0, len);
			}
			fis.close();
			out.flush();
		} else {
			throw new RuntimeException("unsupported request type value:"
					+ request.getRequestType());
		}
	}

	/**
	 * 从输入流中读取SR请求信息。调用者应该调用in.close()关闭输入流。
	 * 
	 * @return
	 * @throws IOException
	 */
	public SRRequest readRequest() throws IOException {
		InputStream in = socket.getInputStream();

		SRRequest returnData = new SRRequest();
		DataInputStream dataInputStream = new DataInputStream(in);
		byte[] buf = new byte[20];
		// 读取magic字段
		dataInputStream.read(buf, 0, returnData.getMagic().length());
		String flag = new String(buf, 0, returnData.getMagic().length(),
				CHARSET);

		if (!returnData.getMagic().equals(flag)) {
			throw new RuntimeException("Not a SR request");
		}

		// 读取requestId字段
		byte[] ridbuf = new byte[36];
		dataInputStream.read(ridbuf, 0, 36);
		String reqId = new String(ridbuf, 0, 36, CHARSET);
		returnData.setRequestId(reqId);

		// 读取type字段
		byte type = dataInputStream.readByte();
		returnData.setRequestType(type);

		if (type == SRRequest.REQUEST_TYPE_PING) {
			return returnData;
		}

		// 读取deviceId信息
		byte deiviceIdLenth = dataInputStream.readByte();
		byte[] buff = new byte[deiviceIdLenth];
		dataInputStream.read(buff, 0, deiviceIdLenth);
		String deiviceId = new String(buff, 0, deiviceIdLenth, CHARSET);
		returnData.setDeviceId(deiviceId);

		if (type == SRRequest.REQUEST_TYPE_CLEAR
				|| type == SRRequest.REQUEST_TYPE_GETMODEL) {
			return returnData;
		}

		if (type == SRRequest.REQUEST_TYPE_REGIST
				|| type == SRRequest.REQUEST_TYPE_VERIFY) {

			// 读取文件名信息
			byte fileNameLenth = dataInputStream.readByte();
			byte[] buff2 = new byte[fileNameLenth];
			dataInputStream.read(buff2, 0, fileNameLenth);
			String fileName = new String(buff2, 0, fileNameLenth, CHARSET);

			// 读取文件内容信息
			long audioLenth = dataInputStream.readLong();
			/**
			 * 文件命名规则：timestamp_fileName
			 */
			File outFile = new File(fileUploadDir + "/"
					+ System.currentTimeMillis() + "_" + fileName);
			FileOutputStream outputStream = new FileOutputStream(outFile);
			byte[] buffer = new byte[4096];
			long left = audioLenth;
			int readed = 0;
			/**
			 * 陷阱:这里dataInputStream.read()容易阻塞，一旦读取的字节数不足buffer.length，且没有读到EOF。
			 *
			 */
			while (left > 0) {
				int len = buffer.length;
				while ((readed = dataInputStream.read(buffer, 0, len)) > 0) {
					outputStream.write(buffer, 0, readed);
					left -= readed;
					if (left < buffer.length) {
						len = (int) left;
					}
				}
			}

			outputStream.close();
			returnData.setAudioFile(outFile);
			return returnData;
		} else {
			throw new RuntimeException("unsupported request type value:" + type);
		}
	}

	/**
	 * 从输入流中读取SR响应信息。调用者应该调用in.close()关闭输入流。
	 * 
	 * @return
	 * @throws IOException
	 */
	public SRResponse readResponse() throws IOException {
		InputStream in = socket.getInputStream();
		SRResponse returnData = new SRResponse();
		DataInputStream dataInputStream = new DataInputStream(in);
		byte[] buf = new byte[20];
		// 读取flag字段
		dataInputStream.read(buf, 0, returnData.getMagic().length());
		String flag = new String(buf, 0, returnData.getMagic().length(),
				CHARSET);

		if (!returnData.getMagic().equals(flag)) {
			throw new RuntimeException("Not a SR response");
		}

		// 读取responseId字段
		byte[] ridbuf = new byte[36];
		dataInputStream.read(ridbuf, 0, 36);
		String reqId = new String(ridbuf, 0, 36, CHARSET);
		returnData.setResponseId(reqId);

		// 读取type字段
		byte type = dataInputStream.readByte();
		returnData.setResponseType(type);

		// 读取retcode字段
		byte retcode = dataInputStream.readByte();
		returnData.setRetCode(retcode);

		if (type == SRRequest.REQUEST_TYPE_PING
				|| type == SRRequest.REQUEST_TYPE_REGIST
				|| type == SRRequest.REQUEST_TYPE_VERIFY
				|| type == SRRequest.REQUEST_TYPE_CLEAR) {
			return returnData;
		}

		if (type == SRRequest.REQUEST_TYPE_GETMODEL) {

			// 读取文件名信息
			byte fileNameLenth = dataInputStream.readByte();
			byte[] buff2 = new byte[fileNameLenth];
			dataInputStream.read(buff2, 0, fileNameLenth);
			String fileName = new String(buff2, 0, fileNameLenth, CHARSET);

			// 读取文件内容信息
			long audioLenth = dataInputStream.readLong();
			File outFile = new File(modelFileDir + "/" + fileName);
			FileOutputStream outputStream = new FileOutputStream(outFile);
			byte[] buffer = new byte[4096];
			long left = audioLenth;
			int readed = 0;
			while (left > 0) {
				int len = buffer.length;
				while ((readed = dataInputStream.read(buffer, 0, len)) > 0) {
					outputStream.write(buffer, 0, readed);
					left -= readed;
					if (left < buffer.length) {
						len = (int) left;
					}
				}
			}
			outputStream.close();
			returnData.setModelFile(outFile);
			return returnData;
		} else {
			throw new RuntimeException("unsupported response type value:"
					+ type);
		}
	}

	/**
	 * 将SRServerResponse对象write到输出流。调用者应该调用out.close()关闭输出流。
	 * 
	 * @throws Exception
	 */
	public void sendResponse(SRResponse response) throws IOException {
		OutputStream out = socket.getOutputStream();
		// 写入magic字段
		out.write(response.getMagic().getBytes(CHARSET));
		// 写入responseId字段
		out.write(response.getResponseId().getBytes(CHARSET));
		// 写入type字段
		out.write(response.getResponseType());
		// 写入retCode字段
		out.write(response.getRetCode());

		if (response.getResponseType() == SRRequest.REQUEST_TYPE_PING
				|| response.getResponseType() == SRRequest.REQUEST_TYPE_REGIST
				|| response.getResponseType() == SRRequest.REQUEST_TYPE_VERIFY
				|| response.getResponseType() == SRRequest.REQUEST_TYPE_CLEAR) {
			out.flush();
			return;
		}

		if (response.getResponseType() == SRRequest.REQUEST_TYPE_GETMODEL) {
			// 写入文件名信息
			out.write((byte) response.getModelFile().getName()
					.getBytes(CHARSET).length);
			out.write(response.getModelFile().getName().getBytes(CHARSET));

			// 写入文件内容信息
			long audioLenth = response.getModelFile().length();
			out.write(TypeTransferUtil.longToByteArray(audioLenth));
			FileInputStream fis = new FileInputStream(response.getModelFile());
			byte[] buffer = new byte[4096];
			int len = 0;
			while ((len = fis.read(buffer, 0, buffer.length)) > 0) {
				out.write(buffer, 0, len);
			}
			fis.close();
			out.flush();
		} else {
			throw new RuntimeException("unsupported response type value:"
					+ response.getResponseType());
		}

	}

	public void close() throws IOException {
		socket.close();
	}

}
