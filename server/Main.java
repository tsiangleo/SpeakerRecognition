

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main {
	public static final int port = 8889;
	private static final int NTHREADS = 2;
	private static final Executor executor = Executors.newFixedThreadPool(NTHREADS);
	
	
	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("server started, listening at port:"+port);
		
		String uploadPath = "c://";
		
		while (true) {
			TaskNew task = new TaskNew(serverSocket.accept(),uploadPath);
			System.out.println("come in a new connction...");
			executor.execute(task);
		}
	}
	
}


class TaskNew implements Runnable{
	private Socket socket;
	private String uploadPath;
	
	public TaskNew(Socket socket,String uploadPath) {
		this.socket = socket;
		this.uploadPath = uploadPath;
	}

	@Override
	public void run() {
		
		try {
			
			
			NetHandler netHandler =  new NetHandler(socket);
			netHandler.setFileUploadDir(uploadPath);
			
			//读取客户端请求
			SRRequest requestData = netHandler.readRequest();
			
			System.out.println("request info:"+requestData);
			
			//业务逻辑处理
			SRResponse response = doBusiness(requestData);
			
			//发送响应消息
			netHandler.sendResponse(response);
			
			netHandler.close();
			socket.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * 业务逻辑
	 */
	private SRResponse  doBusiness(SRRequest requestData) {
		
		SRResponse ret = new SRResponse();
		ret.setResponseId(requestData.getRequestId());
		ret.setResponseType(requestData.getRequestType());
		ret.setRetCode(SRResponse.RET_CODE_SUCCESS);
		
		if(requestData.getRequestType() == SRRequest.REQUEST_TYPE_CLEAR){
			String deviceIdString = requestData.getDeviceId();
			System.out.println("正在清除deviceId为："+deviceIdString+"的声纹信息。");
			try {
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return ret;
		}else if(requestData.getRequestType() == SRRequest.REQUEST_TYPE_PING){
			
			return ret;
		}else if(requestData.getRequestType() == SRRequest.REQUEST_TYPE_GETMODEL){
			String deviceIdString = requestData.getDeviceId();
			System.out.println("正在获取deviceId为："+deviceIdString+"的模型信息。");
			try {
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return ret;
		}else if(requestData.getRequestType() == SRRequest.REQUEST_TYPE_REGIST){
			String deviceIdString = requestData.getDeviceId();
			System.out.println("正在注册deviceId为："+deviceIdString+"的声纹信息，文件是:"+requestData.getAudioFile());
			try {
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return ret;
		}else if(requestData.getRequestType() == SRRequest.REQUEST_TYPE_VERIFY){
			String deviceIdString = requestData.getDeviceId();
			System.out.println("正在验证deviceId为："+deviceIdString+"的声纹信息，文件是:"+requestData.getAudioFile());
			try {
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return ret;
		}
		return ret;
	}
	
}
